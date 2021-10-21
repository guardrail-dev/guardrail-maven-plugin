package dev.guardrail.m2repo

import org.apache.maven.shared.transfer.dependencies.DependableCoordinate

class GuardrailCoordinate extends DependableCoordinate {
  private var groupId: String = _
  private var artifactId: String = _
  private var version: String = _
  private var `type`: String = Constants.DEFAULT_TYPE
  private var classifier: String = Constants.DEFAULT_CLASSIFIER
  private var extension: String = _

  override def getGroupId: String = groupId
  override def getArtifactId: String = artifactId
  override def getVersion: String = version
  override def getType: String = `type`
  override def getClassifier: String = classifier
  def getExtension: String = extension

  def isMaybeValid: Boolean = groupId != null || artifactId != null || version != null || extension != null

  override def toString: String = s"$groupId:$artifactId:${`type`}${Option(classifier).fold("")(":" + _)}:$version"
}
