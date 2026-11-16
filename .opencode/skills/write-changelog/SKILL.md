---
name: write-changelog
description: >-
  Writes app-only release entries for a new Clock version in changelog.md,
  changelog.ru.md and changelog.dev.md. Use before merging a feature branch
  into main, or when preparing a release, writing release notes, or bumping
  the version.
license: MIT
metadata:
  author: Microsoft Corporation; adapted for Clock
  source: https://github.com/microsoft/vscode-pull-request-github/tree/main/.github/skills/write-changelog
  version: '1.0.0-clock'
---

# Write Changelog Entry

Generate the entries for one Clock release. The upstream skill used GitHub
milestones; Clock has no milestones, so changes come from git history and the
actual diff.

## Scope

Changelogs describe **only changes to the app itself**: features, behavior, UI,
fixes, permissions, alarm scheduling and data model. Documentation, README
files, agent skills, CI workflows, issue templates, release tooling and
refactors without behavior change are out of scope.

Generate the entries **before merging a feature branch into `main`**, from the
branch diff. If the branch contains no app changes, do not touch any changelog.

## Inputs

The user provides:

1. **Version** in strict `x.y.z` form (for example `3.13.0`), never `x.y`.
2. **Date** `YYYY-MM-DD`; default to today.
3. **Change range or list**; default to `git log main..HEAD` on a feature
   branch, and to the commits since the last tag on `main`. If there is no tag,
   use the full unreleased history plus local `changelog.dev.md` notes.

If the version already exists in `changelog.md` or `changelog.ru.md`, do not
rewrite it. Ask whether to fix it or choose another version: historical entries
are never rewritten.

## Files

| File | Role |
|---|---|
| `version.txt` | Single source of `versionCode` and `versionName`. |
| `changelog.md` | Short public English history. |
| `changelog.ru.md` | Short public Russian history; same versions and dates as `changelog.md`. |
| `changelog.dev.md` | Full technical history, local and gitignored. |
| `PROJECT_NOTES.md` | Current version and architecture notes. |
| `PLAN.md` | Local, gitignored roadmap; update the current version if present. |
| `scripts/release.sh` | Verifies the tree, version and both changelogs, builds, tags and pushes. |
| `.github/workflows/release.yml` | Builds release notes from both public changelogs. |

The GitHub Release notes are assembled from `changelog.md` first (English) and
then `changelog.ru.md` (Russian), separated by a horizontal rule.

## Procedure

### 1. Gather changes

```bash
git branch --show-current
git log --oneline main..HEAD
git diff --stat main..HEAD
```

If the branch only touches documentation, skills, CI or tooling, stop: no
changelog entry is needed.

Also inspect, when touched: Room migrations, `AndroidManifest.xml` (permissions,
components), Gradle files, `.github/workflows`, `scripts/`, settings screens and
`res/values*/strings.xml`. Do not rely on commit messages alone; verify against
the diff.

### 2. Classify

| Bucket | Criteria |
|---|---|
| **App** | User-visible features, behavior, UI, fixes, permissions, alarm scheduling and data model. |
| **Technical app** | App refactors, DB migrations, internal architecture and tests for app behavior. Goes to `changelog.dev.md` only. |
| **Skip** | Documentation, README, agent skills, CI, issue templates, release tooling, dependency or tooling updates with no app behavior change, formatting, version-bump commits, generated files, pure renames. |

If a single user-visible change spans several commits, merge it into one bullet.
If a commit changes both behavior and internals, describe the behavior publicly
and the implementation in the dev changelog.

### 3. Write the English entry

Insert it at the top of `changelog.md`, after the intro and before the previous
version:

```markdown
## 3.13.0 — 2026-11-20

- A noticeable user-facing change and how it behaves.
- A fix: what was broken and what works now.
```

Rules:

- Heading exactly `## x.y.z — YYYY-MM-DD`; the space after the version matters
  to `release.sh` and the release workflow.
- Flat `-` bullets: no `### Changes` / `### Fixes` subheadings, no emoji, no
  tables.
- English, user-facing wording, tense consistent with existing entries.
- Never include commit hashes, file names, class names or issue/PR links.
- Order: features → behavior changes → fixes → permissions/data changes.
- Wrap lines at roughly 80 characters, like the existing entries.
- One blank line between the heading and the bullets, and between versions.
- Keep historical entries and grouped headings (`3.9.x`, `3.0–3.5`) untouched.

### 4. Write the Russian entry

Insert the same version at the top of `changelog.ru.md`:

```markdown
## 3.13.0 — 2026-11-20

- Заметное изменение для пользователя: что изменилось и как это заметно.
- Исправление: что было сломано и что теперь работает.
```

- Use the same heading, date and bullet order as `changelog.md`.
- Write natural Russian in the app's terminology, not a literal translation.
- Keep the same number of bullets unless a Russian sentence needs to be split.
- Apply the same public-format rules: no internals, no links, no emoji.

Both public files must remain synchronized: every version in one file exists in
the other with the same date and equivalent content.

### 5. Write the technical entry

Add `## x.y.z — YYYY-MM-DD` at the top of `changelog.dev.md` with the details
that do not belong in public notes:

- classes, services, receivers, repositories and composables;
- Room migrations (`11 → 12`) and schema changes;
- settings keys, resources, manifest and permission changes;
- app architecture changes and tests for app behavior.

`changelog.dev.md` is internal and stays in Russian. Use backticks for
identifiers and paths; links to commits are allowed when the repository URL is
known. Do not add documentation, README, skills, CI or release-tooling details
here either.

### 6. Sync metadata

- On a feature branch, do not bump `version.txt`: add the entries under the
  planned version and leave the bump for release preparation.
- When preparing the release on `main`: `version.txt`
  `versionName=x.y.z`, `versionCode` greater than the previous value from the
  latest tag (normally +1). Never reuse or lower it.
- `PROJECT_NOTES.md`: current version and any app behavior/architecture notes
  affected by the release.
- `PLAN.md`: current version, if the file exists.
- Documentation and skills: do not record them in changelogs. Update
  `README.md`/`README.ru.md` only when app behavior or permissions changed, as
  required by `AGENTS.md`.

### 7. Validate

```bash
VERSION=$(sed -n 's/^versionName=//p' version.txt)
grep -n "^## $VERSION " changelog.md changelog.ru.md
git diff --check
```

- [ ] Both public changelogs have the same version heading and date.
- [ ] The English and Russian lists describe the same changes in the same order.
- [ ] Every bullet affects app behavior, UI, data or permissions.
- [ ] No documentation, README, skills, CI, issue-template or release-tooling
      entries appear in any changelog.
- [ ] `version.txt` matches the headings and `versionCode` is monotonic, if the
      release is being prepared.
- [ ] Each section is self-contained: release notes can be extracted from the
      heading to the next `## `.
- [ ] Neither public changelog contains technical identifiers or internal links.
- [ ] `changelog.dev.md` has the app implementation details.
- [ ] No historical text changed; the diff only adds the new entries and syncs
      metadata.
- [ ] `./gradlew testDebugUnitTest lintDebug` passes if code changed.
- [ ] A branch with only non-app changes leaves all changelogs untouched.

Do not create tags, commit or push as part of this skill. Releasing is done by
`./scripts/release.sh` after review.

## Upstream

Adapted from Microsoft's `write-changelog` skill
([microsoft/vscode-pull-request-github](https://github.com/microsoft/vscode-pull-request-github),
MIT). Changes: git ranges instead of GitHub milestones, English/Russian public
changelogs, bilingual release notes, version metadata sync and release
validation.
