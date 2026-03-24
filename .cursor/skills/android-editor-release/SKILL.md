---
name: android-editor-release
description: >-
  Cut a new Android Channel Editor release: version bump in git, signed release APK,
  annotated tag, GitHub Release assets. Use when shipping android-editor-v* or when
  the user asks for a release / Play-style build / signed APK from this repo.
---

# Android Channel Editor — release workflow

Goal: **what is in git (version + changelog + tag) matches the signed APK** you publish. The APK is not committed; the **release commit** is the source of truth.

## 1. Single release commit first (sync git with the version)

1. Bump in **`AndroidNICFW_CH_EDITOR/app/build.gradle.kts`**:
   - `versionCode` — monotonic integer (Play / installs).
   - `versionName` — SemVer string shown in UI and docs.
2. Add **`AndroidNICFW_CH_EDITOR/CHANGELOG.md`** section `## [X.Y.Z] — date` under `## Unreleased` (then empty Unreleased if you want).
3. Optional but common for this repo: if you ship an updated **User Guide PDF** on GitHub Releases, regenerate it **after** the Gradle bump so MkDocs/PDF pick up `versionName` (`docs/hooks.py` reads `app/build.gradle.kts`).
4. **`git status`** — no stray edits you did not mean to ship. Ignore untracked build output and local secrets.
5. Commit **one** cut commit on `main`:
   - Subject: `release(android): cut vX.Y.Z` (optionally extend subject for major marketing, e.g. v2.0.0).
   - Body: bullet summary of what shipped; end with `Made-with: Cursor` if that is your habit.
6. **`git push origin main`** before tagging so the remote matches.

**GitHub Pages `user-guide.pdf`:** The live PDF URL in release notes always serves whatever was last deployed from **`main`**. The **Update User Guide** workflow (`.github/workflows/update-userguide.yml`) rebuilds and runs `mkdocs gh-deploy` when `UserGuide.md`, `docs/**`, `mkdocs.yml`, **`AndroidNICFW_CH_EDITOR/app/build.gradle.kts`**, or the workflow file changes, and on each **release** `published`/`edited`. If you ever bump only Android files that are not in those paths, run **Actions → Update User Guide → Run workflow** so the cover `App v…` matches `versionName`.

**Rule:** The commit you are about to tag **must** contain the final `versionCode` / `versionName` for this release. Do not tag an older commit after bumping only locally without pushing.

## 2. Signed release APK (`assembleRelease`)

Signing is **not** in git. In **`AndroidNICFW_CH_EDITOR/local.properties`** (gitignored), require:

- `RELEASE_STORE_FILE` — path to the keystore (often relative to `AndroidNICFW_CH_EDITOR/`, e.g. `nicfw-release.jks`).
- `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

From **`AndroidNICFW_CH_EDITOR/`**:

```bash
./gradlew assembleRelease
```

On Windows: `.\gradlew.bat assembleRelease`

**Verify sync with git:**

- Open **`app/build/outputs/apk/release/output-metadata.json`** and confirm `versionCode` / `versionName` match **`app/build.gradle.kts`** on `HEAD`.
- If they differ, you built from the wrong tree or forgot to sync Gradle after editing — fix before uploading.

Do **not** commit `app/build/` or the APK to the repo unless the project explicitly requires it (this project does not).

## 3. Git tag (must point at the release commit)

- Tag name: **`android-editor-vX.Y.Z`** (matches existing releases).
- **Annotated** tag, first line of message: **`release(android): cut vX.Y.Z`**, body aligned with the cut commit (same bullets as far as practical).
- Tag the **same commit** as the `release(android): cut` commit (the one with the version bump).

```bash
git tag -a android-editor-vX.Y.Z -F tag-message.txt
git push origin android-editor-vX.Y.Z
```

If you must move a tag, `git push origin :refs/tags/android-editor-vX.Y.Z` then push again — coordinate with anyone who pulled the old tag.

## 4. GitHub Release (optional but usual here)

- Create/update release for tag **`android-editor-vX.Y.Z`**.
- Attach **`app-release.apk`** from `app/build/outputs/apk/release/` (use display name `app-release.apk` if matching prior releases).
- Attach User Guide PDF if you ship docs; name like `nicfw-td-h3-editor-android-editor-vX.Y.Z-userguide.pdf`.
- **Windows / PowerShell:** when using `gh release upload`, quote paths so `#displayName` is not treated as a comment, e.g. `'path\to\file.apk#app-release.apk'`.
- Release notes: summarize CHANGELOG; if a prior version had a tag but **no** GitHub Release/APK, state that the current APK includes those changes.

Use **`gh release create`** / **`gh release edit`** with a logged-in `gh` (`repo` scope).

## 5. Checklist (agent or human)

- [ ] `versionCode` / `versionName` updated in `app/build.gradle.kts`
- [ ] `CHANGELOG.md` has `[X.Y.Z]`
- [ ] Commit + push `main`
- [ ] `assembleRelease` succeeds with local signing props
- [ ] `output-metadata.json` matches Gradle version fields
- [ ] Annotated tag `android-editor-vX.Y.Z` on that commit, pushed
- [ ] GitHub Release assets + notes (if publishing)

## 6. Semantic version bumps

- **Patch** (2.3.1): small fixes, same feature set.
- **Minor** (2.4.0): new features, backward compatible.
- **Major** (3.0.0): breaking changes or large product milestone — note in CHANGELOG and release title as needed.

Always increment **`versionCode`** by at least 1 for every Play/device-visible release, even if you skip a `versionName` on a store by mistake.
