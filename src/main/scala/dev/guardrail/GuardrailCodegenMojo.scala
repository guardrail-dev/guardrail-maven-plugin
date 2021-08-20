package dev.guardrail

import java.io.File
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}

@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class GuardrailCodegenMojo extends AbstractGuardrailCodegenMojo(Main) {
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/swagger-clients", property = "outputPath", required = true)
  var outputPath: File = _

  override protected def cli: CLICommon = CLI
}
