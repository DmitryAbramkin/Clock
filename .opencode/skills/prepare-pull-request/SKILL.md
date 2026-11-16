---
name: prepare-pull-request
description: >-
  Commits all pending Clock changes, updates version.txt and the changelogs,
  pushes the feature branch and opens a GitHub pull request, returning its URL.
  Use when asked to commit everything, finish a feature branch, bump the
  version and changelogs, push, and create a PR/MR.
license: MIT
metadata:
  author: Clock project
  version: '1.0.0'
---

# Prepare Pull Request

Finish a Clock feature branch in one pass:

1. commit all pending work;
2. add the app release entry and bump the version;
3. commit the version and metadata;
4. push the branch and open a GitHub pull request.

GitHub calls this a **pull request** (PR). When the user says "MR", create a PR.
The skill does **not** tag a release or publish APKs: after the PR is merged,
`./scripts/release.sh` still owns tagging and publishing.

## Precedence

1. the user's explicit request for the current task;
2. `AGENTS.md`;
3. this skill and `write-changelog`.

If the user asks only for a commit, only for a version bump, or only for a PR,
do only that part. Do not merge the PR unless the user explicitly asks to merge
it.

## Safety

- Never force-push, rewrite published history, create tags, or publish releases.
- Never commit secrets, keystores, `local.properties`, APKs, build output or
  generated files. `PLAN.md` and `changelog.dev.md` are gitignored and stay
  local.
- Review `git status --short` before staging. If an unexpected file appears
  (`.env`, `*.jks`, `*.keystore`, a new binary), stop and ask.
- Never use `git commit --no-verify`; fix the hook failure instead.
- Do not merge or delete branches unless the user asked for it.
- Do not amend commits that are already pushed.

## 1. Inspect the repository

```bash
git remote get-url origin
git branch --show-current
git status --short
git log --oneline -5
git fetch origin main --quiet
```

- Stop if there is no `origin` or the directory is not a git repository.
- If HEAD is detached, stop and ask which branch to use.
- If the current branch is `main` and there are changes to commit, create a
  working branch first: `git switch -c feature/<slug>` (or `fix/<slug>`,
  `chore/<slug>`). Never commit feature work directly to `main`.
- If the branch is behind `origin/main`, stop and ask whether to rebase or
  merge; do not rewrite it silently.
- If the branch has no changes and no commits ahead of `main`, there is nothing
  to publish: report that and stop.

## 2. Commit all pending changes

Review the tree, then stage everything that belongs to the task:

```bash
git diff --stat
git add -A
git status --short
git diff --cached --stat
```

- Prefer one commit for the pending work; split only when the tree contains
  clearly unrelated changes.
- Write an English imperative subject that describes the user-visible change,
  for example `Add wheel haptics fallback`. No `WIP`, no emoji, no version
  numbers in this commit.
- If `git status --porcelain` is already empty, skip this step.
- Do not include ignored files; `git add -A` will not add them.

## 3. Decide the version

Before choosing a version, confirm that the branch has app changes using the
scope check in step 4. If it has none, skip this step, step 5 and step 7, then
go to step 8: docs-only branches are committed, pushed and opened as a PR
without a version bump.

Read the current state:

```bash
CURRENT=$(sed -n 's/^versionName=//p' version.txt)
CURRENT_CODE=$(sed -n 's/^versionCode=//p' version.txt)
LATEST_TAG=$(git tag --list 'v*' --sort=-v:refname | head -n1)
TOP=$(grep -m1 -E '^## [0-9]+\.[0-9]+\.[0-9]+ ' changelog.md \
  | sed -E 's/^## ([0-9]+\.[0-9]+\.[0-9]+).*/\1/')
git show origin/main:version.txt
```

Use the version in this order:

1. **User-provided version** — must match `x.y.z` and be greater than the latest
   released tag.
2. **Unreleased changelog entry** — if `TOP` is greater than `CURRENT` and
   `v$TOP` does not exist, the changelog already plans that version. Use `TOP`
   and only sync `version.txt` and metadata; do not add a duplicate section.
3. **Next version** — bump from `CURRENT`:
   - user-visible features or behavior changes → minor (`3.12.0` → `3.13.0`);
   - fixes only → patch (`3.12.0` → `3.12.1`);
   - explicitly requested breaking release → major;
   - mixed or unclear → minor, matching recent project history.

