This project's version is pinned to the main guardrail module version.
By design, this should bump at a slower rate than the sum of all modules.
As a result, we pin our version to the main `guardrail` module version and let users bump module versions should they need something newer.

1. When a PR is merged that bumps the guardrail version, release-drafter will create a new draft release

2. Promote that draft to a release in order to trigger the `Release` GitHub Actions workflow

3. The artifact should appear in s01.oss.sonatype.org after metadata resync
