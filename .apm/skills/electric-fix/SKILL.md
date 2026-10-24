---
name: electric-fix
description: >-
  Fix an Electric v3 project with lint, format, test, advanced, and dry
  (duplicate-form) steps across its Clojure server and ClojureScript client.
  The format step writes by default. Use when asked to lint, format, test, or
  check an Electric project, to run its tests, or to verify its production
  client build. Use --report to replace the formatting write with cljfmt
  check.
argument-hint: "[lint|format|test|advanced|dry] [--report] [all]"
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

# Electric Fix

Run lint, format, test, advanced-compilation, and duplicate-form checks on an Electric v3 project laid out like the [`electric3-starter-app`](https://github.com/hyperfiddle/electric3-starter-app). Electric programs are `.cljc` files compiled for both a JVM server and a ClojureScript client, so the `advanced` step runs the production client build to surface failures that development builds hide. The format step writes by default, and the other four steps only read source.

No step starts the development build, so no step triggers the Electric login. The production build and JVM test runs do not ask for it.

The argument grammar follows `CONVENTIONS.md` in the electric-skills source repository. The table below is complete for this skill.

## Arguments

| Input             | Target                                                                       |
|-------------------|------------------------------------------------------------------------------|
| (no argument)     | Run all five steps (`lint`, `format`, `test`, `advanced`, `dry`) on the project (`src`, `test`) |
| `all`             | Same as (no argument); accepted for family consistency                       |
| `lint`            | Run lint only                                                                |
| `format`          | Run format only                                                              |
| `test`            | Run the JVM test suite through the `:test` alias                             |
| `advanced`        | Run the production client build (`build-client`) as a production-build sanity check |
| `dry`             | Run the dry4clj duplicate-form scan only                                     |
| `<path>` `<glob>` | Restrict lint, format, and dry steps to those files or directories. The `test` and `advanced` steps ignore paths and always run the configured suite and build |
| `--report`        | Replace `cljfmt fix` with non-writing `cljfmt check` in the format step      |

Step keywords are combinable (for example, `/electric-fix lint test`). The `--report` flag may appear in any position. When `--report` is present without an explicit step keyword, every step still runs, and only the format step's behavior changes.

## Mutation

Only the `format` step writes source. It runs `clj -M:cljfmt fix` by default, rewriting files in place. With `--report`, the step runs `clj -M:cljfmt check`, which exits non-zero when files would change but does not write. The `lint`, `test`, and `dry` steps are pure-read of source regardless of `--report`. The `advanced` step deletes and rewrites the client build output under `resources/public/<app>/js/` and rewrites `resources/electric-manifest.edn` (build artifacts, not source), unaffected by `--report`. The `lint` step's bootstrap writes the upstream clj-kondo exports into `.clj-kondo/imports/` and the lint cache into `.clj-kondo/.cache/`. Those are tooling files, not source, and `--report` does not change that write.

This skill excludes vendored, generated, and dependency-locked paths from the file set it walks. The filter combines `.gitignore` matches and a hardcoded floor (`node_modules/`, `vendor/`, `third_party/`, `.bundle/`, `target/`, `build/`, `dist/`, `out/`, `.shadow-cljs/`, `cljd-out/`, `*.lock`, `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`, `Gemfile.lock`, `Cargo.lock`, `poetry.lock`, `composer.lock`). The `advanced` step's writes are build artifacts produced by the build itself and are outside the source-mutation scope of the rule. Naming a vendored source path directly through `<path>` or `<glob>` bypasses the filter for that target. When the filter excludes any path, add a single "skipped N vendored or generated paths" line to the report. Under `--report`, list every skipped path. This follows Rule 4 of the electric-skills `CONVENTIONS.md`.

## Steps

Parse `$ARGUMENTS` to determine which steps to run and whether `--report` is present. If no step keyword is supplied (or only `all` is supplied), run every step in order.

### 1. Lint

Bootstrap the upstream Electric clj-kondo exports before every lint. clj-kondo copies configurations only into an existing `.clj-kondo/` directory, so create it first:

```bash
mkdir -p .clj-kondo
clj-kondo --copy-configs --dependencies --lint "$(clj -A:dev -Spath)"
```

`--dependencies` skips each dependency jar that clj-kondo has already linted, so a repeat run is fast. After an Electric upgrade, the new jar is linted and its exports are copied into `.clj-kondo/imports/hyperfiddle/electric/`.

Then lint the application sources:

```bash
clj-kondo --lint src test
```

If the project has no `test/` directory, lint `src` only. The starter's `src-dev`, `src-prod`, and `src-build` directories hold upstream boot and build code and are linted only when named through `<path>`: upstream's `src-prod/prod.cljc` reports clj-kondo errors for its `.cljc` macro.

Report pass if exit code is 0 and there are no errors. Show the clj-kondo output regardless.

Without the Electric exports, `e/defn`, `e/fn`, and `e/for` forms generate false positives. Two kinds of warnings are expected in an Electric project whose `.clj-kondo/config.edn` lacks the matching exclusions: "Unresolved namespace js" for `js/` interop inside `e/client` (clj-kondo also analyzes `.cljc` files as Clojure) and "unused binding ring-request" in `electric-boot`. Report them with the fix rather than editing source:

```clojure
{:config-in-call {hyperfiddle.electric3/client {:linters {:unresolved-namespace {:exclude [js]}}}}
 :linters        {:unused-binding {:exclude-patterns ["^ring-request$"]}}}
```

### 2. Format

Without `--report`, run cljfmt to fix formatting. `cljfmt` covers `.clj`, `.cljs`, and `.cljc`:

```bash
clj -M:cljfmt fix
```

With `--report`, run cljfmt in check mode (no writes):

```bash
clj -M:cljfmt check
```

Report pass if exit code is 0, fail otherwise. Show any formatting changes (under `fix`) or the diff (under `check`).

The starter's `.cljfmt.edn` sets `:indents {#re ".*" [[:inner 0]]}`, which replaces every default cljfmt rule with Tonsky two-space indentation. Electric upstream asks for that style, so do not report the catch-all `:indents` entry as a misconfiguration. If the project has no `:cljfmt` alias, report `format: SKIPPED` and show the alias to add under `:aliases` rather than inventing a command:

```clojure
:cljfmt {:extra-deps {dev.weavejester/cljfmt {:mvn/version "0.16.6"}}
         :main-opts  ["-m" "cljfmt.main"]}
```

### 3. Test

Run the JVM test suite:

```bash
clj -X:test
```

Use `clj -X:test` when the `:test` alias defines `:exec-fn`, and `clj -M:test` when it defines only `:main-opts`. If `deps.edn` has no `:test` alias, report `test: SKIPPED` and show the alias to add under `:aliases`:

```clojure
:test {:extra-paths ["test"]
       :extra-deps  {io.github.cognitect-labs/test-runner {:git/tag "v0.5.1" :git/sha "dfb30dd"}}
       :main-opts   ["-m" "cognitect.test-runner"]
       :exec-fn     cognitect.test-runner.api/test}
```

Report pass if the runner exits 0 and the output contains no failure or error summary. Show the runner output.

Requiring a namespace that contains Electric functions compiles them on the JVM, so a test that requires the application's entry namespace also catches Electric compile errors. Electric has no documented public harness for running Electric functions in tests. Upstream's own tests use `hyperfiddle.rcf` and `hyperfiddle.electric-local-def3`, so run those only when the project already uses them.

### 4. Advanced

Run the production client build:

```bash
clj -X:build:prod build-client
```

`build-client` (in `src-build/build.clj`) runs a shadow-cljs release build of the `:prod` build under `:advanced` optimizations. Report fail if the exit code is non-zero (the function asserts that shadow-cljs finished with `:done`), or if the output contains `WARNING:` lines from the Google Closure Compiler about property renaming or missing externs. Show the shadow-cljs summary line, such as `[:prod] Build completed. (165 files, 98 compiled, 0 warnings, 9.74s)`.

- Run it with `clj -X:build:prod`, not `clj -T`. Electric's shadow-cljs compilation needs the application classpath.
- If `deps.edn` has no `:build` alias or `src-build/build.clj` has no `build-client` function, ask the user for the project's production build command. Do not guess.
- The build reads its version from `git describe --tags --long --always --dirty`. If the output contains `fatal: not a git repository` (or `git describe` otherwise fails), the build still passes, but `resources/electric-manifest.edn` now records a `nil` version, which `prod/-main` rejects at startup. Note this in the report and tell the user to rebuild with `:version '"<version>"'` or from a committed checkout before running the prod server.
- The starter needs no Node or npm, because shadow-cljs runs inside the JVM. A project that adds npm dependencies needs `npm install` first.
- The first run compiles the whole program and is slow. Later runs reuse the shadow-cljs cache in `.shadow-cljs/`.

### 5. Dry

Scan for duplicate top-level forms with [dry4clj](https://github.com/unclebob/dry4clj). This step is advisory. It reports duplication candidates for review but never fails the run and never halts the pipeline.

Determine the project's production source directories from its build configuration instead of assuming a directory name:

- `deps.edn`: the top-level `:paths` (the `:dry4clj` alias is defined here). In the starter, `:paths` is `["src" "resources"]`. Scan the paths that contain Clojure source (`src`), not `resources`. Tests live under the `:test` alias's `:extra-paths`, so scanning `:paths` excludes them.

Scan the production source paths only and exclude test paths. If no configuration declares source paths, scan the source directories that exist in the repository, excluding any test directory. Do not assume `src`.

Check that `deps.edn` defines a `:dry4clj` alias before running the scan. When it does not, report `SKIP` and show the alias to add under `:aliases`:

```clojure
:dry4clj {:extra-deps {io.github.unclebob/dry4clj {:git/url "https://github.com/unclebob/dry4clj.git"
                                                    :git/sha "5994ef2bcaaae8d12bcd0afb4f091ae09083228c"}}
           :main-opts  ["-m" "dry4clj.core"]}
```

Run dry4clj with EDN output over the resolved paths:

```bash
clj -M:dry4clj --edn <source-paths...>
```

Do not pass `--threshold`, `--min-lines`, or `--min-nodes`. Scoping to production source removes the dominant noise, which is repeated test scaffolding. Tightening the score or size settings either hides real matches or has no effect on exact-structure duplicates.

dry4clj exits 0 after a completed scan, and the EDN output never prints a clean-state message, so do not use the exit code or any text match to detect duplicates. A non-zero exit means the scan itself failed (for example, a file the reader cannot parse) and is reported as `ERROR`. Parse the EDN map `{:candidates [...]}` and classify the result:

| Result   | Condition                                                                                                                  |
|----------|----------------------------------------------------------------------------------------------------------------------------|
| `PASS`   | `:candidates` is empty.                                                                                                     |
| `REVIEW` | `:candidates` has one or more entries. List them and continue.                                                             |
| `ERROR`  | The command fails or its output cannot be parsed. Report the cause, and do not report `PASS`. |
| `SKIP`   | No production source path can be identified, or `deps.edn` defines no `:dry4clj` alias. |

For `REVIEW`, sort candidates by exact matches first (`:score` equal to `1.0`), then by descending `min(:left-nodes, :right-nodes)`, then by descending `:score`. Report the scanned source paths, the candidate count, and the highest-priority candidates with their score, node counts, and both file ranges. Note that each candidate needs source inspection before extraction, and that test directories were excluded because repeated test scaffolding is often intentional.

Upstream dry4clj scans `.clj`, `.cljc`, and `.cljs` files by default, so the `.cljc` Electric program and any single-platform helpers are all covered.

## Report

After running all requested steps, print a summary:

```
Electric Fix Results:
  Lint:     PASS/FAIL/SKIPPED
  Format:   PASS/FAIL/SKIPPED
  Test:     PASS/FAIL/SKIPPED
  Advanced: PASS/FAIL/SKIPPED
  Dry:      PASS/REVIEW/ERROR/SKIPPED
```

If `--report` was passed, append `(report mode: format checked, not written)` after the summary. If a lint, format, test, or advanced step fails, stop and report the failure. Do not continue to later steps.

## Gotchas

- **dry4clj cannot read alias-qualified auto-resolved keywords.** dry4clj reads source without resolving namespace aliases, so a keyword such as `::e/ok` or `::forms/pending` stops the scan with an exception. Plain auto-resolved keywords such as `::ok` read correctly. Report the file and the error, and keep the step advisory. Do not rewrite source to work around the reader.
- **A running dev server holds the same project.** `dev/-main` keeps a shadow-cljs server and a watch of the `:dev` build running in its JVM. Stop it before running `advanced` if the release build fails with a shadow-cljs server or port error, rather than reporting that failure as a source problem.
- **`dry` is a duplicate-form scan, not a dry-run mode.** The step keyword names the dry4clj tool. To preview the format step without writing, pass `--report`, not `dry`.
- **Host-neutral checks live in clojure-skills.** `/clj-fix` and `/clj-smells-fix` cover host-neutral lint fixes and smells in `.clj`, `.cljc`, and `.cljs` files. This skill covers the Electric project layout and its production build.
