##  (2025-02-13)

### ⚠ BREAKING CHANGES

* Move to Spring Framework 6 and Spring Batch 5

### Features

* Move to Spring Framework 6 and Spring Batch 5 ([05758d8](https://github.com/madgeek-arc/registry/commit/05758d85b865a0f03a152625690512fc649126a1))

### Bug Fixes

* Add conditional bean creation for dump/restore configuration ([38fcf6f](https://github.com/madgeek-arc/registry/commit/38fcf6f302d824270ce9f5f1df86f45ed2b8650e))
* Add @PropertySource to re-enable @Value annotation ([faa3b33](https://github.com/madgeek-arc/registry/commit/faa3b337becdcb40a73f86b5299fab944d853a2e))
* Apply datasource configuration properties ([e04a307](https://github.com/madgeek-arc/registry/commit/e04a3073cbb17090e331f3c730e7449e3d74aea2))
* Correct issues on dump jobs ([6fcbdd0](https://github.com/madgeek-arc/registry/commit/6fcbdd0aafbe9772d50501b69a588ce347f982d3))
* Handle SQL ARRAY type for specific resource types on DefaultSearchService ([0cb26d2](https://github.com/madgeek-arc/registry/commit/0cb26d2c63a2bf913aa3869842f31f2e9b7fa155))
* Restore indexing functionality upon using /restore controller when elasticsearch dependency is present ([03b92fe](https://github.com/madgeek-arc/registry/commit/03b92fe05f5754c3777f04b245aae74aa8b1dd7d))
* **search db:** Fix searching with Boolean filters ([5ce1648](https://github.com/madgeek-arc/registry/commit/5ce1648aa4fa7269f6059bca01c24abbb78eba46))
