1. Grab the associated release text from [guardrail-dev/guardrail/releases](https://github.com/guardrail-dev/guardrail/releases)

2. `mvn versions:set -DnewVersion=0.34.1 -DgenerateBackupPoms=false`

3. Create a release tag: [link](https://github.com/guardrail-dev/guardrail-maven-plugin/releases)

4. Once the tag is created, travis-ci will start building a release immediately.
   Artifacts should be published to sonatype automatically.
