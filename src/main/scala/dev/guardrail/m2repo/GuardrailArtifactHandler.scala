package dev.guardrail.m2repo

import cats.data.NonEmptyList
import java.util.Locale
import org.apache.maven.artifact.handler.ArtifactHandler

sealed trait SpecFileTypeMeta {
  def extensions: NonEmptyList[String]
}

sealed trait SpecFileType {
  def extension: String
  def canonical: String
}

object Yaml extends SpecFileTypeMeta {
  override def extensions: NonEmptyList[String] = NonEmptyList.of("yaml", "yml")
}
object Json extends SpecFileTypeMeta {
  override def extensions: NonEmptyList[String] = NonEmptyList.of("json")
}

case class Yaml(extension: String) extends SpecFileType { override def canonical: String = Yaml.extensions.head }
case class Json(extension: String) extends SpecFileType { override def canonical: String = Json.extensions.head }

object SpecFileType {
  def apply(extension: String): Option[SpecFileType] = extension.toLowerCase(Locale.US) match {
    case ext if Yaml.extensions.exists(_ == ext) => Some(Yaml(extension))
    case ext if Json.extensions.exists(_ == ext) => Some(Json(extension))
    case _ => None
  }

  val allExtensions: NonEmptyList[String] = Yaml.extensions ::: Json.extensions
}

class GuardrailArtifactHandler(specFileType: SpecFileType, directory: String, classifier: String) extends ArtifactHandler {
  override def getExtension: String = specFileType.extension
  override def getDirectory: String = directory
  override def getClassifier: String = classifier
  override def getPackaging: String = specFileType.canonical
  override def isIncludesDependencies: Boolean = false
  override def getLanguage: String = specFileType.canonical
  override def isAddedToClasspath: Boolean = false
}
