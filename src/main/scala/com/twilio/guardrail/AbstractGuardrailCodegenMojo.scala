package com.twilio.guardrail

import cats.data.NonEmptyList
import cats.implicits._
import java.io.File
import java.util
import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException, MojoFailureException}
import org.apache.maven.plugins.annotations.{Component, Parameter}
import org.apache.maven.project.{DefaultProjectBuildingRequest, MavenProject}
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate
import org.apache.maven.shared.transfer.artifact.resolve.{ArtifactResolver, ArtifactResolverException}
import scala.collection.JavaConverters._
import scala.io.AnsiColor
import scala.language.higherKinds
import scala.util.control.NonFatal

import com.twilio.guardrail.m2repo.{Constants, GuardrailCoordinate, SpecFileType}
import dev.guardrail._
import dev.guardrail.cli.CLICommon
import dev.guardrail.core.StructuredLogger._
import dev.guardrail.core.{LogLevel, LogLevels}

class CodegenFailedException extends Exception

sealed abstract class Phase(val root: String)
object Main extends Phase("main")
object Test extends Phase("test")

abstract class AbstractGuardrailCodegenMojo(phase: Phase) extends AbstractMojo {
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/guardrail-sources", property = "outputPath", required = true)
  def outputPath: File

  @Parameter(property = "language")
  var language: String = _

  @Parameter(property = "kind", defaultValue = "client")
  var kind: String = _

  @Parameter(property = "specPath")
  var specPath: File = _

  @Parameter(property = "specArtifact")
  var specArtifact: GuardrailCoordinate = _

  @Parameter(property = "packageName")
  var packageName: String = _

  @Parameter(property = "dtoPackage")
  var dtoPackage: String = _

  @Parameter(property = "framework", defaultValue = "akka-http")
  var framework: String = _

  @Parameter(property = "tracing", defaultValue = "false")
  var tracing: Boolean = _

  @Parameter(required = false, readonly = false)
  var modules: java.util.List[_] = _

  @Parameter(property = "guardrail.codegen.skip", defaultValue = "false")
  var skip: Boolean = _

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  var project: MavenProject = _

  @Parameter(defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true)
  var remoteRepositories: util.List[ArtifactRepository] = _

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  var session: MavenSession = _

  @Component
  var artifactResolver: ArtifactResolver = _

  @Parameter(required = false, readonly = false)
  var customImports: java.util.List[_] = _

  @Parameter(property = "customExtraction")
  var customExtraction: Boolean = _

  protected def cli: CLICommon

  override def execute(): Unit = {
    if (!outputPath.exists()) {
      outputPath.mkdirs()
    }

    phase match {
      case Main => project.addCompileSourceRoot(outputPath.getAbsolutePath)
      case Test => project.addTestCompileSourceRoot(outputPath.getAbsolutePath)
    }

    if (skip) {
      getLog.info("Skipping guardrail codegen run")
      return
    }

    // The extra gymnastics is because maven will always initialize `specArtifact` to something,
    // even if it's an invalid instance of the object.
    val (_specPath, specDesc) = (Option(specPath), Option(specArtifact)) match {
      case (Some(_), Some(sa)) if sa.isMaybeValid =>
        throw new MojoExecutionException("You must specify ONLY one of 'specPath' and 'specArtifact'")
      case (None, None) =>
        throw new MojoExecutionException("You must specify either 'specPath' or 'specArtifact'")
      case (None, Some(sa)) if !sa.isMaybeValid =>
        throw new MojoExecutionException("You must specify either 'specPath' or 'specArtifact'")
      case (Some(path), _) =>
        (path, path.getAbsolutePath)
      case (None, Some(sa)) =>
        val extensions = Option(sa.getExtension).fold(SpecFileType.allExtensions)(NonEmptyList.of(_))
        val artifact = resolveArtifact(sa, extensions)
        (artifact.getFile, artifact.toString)
    }

    try {
      val _language: String = Option(language).getOrElse({
        getLog.warn(s"[guardrail-maven-plugin] Default behaviour changing: Please specify <language>scala</language> to maintain the current settings. The default language will change to 'java' in a future release.")
        "scala"
      })
      val _kind: CodegenTarget = kind match {
        case "client" => CodegenTarget.Client
        case "server" => CodegenTarget.Server
        case "models" => CodegenTarget.Models
        case x => throw new MojoExecutionException(s"Unsupported codegen type: ${x}")
      }

      val arg = Args.empty.copy(
        kind=_kind,
        specPath=Some(_specPath.getCanonicalPath),
        packageName=Option(packageName).map(_.trim.split('.').toList),
        dtoPackage=Option(dtoPackage).toList.flatMap(_.split('.').filterNot(_.isEmpty).toList),
        context=Context.empty.copy(
          customExtraction=Option(customExtraction).getOrElse(Context.empty.customExtraction),
          framework=Option(framework),
          tracing=Option(tracing).getOrElse(Context.empty.tracing),
          modules=Option(modules).fold(Context.empty.modules)(_.asScala.toList.map(_.toString))
        ),
        imports=Option(customImports).fold[List[String]](List.empty)(_.asScala.toList.map(_.toString))
      )

      val logLevel = Option(System.getProperty("guardrail.loglevel")).flatMap(LogLevels.apply).getOrElse(LogLevels.Warning)

      getLog.info(s"Generating ${_kind} from ${specDesc}")

      guardrailTask(List((_language, arg)), outputPath)(logLevel)
    } catch {
      case NonFatal(e) =>
        getLog.error("Failed to generate client", e)
        throw new MojoFailureException(s"Failed to generate client from '${specDesc}': $e", e)
    }
  }

