# Contributing

Contributions are welcome: bug reports, feature requests and pull requests.

## Reporting issues

Use [GitHub Issues](https://github.com/madgeek-arc/registry/issues) for bugs, feature requests and
questions. For bugs, include the version, the environment, the steps to reproduce, and the expected
and actual behaviour.

Don't report security vulnerabilities in public issues; follow [SECURITY.md](SECURITY.md) instead.

## Branching

- `master` is the integration and release branch and carries the next `-SNAPSHOT` version.
- Major lines are developed on `version/<major>` branches (for example `version/5.0.0`). Features and
  fixes go on `feature/<name>` / `fix/<name>` branches, with pull requests into the target line.
- Without write access to the repository, fork it, create the branch in your fork and open the
  pull request from there.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/) with optional scopes, for example
`feat(elastic,recommend): ...`, `fix(search): ...`, `build: ...`. The changelog is grouped by these
types.

## Pull requests

- Describe the change and its motivation, and link the related issue.
- At least one maintainer review is required before merging.
- Changes to service interfaces, entities, REST paths, JMS payloads or `registry.*` properties affect
  every consumer. Call them out and update `docs/`.
- Add a changelog entry for user-visible changes.

## Required checks

- `mvn -B clean verify` must pass, including OWASP dependency-check (`failBuildOnCVSS=7.6`). Add
  justified suppressions to `dependency-check-suppressions.xml` only.
- Schema changes need a new Flyway migration in `registry-starter-service/src/main/resources/db/migration`
  (`V<major>.<minor>__<description>.sql`). Never edit applied migrations.
- New behaviour needs tests (see [docs/testing.md](docs/testing.md)).

## Releases and versioning

- [Semantic Versioning](https://semver.org/).
- Release with `maven-release-plugin` (`mvn release:prepare release:perform`): tags `v<version>`,
  commits prefixed `chore(release):`, artefacts deployed to the madgik repository
  (`pom.xml` `distributionManagement`).
- After releasing, consumers update their `registry-core.version` property.
