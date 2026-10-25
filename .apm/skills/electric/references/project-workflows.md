# Electric Project Workflows

Reference for the Electric v3 project shape (`deps.edn`, `shadow-cljs.edn`, `src-dev/dev.cljc`), the development loop and its login step, the production build, linting and formatting configuration, testing, and checking for a newer release.

Authoritative sources: the [`electric3-starter-app`](https://github.com/hyperfiddle/electric3-starter-app), the source inside the released [`com.hyperfiddle/electric`](https://clojars.org/com.hyperfiddle/electric) jar, and the [Tutorial and reference](https://docs.google.com/document/d/1frkb5URGFZUdWyCDl9moGFcPcLuDtW-M-HnCJOHkuDA/) document.

The shadow-cljs and JVM specifics covered here are framework-specific. The host-neutral CLI, REPL, lint, format, and test commands live in the [`clojure-jvm`](https://github.com/brackendev/clojure-jvm-skills) and [`clojurescript`](https://github.com/brackendev/clojurescript-skills) baselines.

## Project Layout

A new Electric project starts as a clone of the official [`electric3-starter-app`](https://github.com/hyperfiddle/electric3-starter-app). Its layout, with the starter namespace renamed to `my-app`:

```
deps.edn                         # dependencies and the :dev, :prod, :build, :jetty9 aliases
shadow-cljs.edn                  # :dev, :dev-jetty9, :prod, :prod-jetty9 browser builds
.cljfmt.edn                      # Tonsky two-space indentation
.clj-kondo/config.edn            # lint-as entries for Electric macros
Dockerfile, fly.toml             # container image and Fly.io deployment
src/my_app/main.cljc             # Main entry function and electric-boot
src/my_app/two_clocks.cljc       # example Electric function
src-dev/dev.cljc                 # dev entry point: shadow-cljs watch plus a Jetty server in one JVM
src-dev/dev_jetty9.cljc          # the same for Jetty 9
src-dev/user.clj                 # requires dev when the :dev alias starts a REPL
src-dev/logback.xml              # dev logging, with a logger named after the app (electric-starter-app in the starter)
src-prod/prod.cljc               # prod entry point with stale-client rejection
src-prod/prod_jetty9.cljc        # the same for Jetty 9
src-prod/logback.xml             # prod logging
src-build/build.clj              # build-client and uberjar functions
resources/public/my_app/         # index.dev.html, index.prod.html, index.css; js/ is build output
test-jetty-setups.sh             # upstream manual check: starts the dev build and waits for keyboard input
test-reject-stale-client.sh      # upstream manual check: opens a browser and waits for keyboard input
```

Do not run the two shell scripts from an agent session. `test-jetty-setups.sh` starts `dev/-main`, which asks for the Electric login, and both scripts wait for a person to confirm what the browser shows.

## `deps.edn`

The starter's dependencies and aliases, with the Electric release pinned and the other libraries at the versions this Electric release builds and runs with:

```clojure
{:deps    {com.hyperfiddle/electric       {:mvn/version "v3-alpha-ca05cbc93"}
           org.clojure/clojure            {:mvn/version "1.12.6"}
           org.clojure/clojurescript      {:mvn/version "1.11.121"}
           ring/ring                      {:mvn/version "1.15.6"}
           ch.qos.logback/logback-classic {:mvn/version "1.6.6"}}
 :paths   ["src" "resources"]
 :aliases {:dev   {:extra-paths ["src-dev"]
                   :extra-deps  {thheller/shadow-cljs {:mvn/version "2.26.2"}}}
           :prod  {:extra-paths ["src-prod"]}
           :build {:extra-paths ["src-build"]
                   :ns-default  build
                   :extra-deps  {thheller/shadow-cljs          {:mvn/version "2.26.2"}
                                 io.github.clojure/tools.build {:mvn/version "0.10.14"}}}
           ...}
 :mvn/repos
 {"clojars" {:url "https://repo.clojars.org/" :snapshots {:enabled true :update :always}}}}
```

- The starter's `deps.edn` depends on `v3-alpha-SNAPSHOT`. Replace it with the newest release (see "Checking for a newer release"). Never depend on a SNAPSHOT, because it changes without notice and builds stop being reproducible.
- Keep ClojureScript at 1.11.121 and shadow-cljs at 2.26.2, the versions the starter uses and the ClojureScript version Electric's POM declares. With ClojureScript 1.12.145 and shadow-cljs 3.5.7, `build-client` succeeds, but every JVM entry point that loads an Electric namespace (`clojure -X:test`, `clojure -M:prod -m prod`, `dev/-main`) stops with `NullPointerException` at `cljs.analyzer/lite-mode?`. shadow-cljs 3.5.7 with ClojureScript 1.11.121 fails at startup with `Could not locate cljs/analyzer/passes/lite__init.class`.
- shadow-cljs runs as a Clojure library inside the application JVM. The starter has no `package.json`, so no npm step is needed for the starter itself.
- Invoke the `:build` functions with `clojure -X:build:prod`, never with `clojure -T`. Electric's shadow-cljs compilation needs the application classpath.
- The starter's `:local` alias points at Hyperfiddle's internal sibling checkouts (`../electric`, `../hyperfiddle-contrib`, and others). It is only useful inside Hyperfiddle's own workspace.

## `shadow-cljs.edn`

```clojure
{:builds
 {:dev  {:target :browser
         :devtools {:loader-mode :default, :watch-dir "resources/public"}
         :output-dir "resources/public/my_app/js"
         :asset-path "/my_app/js"
         :modules {:main {:entries [dev] :init-fn dev/-main}}
         :build-hooks [(hyperfiddle.electric.shadow-cljs.hooks3/reload-clj)]}
  :prod {:target :browser
         :output-dir "resources/public/my_app/js"
         :asset-path "/my_app/js"
         :modules {:main {:entries [prod] :init-fn prod/-main}}
         :module-hash-names true}}}
```

The `reload-clj` build hook is required in the `:dev` build. It recompiles the client and reloads changed `.cljc` files on the JVM together, which keeps the client and server programs in step.

## Development Loop

From a shell:

```bash
clojure -A:dev -X dev/-main
```

From a REPL started with the `:dev` alias, evaluate `(dev/-main)`. `src-dev/user.clj` already requires `dev`.

- `dev/-main` starts the shadow-cljs server, watches the `:dev` build, and starts Jetty on `0.0.0.0:8080` with `wrap-electric-websocket` in one JVM.
- **The first dev compile asks for a login.** The Electric compiler prints "Please login with any email to activate automatically" with a link. The user opens the link and logs in. Agents must not automate this step. Prod and CI builds do not ask.
- Open http://localhost:8080. Saving a file reloads the app in the browser.
- Do not start shadow-cljs from a separate process (for example, `npx shadow-cljs watch`). Electric requires the single shared JVM.

## Production Build

```bash
clojure -X:build:prod build-client :version '"<version>"'
clojure -M:prod -m prod
```

- `build-client` runs a shadow-cljs release build of `:prod` under `:advanced` optimizations and writes `resources/electric-manifest.edn` with `:hyperfiddle/electric-user-version`. The same version is compiled into the client.
- Without `:version`, the build computes the version with `git describe --tags --long --always --dirty`. Outside a Git repository, or before the first commit, that prints `fatal: not a git repository` (or a similar error), the build still succeeds, and the manifest holds `nil`. `prod/-main` then fails its `(assert (string? (:hyperfiddle/electric-user-version config)))`. Pass `:version` explicitly or build from a committed checkout.
- Uberjar: `clojure -X:build:prod uberjar :build/jar-name '"app.jar"'`, then `java -cp target/app.jar clojure.main -m prod`.
- Docker: `docker build --build-arg VERSION=$(git rev-parse HEAD) -t my-app:latest .`, then `docker run --rm -it -p 8080:8080 my-app:latest`.
- Fly.io: `fly deploy --remote-only --build-arg VERSION=$(git rev-parse HEAD)`. Fly app names are global, so change `app` in `fly.toml` to a name the user owns before the first deploy.
- `prod.cljc` wraps the stack in `wrap-reject-stale-client`, so a browser holding assets from an older build reloads when it connects to a newer server.

## Linting

Import Electric's exported clj-kondo configuration, then lint the application sources:

```bash
mkdir -p .clj-kondo
clj-kondo --copy-configs --dependencies --lint "$(clojure -A:dev -Spath)"
clj-kondo --lint src test
```

The export maps `hyperfiddle.electric3/defn`, `fn`, `declare`, `for`, and `cursor` to their `clojure.core` equivalents. Two kinds of warnings remain in the unmodified starter's `src`, and these `.clj-kondo/config.edn` entries exclude both precisely:

```clojure
{:config-in-call {hyperfiddle.electric3/client {:linters {:unresolved-namespace {:exclude [js]}}}}
 :linters        {:unused-binding {:exclude-patterns ["^ring-request$"]}}}
```

- clj-kondo analyzes `.cljc` files for both platforms, so `js/` interop inside `e/client` is reported as an unresolved namespace on the Clojure side. The exclusion applies only inside `e/client` calls.
- `Main` does not use its `ring-request` argument, and `electric-boot` takes `ring-request` on both platforms but uses it only on the server.

The starter's `src-prod/prod.cljc` and `src-prod/prod_jetty9.cljc` report clj-kondo errors (`slurp` inside a `.cljc` macro body, linted for ClojureScript). Lint them only when asked.

## Formatting

The starter's `.cljfmt.edn` applies Tonsky two-space indentation to every form:

```clojure
{:indents {#re ".*" [[:inner 0]]}
 :remove-surrounding-whitespace? false
 :remove-trailing-whitespace? false
 :remove-consecutive-blank-lines? false}
```

The starter defines no `:cljfmt` alias. `/electric-fix format` runs cljfmt through the first runner it finds (a `:cljfmt` alias, the `lein-cljfmt` plugin, a `cljfmt` binary on `PATH`, or a `cljfmt` Clojure CLI tool) and shows this alias to add when none exists:

```clojure
:cljfmt {:replace-deps {dev.weavejester/cljfmt {:mvn/version "0.16.6"}}
         :main-opts    ["-m" "cljfmt.main"]}
```

With the alias, run `clojure -M:cljfmt check` or `clojure -M:cljfmt fix`. The unmodified starter passes `cljfmt check`.

## Testing

| Layer | Tool | Notes |
|-------|------|-------|
| Plain Clojure helpers | `clojure.test` with Cognitect `test-runner` (`clojure -X:test`) | Requiring an Electric namespace in a JVM test compiles it without a login. |
| Electric functions | `hyperfiddle.rcf` with `hyperfiddle.electric-local-def3` | Upstream's own test approach. `electric-local-def3` is included in the jar but is not a documented public API. |
| The running app | Manual browser check against the dev server | The dev build needs the interactive login. |

## Checking for a newer release

```bash
curl -sSL -H 'Accept: application/json' https://clojars.org/api/artifacts/com.hyperfiddle/electric \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['latest_release'])"
```

`latest_release` is the newest release that is not a SNAPSHOT. Never use `latest_version`, which is `v3-alpha-SNAPSHOT`. Read the [changelog document](https://docs.google.com/document/d/17BE7xpV9bFoPq9scLJHCF8hD5gBm00K0Up45N3XeG5c/) for the entries between the pinned release and the new one before changing the pin, then refresh dependencies with `clojure -P -A:dev:build:prod`, rebuild, and rerun the tests.

The release's POM lists the ClojureScript version that the release declares:

```bash
curl -sSL https://repo.clojars.org/com/hyperfiddle/electric/<version>/electric-<version>.pom
```

When the POM declares a newer ClojureScript than the project's pin, try the newest ClojureScript and shadow-cljs together, and keep them only if `clojure -X:test`, `clojure -X:build:prod build-client`, and `clojure -M:prod -m prod` all succeed.

## Ecosystem

| Package or repository | Purpose |
|-----------------------|---------|
| `com.hyperfiddle/electric` | Compiler, runtime, `electric-dom3`, `electric-forms5`, ring and http-kit adapters |
| `com.hyperfiddle/hyperfiddle-contrib` | Helper namespaces (`dustingetz.*`, `contrib.*`), pulled in transitively |
| `missionary/missionary` | Reactive streams library Electric builds on; `e/input` and `e/Task` take missionary flows and tasks |
| [`electric-fiddle`](https://github.com/hyperfiddle/electric-fiddle) | Source of the tutorial examples at electric.hyperfiddle.net |
