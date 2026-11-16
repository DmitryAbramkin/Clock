#!/usr/bin/env bash
#
# Prepare and publish a Clock release.
#
# The script verifies the working tree and version files, optionally runs the
# full local build, then pushes an annotated tag. GitHub Actions builds the APK
# and attaches it to the release.
#
# Usage:
#   ./scripts/release.sh              # build locally, then tag and push
#   ./scripts/release.sh --skip-build # tag and push without a local build
#   ./scripts/release.sh --dry-run    # validate everything, do not tag or push
#
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

SKIP_BUILD=0
DRY_RUN=0
for arg in "$@"; do
    case "$arg" in
        --skip-build)
            SKIP_BUILD=1
            ;;
        --dry-run)
            DRY_RUN=1
            ;;
        -h|--help)
            sed -n '/^# Usage:/,/^set -euo pipefail$/p' "$0" | sed '$d'
            exit 0
            ;;
        *)
            echo "Unknown option: $arg" >&2
            exit 1
            ;;
    esac
done

if [ -n "$(git status --porcelain)" ]; then
    echo "Working tree is not clean. Commit or stash changes first." >&2
    exit 1
fi

BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$BRANCH" != "main" ]; then
    echo "Release must be created from main (current branch: $BRANCH)." >&2
    exit 1
fi

VERSION_NAME=$(sed -n 's/^versionName=//p' version.txt)
VERSION_CODE=$(sed -n 's/^versionCode=//p' version.txt)
if [ -z "$VERSION_NAME" ] || [ -z "$VERSION_CODE" ]; then
    echo "version.txt is missing versionName or versionCode." >&2
    exit 1
fi
if ! [[ "$VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "versionName must use x.y.z format, got: $VERSION_NAME" >&2
    exit 1
fi
TAG="v$VERSION_NAME"

for file in changelog.md changelog.ru.md; do
    if ! grep -q "^## $VERSION_NAME " "$file"; then
        echo "$file has no '## $VERSION_NAME' section." >&2
        exit 1
    fi
done

git fetch origin main --tags --quiet

if [ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]; then
    echo "Local main differs from origin/main. Push or pull first." >&2
    exit 1
fi

if git rev-parse --verify --quiet "refs/tags/$TAG" >/dev/null; then
    echo "Local tag $TAG already exists." >&2
    exit 1
fi
if git ls-remote --exit-code --tags origin "refs/tags/$TAG" >/dev/null 2>&1; then
    echo "Remote tag $TAG already exists." >&2
    exit 1
fi

PREV_TAG=$(git tag --list 'v*' --sort=-v:refname | head -n1 || true)
if [ -n "$PREV_TAG" ]; then
    PREV_CODE=$(git show "$PREV_TAG:version.txt" 2>/dev/null | sed -n 's/^versionCode=//p' || true)
    if [ -n "$PREV_CODE" ] && [ "$VERSION_CODE" -le "$PREV_CODE" ]; then
        echo "versionCode $VERSION_CODE must be greater than $PREV_CODE from $PREV_TAG." >&2
        exit 1
    fi
fi

if [ "$SKIP_BUILD" -eq 0 ]; then
    export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
    export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
    ./gradlew testDebugUnitTest lintDebug assembleRelease --console=plain
fi

if [ "$DRY_RUN" -eq 1 ]; then
    echo "Dry run: would create tag $TAG and push main with the tag."
    exit 0
fi

git tag -a "$TAG" -m "Clock $VERSION_NAME (versionCode $VERSION_CODE)"
git push origin main "$TAG"

echo
echo "Pushed $TAG. GitHub Actions will publish the release:"
echo "  https://github.com/DmitryAbramkin/Clock/releases/tag/$TAG"
