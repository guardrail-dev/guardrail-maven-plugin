package com.twilio.guardrail

import java.io.File
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}

import dev.guardrail.cli.{CLI, CLICommon}

@Mojo(name = "generate-test-sources", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
class GuardrailTestCodegenMojo extends AbstractGuardrailCodegenMojo(Test) {
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/swagger-test-clients", property = "outputPath", required = true)
  var outputPath: File = _

  override protected def cli: CLICommon = CLI
}
