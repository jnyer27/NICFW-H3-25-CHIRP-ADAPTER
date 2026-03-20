"""
MkDocs build hook — single source of truth for documentation.

- UserGuide.md (repo root) → docs/index.md on each build.
- Android app versionName (app/build.gradle.kts) → Material header `extra.version`.
- docs/pdf-cover.html: a tiny placeholder is committed so mkdocs-exporter passes config
  validation; on_pre_build overwrites it from pdf-cover.in.html + version.

docs/index.md is git-ignored; edit pdf-cover.in.html for PDF cover layout.
"""

import os
import re
import shutil
from pathlib import Path

_GRADLE_REL = Path("AndroidNICFW_CH_EDITOR") / "app" / "build.gradle.kts"
_VERSION_RE = re.compile(r'versionName\s*=\s*"([^"]+)"')


def _repo_root(config: dict) -> Path:
    return Path(config["docs_dir"]).resolve().parent


def _read_app_version(repo_root: Path) -> str:
    gradle = repo_root / _GRADLE_REL
    if not gradle.is_file():
        return os.environ.get("MKDOCS_APP_VERSION", "dev")
    text = gradle.read_text(encoding="utf-8")
    m = _VERSION_RE.search(text)
    if not m:
        return os.environ.get("MKDOCS_APP_VERSION", "dev")
    return m.group(1)


def on_config(config, **kwargs):
    """Material theme header version from Android versionName."""
    version = _read_app_version(_repo_root(config))
    extra = config.setdefault("extra", {})
    extra["version"] = version
    print(f"  [hook] extra.version = {version!r} (from {_GRADLE_REL.as_posix()})")


def on_pre_build(config, **kwargs):
    repo_root = _repo_root(config)
    docs_dir = Path(config["docs_dir"])
    version = _read_app_version(repo_root)

    src = repo_root / "UserGuide.md"
    dst = docs_dir / "index.md"
    if not src.exists():
        raise FileNotFoundError(f"UserGuide.md not found at {src}")
    shutil.copy2(src, dst)
    print(f"  [hook] Copied UserGuide.md -> docs/index.md")

    template = docs_dir / "pdf-cover.in.html"
    cover_out = docs_dir / "pdf-cover.html"
    if not template.is_file():
        raise FileNotFoundError(f"PDF cover template missing: {template}")
    cover_out.write_text(
        template.read_text(encoding="utf-8").replace("__APP_VERSION__", version),
        encoding="utf-8",
    )
    print(f"  [hook] Refreshed docs/pdf-cover.html (App v{version})")
