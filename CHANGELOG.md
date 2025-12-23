## [4.1.0](https://github.com/madgeek-arc/registry/compare/v4.0.4...v4.1.0) (2025-12-23)

### Features

* adds owasp checking for vulnerabilities ([b0cdf4f](https://github.com/madgeek-arc/registry/commit/b0cdf4f34000a5c326ced33c6cee77b2e10a6ca8))
* adds score in HighlightedResult ([a1ef737](https://github.com/madgeek-arc/registry/commit/a1ef737649b050d11dc329bf4eb665c2fd60237c))
* **elastic:** keyword fields are also mapped as text subfields named 'analyzed' (e.g. field1 is a keyword and field1.analyzed is a text field) ([fcfbfd1](https://github.com/madgeek-arc/registry/commit/fcfbfd1cf97c90f8b25e4236a39f3f1f412dbeb1))
* **elastic:** searching for a keyword utilized every 'text' field by default ([ca55a71](https://github.com/madgeek-arc/registry/commit/ca55a71efaa03e689c07695a1e25faca531d2b54))
* enables search with highligh functionality when using Elasticsearch ([8a2b4d0](https://github.com/madgeek-arc/registry/commit/8a2b4d070eb21f82e9851af4a76f762fd1f8d677))
* **jms:** serialization of dates return formatted date instead of timestamp ([c95e972](https://github.com/madgeek-arc/registry/commit/c95e972738dfc1397c8454c543274a14e53a838e))

### Bug Fixes

* removes posix permissions for portability ([6be85be](https://github.com/madgeek-arc/registry/commit/6be85bef005cb702ee19638dd91ce1e5ed569e9d))

## [4.0.4](https://github.com/madgeek-arc/registry/compare/v4.0.3...v4.0.4) (2025-07-09)

### Bug Fixes

* **versions:** Change Resource's 'version' values from formatted dates to uuid ([ff5c2b3](https://github.com/madgeek-arc/registry/commit/ff5c2b398330872f67fd4ddfb9fc37ed5182ebaf))
* **versions:** Increase resilience of method retrieving Versions from DB ([6e17b17](https://github.com/madgeek-arc/registry/commit/6e17b1710e7206c2e17d5baa7c2a92e63bc8c8ab))

## [4.0.3](https://github.com/madgeek-arc/registry/compare/v4.0.2...v4.0.3) (2025-05-22)

## [4.0.2](https://github.com/madgeek-arc/registry/compare/v4.0.1...v4.0.2) (2025-04-14)

### Bug Fixes

* auto migrate sequence to hibernate v6 ([ca93066](https://github.com/madgeek-arc/registry/commit/ca930662db0d17c2c469a5076def562f4be3e66c))

## [4.0.1](https://github.com/madgeek-arc/registry/compare/v4.0.0...v4.0.1) (2025-04-01)

### Features

* add method counting resources ([b6e4f49](https://github.com/madgeek-arc/registry/commit/b6e4f4912925b81ffc61d04487ebef52cd1d15da))

### Bug Fixes

* remove trailing / character ([80d4fcd](https://github.com/madgeek-arc/registry/commit/80d4fcdf679ecc160f347a6d5fd3db0f1c1ae278))
* remove trailing / character from rest api ([29f21fa](https://github.com/madgeek-arc/registry/commit/29f21fa52bf8d9adc96e4c87d6a2f7d5868f64df))
* **search:** improve search and order by functionality ([1185c48](https://github.com/madgeek-arc/registry/commit/1185c48146245ff325bde3c69c79e1690e09f3af))
* sort index fields in lowercase (like they will be ordered by in the database) ([104bbec](https://github.com/madgeek-arc/registry/commit/104bbec610c094afb5a0c271bcb620bc8e232723))

## [4.0.0](https://github.com/madgeek-arc/registry/compare/55d75bf8df3f897e8c61ba68ffe072e4370889bb...v4.0.0) (2025-02-13)

### Bug Fixes

* accidentally not commited dependency ([75287ba](https://github.com/madgeek-arc/registry/commit/75287bad8303cfed9217e30704407bfe365e1e0d))
* add conditional bean creation for dump/restore configuration ([38fcf6f](https://github.com/madgeek-arc/registry/commit/38fcf6f302d824270ce9f5f1df86f45ed2b8650e))
* added @PropertySource to re-enable @Value annotation ([faa3b33](https://github.com/madgeek-arc/registry/commit/faa3b337becdcb40a73f86b5299fab944d853a2e))
* apply datasource configuration properties ([e04a307](https://github.com/madgeek-arc/registry/commit/e04a3073cbb17090e331f3c730e7449e3d74aea2))
* correct imports ([e5813d5](https://github.com/madgeek-arc/registry/commit/e5813d56ab8fdda76635d0cafadeaae73648bf3e))
* correct issues on dump jobs ([6fcbdd0](https://github.com/madgeek-arc/registry/commit/6fcbdd0aafbe9772d50501b69a588ce347f982d3))
* Handle SQL ARRAY type for specific resource types on DefaultSearchService\n\nCo-authored-by: Konstantinos Spyrou <spyroukon@gmail.com> ([0cb26d2](https://github.com/madgeek-arc/registry/commit/0cb26d2c63a2bf913aa3869842f31f2e9b7fa155))
* move migration version to avoid flyway migration conflicts ([32ebb38](https://github.com/madgeek-arc/registry/commit/32ebb381d076d08b0e14eb92079fabbf7f5a5801))
* replace cast ([a1d5348](https://github.com/madgeek-arc/registry/commit/a1d5348e321d6d8d2daec138ad78bd454e4b31e8))
* restored indexing functionality upon using /restore controller when elasticsearch dependency is present ([03b92fe](https://github.com/madgeek-arc/registry/commit/03b92fe05f5754c3777f04b245aae74aa8b1dd7d))
* restored json-path version ([55d75bf](https://github.com/madgeek-arc/registry/commit/55d75bf8df3f897e8c61ba68ffe072e4370889bb))
* **search db:** Fix searching with Boolean filters ([5ce1648](https://github.com/madgeek-arc/registry/commit/5ce1648aa4fa7269f6059bca01c24abbb78eba46))

### Reverts

* Revert "Elasticsearch: removed usage of deprecated _id field" ([dff2d65](https://github.com/madgeek-arc/registry/commit/dff2d654c198ddc35f47fa3f2195f7c774bd8c95))
* Revert "replaced method creating request parameters to avoid uri decoding issues" ([e01000a](https://github.com/madgeek-arc/registry/commit/e01000a09650fa8e90f855fff6d60ef2d75835b1))
* Revert "implemented equals and hash" ([db914d9](https://github.com/madgeek-arc/registry/commit/db914d9e8ba09bd9344922c9e4686845463d8888))
* Revert "[maven-release-plugin] prepare release registry-2.3.0" ([cd876f8](https://github.com/madgeek-arc/registry/commit/cd876f89d2d1b20a6cf96f13418292f4e508fb2d))
* Revert "[maven-release-plugin] prepare release registry-2.3.0" ([37e07e0](https://github.com/madgeek-arc/registry/commit/37e07e0e75a5a9717e4801d532df3b6da3a3a9fb))
* Revert "[maven-release-plugin] prepare for next development iteration" ([a0b3066](https://github.com/madgeek-arc/registry/commit/a0b3066f43ec82cddd7e7f0b3145c98949a9733a))
