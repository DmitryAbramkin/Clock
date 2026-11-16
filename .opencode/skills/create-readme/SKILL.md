---
name: create-readme
description: >-
  Creates and updates README.md and README.ru.md for the Clock Android app. Use
  when writing or updating the README, generating project documentation, or
  keeping the English and Russian versions in sync.
license: MIT
metadata:
  author: GitHub, Inc.; adapted for Clock
  source: https://github.com/github/awesome-copilot/tree/main/skills/create-readme
  version: '1.0.0-clock'
---

# Create README

## Role

You are a senior open-source engineer. README files must be accurate, appealing
and easy to scan. Accuracy matters more than marketing: never describe a feature
that the code does not implement.

## Project context

Read these before writing:

- `AGENTS.md` — project rules, languages and versioning.
- `PROJECT_NOTES.md` — architecture and behavioral details.
- `version.txt`, `app/build.gradle.kts`, `AndroidManifest.xml`.
- `THIRD-PARTY-NOTICES.md`, `LICENSE`, `docs/screenshots/`.
- `changelog.md`, `changelog.ru.md` and, if present, `changelog.dev.md`.

Key facts:

- **Clock** is a personal fork of [Mascot](https://github.com/Hotaro26/Mascot)
  by Hotaro26: a quiet, offline 24-hour alarm clock built around shift
  schedules, with timers, a stopwatch and Material 3.
- Android 8.0+ (`minSdk 26`), `compileSdk`/`targetSdk` 34.
- Kotlin + Jetpack Compose, Material 3; no DI framework.
- No `INTERNET`, analytics, ads, AI/ML or camera access.
- The project works in two primary languages, English and Russian:
  `README.md`/`changelog.md` are English by default, `README.ru.md`/
  `changelog.ru.md` are Russian. Both pairs must stay in sync and cross-link
  each other in the header.
- Do not put the version number in the README header.
- The project is developed with LLM agents; keep the existing AI-assisted note.
- Interface terminology in `README.ru.md` must match the app strings
  (`app/src/main/res/values-ru/strings.xml`), not a literal translation.

## Task

1. Review the whole workspace, then update the existing README files. Do not
   rewrite from scratch and do not drop useful content.
2. Keep the current section order unless there is a reason to change it:
   header, at a glance, more screenshots, highlights, differences from Mascot,
   download, build, install, permissions & background, documentation,
   license and credits.
3. Write `README.md` first, then translate it into natural Russian for
   `README.ru.md`. Both files must have the same sections, screenshots and
   links.
4. Use GitHub Flavored Markdown and GitHub admonitions where they add value.
   Avoid emoji clutter. Use the app icon in the header.
5. Do not duplicate dedicated files: link `LICENSE`,
   `THIRD-PARTY-NOTICES.md`, `changelog.md`, `changelog.ru.md` and
   `PROJECT_NOTES.md` instead of copying their content. A short
   "License and credits" section with links is required because this is a fork.
6. Verify every claim against code, resources, Gradle files and workflows.
   Screenshots live in `docs/screenshots/en/` and `docs/screenshots/ru/`; use
   the matching language directory in each README.
7. Keep commands copy-pasteable and consistent with `AGENTS.md`:

   ```bash
   export ANDROID_HOME="$HOME/Android/Sdk"
   export ANDROID_SDK_ROOT="$ANDROID_HOME"
   ./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
   ```

## Style

- Concise, factual, user-facing tone; no hype and no invented benchmarks.
- One idea per bullet; short paragraphs.
- Backticks for commands, settings, permissions and class names.
- Relative links for repository files; absolute GitHub links only for external
  projects.
- Use `<details>` for long screenshot sets, as the current README does.
- Do not add badges that are not actually maintained (CI, downloads, license).

## Validation

- [ ] Both files updated; headers cross-link each other.
- [ ] Section lists of `README.md` and `README.ru.md` match.
- [ ] Every referenced image and file exists.
- [ ] Documentation links include both `changelog.md` and `changelog.ru.md`.
- [ ] No version number in the header; download links point to Releases.
- [ ] No claims of online features, telemetry or permissions the app lacks.
- [ ] Commands match the Gradle setup; APK names match the release workflow.
- [ ] Tables, admonitions, `<details>` and code fences render correctly.
- [ ] Russian text uses the app's terminology and has no machine-translation
      artifacts.

## Upstream

Adapted from GitHub's `create-readme` skill
([github/awesome-copilot](https://github.com/github/awesome-copilot), MIT).
Changes: bilingual README and changelog workflow, Clock project facts,
preservation of existing content, fork attribution and project validation
steps.
