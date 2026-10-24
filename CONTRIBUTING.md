# Contributing

For people working on the plugin source. End-user install instructions live in [README.md](README.md).

## Layout

| Path | Purpose |
|------|---------|
| `apm.yml` | Package manifest (`type: skill`, no MCP dependencies). |
| `.apm/skills/<name>/` | Canonical skill source. APM deploys from here. Each directory holds `SKILL.md` plus optional `agents/openai.yaml`, `references/`, `scripts/`, `examples/`. |
| `.opencode/skills` | Symlink to `../.apm/skills`. OpenCode finds it from any directory in the repository, so an OpenCode session loads the canonical skills and their supporting files without an APM install. |
| `opencode.jsonc` | Local OpenCode configuration. |
| `README.md` | End-user documentation. |
| `CHANGELOG.md` | User-facing changes per version. |
| `CONVENTIONS.md` | Canonical argument grammar, scope vocabulary, and mutation defaults for every user-invocable skill. |
| `AGENTS.md`, `GEMINI.md`, `TODO.md` | Local agent instructions (`GEMINI.md` is a symlink to `AGENTS.md`) and working notes. Gitignored globally and never committed. |

## APM lockfile rule

Do not add `.claude-plugin/`, `.codex-plugin/`, `.agents/plugins/marketplace.json`, or a root `plugin.json`. Any of those change the APM lockfile classification from `apm_package` to `marketplace_plugin` and suppress skill deployment to every runtime.

## Adding or modifying a skill

1. Edit `.apm/skills/<name>/SKILL.md`.
2. Update `README.md` if the change is user-facing.
3. Add a `CHANGELOG.md` entry under `[Unreleased]` for every change to a published file, including documentation-only changes.
4. Increment the `version` field in `apm.yml`.

## Validation

Configuration files:

```bash
python3 -m json.tool opencode.jsonc >/dev/null
python3 -c "import yaml, pathlib; yaml.safe_load(pathlib.Path('apm.yml').read_text())"
python3 -c "import yaml, pathlib; [yaml.safe_load(p.read_text()) for p in pathlib.Path('.apm/skills').glob('*/agents/openai.yaml')]"
```

Runtime install (requires `apm` and the runtime CLIs you want to verify: `claude`, `codex`, `copilot`, `gemini`, `opencode`):

- Run `apm install --target all`, `apm update`, and `apm uninstall` in a clean temporary project. Pre-create the runtime roots: `.agents/`, `.claude/`, `.cursor/`, `.opencode/`, `.gemini/`, `.github/`, `.windsurf/`. With several runtime roots present, APM stops with "Multiple harnesses detected" unless the install names a target. `apm update` reuses the targets that the install records in the project's `apm.yml`.
- Exercise user scope with `apm install brackendev/electric-skills -g [--target ...]` and `apm uninstall brackendev/electric-skills -g`. A local filesystem path (`apm install /absolute/path -g`) is also accepted.
- Confirm OpenCode deployment from the temporary project with `opencode api GET "/api/skill?location[directory]=$PWD"`, which lists the skills OpenCode discovers and their source paths. The first request for a directory can return an empty list while OpenCode loads the skills, so repeat it before reading the result.

## Skill conventions

For the argument grammar, scope vocabulary, and mutation defaults that every user-invocable skill follows, see [CONVENTIONS.md](CONVENTIONS.md).

| Setting | When to use |
|---------|-------------|
| `user-invocable: true`, `disable-model-invocation: true` | User-only slash command, for scaffolding, dependency upgrades, and any skill whose changes reach beyond the working tree or are hard to undo (for example `electric-new`). |
| `user-invocable: true`, no `disable-model-invocation` | Slash command the model can also run from a natural-language request, for a fixer whose edits stay in the working tree (for example `electric-fix`). Write the description as a trigger. |
| `user-invocable: false` | Model-invoked from conversation context (for example `electric`, `electric-lenses`). |

Every skill carries `agents/openai.yaml` whose `policy.allow_implicit_invocation` matches the table above: `false` for a user-only skill and `true` for every skill the model can run. Skills in this package use the Hyperfiddle lavender brand color, `#B7A4C2`, taken from the Hyperfiddle logo because Electric has no brand color of its own, so the runtimes can distinguish Electric-specific guidance from the host-neutral `clojure` baseline (Clojure logo blue, `#5881D8`), the ClojureScript-specific `clojurescript` skill (JavaScript yellow, `#F7DF1E`), the JVM `clojure-jvm` skill (Java orange, `#E76F00`), the Biff framework skill (Biff indigo, `#4338CA`), the Fulcro framework skill (Fulcro teal, `#009E9E`), and the ClojureDart skill (Flutter blue, `#02569B`).
