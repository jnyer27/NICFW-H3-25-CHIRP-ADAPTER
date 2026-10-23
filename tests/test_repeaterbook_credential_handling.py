"""Source and archive checks for RepeaterBook credential handling."""

import subprocess
import sys
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
VERIFIER = REPO_ROOT / "scripts" / "verify_no_repeaterbook_shared_credential.py"


def test_source_tree_has_no_embedded_credential_design():
    result = subprocess.run(
        [sys.executable, str(VERIFIER)],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stderr
    assert "source_ok" in result.stdout


def test_apk_scan_reports_a_token_without_printing_it(tmp_path: Path):
    secret = b"rbu" + b"app_" + b"abcd2345efgh"
    archive = tmp_path / "sample.apk"
    with zipfile.ZipFile(archive, "w") as zf:
        zf.writestr("classes.dex", b"noise " + secret + b" noise")
    result = subprocess.run(
        [sys.executable, str(VERIFIER), "--apk", str(archive)],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 1
    assert "user_app_token_count=1" in result.stdout
    assert secret.decode("ascii") not in result.stdout
    assert secret.decode("ascii") not in result.stderr
    assert "token-shaped" in result.stderr


def test_clean_archive_passes(tmp_path: Path):
    archive = tmp_path / "clean.apk"
    with zipfile.ZipFile(archive, "w") as zf:
        zf.writestr("classes.dex", b"NicFwChannelEditor X-RB-App-Token header only")
    result = subprocess.run(
        [sys.executable, str(VERIFIER), "--apk", str(archive)],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stderr
    assert "user_app_token_count=0" in result.stdout
    assert "shared_app_token_count=0" in result.stdout