`versionCode` must be an integer greater than both `CURRENT_CODE` and the code
in the latest tag (`git show "$LATEST_TAG:version.txt"`). Normally add `1`.

Before writing, check `git show origin/main:version.txt`: if `main` already uses
the target version or a newer one, stop and ask the user to rebase the branch.
Never reuse a released version or lower `versionCode`.

If a changelog heading belongs to a version that already has a tag, it is
historical: never rewrite it.

## 4. Add the app changes to the changelogs

Determine whether the branch contains app changes:

```bash
git log --oneline main..HEAD
git diff --stat main...HEAD
git diff --name-only main...HEAD
```

Classify with `write-changelog`: features, behavior, UI, fixes, permissions,
alarm scheduling and data model are app changes. Documentation, README, agent
skills, CI, issue templates, release tooling and pure refactors are not.

- **No app changes** — do not add changelog entries and do not bump the
  version. A docs-only branch must not create an empty release section:
  `release.sh` requires a changelog section for every released version, and
  public changelogs are app-only. Continue with step 8 and explain the skipped
  bump in the final response.
- **App changes present** — follow `write-changelog` for wording:
  - same `## x.y.z — YYYY-MM-DD` heading in `changelog.md` and
    `changelog.ru.md`, English first, equivalent Russian bullets in the same
    order;
  - user-facing language only: no classes, files, commit hashes, links or
    emoji;
  - `changelog.dev.md` gets the technical details in Russian and stays local
    (gitignored);
  - never rewrite versions that already have a tag.

If the target section already exists in the unreleased range, keep it and add or
fix missing bullets instead of duplicating the heading. Synchronize the date
between the English and Russian files.

Use `date +%F` for the date. Use the existing date if the unreleased section
already has one.

## 5. Sync the version metadata

Update, using targeted edits:

- `version.txt` — `versionName` and incremented `versionCode`;
- `PROJECT_NOTES.md` — the line
  `**Версия:** x.y.z (\`versionCode N\`)` and the `**Обновлено:** YYYY-MM-DD`
  date;
- `PLAN.md` — same version line and `**Обновлено:** YYYY-MM-DD`, if the local
  file exists; it is gitignored;
- `README.md` / `README.ru.md` — only when app behavior or permissions changed,
  as required by `AGENTS.md`; never add a version number there.

Do not modify historical changelog entries or unrelated documentation.

## 6. Validate

```bash
VERSION=$(sed -n 's/^versionName=//p' version.txt)
CODE=$(sed -n 's/^versionCode=//p' version.txt)
grep -n "^## $VERSION " changelog.md changelog.ru.md
git diff --check
git status --short
```

Checklist:

- [ ] `versionName` is strict `x.y.z`; `versionCode` is greater than the latest
      tag's code.
- [ ] Both public changelogs contain the same heading and date.
- [ ] English and Russian bullets describe the same changes in the same order.
- [ ] Changelogs contain app changes only.
- [ ] No historical entries changed; the diff only adds the new section and
      syncs metadata.
- [ ] No secrets, ignored files or build output are staged.

Run checks when `app/`, resources, the manifest or Gradle files changed:

```bash
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
./gradlew testDebugUnitTest lintDebug
```

If the build fails, fix the problem or stop. Do not push a branch that does not
compile or pass its tests. CI runs the full build again after the push.

## 7. Commit the version and changelogs

```bash
git add version.txt changelog.md changelog.ru.md PROJECT_NOTES.md
git commit -m "Bump version to $VERSION"
```

Add `README.md` / `README.ru.md` to the same commit only when they were updated
for app behavior. `PLAN.md` and `changelog.dev.md` are gitignored and are not
committed.

If step 4 skipped the release, there is no version commit; the branch is ready
to push after step 2.

The working tree should now be clean except for ignored files. Commit any
remaining intended change before pushing.

## 8. Push the branch

```bash
BRANCH=$(git branch --show-current)
git push -u origin "$BRANCH"
```

Then verify that the remote branch points at the local HEAD:

```bash
test "$(git rev-parse HEAD)" = "$(git rev-parse "@{u}")"
```

If the push fails because git cannot read credentials:

1. Retry once with the stored credentials, without printing them:
   `git -c credential.helper=store push -u origin "$BRANCH"`.
