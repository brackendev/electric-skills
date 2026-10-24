---
name: electric
description: >-
  Use when writing, editing, reviewing, or discussing Hyperfiddle Electric
  applications. Triggers: .cljc files requiring hyperfiddle.electric3,
  hyperfiddle.electric-dom3, hyperfiddle.electric-forms5, or
  hyperfiddle.electric-ring-adapter3; the forms e/defn, e/fn, e/client,
  e/server, e/for, e/diff-by, e/watch, e/amb, e/Token, e/Offload,
  e/boot-server, e/boot-client, dom/On, and dom/props; deps.edn containing
  com.hyperfiddle/electric; shadow-cljs.edn with the
  hyperfiddle.electric.shadow-cljs.hooks3/reload-clj build hook; legacy
  Electric v2 code (hyperfiddle.electric, hyperfiddle.electric-dom2,
  e/for-by, new on an Electric function); or any mention of Electric Clojure,
  Electric v3, or Hyperfiddle. Covers Electric function definition and the
  capitalization rule, the three siting rules and network transfer, tables
  and e/amb, local state with atoms and e/watch, electric-dom3, events,
  transactions with e/Token, background work with e/Offload, forms,
  serialization, server setup, and the single-JVM development workflow.
user-invocable: false
---

# Electric

Layered on top of three baselines:

