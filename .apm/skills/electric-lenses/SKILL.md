---
name: electric-lenses
description: >-
  Translate code-lenses design philosophies into Electric-specific patterns.
  Auto-triggers when working on Electric v3 code (hyperfiddle.electric3,
  hyperfiddle.electric-dom3, e/defn, e/client, e/server, e/for, e/Token,
  e/Offload) alongside code-lenses skills. Layers on top of clojure-lenses
  (in clojure-skills) and clojurescript-lenses (in clojurescript-skills) and
  records only the Electric-specific deltas. Covers the four default
  code-lenses philosophies (grug, Honest Code, Tidy First, Parse Don't
  Validate) and the two opt-in philosophies (APOSD, Legacy Code) that
  activate when their lens is added with `+aposd` or `+legacy-code` or
  invoked directly.
user-invocable: false
---

# Code Lenses for Electric

Layered on top of [clojure-lenses](https://github.com/brackendev/clojure-skills) (in `clojure-skills`) for host-neutral translations and [clojurescript-lenses](https://github.com/brackendev/clojurescript-skills) (in `clojurescript-skills`) for the JavaScript-host deltas. This skill records only the Electric-specific additions: reactive dataflow, the client and server site boundary, network transfer, tables and `e/amb`, `e/Token` transactions, `e/Offload`, lazy `let`, `electric-forms5`, and migration from Electric v2. The `electric` skill holds the API details these translations assume.

The default code-lenses review set is `grug`, `honest-code`, `tidy-first`, and `parse-dont-validate`. `aposd` and `legacy-code` are opt-in (added with `+aposd` or `+legacy-code`, or invoked directly). The sections below apply when the corresponding lens is active. APOSD and Legacy Code translations are present so the lens can use them when explicitly invoked, but they are not part of the default trigger set.

When the [code-lenses](https://github.com/brackendev/code-lenses) plugin is active in an Electric project, apply the baseline `clojure-lenses` and `clojurescript-lenses` translations first, then use this skill for the Electric-specific additions below.

## Grug Brain

- An Electric input is an expression that returns its current value. `(dom/On "input" #(-> % .-target .-value) v)` replaces the change callback, the state atom, and the synchronization code. Reach for an atom only when a value must flow upward in the program, and then use the atom plus `e/watch` pattern.
- A server query inside `e/server` replaces an API endpoint, a client fetch, and a client cache. Do not add a REST or RPC layer between two parts of the same Electric program.
- `e/for` over `e/diff-by` is the list primitive. A hand-written diff, a client-side index, or a manual row cache is complexity the runtime already provides.
- Keep sites explicit at platform effects (database calls, filesystem calls, DOM effects) and let the rest of the code stay unsited. Wrapping every expression in `e/client` or `e/server` adds noise and hides which calls move data.
- Copy the upstream starter and tutorial examples before inventing structure. Electric is an alpha with unusual semantics, so a pattern that is not in the examples is a guess.

## A Philosophy of Software Design

- An Electric function is a deep module that spans both sites. A component that queries the server and renders the result hides the network, the serialization, and the incremental updates behind one call. Keep that boundary inside the function rather than splitting it into a client half and a server half.
- The site boundary is an information-hiding boundary. References (`!conn`, `!state`) and large collections (`xs!`) stay on the site that owns them. Callers receive derived values or tables, not the reference.
- Dynamic siting makes generic components possible. A control that takes an `e/fn` for its options, such as a typeahead, does not need to know which site produces the options. Site the platform effect at the call site that knows.
- Errors at the right layer: catch exceptions inside the `e/Offload` thunk or a Clojure helper, convert them to values, and let the `e/Token` error channel carry them back to the control that started the transaction.

## Tidy First

- Name and capitalize an `e/fn` when extracting it. An extracted lowercase Electric lambda is compiled as a Clojure call at its call sites.
- Extract plain computation into Clojure functions in the same `.cljc` file (or a `.clj` or `.cljs` file when it is single-platform) and call them from Electric code. Clojure helpers are easier to test and do not need the Electric compiler.
- A `let` binding that exists only for its effect must stay referenced in the body. Removing an apparently unused binding during a tidying removes its effect, because Electric's `let` is lazy. Keep the forcing reference (`fizz buzz ; force lazy let`) next to its comment.
- Do not reorder statements on the assumption that source order is execution order. Electric evaluation follows the reactive graph. A tidying that relies on order needs a data dependency instead.
- Move a misplaced `e/server` outward to the smallest expression that contains the platform effect. Moving it inward can add transfers, and moving it too far outward can pull client effects into server authority.

## Parse, Don't Validate

- The server site is the trust boundary. Parse and validate commands inside `e/server` before transacting, as the upstream form example does in its `User-form-submit` function.
- `Form!` with `:Parse` turns dirty form fields into a command value. Treat the command as the parsed form of the user's intent, and transact only commands.
- DOM inputs deliver strings, including `<input type="number">`. Parse once at the input with `parse-long`, `parse-double`, or `js/parseFloat`, and keep numbers inside the program.
- Serializability is a type boundary. Only values with transit handlers can cross sites, so a custom type that must cross needs read and write handlers on both `e/boot-server` and `e/boot-client`, or a plain-data representation.

## Honest Code

- The site marker is honest about where an effect runs. A database call without an explicit `e/server` relies on dynamic siting, which is correct but hides from the reader where the call runs. Site platform effects explicitly.
- `e/Token` exposes transaction state as a value: ready (`nil`), busy (non-nil), and rejected (an error). Disable the control while the token is busy, and spend the token exactly once with `(t)` or `(t err)`. Hiding the state machine behind a callback makes the busy and error states invisible.
- `e/Offload` is the effect edge for blocking work. The thunk is a Clojure function, so Electric code cannot leak into it, and the pending state is `(e/amb)`, which dependents see as "no value yet" rather than a fake default.
- `(reset! !s (Input s))` is an honest cycle through a side effect. It is a statement that runs when its argument changes, not a hidden callback.
- Let It Crash, Electric edition: Electric code cannot contain `try`, so do not wrap Electric bodies to swallow failures. Catch in the Clojure thunk, return a value that names the outcome, and show it to the user through the token's error channel.

## Legacy Code

- Electric v2 and v3 differ in namespaces (`hyperfiddle.electric` versus `hyperfiddle.electric3`), call syntax (`new` versus capitalized calls), lists (`e/for-by` versus `e/for` over `e/diff-by`), and background work (`e/offload` versus `e/Offload`). Migrate one namespace at a time and do not mix v2 and v3 forms in one file.
- Seams in Electric programs: Electric dynamic vars declared with `e/declare` and set with `binding` (the starter binds `dom/node`, and the form example binds `!conn` and its debug flags), the `e/fn` arguments of generic components, and the Clojure helpers that Electric functions call.
- Characterization tests belong on the Clojure helpers. The Electric test tools upstream uses (`hyperfiddle.rcf` and `electric-local-def3`) are not a documented public API, so test business logic in plain `clojure.test` and check the Electric code in the running app.
- Effect sketching: changing what a server query returns ripples through every `e/diff-by` key function and every client expression that reads the rows. Keep the key function stable across the change, or rows remount.

## Gotchas

- "Extract helper" applied to a block containing `e/client`, `e/server`, or an Electric call must produce an `e/defn` or `e/fn`, not a Clojure `defn`. Electric forms inside a Clojure function throw "Electric code (...) inside a Clojure function".
- "Replace the atom with a pure function" does not apply to the atom plus `e/watch` cycle when the value must flow upward in the program. Electric has no language-level binding recursion, so the atom is the idiom.
- `try` and `:pre` / `:post` maps are not supported in Electric functions. A lens suggestion to add either belongs in a Clojure helper instead.
- Lens advice about server-side rendering, virtual DOM diffing, or REST endpoint design does not translate. Electric renders on the client with direct DOM effects and connects the sites over one websocket.
