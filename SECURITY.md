# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| 5.0.x | :white_check_mark: |
| 4.3.x | :white_check_mark: |
| < 4.3 | :x: |

5.0.x is the `version/5.0.0` line (Spring Boot 4) and 4.3.x the `master` line (Spring Boot 3). Until 5.0.0 is released, fixes for it land on `version/5.0.0`.

## Reporting a Vulnerability

Report vulnerabilities privately through GitHub: open the repository's **Security** tab and choose
**Report a vulnerability** ([direct link](https://github.com/madgeek-arc/registry/security/advisories/new)).
Don't report them in public issues, pull requests or discussions.

Include:

- the affected version or commit;
- steps to reproduce, or a proof of concept;
- the impact you expect.

The maintainers follow up in the private advisory, fix the issue in the supported versions, and publish
the advisory once a fixed release is available.

## Scope

This repository covers the registry starters, the REST controllers they register, the client starter
and the standalone `registry-service`. The controllers have no authentication of their own, so
guarding their paths (or setting `registry.rest.enabled=false`) is the host application's
responsibility: an unguarded path in a host application is that application's issue.

[docs/security-model.md](docs/security-model.md) describes the component's security model and the
settings a deployment should use.
