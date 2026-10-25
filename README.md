# electric-skills

[Electric](https://github.com/hyperfiddle/electric) v3 full-stack reactive framework skills packaged as an [APM](https://github.com/microsoft/apm) plugin. One install deploys the full set to every runtime in APM's default target set: Claude Code, Codex, OpenCode, Cursor, Copilot, Gemini, Windsurf, Kiro, and Grok Build. Antigravity is supported by naming it explicitly with `--target antigravity`.

Skills follow the [Agent Skills](https://agentskills.io) open standard. Two auto-trigger from conversation context (`electric`, `electric-lenses`). The other one, `/electric-fix`, is a slash command, and the agent can also run it from a natural-language request. Runtimes differ in how they offer this command: Codex lists it under `/skills` and runs it as `$name`, OpenCode attaches it with `@name`, and Gemini CLI also offers the auto-triggered skills as slash commands.

This package layers on top of three companions: the host-neutral [clojure-skills](https://github.com/brackendev/clojure-skills) baseline, [clojurescript-skills](https://github.com/brackendev/clojurescript-skills) for the ClojureScript client, and [clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills) for the JVM Clojure server. Install all four together so the `clojure` skill handles cross-dialect style, the `clojurescript` skill handles JavaScript interop and the shadow-cljs workflow, the `clojure-jvm` skill handles Java interop and the Clojure CLI / `tools.build` workflow, and this package adds Electric-aware siting, network transfer, tables, reactive state, DOM and events, transactions, forms, and the starter-app project workflow.

## Companion packages

Seven sibling APM packages. This package layers on top of [clojure-skills](https://github.com/brackendev/clojure-skills), [clojurescript-skills](https://github.com/brackendev/clojurescript-skills), and [clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills) and covers the Electric framework. Install all four together for Electric projects. The Biff, Fulcro, and ClojureDart packages are not required for Electric work.

| Package | Focus | Layers on |
|---------|-------|-----------|
| [clojure-skills](https://github.com/brackendev/clojure-skills) | Host-neutral [Clojure](https://clojure.org/) family baseline: style, naming, threading, collections, atoms, dispatch, formatting, namespaces, and testing. Triggers on `.clj`, `.cljs`, `.cljc`, `.cljd`. | — |
| [clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills) | [Clojure](https://clojure.org/) on the JVM: Java interop, refs / agents / STM, `with-open`, JVM-typed exceptions, `alter-var-root`, and the Clojure CLI / `tools.build` / `clj-kondo` / `cljfmt` / `test-runner` / nREPL workflow. | `clojure-skills` |
| [clojurescript-skills](https://github.com/brackendev/clojurescript-skills) | [ClojureScript](https://clojurescript.org/) style and tooling: JavaScript interop, externs inference, macro stage separation, `catch :default`, JS-flavored numbers and truthiness, and the `cljs.main` workflow. Triggers on `.cljs`, `.cljc` compiled to JS, `shadow-cljs.edn`, `figwheel-main.edn`. | `clojure-skills` |
| [biff-skills](https://github.com/brackendev/biff-skills) | [Biff](https://biffweb.com/) 2 web framework on the JVM: conventions and deployment. | `clojure-skills` + `clojure-jvm-skills` |
| [electric-skills](https://github.com/brackendev/electric-skills) (this package) | [Electric](https://github.com/hyperfiddle/electric) v3 full-stack reactive framework: `e/defn` components, `e/client` / `e/server` site transfer, `e/for` over `e/diff-by`, `e/watch` state, `e/Token` transactions, `e/Offload`, the `electric-dom3` and `electric-forms5` APIs, and the single-JVM shadow-cljs workflow. Triggers on `hyperfiddle.electric3`, `hyperfiddle.electric-dom3`, `e/defn`, `e/client`, `e/server`, and `com.hyperfiddle/electric`. | `clojure-skills` + `clojurescript-skills` + `clojure-jvm-skills` |
| [fulcro-skills](https://github.com/brackendev/fulcro-skills) | [Fulcro](https://github.com/fulcrologic/fulcro) 3 full-stack framework: `defsc` components, idents and the normalized client database, mutations, `df/load!`, dynamic routing, forms, UI state machines, Fulcro Inspect, and the Pathom 3 server. Triggers on `com.fulcrologic.fulcro.*`, `com.fulcrologic.rad.*`, `com.wsscode.pathom3.*`, `defsc`, `defmutation`, `defrouter`, `df/load!`, and ident vectors. | `clojure-skills` + `clojurescript-skills` + `clojure-jvm-skills` |
| [clojuredart-skills](https://github.com/brackendev/clojuredart-skills) | [ClojureDart](https://github.com/Tensegritics/ClojureDart) on [Flutter](https://flutter.dev/): Dart interop, type hints, `cljd.flutter` directives, async, FFI, REPL, and the Flutter project workflow. Triggers on `.cljd`, `cljd.flutter`. | `clojure-skills` |

## Install

Install [APM](https://github.com/microsoft/apm) first if you don't already have it. Then, in a project:

```bash
apm install brackendev/electric-skills --target all
apm install brackendev/clojurescript-skills --target all
apm install brackendev/clojure-jvm-skills --target all
apm install brackendev/clojure-skills --target all
```

Globally for your user account:

```bash
apm install brackendev/electric-skills -g --target all
apm install brackendev/clojurescript-skills -g --target all
apm install brackendev/clojure-jvm-skills -g --target all
apm install brackendev/clojure-skills -g --target all
```

Update later with `apm update [-g]`. Remove with `apm uninstall brackendev/electric-skills [-g]`. A local filesystem path can replace the shorthand at either scope.

## Requirements

- [Clojure CLI](https://clojure.org/guides/install_clojure) and Java 17 or higher for any skill in this package. The official starter app's Dockerfile builds on Java 17.
- [clojure-skills](https://github.com/brackendev/clojure-skills) installed alongside, for the host-neutral baseline plus `/clj-fix` and `/clj-smells-fix`.
- [clojurescript-skills](https://github.com/brackendev/clojurescript-skills) installed alongside, for the ClojureScript-specific guidance the `electric` skill defers to (JavaScript interop, externs, macro stage separation, JS numerics, the `shadow-cljs` workflow).
- [clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills) installed alongside, for the JVM-specific guidance the Electric server defers to (Java interop, JVM exceptions, refs / agents / STM, the Clojure CLI / `tools.build` / `clj-kondo` / `cljfmt` / `test-runner` / nREPL workflow).
- [clj-kondo](https://github.com/clj-kondo/clj-kondo) for the lint steps in the user-invoked skills below.
- The `electric-fix` dry step requires a [dry4clj](https://github.com/unclebob/dry4clj) `:dry4clj` alias in `deps.edn`. dry4clj is not published to Clojars, so the alias uses a Git coordinate. Without the alias, the dry step reports that it was skipped and shows the alias to add.
- An Electric license that fits your use. Electric v3 is not open source: it is free for bootstrappers and non-commercial use and otherwise commercial (see the [license change announcement](https://tana.pub/lQwRvGRaQ7hM/electric-v3-license-change)). The first development build of a project prints a login link that you follow once. Production and CI builds do not ask for a login.

## Command guide

A quick guide to every slash command. The detailed entries under [Skills](#skills) cover arguments and examples.

| Command | Use it when | What it does |
|---------|-------------------|--------------|
| `/electric-fix` | Lint, format, tests, or the production build need attention | Runs the fix pipeline, rewriting files in the format step with `cljfmt fix` |

## Skills

User-invocable skills in this package share an argument grammar, scope vocabulary, and mutation-as-default rule. See [CONVENTIONS.md](CONVENTIONS.md) for the full standard.

### Quality

#### `/electric-fix [lint|format|test|advanced|dry] [--report] [all]`

Fix an Electric project. Runs lint, format, test, advanced-compilation, and duplicate-form checks. Defaults to the full sequence and writes formatting in place via `cljfmt fix`. Each step is also addressable on its own. The `test` step runs the JVM suite through the `:test` alias, and the `advanced` step runs the starter's `build-client` production build as a production-build sanity check. Pass `--report` to replace the format step's write with `cljfmt check`. The lint, test, advanced, and dry steps are pure-read of source regardless.

```bash
/electric-fix
/electric-fix lint
/electric-fix advanced
/electric-fix --report
```

### Auto-triggered

These skills activate from conversation context. They cannot be invoked directly.

| Skill | Triggers |
|-------|----------|
| **electric** | `.cljc` files requiring `hyperfiddle.electric3`, `hyperfiddle.electric-dom3`, `hyperfiddle.electric-forms5`, or `hyperfiddle.electric-ring-adapter3`; the forms `e/defn`, `e/fn`, `e/client`, `e/server`, `e/for`, `e/diff-by`, `e/watch`, `e/amb`, `e/Token`, `e/Offload`, `e/boot-server`, `e/boot-client`, `dom/On`, and `dom/props`; `deps.edn` containing `com.hyperfiddle/electric`; `shadow-cljs.edn` with the `hyperfiddle.electric.shadow-cljs.hooks3/reload-clj` build hook; legacy Electric v2 code; or any mention of Electric Clojure, Electric v3, or Hyperfiddle. Covers Electric function definition and the capitalization rule, the three siting rules and network transfer, tables and `e/amb`, local state with atoms and `e/watch`, `electric-dom3`, events, transactions with `e/Token`, background work with `e/Offload`, forms, serialization, server setup, and the single-JVM development workflow. Defers to the [clojure](https://github.com/brackendev/clojure-skills) skill for host-neutral style, to the [clojurescript](https://github.com/brackendev/clojurescript-skills) skill for JavaScript interop and the CLJS workflow, and to the [clojure-jvm](https://github.com/brackendev/clojure-jvm-skills) skill for Java interop on the server and the JVM tooling workflow. The skill opens with Iron Laws (ELEC-1 through ELEC-5), rules that prevent runtime failures, frozen controls, and security holes. The agent does not write code that breaks them, and it reports a violation it finds in existing code before changing it. |
| **electric-lenses** | Auto-triggers alongside the [code-lenses](https://github.com/brackendev/code-lenses) plugin in Electric work. Layers on top of `clojure-lenses` (in `clojure-skills`) and `clojurescript-lenses` (in `clojurescript-skills`) and records only the Electric-specific deltas: reactive dataflow in place of manual synchronization, the client and server site boundary, network transfer and serializability, tables and `e/amb`, `e/Token` transaction state, `e/Offload` as the effect edge, lazy `let` and statement ordering, forms that parse at the server boundary, and migration from Electric v2. Covers the four default code-lenses philosophies (grug, Honest Code, Tidy First, Parse Don't Validate) and the two opt-in philosophies (APOSD, Legacy Code) that activate when their lens is added with `+aposd` or `+legacy-code` or invoked directly. |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT. See [LICENSE](LICENSE).
