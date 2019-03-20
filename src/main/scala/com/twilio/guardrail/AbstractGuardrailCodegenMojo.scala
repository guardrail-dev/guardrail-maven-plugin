package com.twilio.guardrail

import cats.data.{EitherT, NonEmptyList, WriterT}
import cats.free.Free
import cats.implicits._
import cats.~>
import com.twilio.guardrail._
import com.twilio.guardrail.core.CoreTermInterp
import com.twilio.guardrail.languages.{ ScalaLanguage, LA }
import com.twilio.guardrail.terms.CoreTerms
import com.twilio.guardrail.terms.{CoreTerm, CoreTerms, GetDefaultFramework}
import java.io.File
import org.apache.maven.plugin.{AbstractMojo, MojoExecutionException, MojoFailureException}
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import scala.collection.JavaConverters._
import scala.io.AnsiColor
import scala.language.higherKinds
import scala.meta._
import scala.util.control.NonFatal

class CodegenFailedException extends Exception

sealed abstract class Phase(val root: String)
object Main extends Phase("main")
object Test extends Phase("test")

abstract class AbstractGuardrailCodegenMojo(phase: Phase) extends AbstractMojo {
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/guardrail-sources", property = "outputPath", required = true)
  def outputPath: File

  @Parameter(property = "kind", defaultValue = "client")
  var kind: String = _

  @Parameter(property = "specPath", required = true)
  var specPath: File = _

  @Parameter(property = "packageName")
  var packageName: String = _

  @Parameter(property = "dtoPackage")
  var dtoPackage: String = _

  @Parameter(property = "tracing", defaultValue = "false")
  var tracing: Boolean = _

  @Parameter(property = "framework", defaultValue = "akka-http")
  var framework: String = _

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  var project: MavenProject = _

  @Parameter(required = false, readonly = false)
  var customImports: java.util.List[_] = _

  private[this] def runM[L <: LA, F[_]](args: List[Args])(implicit C: CoreTerms[L, F]): Free[F, NonEmptyList[ReadSwagger[Target[List[WriteTree]]]]] = {
    import C._

    for {
      args <- validateArgs(args)
      writeTrees <- Common.processArgs(args)
    } yield writeTrees
  }

  override def execute(): Unit = {
    if (!outputPath.exists()) {
      outputPath.mkdirs()
    }

    phase match {
      case Main => project.addCompileSourceRoot(outputPath.getAbsolutePath)
      case Test => project.addTestCompileSourceRoot(outputPath.getAbsolutePath)
    }

    if (!specPath.exists()) {
      throw new MojoExecutionException(s"Swagger spec file at '${specPath.getAbsolutePath}' does not exist")
    }

    val packageNames = Option(packageName).map(_.trim.split('.').toList)
    val dtoPackages = Option(dtoPackage).fold(List.empty[String])(_.trim.split('.').toList)
    val context = Context(Option(framework), tracing)
    val _kind: CodegenTarget = kind match {
      case "client" => CodegenTarget.Client
      case "server" => CodegenTarget.Server
      case "models" => CodegenTarget.Models
      case x => throw new MojoExecutionException(s"Unsupported codegen type: ${x}")
    }

    getLog.info(s"Generating ${_kind} from ${specPath.getName}")

    try {
      val processedCustomImports: List[String] = Option(customImports).fold[List[String]](List.empty)(_.asScala.toList.map(_.toString))

      val arg = Args.empty.copy(
        kind=_kind
      , specPath=Some(specPath.getCanonicalPath())
      , outputPath=Some(outputPath.getCanonicalPath())
      , packageName=packageNames
      , dtoPackage=dtoPackages
      , context=context
      , imports=processedCustomImports
      )

    val preppedTasks = List(arg)

    val result = runM[ScalaLanguage, CoreTerm[ScalaLanguage, ?]](preppedTasks)
      .foldMap(CoreTermInterp[ScalaLanguage](
        "akka-http", {
          case "akka-http" => com.twilio.guardrail.generators.AkkaHttp
          case "http4s"    => com.twilio.guardrail.generators.Http4s
        }, {
          _.parse[Importer].toEither.bimap(err => UnparseableArgument("import", err.toString), importer => Import(List(importer)))
        }
      )).fold[List[ReadSwagger[Target[List[WriteTree]]]]]({
          case MissingArg(args, Error.ArgName(arg)) =>
            println(s"${AnsiColor.RED}Missing argument:${AnsiColor.RESET} ${AnsiColor.BOLD}${arg}${AnsiColor.RESET} (In block ${args})")
            throw new CodegenFailedException()
          case NoArgsSpecified =>
            List.empty
          case NoFramework =>
            println(s"${AnsiColor.RED}No framework specified${AnsiColor.RESET}")
            throw new CodegenFailedException()
          case PrintHelp =>
            List.empty
          case UnknownArguments(args) =>
            println(s"${AnsiColor.RED}Unknown arguments: ${args.mkString(" ")}${AnsiColor.RESET}")
            throw new CodegenFailedException()
          case UnparseableArgument(name, message) =>
            println(s"${AnsiColor.RED}Unparseable argument ${name}: ${message}${AnsiColor.RESET}")
            throw new CodegenFailedException()
          case UnknownFramework(name) =>
            println(s"${AnsiColor.RED}Unknown framework specified: ${name}${AnsiColor.RESET}")
            throw new CodegenFailedException()
        }, _.toList)

    val (coreLogger, deferred) = result.runEmpty

    val (logger, paths) = deferred
      .traverse({ rs =>
        ReadSwagger
          .readSwagger(rs)
          .fold(
            { err =>
              println(s"${AnsiColor.RED}${err}${AnsiColor.RESET}")
              throw new CodegenFailedException()
            },
            _.fold(
              {
                case err =>
                  println(s"${AnsiColor.RED}Error: ${err}${AnsiColor.RESET}")
                  throw new CodegenFailedException()
              },
              _.map(WriteTree.unsafeWriteTree)
            )
          )
      })
      .map(_.flatten)
      .runEmpty
    } catch {
      case NonFatal(e) =>
        getLog.error("Failed to generate client", e)
        throw new MojoFailureException(s"Failed to generate client from '${specPath.getAbsolutePath}': $e", e)
    }
  }
}
