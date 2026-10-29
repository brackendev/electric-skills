# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.5] - 2026-10-29

### Added

- `electric-rules` hook: delivers the condensed ELEC-1 through ELEC-5 Iron Laws to the agent at session start, after `/clear` or a compaction, and on the first matching edit when the session started outside a matching project, with an instruction to load the `electric` skill along with the `clojure`, `clojurescript`, and `clojure-jvm` skills. Verified only under Claude Code. Copilot and Cursor receive no copy.
- Hook test suites in `tests/hooks/` (`bb tests/hooks/run.bb`), and empty `copilot-hooks.json` and `cursor-hooks.json` files.

## [0.1.4] - 2026-10-29

### Changed

- The Install section requires APM 0.34.0 and installs it from PyPI with `uv tool install apm-cli==0.34.0` after removing any Homebrew copy. APM 0.35.0 is not supported because it can delete installed skill files ([microsoft/apm#3372](https://github.com/microsoft/apm/issues/3372)).

## [0.1.3] - 2026-10-25

### Added

- The `electric` skill explains why Electric projects keep ClojureScript 1.11.121 and shadow-cljs 2.26.2. With the newest ClojureScript, the production client build succeeds, but the server, the tests, and the development REPL cannot load Electric code. The skill lists the newer ClojureScript features that Electric client code cannot use, and it tells the agent not to upgrade either library until an Electric release supports a newer ClojureScript.
- The project workflow reference shows how to read the ClojureScript version that an Electric release declares, and which checks must pass before a project moves to a newer ClojureScript.
- The `electric` skill names the compile error that a `binding` of an ordinary Clojure dynamic var produces in Electric code.
- The project layout lists the starter's logging configuration files and its two manual test scripts, and warns that the scripts start the development build or wait for keyboard input, so the agent does not run them.

### Changed

- The `deps.edn` example uses Clojure 1.12.6, Ring 1.15.6, Logback 1.6.6, and tools.build 0.10.14. These versions pass the starter app's production build and JVM tests with Electric `v3-alpha-ca05cbc93`.
- The skills tell the agent to pin the newest Electric release and never to depend on `v3-alpha-SNAPSHOT`, even though the upstream README and the starter app use it.
- The README states that Hyperfiddle publishes Electric v3 only as alpha releases and that the package targets the newest published alpha release.
- `/electric-fix` formats with the first cljfmt runner the project supports: a `:cljfmt` alias, the `lein-cljfmt` plugin, a `cljfmt` binary on `PATH`, or a `cljfmt` Clojure CLI tool. When none is installed, the format step reports that it was skipped and shows the `:cljfmt` alias to add. The step also explains which configuration files cljfmt reads and how to pass paths to each runner.
- The `:cljfmt` alias that `/electric-fix` and the project workflow reference show uses `:replace-deps`, so a project dependency that conflicts with cljfmt or cannot be resolved does not stop formatting.
- The `/electric-fix` signature lists the path and glob arguments that its arguments table already accepted: `/electric-fix [lint|format|test|advanced|dry] [--report] [all|<path>|<glob>]`.
- Every command that the skills give starts with `clojure` instead of `clj`, for example `clojure -X:test`, `clojure -X:build:prod build-client`, and the development server command `clojure -A:dev -X dev/-main`. The `clj` launcher exits with an error when `rlwrap` is not installed.

### Fixed

- The lint notes name both functions in the starter that leave `ring-request` unused, which explains all three warnings that clj-kondo reports in the unmodified starter.
- The `/electric-fix` dry step lists `.cljd` among the file types that dry4clj scans by default.
- The `:dry4clj` alias that `/electric-fix` shows aligns its continuation lines, so the alias passes `cljfmt check` once it is pasted into `deps.edn`.
- The `/electric-fix` format step looks for a cljfmt runner before it runs cljfmt. It listed the `clj -M:cljfmt` commands first and the alias check after them, so the agent could run a command that fails without the alias before it checked.
- The `/electric-fix` dry step reports `SKIPPED`, the term that the other steps and the summary use. It said `SKIP`.

### Removed

- Guidance for Electric v2: the v2 to v3 comparison table, the v2 trigger terms in the `electric` skill description, and the v2 migration notes in `electric-lenses` and the README.
- The notes about forms namespaces that came before `electric-forms5`. The `electric` skill now describes only `electric-forms5`.

## [0.1.2] - 2026-10-25

### Added

- The `electric` skill opens with Iron Laws (ELEC-1 through ELEC-5): numbered rules that prevent runtime failures, frozen controls, and security holes. The agent does not write code that breaks a law, and when existing code breaks one, it reports the law ID, the location, and the correct pattern, then asks before changing code outside the task. Three Key Rules (capitalized Electric function names, serializable values across sites, and `e/Offload` for blocking work) moved into the laws, and the remaining Key Rules are renumbered. Two new laws require spending every `e/Token` and siting security decisions on the server.

## [0.1.1] - 2026-10-25

### Changed

- When a project lacks the `:cljfmt` or `:test` alias, `/electric-fix` now shows the alias definition to add, as its dry step already does for `:dry4clj`.
- The Companion packages table describes biff-skills as covering Biff 2 conventions and deployment, because that package no longer scaffolds projects.

### Removed

- The `/electric-new` scaffolding skill is removed. It wrote a fixed project layout with pinned dependency versions, and only a typed slash command could start it. New projects start from a clone of the official [electric3-starter-app](https://github.com/hyperfiddle/electric3-starter-app), and the `electric` skill's project workflow reference describes its layout.

## [0.1.0] - 2026-10-24

### Added

- Initial release. [Electric](https://github.com/hyperfiddle/electric) v3 full-stack reactive framework skills layered on top of [clojure-skills](https://github.com/brackendev/clojure-skills) for host-neutral style, [clojurescript-skills](https://github.com/brackendev/clojurescript-skills) for the ClojureScript client, and [clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills) for the JVM Clojure server. Install all four together for Electric projects. The skills reflect Electric `v3-alpha-ca05cbc93` and the official `electric3-starter-app`.
- **electric** (model-invoked): Electric function definition with `e/defn` and `e/fn` and the capitalization rule the compiler uses to tell Electric calls from Clojure calls; the three siting rules for `e/client` and `e/server` and which values cross the network; transit serializability and custom handlers on `e/boot-server` and `e/boot-client`; tables with `e/amb`, `e/for`, `e/diff-by`, and `e/as-vec`; local state with atoms and `e/watch`; `electric-dom3` elements, `dom/text`, `dom/props`, and `dom/On`; transactions with `e/Token`; background work with `e/Offload`; the `electric-forms5` controls; server setup with the Ring adapter, stale-client rejection, and the required shadow-cljs build hook; a v2 to v3 recognition table; and the dev-time login that Electric's license requires. Defers to the [clojure](https://github.com/brackendev/clojure-skills) baseline for host-neutral style, the [clojurescript](https://github.com/brackendev/clojurescript-skills) skill for JavaScript interop and the CLJS workflow, and the [clojure-jvm](https://github.com/brackendev/clojure-jvm-skills) skill for Java interop on the server and the JVM tooling workflow. Pulls deeper detail from `references/project-workflows.md` on demand (the starter layout, `deps.edn`, `shadow-cljs.edn`, the single-JVM development loop, the production build and its version manifest, linting, formatting, testing, and checking for a newer release).
- **electric-lenses** (model-invoked): Code-lenses translations layered on top of `clojure-lenses` (in `clojure-skills`) and `clojurescript-lenses` (in `clojurescript-skills`). Records only the Electric-specific deltas: reactive dataflow in place of manual synchronization, the client and server site boundary, network transfer and serializability, tables and `e/amb`, `e/Token` transaction state, `e/Offload` as the effect edge, lazy `let` and statement ordering, forms that parse at the server boundary, and migration from Electric v2. Covers the four default code-lenses philosophies (grug, Honest Code, Tidy First, Parse Don't Validate) and the two opt-in philosophies (APOSD, Legacy Code) that activate when their lens is added with `+aposd` or `+legacy-code` or invoked directly. Auto-triggers alongside the [code-lenses](https://github.com/brackendev/code-lenses) plugin in Electric work.
- **electric-new** (user-invoked): Scaffold a new Electric project by cloning the official [electric3-starter-app](https://github.com/hyperfiddle/electric3-starter-app). Removes the starter's Git history, maintenance scripts, internal `:local` alias, and license file; renames the starter namespace, source and asset directories, logger, jar, and Fly.io app name; pins `com.hyperfiddle/electric` to the latest Clojars release; adds `:test`, `:cljfmt`, and `:dry4clj` aliases, a sample test, and clj-kondo configuration for `js/` interop inside `e/client`; imports Electric's clj-kondo exports; and verifies the production client build, lint, formatting, and tests. It never starts the development server, because the first development build asks the user to log in.
- **electric-fix** (user-invoked, and runnable by the agent from a natural-language request): Run the Electric quality pipeline (lint, format, test, advanced, dry). Lint imports Electric's clj-kondo exports and lints `src` and `test`. Format runs `clj -M:cljfmt fix` with the starter's Tonsky indentation. Test runs the JVM suite through the `:test` alias. Advanced runs the starter's `build-client` production build and reports when a missing Git version leaves a `nil` version in `resources/electric-manifest.edn`. Dry runs `clj -M:dry4clj --edn` over the production source paths. Each step is also addressable on its own.
