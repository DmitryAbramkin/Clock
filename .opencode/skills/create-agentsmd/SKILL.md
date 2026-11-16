---
name: create-agentsmd
description: >-
  Audits and updates the root AGENTS.md so coding agents get accurate setup,
  build, release, documentation and security instructions for Clock. Use when
  creating or refreshing AGENTS.md, fixing stale agent instructions, or checking
  that the file matches the repository.
license: MIT
metadata:
  author: GitHub, Inc.; adapted for Clock
  source: https://github.com/github/awesome-copilot/tree/main/skills/create-agentsmd
  version: '1.0.0-clock'
---

# Create AGENTS.md

Create or update the root `AGENTS.md` following the public guidance at
<https://agents.md/> and the conventions of this repository.

## The existing file is the source of truth

`AGENTS.md` already exists and contains hard project rules: languages,
versioning, release process, documentation sync, dependency and permission
restrictions, agent skills policy, alarm diagnostics. **Never regenerate the
file from the upstream template and overwrite it.** Read it, find drift, and
produce a minimal diff.

Precedence when instructions differ:

1. the user's explicit request for the current task;
2. `AGENTS.md`;
3. skill recommendations.

Do not weaken or delete project rules without an explicit request.

## Principles

- `AGENTS.md` is read at the start of every agent session: keep it short and
  actionable. Aim for roughly 120 lines; move narrative details to
  `PROJECT_NOTES.md` and link them.
- Do not duplicate `README.md`. Link it instead.
- No generic advice ("write clean code"), no monorepo boilerplate, no secrets.
- Include exact commands with required environment variables.
- The file is written in Russian; code identifiers, commands and file names stay
  as-is. Agent skill instructions are written in English, not in `AGENTS.md`.

## Required sections for Clock

1. **Languages** — English and Russian are the two primary project languages;
   `README.md`/`changelog.md` are English, `README.ru.md`/`changelog.ru.md` are
   Russian; internal docs and `changelog.dev.md` are Russian; code comments are
   English.
2. **Project and stack** — personal fork of Mascot, `com.olivka.clock`, Android
   8.0+ (`minSdk 26`), `compileSdk`/`targetSdk` 34, Kotlin/Compose, no DI.
3. **Versions** — strict `x.y.z`; `version.txt` is the single source of
   `versionName`/`versionCode`; the release sync list including both public
   changelogs.
4. **Changelog** — only app changes; generated before merging a feature branch
   into `main`; documentation, README, skills, CI, issue templates and release
   tooling are excluded.
5. **Build and checks** — `ANDROID_HOME`/`ANDROID_SDK_ROOT` and the Gradle
   command from the project rules.
6. **Releases** — `build.yml`; `release.yml` auto-publishes on `main` when
   `version.txt` differs from the latest release, and also runs on tags and
   manually; tag `vX.Y.Z`, APK names, `scripts/release.sh` as the manual
   fallback, debug keystore constraints. Release notes are assembled from
   `changelog.md` (English) and then `changelog.ru.md` (Russian).
7. **Documentation** — synchronized `README.md`/`README.ru.md` and
   `changelog.md`/`changelog.ru.md` pairs, `PROJECT_NOTES.md`, local `PLAN.md`
   and `changelog.dev.md`.
8. **Dependencies and permissions** — no `INTERNET`, analytics, ads, AI/ML or
   extra permissions without an explicit task.
9. **Agent skills** — `.opencode/skills/`, sources in `.opencode/SKILLS.md`,
   English skill text, precedence over skill recommendations.
10. **Alarm diagnostics** — the `appops`/`dumpsys alarm`/`logcat` commands for
    restricted background execution, with a pointer to `PROJECT_NOTES.md`.

Add a section only if it changes what an agent would actually do.

## Procedure

1. Read the current `AGENTS.md`, `README.md`, `README.ru.md`, `PROJECT_NOTES.md`,
   `version.txt`, `app/build.gradle.kts`, `AndroidManifest.xml`,
   `.github/workflows/` and `scripts/release.sh`.
2. List drift: stale versions, wrong commands, missing files, changed release
   steps, new permissions or dependencies, outdated skill list, broken
   language/changelog pairing, non-app entries in changelogs.
3. Patch only what is inaccurate or missing; preserve existing wording and
   section order where it is still correct.
4. Verify every path and command exists. Use `bash -n` for scripts and
   `./gradlew tasks` only when necessary; do not run a full build for a docs
   change unless asked.
5. Keep the file navigable: short sections, flat bullets, links instead of long
   explanations.
6. If a rule affects user-visible behavior, permissions or alarm scheduling,
   update the documentation set required by `AGENTS.md` itself.

## Validation

- [ ] File is at the repository root and uses standard Markdown.
- [ ] Every listed command works with the documented environment.
- [ ] Language rules and both changelog files are described consistently.
- [ ] Changelog scope is app-only and generated before merging a feature branch.
- [ ] Version rules and the release checklist match `version.txt`,
      `release.yml` (auto-release on `main`), `scripts/release.sh` (manual
      fallback) and the workflows.
- [ ] Release notes order (English, then Russian) is documented.
- [ ] Documentation sync list matches the files that actually exist.
- [ ] Project restrictions (offline, no Google Play, no extra permissions) are
      intact.
- [ ] No secrets, tokens or keystore contents.
- [ ] No duplicated README content and no generic filler.
- [ ] Russian text is consistent; code and commands are unchanged.

## Upstream

Adapted from GitHub's `create-agentsmd` skill
([github/awesome-copilot](https://github.com/github/awesome-copilot), MIT).
Changes: update-in-place instead of generation from a blank template, Clock
project sections, bilingual documentation rules, precedence and repository
validation.