- `clojure` (in [clojure-skills](https://github.com/brackendev/clojure-skills)) for host-neutral style, threading, collections, atoms, dispatch, formatting, namespaces, REPL conventions.
- `clojurescript` (in [clojurescript-skills](https://github.com/brackendev/clojurescript-skills)) for JavaScript interop, externs inference, macro stage separation, `catch :default`, JS-flavored numbers and truthiness, and the `cljs.main` / `shadow-cljs` workflow surface.
- `clojure-jvm` (in [clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills)) for Java interop on the server, JVM-typed exceptions, `with-open`, refs / agents / STM, `alter-var-root`, and the Clojure CLI / `tools.build` / `clj-kondo` / `cljfmt` / `test-runner` / nREPL workflow.

This skill applies when the code uses Electric v3 (`hyperfiddle.electric3`). It owns the framework-specific deltas: Electric function definition, client and server siting, network transfer, tables, reactive state, `electric-dom3`, events, `e/Token` transactions, `e/Offload`, forms, serialization, server setup, and the starter-app project shape (`deps.edn` + `shadow-cljs.edn` + `src-dev/dev.cljc`).

This skill reflects Electric v3 release `v3-alpha-ca05cbc93` on Clojars (published 2026-05-19) and the official [`electric3-starter-app`](https://github.com/hyperfiddle/electric3-starter-app). Electric v3 is an alpha. The upstream changelog says minor breaking changes are allowed, and releases are named `v3-alpha-<git sha>`, so the version string does not show which release is newer. Clojars is the only release channel: GitHub has no releases, and the GitHub `master` branch lags the published jar. When an API detail matters, read the source inside the released jar, which is the authoritative reference for the version in use.

Upstream documentation: the [Tutorial and reference](https://docs.google.com/document/d/1frkb5URGFZUdWyCDl9moGFcPcLuDtW-M-HnCJOHkuDA/) document, the live examples at [electric.hyperfiddle.net](https://electric.hyperfiddle.net/), the example sources in [`electric-fiddle`](https://github.com/hyperfiddle/electric-fiddle) under `src/electric_tutorial/`, and the [changelog document](https://docs.google.com/document/d/17BE7xpV9bFoPq9scLJHCF8hD5gBm00K0Up45N3XeG5c/).

**License.** Electric v3 is not open source. Its POM declares the Hyperfiddle Business Source License. The upstream README says it is free for bootstrappers and non-commercial use and otherwise commercial (see the [license change announcement](https://tana.pub/lQwRvGRaQ7hM/electric-v3-license-change)). Activation is a login at dev-build compile time. The first dev compile prints a login link, and prod and CI builds need no login. Never try to automate, bypass, or script that login. Tell the user to follow the link themselves.

## Key Rules

1. **Capitalize every Electric function name.** The compiler inspects only the first letter of a symbol in call position (`electric-sym?` in `hyperfiddle/electric/impl/lang3.clj`): `(Foo x)` compiles as an Electric call and `(foo x)` compiles as a Clojure call. Name `e/defn` and named `e/fn` forms with a capital (`Main`, `TodoList`, `Typeahead`), and keep Clojure functions lowercase.
2. **Put Electric definitions in `.cljc` files.** Both the client build and the server build must see into every Electric body, including bodies that look single-sited. Plain Clojure and ClojureScript helpers may stay in `.clj` and `.cljs` files.
3. **Site effects, not bindings.** `let` bindings and Electric function arguments have no site and cause no transfer. Effects (host calls, DOM operations) and control flow (`if`, `cond`, `case`, `e/for`) are sited, and a value crosses the network only when an effect on the other site needs it. Site authority decisions on the server, because a server-sited `cond` decides the outcome and the client cannot override it.
4. **Transfer only serializable values.** Electric serializes values with transit and accepts only values that have a transit write handler. Atoms, functions, and most Java and JavaScript objects throw an `unserializable` error when code on the other site needs them. Prefix references with `!` (`!conn`, `!search`) and keep them on their own site. Suffix large collections with `!` (`xs!`) and keep them on the server, sending a windowed or diffed view instead.
5. **Render collections with `e/for` over `e/diff-by`.** `(e/for [x (e/server (e/diff-by :db/id xs))] ...)` keys each row by a stable id, mounts each row once, and updates rows in place. Diff on the site that holds the data, so only diffs cross the network.
6. **Hold local state in an atom read through `e/watch`.** `(let [!s (atom "") s (e/watch !s)] ...)`. Writing the atom from an expression, such as `(reset! !s (Input s))`, is a cycle through a side effect and is idiomatic.
7. **Run blocking or slow work in `e/Offload` with a Clojure thunk.** `(e/server (e/Offload #(query db search)))` runs the thunk on a thread and returns `(e/amb)` (no value) until the result arrives. It keeps the latest result while a newer call is pending. The thunk must be a Clojure function, which keeps Electric code out of it.
8. **Do not write `try` in Electric code.** The compiler rejects it with "try is TODO". Catch exceptions inside the Clojure thunk passed to `e/Offload` or inside a Clojure helper, and return a value that represents the outcome (for example `::ok`, or the error).
9. **Keep Electric forms out of Clojure `fn` bodies.** `e/client`, `e/server`, `e/watch`, Electric calls, and the other Electric macros throw "Electric code (...) inside a Clojure function" when they appear inside `fn` or `#(...)`. Write an `e/fn` for an Electric lambda, and keep Clojure callbacks (event extractors, `e/Offload` thunks) free of Electric forms.
10. **Model user-initiated transactions with `e/Token`.** `(e/Token event)` returns `[t err]`. `t` is non-nil while a transaction is in flight, and spending it with `(t)` for success or `(t err)` for rejection returns the control to the ready state. Disable the control while `t` is non-nil.
11. **Mount under a wrapper element.** The starter mounts with `(binding [dom/node js/document.body] (dom/div ...))`. The wrapper `div` is mandatory to keep node ordering correct ([electric#74](https://github.com/hyperfiddle/electric/issues/74)).
12. **Reference a `let` binding when you need its effect.** Electric's `let` is lazy, so a binding the body never uses is not evaluated, and a binding used only inside a branch runs only when that branch does. Upstream examples mention such bindings in the body to force them: `fizz buzz ; force lazy let statements`.
13. **Make reactive cycles reach a fixed point.** Electric recomputes an expression when `clojure.core/=` reports a changed argument. Round floating-point values at conversion points in cycles (for example, Celsius to Fahrenheit and back), or the cycle never settles and saturates the CPU.
14. **Parse DOM input values.** Input events deliver strings, even for `<input type="number">`. Parse with `parse-long`, `parse-double`, or `js/parseFloat` before computing with them.
15. **Develop in one JVM.** The Clojure REPL and the shadow-cljs compiler must share a single JVM, which the starter achieves by starting shadow-cljs from `src-dev/dev.cljc`. A second shadow-cljs process lets the client and server programs diverge, with undefined behavior.
16. **Set inline styles with `dom/props`.** `(dom/props {:style {:color :red}})` sets inline styles. `dom/style` creates a `<style>` element, which is what the upstream examples use for page CSS.
17. **Use `binding` only on Electric dynamic vars.** Declare them with `e/declare` (as `dom/node` and `e/http-request` are declared) and bind them with `binding` inside Electric code. Electric does not convey these bindings into host interop on its own. `e/*bindings*` holds bindings intended for host interop, and its docstring shows a `With-electric-bindings` helper that rebinds them around a Clojure function.

## Electric functions

`e/defn` defines an Electric function. Electric functions are reactive per expression: an expression recomputes only when one of its arguments changes.

```clojure
(ns app.greeting
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]))

(e/defn Greeting [name]
  (dom/p (dom/text "Hello, " name)))
```

- `e/defn` supports docstrings, multiple arities, and variadic arguments. It rejects `:pre` and `:post` condition maps with "`:pre` and `:post` conditions not supported".
- `e/fn` creates an Electric lambda. Give it a capitalized name when it is passed as an argument, such as `(e/fn Fields [form-fields] ...)`, and call it like any Electric function.
- `e/call` (and its compatibility alias `e/$`) calls an Electric function explicitly, which is useful when the function value is held in a lowercase local: `(e/call f x)`.
- `e/declare` declares Electric dynamic vars for `binding`, for example `(e/declare !conn)` followed by `(binding [!conn (e/server (ensure-conn!))] ...)`.
- Most Clojure and ClojureScript forms work inside Electric bodies, including `clojure.core`, `when`, `->>`, and destructuring. Statements do not necessarily run in source order, because evaluation follows the reactive graph. Macros that depend on runtime mutation or host concurrency (`future`, `push-thread-bindings`) do not work inside Electric code.

## Sites and network transfer

`e/client` and `e/server` mark the site of their body. Electric compiles the program into a client process and a server process that communicate over a websocket.

Electric v3 has three siting rules:

1. **Effects are sited, edges are not.** `let` names edges in the reactive graph, and edges have no site. A host call such as `(.isDirectory handle)` is an effect and runs on its site, so its arguments transfer to that site if they are not already there. Control flow is sited, so a server-sited `cond` is decided by the server.
2. **Auto-siting.** `electric-dom3` operators wrap their own DOM effect in `e/client` and leave the site of their children unchanged. `(e/server (dom/li (dom/text (.getName handle))))` calls `.getName` on the server and creates the `li` on the client.
3. **Dynamic siting.** An unsited expression inherits the site of its caller at runtime, so `(e/defn F [x] (inc x))` runs on whichever site calls it. Site platform effects explicitly anyway, because the reader cannot otherwise tell where a database call runs.

The upstream tutorial's directory tree uses one `e/server` and no explicit `e/client`:

```clojure
(e/defn DirTree [handle search]
  (e/server
    (let [name_ (.getName handle)]
      (cond
        (.isDirectory handle)
        (dom/li (dom/text name_)
          (dom/ul
            (let [children (.listFiles handle)]
              (e/for [handle (e/diff-by identity children)]
                (DirTree handle search)))))

        (and (.isFile handle) (includes-str? name_ search))
        (dom/li (dom/text name_))))))
```

## Tables and `e/amb`

Electric values are tables: reactive sequences of zero or more items.

- `(e/amb)` is the empty table. It means "no value", and expressions that depend on it do not run. A pending `e/Offload` returns `(e/amb)`.
- `(e/amb a b)` concatenates tables. Branches commonly return `(e/amb)` to contribute nothing: `(if (pred x) x (e/amb))`.
- `(e/for [x xs] body)` calls `body` once per item of table `xs` (and over the cartesian product when given several bindings), returning the concatenation of the results.
- `(e/diff-by kf coll)` turns a Clojure collection into a table whose items are stabilized by `kf`. `e/diff` uses `identity`. `(e/Range n)` produces a table of numbers.
- `(e/as-vec table)` collects a table into one vector. `e/for-by` combines `e/diff-by`, `e/for`, and `e/as-vec`.
- `e/Count`, `e/Filter`, `e/Some?`, and the `e/When` macro operate on tables. `e/Reconcile` collapses a branch switch into a single change, which prevents duplicate effects from code like `(if x a a)`.

## Reactive state

- `(e/watch !ref)` returns the current value of an atom or other watchable reference and errors with "Not watchable" otherwise.
- `(e/with-cycle [s init] body)` binds `s` to the previous result of `body`, starting from `init`. It is an atom plus `e/watch`.
- `(e/input flow)` returns the current state of a missionary continuous flow, and `e/join` consumes an incremental sequence.
- `(e/snapshot x)` captures the first non-pending value of `x` and ignores later updates.
- `(e/System-time-ms)` and `(e/System-time-secs)` are reactive clocks. They return `(e/amb)` while the browser tab is hidden.

## DOM (`electric-dom3`)

`electric-dom3` has no virtual DOM. Its operators call the native DOM methods and mutate nodes in place.

- Element macros (`dom/div`, `dom/button`, `dom/input`, and the rest of the HTML elements) mount an element under the current `dom/node`, run their body inside it, and return the value of the last child expression.
- `dom/text` mounts one text node per argument. Text nodes are explicit, and changing an argument updates the existing node's text.
- `(dom/props {...})` sets attributes and properties on the element in scope. `nil` removes an attribute, `:class` accepts a string or a collection, and `:style` accepts a map that may include CSS variables (`{:--my-color :red}`).
- `dom/node` is the dynamic var holding the current element. `(dom/With element Body)` mounts an Electric UI inside an existing element, for library integration. `(dom/element :tag ...)` mounts an arbitrary tag.

## Events

`(dom/On event-type f init)` listens for events on the element in scope and returns the latest value of `(f event)`, starting with `init`. An input is an expression whose value is its current state, so no change callback or atom is needed:

```clojure
(e/defn TextInput [v]
  (dom/input (dom/props {:value v})
    (dom/On "input" #(-> % .-target .-value) v)))
```

- `f` is a Clojure function that extracts a value from the event, and it is the place to call `.preventDefault` or `.stopPropagation`. `dom/On` sites `f` on the client automatically.
- `(dom/On-all event-type)` returns a table with one `[t event]` item per event that is still in flight, for concurrent handling.
- `dom/Mouse-over?`, `dom/Focused?`, `dom/Focused-in?`, and `dom/Mouse-down?` return reactive booleans for the element in scope.

## Transactions (`e/Token`)

`e/Token` turns a transactional intent into a value. The token is non-nil while the transaction is in flight, further events are ignored until it is spent, and an error can flow back to the originating control. This is the upstream tutorial's button:

```clojure
#?(:clj (defn transaction [] (Thread/sleep 800) (rand-nth [true false])))

(e/defn TxButton [label]
  (dom/button (dom/text label)
    (let [e (dom/On "click" identity nil)
          [?t ?err] (e/Token e)]
      (when ?err (dom/text " " ?err))
      (dom/props {:aria-busy (some? ?t)
                  :disabled (some? ?t)
                  :aria-invalid (some? ?err)})
      ?t)))

(e/defn App []
  (when-some [t (TxButton "transact!")]
    (let [succeeded? (e/server (e/Offload #(transaction)))]
      (if succeeded?
        (t)                 ; success: spend the token
        (t "rejected")))))  ; failure: spend it with an error for the control
```

## Background work (`e/Offload`)

- `(e/Offload f)` and `(e/Offload f executor)` run the thunk `f` on a thread (missionary's blocking executor by default), return `(e/amb)` until the first result, and keep showing the latest result while a newer call is pending.
- `e/Offload-reset` returns to `(e/amb)` whenever `f` changes, which suits a spinner. `e/Offload-latch` is the behavior `e/Offload` uses.
- `e/Task` runs a missionary task.
- Catch exceptions inside the thunk and return a sentinel, as the upstream form example does:

```clojure
(e/defn User-form-submit [id str1]
  (e/server
    (let [tx [{:db/id id :user/str1 str1}]]
      (e/Offload #(try (transact! !conn tx)
                    ::ok
                    (catch InterruptedException _ ::interrupted)
                    (catch Exception e (doto ::fail (prn e))))))))
```

## Forms (`electric-forms5`)

`hyperfiddle.electric-forms5` is the current forms namespace. `electric-forms3` is deprecated, and `forms4` and `forms0` were removed. Upstream describes the forms library as tricky to use and still in progress, so copy patterns from the upstream examples rather than inventing call shapes.

- Controlled inputs that return their current value: `Input`, `Input*`, `Checkbox`, `Checkbox*`.
- Transactional controls and containers: `Input!`, `Checkbox!`, `Form!` (with `:Parse` to turn dirty fields into a command), `SubmitButton!`, `DiscardButton!`, `Button!`, and `Service`.
- Reference example: `src/electric_tutorial/form_service.cljc` in `electric-fiddle`, which builds a `Form!`, parses it into a command, and runs the command on the server with `e/Offload`.

## Serialization

Values that cross sites go through transit. Register handlers for custom types on both entry points, using the same handler maps for each:

```clojure
(e/boot-server
  {:cognitect.transit/read-handlers
   {"app/CustomType" (cognitect.transit/read-handler (fn [[x]] (CustomType. x)))}
   :cognitect.transit/write-handlers
   {CustomType (cognitect.transit/write-handler (constantly "app/CustomType") (fn [o] [(.-x o)]))}}
  Main (e/server ring-request))
```

Pass the same options to `e/boot-client`. Without a write handler, the value throws an `unserializable` error when the other site needs it.

## Server setup

The starter's `main.cljc` boots both sites from one function with the same arity on each side:

```clojure
(defn electric-boot [ring-request]
  #?(:clj  (e/boot-server {} Main (e/server ring-request))
     :cljs (e/boot-client {} Main (e/server (e/amb)))))
```

- The client passes `(e/server (e/amb))` as a no-value placeholder where the server passes the Ring request.
- `hyperfiddle.electric-ring-adapter3/wrap-electric-websocket` installs the Electric server in the Ring middleware stack. `hyperfiddle.electric-httpkit-adapter3` and `hyperfiddle.electric-jetty9-ring-adapter3` serve other servers.
- In production, `wrap-reject-stale-client` rejects a client whose version differs from the server's, and the client boot is wrapped in `hyperfiddle.electric-client3/reload-when-stale` so the page reloads to fetch new assets. The version comes from `resources/electric-manifest.edn`, written by the build.
- The `:dev` shadow-cljs build must list `(hyperfiddle.electric.shadow-cljs.hooks3/reload-clj)` under `:build-hooks`. Without it, `e/boot-client` prints "Electric compilation error: a required shadow build hook is missing".
- `e/http-request` is an Electric dynamic var bound to the HTTP request of the page.

## Project Workflow

The starter-app layout (`src`, `src-dev`, `src-prod`, `src-build`), the `deps.edn` aliases, the `shadow-cljs.edn` builds, the development loop and its login step, the production build and its version manifest, linting and formatting configuration, testing notes, and how to check for a newer release live in `references/project-workflows.md`. Load it on demand.

## Electric v2 code

Electric v2 (`com.hyperfiddle/electric` `v2-alpha-*`) received its last release in January 2024 and was licensed EPL-2.0. Its API differs from v3, so do not mix the two. When a project still uses v2, apply v2 forms consistently or migrate it to v3, and treat v2 tutorials and blog posts as v2-only.

| Electric v2 | Electric v3 |
|-------------|-------------|
| `hyperfiddle.electric`, `hyperfiddle.electric-dom2` | `hyperfiddle.electric3`, `hyperfiddle.electric-dom3` |
| Calling Electric functions with `new` or `(F.)` | Calling them with a capitalized name, `(F)`; `new` constructs host classes |
| `e/def` | `e/declare` plus `binding` |
| `dom/on` | `dom/On` |
| `(e/for-by key [x xs] ...)` | `(e/for [x (e/diff-by key xs)] ...)` |
| `e/offload`, `e/wrap` | `e/Offload` with a thunk |
| `hyperfiddle.electric-ui4`, `electric-ui5` | `hyperfiddle.electric-forms5` |

## Formatting and linting

Upstream asks for Tonsky two-space indentation, and the starter includes the configuration:

```clojure
{:indents {#re ".*" [[:inner 0]]}
 :remove-surrounding-whitespace? false
 :remove-trailing-whitespace? false
 :remove-consecutive-blank-lines? false}
```

The catch-all `:indents` entry deliberately replaces cljfmt's default rules, which matches the `clojure` skill's two-space indentation rule. Follow the project's `.cljfmt.edn`. Electric exports clj-kondo configuration for its macros, which `clj-kondo --copy-configs --dependencies` imports into `.clj-kondo/imports/hyperfiddle/electric/`.

## Asking upstream for help

The Hyperfiddle team asks people not to send them unverified AI-generated code to debug. Before suggesting that the user ask in `#hyperfiddle` on the Clojurians Slack or open an upstream issue, reduce the problem to a minimal reproduction based on the starter app, confirm it against the source of the Electric version in use, and let the user decide whether to post it.

## Gotchas

### REPL redefinition does not hot-reload Electric code

The client and server programs must come from the same build, so redefining an `e/defn` at the REPL does not update the running app. Save the file instead. The Electric build hook recompiles the client and reloads the changed `.cljc` files on the JVM together.

### Occasional stale ClojureScript compile state

Upstream reports occasional ClojureScript compile errors that clear after deleting `.shadow-cljs/` and restarting the dev build. Try that before debugging a compile error that does not match the source.

### Glitches are a known open issue

Upstream lists glitches (for example, `(when ?x (prn ?x))` printing `nil`) as a known major issue in the current alpha. When reasoning about intermediate values, do not assume glitch-free propagation.

### The dev server and other services can contend for port 8080

The starter's dev and prod servers bind `0.0.0.0:8080`. Another service already listening on port 8080 can stop the server from starting, or, on macOS, keep answering some addresses: a service bound to `127.0.0.1:8080` answers requests to `127.0.0.1` and `0.0.0.0` while the Electric server answers `localhost`. Check `lsof -nP -iTCP:8080 -sTCP:LISTEN` when the page does not match the code.
