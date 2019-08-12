package com.twilio.guardrail.m2repo

import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}

// using PACKAGE here to ensure that the artifact gets added *before*
// both the install and deploy stages.
@Mojo(name = "deploy-openapi-spec", defaultPhase = LifecyclePhase.PACKAGE)
class GuardrailDeployMojo extends AbstractGuardrailDeployMojo {
  @Parameter(name = "groupId", defaultValue = "${project.groupId}")
  var groupId: String = _

  @Parameter(name = "artifactId", defaultValue = "${project.artifactId}")
  var artifactId: String = _

  @Parameter(name = "type", defaultValue = Constants.DEFAULT_TYPE)
  var `type`: String = _

  @Parameter(name = "classifier", defaultValue = Constants.DEFAULT_CLASSIFIER)
  var classifier: String = _
}
