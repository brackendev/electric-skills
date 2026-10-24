# Skill Conventions

This document defines the argument grammar, scope vocabulary, and mutation defaults that every user-invocable skill in `electric-skills` follows. A reader who learns one skill should be able to predict every other skill. New skills follow this document.

These conventions apply to user-invocable skills (`user-invocable: true` in frontmatter). Model-invocable reference skills with no argument surface (currently `electric` and `electric-lenses`) carry no argument grammar and are exempt from rules 1 and 2.

## The four rules

### Rule 1: argument grammar

User-invocable skills accept natural-language keywords and bare paths. The single sanctioned flag is `--report`. No other `--name` flags exist.

Skill-specific modifiers are bare phrases, not flags. Examples used in this plugin:

- `lint`, `format`, `test`, `advanced`, `dry`: step keywords for `/electric-fix`
- `all`, `<path>`, `<glob>`: shared scope keywords listed in rule 2

Each skill documents its own modifiers in its `## Arguments` table.

Exemptions are listed in the [Exemptions](#exemptions) section with a reason. The standard exemption pattern is a scaffolding skill that needs a required positional argument because no useful default exists.

### Rule 2: scope vocabulary

Skills that operate on files, diffs, pull requests, or commit messages share these core rows in their `## Arguments` table:

| Input             | Target                                                          |
|-------------------|-----------------------------------------------------------------|
| (no argument)     | The skill's narrowest useful default                            |
| `all`             | Widen the selected scope to its maximum                         |
| `<path>` `<glob>` | Operate on those files or directories                           |

Each skill states what `all` resolves to in concrete terms (whole project, the full codebase, every detected step). The keyword has one uniform meaning: widen the selected scope to the maximum. The unit varies per skill.

A skill whose narrowest useful default is already maximum scope still accepts `all` for family consistency. The table row reads "Same as (no argument); accepted for family consistency."

Opt-in rows (`#N` or PR URL, `pr`, `commit`) appear only when the skill genuinely supports them. None of the current `electric-skills` skills operate on pull requests or commit messages, so the opt-in rows do not appear in this plugin today.

### Rule 3: mutation is the default

Skills that can mutate the workspace apply safe, deterministic source and project-configuration edits when invoked, and they report findings that require judgment or lack a safe rewrite. The operator passes `--report` to suppress those intentional edits and instead receive a description of what the skill would change.

Report-only mode suppresses intentional source and project-configuration edits. It does not suppress the incidental output of verification steps. Compilation, release builds, and linter bootstrap may still write caches and build artifacts under `--report`. Each skill discloses any such write in its `## Mutation` section.

Only the literal token `--report` enables report-only mode. Natural-language phrases ("preview", "dry run", "rehearse") are scope input or step keywords, not mode triggers. A skill that conflates them is wrong.

Command suffixes reinforce the default. The family follows a noun-first `<target>-<verb>` pattern, so the trailing verb signals behavior. Skills with suffix `-fix`, `-new`, `-upgrade` (verbs that imply action) mutate by default. In this package they are `/electric-fix` and `/electric-new`. Skills with suffix `-review` (a reading verb) are pure-report, and this package has none.

### Rule 4: vendored and generated paths are excluded by default

A mutating skill that walks the workspace excludes vendored, generated, and dependency-locked paths from its scope. The operator opts back in per file by naming the path explicitly. No new `--name` flag is introduced. The override uses Rule 1's `<path>` `<glob>` row.

The boundary statement: Rule 4 applies to mutating skills that discover candidate files from the workspace. It does not apply to skills whose target set is defined by an explicit project operation, template, dependency model, git operation, or named path argument.

**Exclusion set.** Two filters apply together. A path that matches either filter is excluded.

1. `.gitignore`-matched paths. Anything excluded by the project's `.gitignore`, `.git/info/exclude`, or the global excludes file is out of scope. Resolve membership with `git check-ignore -v -- <path>`.
2. Hardcoded floor (excluded even when the project tracks the path):

   | Category | Patterns |
   |----------|----------|
   | Dependency directories | `node_modules/`, `vendor/`, `third_party/`, `.bundle/` |
   | Build outputs | `target/`, `build/`, `dist/`, `out/`, `.shadow-cljs/`, `cljd-out/` |
   | Lock files | `*.lock`, `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`, `Gemfile.lock`, `Cargo.lock`, `poetry.lock`, `composer.lock` |

**Override.** When the operator names a vendored or generated file in the arguments through the `<path>` `<glob>` row, the filter does not apply to that target. Naming the path directly is treated as informed consent. The filter remains active for broad scopes: `(no argument)`, `all`, or a directory whose contents include vendored sub-paths.

**Reporting.** The skill includes a single "skipped N vendored or generated paths" line in its results when the filter excluded any path. Under `--report`, the skill emits the full list so the operator can audit scope.

**Scope of Rule 4 in this plugin.** Applies to `/electric-fix`, which discovers candidate files from the workspace. Exempt: `/electric-new` is scaffolding, and its target set is the cloned starter app, not workspace discovery. The `advanced` step of `/electric-fix` writes the client build output under `resources/public/<app>/js/` and `resources/electric-manifest.edn`. Build-artifact writes are out of scope for Rule 4 because the rule governs source mutation, not compiler outputs.

The file-aware mutating skills include a one-line reference to Rule 4 in their own `## Mutation` or `## Scope` section.

## Classification

Every user-invocable skill falls into one of two classes.

**Mutating skill** -- default behavior. May carry `--report` when preview is useful. May omit `--report` when preview is meaningless (the operator inspects `git diff` after the fact) or the action is small and reversible.

**Pure report** -- never mutates. No `--report` flag because there is nothing to invert.

A skill is classified by its actual behavior, not by its name. If the name and behavior disagree, the rename is the fix. The classification appears in the skill's own description so the operator knows what to expect.

### Skills in this plugin

| Skill                   | Class           | `--report` available? | Notes |
|-------------------------|-----------------|------------------------|-------|
| `/electric-fix`         | Mutating        | Yes                    | `format` step runs `cljfmt fix` by default. `--report` replaces it with `cljfmt check`. `lint`, `test`, `advanced`, and `dry` are pure-read of source regardless. The `advanced` step writes the client build output under `resources/public/<app>/js/` and `resources/electric-manifest.edn` (build artifacts, not source). |
| `/electric-new`         | Mutating        | No                     | Clones and renames the starter app. Preview the side effects by reading `SKILL.md`. |

The model-only reference skills (`electric`, `electric-lenses`) have no argument surface and are not classified here.

## Section structure

User-invocable skills with an argument surface use these section conventions:

- `## Arguments` -- required. Contains the canonical scope table.
- `## Scope` -- optional. Add only when detection order, fallback, or base-branch resolution exceeds what the table can express.
- `## Mutation` -- optional. Add only when mutation behavior needs clarification beyond a single table row (for example, when only one of several steps writes source, or when a step writes build artifacts but no source).

The section name `## Customization` is retired.

## Worked examples

### `/electric-fix` -- mutating skill with `--report`

```
/electric-fix                     # all five steps; format writes
/electric-fix lint                # lint only (pure-read; no source writes)
/electric-fix format              # format step; writes via cljfmt fix
/electric-fix lint test           # combined step keywords
/electric-fix --report            # all five steps; format reads via cljfmt check
/electric-fix format --report     # format step; no source writes
/electric-fix all                 # synonym for (no argument)
```

The skill writes source when the `format` step runs without `--report`. With `--report`, the format step runs `cljfmt check`, which reports diffs without writing. The other four steps (`lint`, `test`, `advanced`, `dry`) are pure-read of source regardless. The `advanced` step writes build output under `resources/public/<app>/js/` and `resources/electric-manifest.edn` (the `build-client` production build). That is a build artifact, not a source mutation, and is unaffected by `--report`. The `## Mutation` section in the skill body documents this asymmetry.

### `/electric-new` -- mutating skill, exemption from `all`/path rows

```
/electric-new my-app               # creates project tree at ./my-app
/electric-new                      # prompts the operator for a project name
```

`<project-name>` is a required positional argument. The skill omits the `all` and `<path>` rows because they would not be meaningful for scaffolding. It also omits `--report` because preview is meaningless: the operator reads `SKILL.md` to see what files will be written. The exemption is listed below.

## Exemptions

`/electric-new` accepts a required positional `<project-name>` because no useful default exists for scaffolding. It omits the `all` and `<path>` scope rows and the `--report` flag for the same reason. The `## Arguments` table in its `SKILL.md` documents the positional grammar.

## Ambiguity notes

**`(no argument)` outside a git worktree.** A skill whose narrowest useful default depends on git state (for example, "review changed files") must define the fallback when no git worktree is present. The expected fallback is to ask the operator what to review rather than to widen silently to `all`. In this plugin, `/electric-fix` derives scope from `deps.edn` and the project tree rather than from a diff, so the question does not apply.

**`commit` versus staged-and-unstaged state.** When a future skill accepts `commit` as a scope keyword, it must state whether `commit` means the most recent commit, the staged tree, or the staged-plus-unstaged working tree. The expected default is the most recent commit. None of the current skills carry this scope.

**`--report` versus natural-language synonyms.** "Preview", "dry run", "rehearse", and similar phrases are scope input or step keywords (or operator chatter), never mode triggers. Only the literal `--report` token disables writes. The `/electric-fix` step keyword `dry` is the dry4clj duplicate-form scan, not a dry-run mode. The conflict is named here so that operators do not read `dry` as "dry run".

**Step keywords versus scope keywords.** A skill like `/electric-fix` accepts step keywords (`lint`, `format`, `test`, `advanced`, `dry`) that select work to run, and scope keywords (`all`) that widen scope. Step keywords are skill-specific and listed in the skill's own table. Scope keywords are shared and listed here. When a skill has both, the `## Arguments` table lists both in separate rows.

**Tool-level flags the skill calls internally.** A skill may invoke a tool that itself uses POSIX flags (for example, `clj-kondo --lint`, `cljfmt fix`, `cljfmt check`, `clj -X:build:prod build-client`, `git clone --depth 1`, `clj -P`, `curl -sSL`). Those are tool-level flags, not skill flags, and do not count against rule 1. The skill body should disambiguate when a tool flag could be mistaken for a skill flag.

## Author checklist

When adding or modifying a user-invocable skill, confirm each item before committing.

- [ ] Skill has a `## Arguments` section (or is listed under [Exemptions](#exemptions)).
- [ ] Scope rows match the canonical table, and opt-in rows appear only where the skill supports them.
- [ ] If the skill mutates, the command suffix signals it (`-fix`, `-new`, `-upgrade`, `-deploy`, `-test`, `-sync`, `-prune`, `-rebuild`, `-create`, `-apply`).
- [ ] If the skill mutates and preview is useful, `--report` is documented.
- [ ] If the skill is pure-report, the suffix signals it (`-review`, `-audit`, `-check`) and the skill has no `--report` flag.
- [ ] No `## Customization` section.
- [ ] No `--name` flags other than `--report`. Tool-level flags the skill calls internally (for example, `cljfmt check`, `clj-kondo --lint`, `git clone --depth 1`) are not skill flags and do not count.
- [ ] Mutating skills that walk the workspace include a one-line Rule 4 reference in their `## Mutation` or `## Scope` section. Exempt skills (scaffolding, dependency upgrade, fixed targets, git operations, named paths) carry no reference.
- [ ] Frontmatter `name` matches the skill's directory name.
- [ ] `agents/openai.yaml` `default_prompt` references the current command name.
- [ ] `CHANGELOG.md` records the change under `[Unreleased]` when the change is user-facing.