2. If that still fails, stop. Give the user the exact push command and explain
   that the PR cannot be opened until the branch exists on GitHub. Do not
   recreate commits through the API and do not pretend the push succeeded.

Never force-push.

## 9. Open the pull request

Extract `owner` and `repo` from `git remote get-url origin`, then:

1. Check for an existing open PR for this branch. If one exists, return its URL
   instead of creating a duplicate.
2. Prefer the GitHub MCP tool `create_pull_request`:
   `owner`, `repo`, `base: "main"`, `head: "<branch>"`, `title`, `body`.
3. Fallback when MCP is unavailable: `gh pr create --base main --head "$BRANCH"
   --title "$TITLE" --body-file /tmp/opencode/pr-body.md`.
4. If neither is available, print the compare URL:
   `https://github.com/<owner>/<repo>/compare/main...<branch>?expand=1`.

Title: `<summary>` or `<summary> (x.y.z)` when the version was bumped. Keep it
under ~70 characters and do not start it with a version number alone.

Body template (English, no emoji):

```markdown
## Summary

- The user-visible change.
- The fix or behavior change.

## Version

- `3.13.0` (`versionCode 38`), or "No version bump: no app changes."

## Changelog

- `changelog.md` / `changelog.ru.md`, or "Not needed: no app changes."

## Checks

- [x] `./gradlew testDebugUnitTest lintDebug`
```

Include the changelog bullets in the summary only when they are short; otherwise
link the two files. Never include secrets or internal file paths.

## 10. Merge only when asked

By default, stop after the PR is created and return its URL.

If the user explicitly asked to merge, merge after the PR exists:

- use the GitHub MCP `merge_pull_request` tool with `merge_method: "squash"` to
  keep `main` linear, unless the user requested a merge commit or rebase;
- then update `main` locally:
  `git switch main && git pull --ff-only`;
- delete the local and remote branch only if the user asked for cleanup;
- return the PR link and the merge result.

Do not merge when checks are failing, the PR has conflicts, or the user asked
only for a review.

## 11. Report

The final response must start with the PR link, then list:

- the version and `versionCode`, or why the bump was skipped;
- the commits created;
- which checks ran;
- anything left uncommitted, skipped or blocked.

Example:

> PR: <https://github.com/DmitryAbramkin/Clock/pull/12>
>
> Version `3.13.0` (`versionCode 38`), changelogs updated in English and Russian.
> Commits: `Add wheel haptics fallback`, `Bump version to 3.13.0`.
> Checks: `./gradlew testDebugUnitTest lintDebug` passed.

## Edge cases

| Situation | Action |
|---|---|
| No changes and no commits ahead of `main` | Report nothing to publish; do not create an empty PR. |
| Only docs, skills, CI or issue templates changed | Commit, push, open the PR; skip the version and all changelogs. |
| Changelog top version is ahead of `version.txt` | Treat it as the planned version; bump metadata only. |
| Target version is already tagged | Choose the next version; never reuse or rewrite it. |
| An open PR already exists for the branch | Return its URL; do not create another. |
| Branch is behind `origin/main` | Stop and ask; rebase only on explicit request. |
| Push rejected as non-fast-forward | Fetch and inspect; never force-push. |
| Checks fail | Fix first or stop; do not open a red PR. |
| User says "merge" | Create the PR first, then merge and update `main`. |

## Validation checklist

- [ ] All intended changes are committed; no `--no-verify`.
- [ ] The branch is not `main`.
- [ ] `version.txt` uses `x.y.z` and a monotonic `versionCode`, unless the bump
      was correctly skipped.
- [ ] Both public changelogs have the same unreleased heading and date.
- [ ] Changelogs contain app changes only; historical entries are untouched.
- [ ] `changelog.dev.md` has the technical details and stays local.
- [ ] Tests and lint pass when app code changed.
- [ ] The push succeeded and the remote HEAD matches the local HEAD.
- [ ] The pull request link is returned in the final answer.
- [ ] The PR is not merged unless the user asked for it.

## Related skills

- `write-changelog` — changelog wording and scope; reuse it for step 4.
- `wiki-changelog` — full history audits, not this flow.
- `create-readme` — README updates when app behavior changed.
- `release.sh` — tags and publishes after the PR is merged; not this skill.
