---
name: electric-new
description: "Scaffold a new Electric v3 project from the official starter app with a pinned Electric release"
argument-hint: "<project-name>"
user-invocable: true
disable-model-invocation: true
---

# Scaffold an Electric Project

Create a new [Electric](https://github.com/hyperfiddle/electric) v3 project from the official [`electric3-starter-app`](https://github.com/hyperfiddle/electric3-starter-app): a ClojureScript client and a Clojure JVM server in one `.cljc` program, a single-JVM development entry point with hot code reloading, and a production build. The skill renames the starter to the project, pins the latest Electric release, and adds `clj-kondo`, `cljfmt`, test, and dry4clj support so `/electric-fix` works from the first run.

The starter tracks upstream, so the project gets upstream's current boot, build, and logging code instead of a copy kept inside this skill.

## Arguments

| Input             | Target                                                                       |
|-------------------|------------------------------------------------------------------------------|
| `<project-name>`  | Required. Use hyphens (for example, `my-app`). The skill maps them to underscores for file paths (`my_app/`) and keeps hyphens in namespace symbols. |
| (no argument)     | Prompt the operator for a project name.                                      |

This skill is exempt from the `all` and `<path>` rows of the standard scope vocabulary because scaffolding has no useful default scope, as documented in `CONVENTIONS.md` in the electric-skills source repository.

## Mutation

Mutates by default: clones the starter into a new project directory, removes the starter's Git history, upstream maintenance scripts, internal `:local` alias, and license file, renames the starter namespace and paths, pins `com.hyperfiddle/electric` in `deps.edn`, adds the `:test`, `:cljfmt`, and `:dry4clj` aliases, writes a sample test, updates `.clj-kondo/config.edn`, and imports the upstream clj-kondo exports into `.clj-kondo/imports/`. Also downloads dependencies, runs a production client build into `resources/public/<project_name>/js/` and `resources/electric-manifest.edn`, lints, checks formatting, and runs the tests as a first-build sanity check. Never starts the dev server, because the first dev build requires an interactive login. There is no `--report` flag. Read this `SKILL.md` to preview the side effects.

## Prerequisites

Verify these are installed before proceeding. If any are missing, stop and tell the user.

- Java 17 or higher (the starter's Dockerfile builds on Temurin 17): `java --version`
- Clojure CLI: `clj --version`
- Git (to clone the starter): `git --version`
- `clj-kondo` (linter): `clj-kondo --version`

Outbound network access to `github.com`, `clojars.org`, and Maven Central is required.

Tell the user, before cloning, that Electric v3 is not open source: it is free for bootstrappers and non-commercial use and otherwise requires a commercial license (see the [license change announcement](https://tana.pub/lQwRvGRaQ7hM/electric-v3-license-change)), and the first dev build asks them to log in. Continue only after the user confirms.

## Steps

### 1. Get Project Name and Namespace

Use `$ARGUMENTS` as the project name. If empty, ask the user for one.

The project name uses hyphens (for example, `my-app`). The main namespace prefix defaults to the project name (`my-app`, so the entry namespace is `my-app.main`). Ask the user if they prefer a qualified prefix such as `com.example.my-app`. File paths use underscores where the namespace uses hyphens and slashes where it uses dots, so `com.example.my-app.main` lives at `src/com/example/my_app/main.cljc`.

Confirm the project name and namespace prefix with the user before continuing. The steps below use these placeholders:

| Placeholder | Example | Meaning |
|-------------|---------|---------|
| `<project-name>` | `my-app` | Directory name, Fly.io app name, jar name, Docker tag |
| `<project_name>` | `my_app` | Asset directory under `resources/public/` |
| `<namespace>` | `com.example.my-app` | Namespace prefix |
| `<namespace-path>` | `com/example/my_app` | Source directory under `src/` and `test/` |

### 2. Resolve the Latest Electric Release

Fetch the latest released version from Clojars:

```bash
curl -sSL -A 'Mozilla/5.0' \
  'https://clojars.org/api/artifacts/com.hyperfiddle/electric' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['latest_release'])"
```

The result has the form `v3-alpha-<sha>`. Clojars' `latest_version` field is `v3-alpha-SNAPSHOT`, so read `latest_release`. If the fetch fails, ask the user for the version to use. Do not invent a version number.

### 3. Clone the Starter

Stop if `<project-name>` already exists. Then:

```bash
git clone --depth 1 https://github.com/hyperfiddle/electric3-starter-app <project-name>
cd <project-name>
git log -1 --format='%H %cs'
```

Record the starter commit for the report. Then remove the starter's history and the files that belong to the starter rather than to the new project:

```bash
rm -rf .git test-jetty-setups.sh test-reject-stale-client.sh LICENSE
```

- `test-jetty-setups.sh` and `test-reject-stale-client.sh` are upstream's manual checks for the starter itself, and they expect upstream's `:local` alias.
- `LICENSE` is the starter's MIT No Attribution license, which needs no attribution. The project's license is the user's choice.

### 4. Rename the Starter

Move the source and asset directories:

```bash
mkdir -p "src/$(dirname <namespace-path>)"
mv src/electric_starter_app src/<namespace-path>
mv resources/public/electric_starter_app resources/public/<project_name>
```

Then rewrite the starter's names in every file. The substitutions run in this order on each line, and each one targets one form of the name. The pipeline through `xargs` behaves the same in bash and zsh (zsh does not split an unquoted variable into words):

```bash
grep -rlE 'electric[-_]starter[-_]app|electric3-starter-app' . | xargs perl -pi -e '
  s#src/electric_starter_app/#src/<namespace-path>/#g;
  s/electric-starter-app\./<namespace>./g;
  s/name="electric-starter-app"/name="<namespace>"/g;
  s/electric3-starter-app/<project-name>/g;
  s/electric-starter-app/<project-name>/g;
  s/electric_starter_app/<project_name>/g;'
perl -pi -e 's/^# Electric v3 Starter App$/# <project-name>/' README.md
```

| Pattern | Where it appears | Replacement |
|---------|------------------|-------------|
| `src/electric_starter_app/` | README link to the root function | `src/<namespace-path>/` |
| `electric-starter-app.` | Namespace symbols in `src`, `src-dev`, `src-prod` | `<namespace>.` |
| `name="electric-starter-app"` | Logger name in `src-dev/logback.xml` | `name="<namespace>"` |
| `electric3-starter-app`, `electric-starter-app` | `fly.toml` app name, jar name in `build.clj`, Docker tags in the READMEs | `<project-name>` |
| `electric_starter_app` | Asset paths in `shadow-cljs.edn`, the HTML shells, `prod.cljc`, `dev.cljc`, `build.clj`, `.gitignore`, `.dockerignore` | `<project_name>` |

Confirm nothing was missed. This command must print nothing:

```bash
grep -rnE 'electric[-_]starter[-_]app|electric3-starter-app' .
```

### 5. Edit `deps.edn`

1. Replace `{:mvn/version "v3-alpha-SNAPSHOT"}` for `com.hyperfiddle/electric` with the release from step 2.
2. Remove the `:local` alias. It overrides Electric and three other Hyperfiddle libraries with `:local/root` paths into Hyperfiddle's internal workspace (`../electric`, `../hyperfiddle-contrib`, `../electric-secret`, `../hyperfiddle-agent`).
3. Add these aliases next to the existing `:dev`, `:prod`, `:build`, and `:jetty9` aliases:

```clojure
:test    {:extra-paths ["test"]
          :extra-deps  {io.github.cognitect-labs/test-runner {:git/tag "v0.5.1" :git/sha "dfb30dd"}}
          :main-opts   ["-m" "cognitect.test-runner"]
          :exec-fn     cognitect.test-runner.api/test}
:cljfmt  {:extra-deps {dev.weavejester/cljfmt {:mvn/version "0.16.6"}}
          :main-opts  ["-m" "cljfmt.main"]}
:dry4clj {:extra-deps {io.github.unclebob/dry4clj {:git/url "https://github.com/unclebob/dry4clj.git"
                                                   :git/sha "5994ef2bcaaae8d12bcd0afb4f091ae09083228c"}}
          :main-opts  ["-m" "dry4clj.core"]}
```

The fixed versions above were current on 2026-10-24. `dry4clj` is not published to Clojars, so the alias uses a Git coordinate pinned to a commit. Keep the starter's other dependency versions: upstream tests the starter with them.

### 6. Create a Sample Test

Create `test/<namespace-path>/main_test.clj`:

```clojure
(ns <namespace>.main-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [<namespace>.main :as main]))

(deftest electric-boot-test
  (testing "the Electric program compiles on the JVM and exposes its entry point"
    (is (fn? main/electric-boot))))
```

Requiring `<namespace>.main` compiles every Electric function it reaches, so a compile error in the Electric program fails this test. JVM test runs do not trigger the Electric login.

### 7. Update `.clj-kondo/config.edn`

Keep the starter's `:lint-as` and `:linters` entries and add two entries so the unmodified project lints clean:

```clojure
{:lint-as {hyperfiddle.electric3/defn       clojure.core/defn
           hyperfiddle.electric3/for        clojure.core/let
           hyperfiddle.electric3/cursor     clojure.core/let
           hyperfiddle.electric3/with-cycle clojure.core/let
           hyperfiddle.electric3/fn         clojure.core/fn
           hyperfiddle.electric.impl.array-fields/deffields clojure.core/declare}
 :config-in-call {hyperfiddle.electric3/client {:linters {:unresolved-namespace {:exclude [js]}}}}
 :linters {:redundant-expression {:level :off}
           :var-same-name-except-case {:level :off}
           :clojure-lsp/unused-public-var {:level :off}
           :unused-binding {:exclude-patterns ["^ring-request$"]}}}
```

- `:config-in-call` excludes the `js` namespace only inside `e/client`. clj-kondo also analyzes `.cljc` files as Clojure, where `js/` interop is otherwise reported.
- `:exclude-patterns` covers `electric-boot`, which takes `ring-request` on both platforms but uses it only on the server.

Import the upstream exports:

```bash
mkdir -p .clj-kondo
clj-kondo --copy-configs --dependencies --lint "$(clj -A:dev -Spath)"
```

The output lists `.clj-kondo/imports/hyperfiddle/electric` among the copied configurations. Commit `.clj-kondo/` with the project, and rerun the import after changing the Electric version.

### 8. First Build and Checks

```bash
clj -P -A:dev:build:prod:test:cljfmt
clj -X:build:prod build-client :version '"<project-name>-dev"'
clj-kondo --lint src test
clj -M:cljfmt check
clj -X:test
```

- `clj -P` downloads every alias's dependencies.
- `build-client` runs the shadow-cljs release build, the production compile check. It needs no login. Passing `:version` is required here, because the project is not a Git repository yet and the build otherwise reads its version from `git describe` and writes `nil` into `resources/electric-manifest.edn`, which the prod server rejects at startup.
- `clj-kondo --lint src test` must exit 0. `src-prod` is not linted: upstream's `prod.cljc` reports clj-kondo errors for its `.cljc` macro.
- If any command fails, capture the output and report the error rather than continuing.

### 9. Report

Print the result and the next steps:

```
Created Electric project at <project-name>/ from electric3-starter-app <starter commit>.
Electric version: <release from step 2>
Main namespace: <namespace>.main

Changed from the starter:
  src/<namespace-path>/                (renamed from src/electric_starter_app/)
  resources/public/<project_name>/     (renamed from resources/public/electric_starter_app/)
  deps.edn                             (Electric pinned, :local removed, :test, :cljfmt, :dry4clj added)
  test/<namespace-path>/main_test.clj  (new)
  .clj-kondo/config.edn                (js and ring-request lint exclusions)
  .clj-kondo/imports/                  (upstream exports)
  Removed: .git, LICENSE, test-jetty-setups.sh, test-reject-stale-client.sh

License: Electric v3 is free for bootstrappers and non-commercial use and
otherwise commercial. Choose a license for your own code.

Next steps:
  cd <project-name>
  git init && git add -A && git commit -m "Create project from the Electric starter"
  clj -A:dev -X dev/-main      # first run prints an Electric login link; follow it
  open http://localhost:8080

Run /electric-fix to run the full quality pipeline.
Run /clj-fix for the host-neutral checks.
```

## Gotchas

- **The first dev build asks for a login.** `clj -A:dev -X dev/-main` prints "[Electric compiler] Please login with any email to activate automatically" with a link. The user follows it. Never automate it, and never start the dev server from this skill.
- **The prod build reads its version from Git.** Without `:version`, `build-client` runs `git describe --tags --long --always --dirty`. Outside a Git repository or before the first commit, it writes a `nil` version and `clj -M:prod -m prod` fails its version assertion. Commit first or pass `:version`.
- **Run `:build` functions with `clj -X:build:prod`, not `clj -T`.** Electric's shadow-cljs compilation needs the application classpath.
- **Port 8080 must be free.** The dev and prod servers bind `0.0.0.0:8080`. Another service already listening on port 8080 can stop the server from starting, or, on macOS, keep answering requests to `127.0.0.1` while the app answers `localhost`. Check with `lsof -nP -iTCP:8080 -sTCP:LISTEN`.
- **Change the Fly.io app name before deploying.** Fly app names are global, and `fly.toml` now uses `<project-name>`, which another account may already own.
- **The starter is the reference scaffold.** When this skill and upstream disagree, the starter wins. Check [`electric3-starter-app`](https://github.com/hyperfiddle/electric3-starter-app) before reporting a discrepancy as a bug, and update this skill.
