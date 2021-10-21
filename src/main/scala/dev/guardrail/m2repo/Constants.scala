package dev.guardrail.m2repo

import org.apache.maven.artifact.Artifact

object Constants {
  final val DEFAULT_TYPE = "openapi-spec"
  final val DEFAULT_CLASSIFIER = "openapi-spec"

  // SCOPE_PROVIDED is probably a more correct fit here, but maven won't
  // actually install/deploy the artifact when the scope is 'provided'.
  final val SCOPE = Artifact.SCOPE_COMPILE
}
