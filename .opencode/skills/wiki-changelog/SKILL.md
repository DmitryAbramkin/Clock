---
name: wiki-changelog
description: >-
  Builds a complete, version-by-version changelog for Clock from git history,
  tags and the existing changelog files, and finds missing or stale entries.
  Use when asked for a full project changelog, a history audit, or what changed
  across releases.
license: MIT
metadata:
  author: Microsoft Corporation; adapted for Clock
  source: https://github.com/microsoft/skills/tree/main/.github/plugins/deep-wiki/skills/wiki-changelog
  version: '1.0.0-clock'
---

# Wiki Changelog

Generate or audit the full version history of Clock. Unlike the upstream skill,
which groups commits by day and week with emoji categories, Clock groups changes
by **release version**, without emoji, in paired English and Russian files.

## Scope

Changelogs describe **only changes to the app itself**: features, behavior, UI,
fixes, permissions, alarm scheduling and data model. Documentation, README
files, agent skills, CI workflows, issue templates and release tooling are out
of scope. Preserve legacy historical entries, but do not add new non-app items.

## Source repository resolution

1. Run `git remote get-url origin`. In this repository it returns
   `https://github.com/DmitryAbramkin/Clock.git`, so set `REPO_URL` to
   `https://github.com/DmitryAbramkin/Clock` and continue without asking.
2. Ask only if the remote is missing or the user points to another repository.
3. With `REPO_URL`, the technical and standalone documents may link commits
   (`REPO_URL/commit/<sha>`) and files (`REPO_URL/blob/main/<path>`). The public
   `changelog.md` and `changelog.ru.md` never contain such links.

## Sources of truth

| Source | Covers |
|---|---|
| `changelog.md` | Public English history from 2.0 to the current version. |
| `changelog.ru.md` | Public Russian history; same versions and dates as `changelog.md`. |
| `changelog.dev.md` | Detailed technical history in Russian, local and gitignored. |
| `PROJECT_NOTES.md`, `PLAN.md`, `README.md`, `README.ru.md` | Context for old versions. |
| `git log`, `git tag` | Only from the initial public commit (`Clock 3.12.0`) onward. |
| `version.txt` | Current `versionCode`/`versionName`. |

History is squashed: older versions have no commits and no tags, only changelog
entries. **Do not pretend that commits exist for 2.0–3.11.x.** Reconstruct those
versions from `changelog.md`, `changelog.ru.md`, `changelog.dev.md` and project
notes. Tags start at `v3.12.0`.

## Modes

Choose the mode from the request; default to **audit** when the user asks what is
missing or whether the history is complete.

### A. Audit

- Collect versions from `git tag --sort=-v:refname`, headings in
  `changelog.md`, `changelog.ru.md` and `changelog.dev.md`, and `version.txt`.
- Report: missing versions in either language, duplicate versions, wrong dates,
  broken English/Russian parity, public entries that leak internals, technical
  entries missing from the dev changelog, non-app entries added after the
  app-only policy, and grouped headings that hide individual patch releases.
- Do not modify files in this mode.

### B. Backfill

- Add only missing entries; never rewrite or reorder existing history.
- Add the English entry to `changelog.md`, then the equivalent Russian entry to
  `changelog.ru.md`, following `write-changelog`.
- Put technical app details in `changelog.dev.md`; do not add documentation,
  skills, CI or release-tooling entries.
- Keep the established heading styles, including grouped labels such as
  `3.9.x`, `3.0–3.5` and pre-3.6.0 labels like `3.5` without the patch part.

### C. Standalone full document

- Build one version-by-version document, newest first.
- Write it to the path the user provides. If no path is given, default to
  `docs/changelog-full.md` (English) and `docs/changelog-full.ru.md` (Russian).
  Ask before overwriting existing files.
- Link commits and files when `REPO_URL` is known.

## Procedure

1. Inventory versions and tags in both public changelogs. Map each version to
   the commit range it covers; for untagged versions use the initial commit
   boundary and changelog dates.
2. For each version range, read commits and diffs, then merge related commits
   into one coherent change. Never list raw commit subjects.
3. Classify with text, not emoji: `New`, `Changes`, `Fixes`, `Build and release`,
   `Documentation`; in the Russian file use `Новое`, `Изменения`,
   `Исправления`, `Сборка и релиз`, `Документация`. Omit empty categories.
4. Write the English list first, then the Russian translation with the same
   order and scope. Both lists must contain only app changes. Use the app's
   terminology in Russian: будильники, таймеры, секундомер, графики смен,
   звук и вибрация, разрешения и работа в фоне.
5. Keep technical app details (classes, migrations, app architecture) in the dev
   or standalone document, not in the public changelogs. Do not include docs,
   skills, CI or release tooling anywhere.
6. Highlight breaking changes prominently: data migrations, permission model
   changes, dropped features, new minimum Android version.
7. Verify each claim against the diff or an existing changelog entry. If a
   version has no reliable source, mark it as needing confirmation instead of
   inventing content.

## Style

- Heading: `## x.y.z — YYYY-MM-DD`, with grouped historical labels kept as-is.
- Flat bullets, no tables of changes, no emoji, no marketing language.
- One change per bullet; merge duplicates across commits.
- Line width around 80 characters.
- Newest version first; never reorder historical sections.
- Both language files stay synchronized: same versions, dates and bullet order.
- Do not modify `README.md` from this skill; documentation sync belongs to
  `create-readme`.

## Validation

- [ ] Versions are unique and ordered newest first in both files.
- [ ] Every version present in `changelog.md` exists in `changelog.ru.md` with
      the same date, and vice versa.
- [ ] Every tag from `v3.12.0` has a matching section in both files.
- [ ] Grouped historical headings and pre-3.6.0 labels are unchanged.
- [ ] Dates are valid `YYYY-MM-DD`; the current version matches `version.txt`.
- [ ] Public changelogs have no file names, class names or commit hashes.
- [ ] Public and dev entries contain only app changes.
- [ ] No documentation, README, skills, CI, issue-template or release-tooling
      entries were added.
- [ ] The current sections are still extractable by `release.yml`.
- [ ] Links (if any) resolve against `REPO_URL`.
- [ ] `git diff --check` passes and the diff contains no rewritten history.

## Upstream

Adapted from Microsoft's `wiki-changelog` skill
([microsoft/skills](https://github.com/microsoft/skills), MIT). Changes:
version-based grouping instead of daily/weekly, paired English/Russian public
changelogs, squashed-history handling and repository-specific validation.
