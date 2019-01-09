1. Grab the associated release text from [twilio/guardrail/releases](https://github.com/twilio/guardrail/releases)

2. `mvn versions:set -DnewVersion=0.34.1 -DgenerateBackupPoms=false`

2. Create a release tag: [link](https://github.com/twilio/guardrail-maven-plugin/releases)

3. `mvn clean deploy -Possrh`

3a. Close and release the staged repository in https://oss.sonatype.org/

4. `mvn clean deploy -Pbintray`
