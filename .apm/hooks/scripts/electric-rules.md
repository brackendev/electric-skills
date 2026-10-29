# Electric rules from electric-skills

This is an Electric v3 project. The Iron Laws below come from the `electric` skill and add to the `clojure` baseline laws. Never write code that breaks one. Before writing, reviewing, or refactoring code in this project, load the `electric` skill along with the `clojure`, `clojurescript`, and `clojure-jvm` skills. The skills hold the full laws, their exceptions, and the references.

## Iron Laws

- ELEC-1: Capitalize every Electric function name, because the compiler treats a lowercase call as a Clojure call. Keep Clojure functions lowercase.
- ELEC-2: Send only transit-serializable values across sites. Prefix references with `!` and keep them on their own site, and keep large collections on the server.
- ELEC-3: Run blocking server calls inside `e/Offload`, and make a thunk with side effects tolerate interruption.
- ELEC-4: Spend every `e/Token` on every outcome: call `(t)` on success and `(t err)` on failure, including when the work throws.
- ELEC-5: Site security decisions on the server. Put authorization checks and the data they guard inside `e/server`.
