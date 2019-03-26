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

  @Parameter(property = "language", defaultValue = "java")
  var language: String = _

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

  override def execute(): Unit = {
    if (!outputPath.exists()) {
      outputPath.mkdirs()
    }

    phase match {
      case Main => project.addCompileSourceRoot(outputPath.getAbsolutePath)
      case Test => project.addTestCompileSourceRoot(outputPath.getAbsolutePath)
    }


    try {
      val _kind: CodegenTarget = kind match {
        case "client" => CodegenTarget.Client
        case "server" => CodegenTarget.Server
        case "models" => CodegenTarget.Models
        case x => throw new MojoExecutionException(s"Unsupported codegen type: ${x}")
      }

      val arg = Args.empty.copy(
        kind=_kind,
        specPath=Some(specPath.getCanonicalPath()),
        packageName=Option(packageName).map(_.trim.split('.').toList),
        dtoPackage=Option(dtoPackage).toList.flatMap(_.split('.').filterNot(_.isEmpty).toList),
        context=Context.empty.copy(
          tracing=Option(tracing).getOrElse(Context.empty.tracing)
        ),
        imports=Option(customImports).fold[List[String]](List.empty)(_.asScala.toList.map(_.toString))
      )

      getLog.info(s"Generating ${_kind} from ${specPath.getName}")

      guardrailTask(List((language, arg)), outputPath)
    } catch {
      case NonFatal(e) =>
        getLog.error("Failed to generate client", e)
        throw new MojoFailureException(s"Failed to generate client from '${specPath.getAbsolutePath}': $e", e)
    }
  }

  type Language = String
  def guardrailTask(tasks: List[(Language, Args)], sourceDir: java.io.File): Seq[java.io.File] = {
    val preppedTasks: Map[String, NonEmptyList[Args]] = tasks.foldLeft(Map.empty[String, NonEmptyList[Args]]) { case (acc, (language, args)) =>
      val prepped = args.copy(outputPath=Some(sourceDir.getPath))
      acc.updated(language, acc.get(language).fold(NonEmptyList.one(prepped))(_ :+ prepped))
    }

    val (logger, paths) =
      CLI.guardrailRunner(_ => PartialFunction.empty)
        .apply(preppedTasks)
        .fold[List[java.nio.file.Path]]({
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
          case RuntimeFailure(message) =>
            println(s"${AnsiColor.RED}Error:${AnsiColor.RESET}${message}")
            throw new CodegenFailedException()
          case UserError(message) =>
            println(s"${AnsiColor.RED}Error:${AnsiColor.RESET}${message}")
            throw new CodegenFailedException()
        }, identity)
        .runEmpty
    paths.map(_.toFile).distinct
  }
}
