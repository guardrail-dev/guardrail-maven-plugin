package dev.guardrail

import java.io.File
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}

@Mojo(name = "generate-test-sources", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
class GuardrailTestCodegenMojo extends AbstractGuardrailCodegenMojo(Test) {
  val guardrailModulePrefix: String = "guardrail"

  @Parameter(defaultValue = "${project.build.directory}/generated-sources/swagger-test-clients", property = "outputPath", required = true)
  var outputPath: File = _
}
