package com.twilio.guardrail.m2repo

import java.io.File
import java.util.Locale
import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.ArtifactHandler
import org.apache.maven.plugin.{AbstractMojo, MojoFailureException}
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

private[guardrail] sealed trait SpecFileType {
  def extension: String
}
case class Yaml(extension: String) extends SpecFileType { override def toString: String = "yaml" }
case class Json(extension: String) extends SpecFileType { override def toString: String = "json" }

private[guardrail] object SpecFileType {
  def apply(extension: String): Option[SpecFileType] = extension.toLowerCase(Locale.US) match {
    case "yaml" | "yml" => Some(Yaml(extension))
    case "json" => Some(Json(extension))
    case _ => None
  }
}

private[guardrail] class GuardrailArtifactHandler(specFileType: SpecFileType, directory: String, classifier: String) extends ArtifactHandler {
  override def getExtension: String = specFileType.extension
  override def getDirectory: String = directory
  override def getClassifier: String = classifier
  override def getPackaging: String = specFileType.toString
  override def isIncludesDependencies: Boolean = false
  override def getLanguage: String = specFileType.toString
  override def isAddedToClasspath: Boolean = false
}

abstract class AbstractGuardrailDeployMojo extends AbstractMojo {
  protected def groupId: String
  protected def artifactId: String
  protected def `type`: String
  protected def classifier: String

  @Parameter(name = "specPath", required = true)
  var specPath: File = _

  @Parameter(name = "guardrail.deploy.skip", defaultValue = "false")
  var skip: Boolean = _

  @Parameter(defaultValue = "${project}", required = true, readonly = false)
  var project: MavenProject = _

  override def execute(): Unit = {
    if (skip) {
      getLog.info("Skipping guardrail deploy run")
      return
    }

    getExtension(specPath.getName).flatMap(SpecFileType.apply).fold(
      throw new MojoFailureException("OpenAPI spec file extension is not recognized")
    )({ specFileType =>
      val handler = new GuardrailArtifactHandler(
        specFileType,
        "/" + groupId.replaceAllLiterally(".", "/") + artifactId,
        classifier
      )
      val artifact = new DefaultArtifact(groupId, artifactId, project.getVersion, Constants.SCOPE, `type`, classifier, handler)
      artifact.setFile(specPath)
      artifact.setRelease(!project.getVersion.endsWith("-SNAPSHOT"))
      project.addAttachedArtifact(artifact)
    })
  }

  private def getExtension(name: String): Option[String] =
    Option(name.lastIndexOf('.')).filter(_ >= 0).map(dot => name.substring(dot + 1)).filter(_.nonEmpty)
}
