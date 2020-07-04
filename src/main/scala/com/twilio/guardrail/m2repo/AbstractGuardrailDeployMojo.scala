package com.twilio.guardrail.m2repo

import java.io.File
import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.plugin.{AbstractMojo, MojoFailureException}
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.artifact.versioning.VersionRange

abstract class AbstractGuardrailDeployMojo extends AbstractMojo {
  protected def groupId: String
  protected def artifactId: String
  protected def `type`: String
  protected def classifier: String

  @Parameter(property = "specPath", required = true)
  var specPath: File = _

  @Parameter(property = "guardrail.deploy.skip", defaultValue = "false")
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
      val artifact = new DefaultArtifact(groupId, artifactId, VersionRange.createFromVersionSpec(project.getVersion), Constants.SCOPE, `type`, classifier, handler)
      artifact.setFile(specPath)
      artifact.setRelease(!project.getVersion.endsWith("-SNAPSHOT"))
      project.addAttachedArtifact(artifact)
    })
  }

  private def getExtension(name: String): Option[String] =
    Option(name.lastIndexOf('.')).filter(_ >= 0).map(dot => name.substring(dot + 1)).filter(_.nonEmpty)
}