  type Language = String
  def guardrailTask(tasks: List[(Language, Args)], sourceDir: java.io.File)(implicit logLevel: LogLevel): Seq[java.io.File] = {
    val preppedTasks: Map[String, NonEmptyList[Args]] = tasks.foldLeft(Map.empty[String, NonEmptyList[Args]]) { case (acc, (language, args)) =>
      val prepped = args.copy(outputPath=Some(sourceDir.getPath))
      acc.updated(language, acc.get(language).fold(NonEmptyList.one(prepped))(_ :+ prepped))
    }

    val /*(logger,*/ paths/*)*/ =
      cli.guardrailRunner
        .apply(preppedTasks)
        .fold[List[java.nio.file.Path]]({
          case MissingArg(args, Error.ArgName(arg)) =>
            getLog.error(s"Missing argument: ${AnsiColor.BOLD}${arg}${AnsiColor.RESET} (In block ${args})")
            throw new CodegenFailedException()
          case MissingDependency(name) =>
            getLog.error(s"""${AnsiColor.RED}Missing dependency:${AnsiColor.RESET}
            |${AnsiColor.BOLD}<dependency>
            |  <groupId>dev.guardrail</groupId>
            |  <artifactId>${name}_2.12</artifactId>
            |  <version>Check latest version!</version>
            |</dependency>${AnsiColor.RESET}
            |""".stripMargin)
            throw new CodegenFailedException()
          case NoArgsSpecified =>
            List.empty
          case NoFramework =>
            getLog.error("No framework specified")
            throw new CodegenFailedException()
          case PrintHelp =>
            List.empty
          case UnknownArguments(args) =>
            getLog.error(s"Unknown arguments: ${args.mkString(" ")}")
            throw new CodegenFailedException()
          case UnparseableArgument(name, message) =>
            getLog.error(s"Unparseable argument ${name}: ${message}")
            throw new CodegenFailedException()
          case UnknownFramework(name) =>
            getLog.error(s"Unknown framework specified: ${name}")
            throw new CodegenFailedException()
          case RuntimeFailure(message) =>
            getLog.error(s"Error: ${message}")
            throw new CodegenFailedException()
          case UserError(message) =>
            getLog.error(s"Error: ${message}")
            throw new CodegenFailedException()
          case MissingModule(section, choices) =>
            getLog.error(s"Error: Missing module ${section}. Options are: ${choices.mkString(",")}")
            throw new CodegenFailedException()
          case ModuleConflict(section) =>
            getLog.error(s"Error: Too many modules specified for ${section}")
            throw new CodegenFailedException()
          case UnconsumedModules(modules) =>
            getLog.error(s"Error: Unconsumed modules: ${modules.mkString(", ")}")
            throw new CodegenFailedException()
        }, identity)
        //.runEmpty

    //print(logger.show)

    paths.map(_.toFile).distinct
  }

  def resolveArtifact(artifact: GuardrailCoordinate, extensions: NonEmptyList[String]): Artifact = {
    val buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest)
    buildingRequest.setRemoteRepositories(this.remoteRepositories)

    val coordinate = new DefaultArtifactCoordinate
    coordinate.setGroupId(Option(artifact.getGroupId).getOrElse(project.getGroupId))
    coordinate.setArtifactId(Option(artifact.getArtifactId).getOrElse(throw new MojoExecutionException("Missing artifactId for OpenAPI spec")))
    coordinate.setVersion(Option(artifact.getVersion).getOrElse(throw new MojoExecutionException("Missing version for OpenAPI spec")))
    coordinate.setExtension(extensions.head)
    coordinate.setClassifier(Option(artifact.getClassifier).getOrElse(Constants.DEFAULT_CLASSIFIER))

    try {
      val result = this.artifactResolver.resolveArtifact(buildingRequest, coordinate)
      Option(result.getArtifact).getOrElse(throw new ArtifactResolverException("Resolver returned null artifact", new NullPointerException))
    } catch {
      case e: ArtifactResolverException =>
        NonEmptyList.fromList(extensions.tail)
          .fold(throw new MojoExecutionException(e.getMessage, e))(resolveArtifact(artifact, _))
    }
  }
}
