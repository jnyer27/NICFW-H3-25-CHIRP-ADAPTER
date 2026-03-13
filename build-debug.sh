#!/usr/bin/env bash
# Build the debug APK and copy it to the canonical output location in the
# main repository tree, regardless of whether this script is run from a
# git worktree or the main working directory.
#
# Usage: ./build-debug.sh  (from repo root or any worktree root)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAIN_REPO="C:/Users/jason/Documents/GitHub/NICFW-H3-25-CHIRP-ADAPTER"
APK_DEST="$MAIN_REPO/AndroidNICFW_CH_EDITOR/app/build/outputs/apk/debug/app-debug.apk"

cd "$SCRIPT_DIR/AndroidNICFW_CH_EDITOR"

# Ensure local.properties exists (worktrees don't inherit it)
if [ ! -f local.properties ]; then
    cp "$MAIN_REPO/AndroidNICFW_CH_EDITOR/local.properties" local.properties
fi

./gradlew assembleDebug

BUILT_APK="$SCRIPT_DIR/AndroidNICFW_CH_EDITOR/app/build/outputs/apk/debug/app-debug.apk"

if [ "$BUILT_APK" != "$APK_DEST" ]; then
    cp "$BUILT_APK" "$APK_DEST"
    echo "APK copied to: $APK_DEST"
else
    echo "APK built at: $APK_DEST"
fi
