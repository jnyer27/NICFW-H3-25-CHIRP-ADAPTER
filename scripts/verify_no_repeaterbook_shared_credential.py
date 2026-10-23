#!/usr/bin/env python3
"""Verify RepeaterBook credentials are not embedded and query-string auth is gone.

Prints counts and lengths only. Token values are never written to stdout.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
import zipfile
from pathlib import Path

TOKEN_RE = re.compile(rb"(?<![A-Za-z0-9_])((?:rbuapp_|app_)[A-Za-z0-9_\-]{8,})")
UTF16_RE = re.compile(
    b"(?:r\x00b\x00u\x00a\x00p\x00p\x00_\x00|a\x00p\x00p\x00_\x00)"
    b"(?:[A-Za-z0-9_\\-]\x00){8,}"
)

ROOT = Path(__file__).resolve().parent.parent
HTTP_CLIENT = (
    ROOT
    / "AndroidNICFW_CH_EDITOR/app/src/main/java/com/nicfw/tdh3editor/radio/repeaterbook/RepeaterBookApi.kt"
)
GRADLE = ROOT / "AndroidNICFW_CH_EDITOR/app/build.gradle.kts"
STRINGS = ROOT / "AndroidNICFW_CH_EDITOR/app/src/main/res/values/strings.xml"
GUIDE = ROOT / "UserGuide.md"

QUERY_MODE_NAMES = ("query_key", "query_token", "query_api_key")


def _fail(errors: list[str], message: str) -> None:
    errors.append(message)


def check_source() -> list[str]:
    errors: list[str] = []
    http = HTTP_CLIENT.read_text(encoding="utf-8")
    gradle = GRADLE.read_text(encoding="utf-8")
    strings = STRINGS.read_text(encoding="utf-8")
    guide = GUIDE.read_text(encoding="utf-8")

    if "X-RB-App-Token" not in http and "HEADER_NAME" not in http:
        _fail(errors, "export client does not send the approved token header")
    for name in QUERY_MODE_NAMES:
        if name in http:
            _fail(errors, f"export client still mentions {name}")
        if name in strings:
            _fail(errors, f"troubleshooting strings still mention {name}")
        if name in guide:
            _fail(errors, f"user guide still mentions {name}")
    for snippet in (
        'addQueryParameter("key"',
        'addQueryParameter("token"',
        'addQueryParameter("api_key"',
        "REPEATERBOOK_APP_TOKEN",
        "REPEATERBOOK_AUTH_MODE",
    ):
        if snippet in http:
            _fail(errors, f"export client still contains {snippet}")
    if 'buildConfigField("String", "REPEATERBOOK_APP_TOKEN"' in gradle:
        _fail(errors, "Gradle still embeds REPEATERBOOK_APP_TOKEN")
    if 'buildConfigField("String", "REPEATERBOOK_AUTH_MODE"' in gradle:
        _fail(errors, "Gradle still embeds REPEATERBOOK_AUTH_MODE")
    if "REPEATERBOOK_APP_TOKEN is set in local.properties" not in gradle:
        _fail(errors, "Gradle does not reject a build-time RepeaterBook token")
    if "REPEATERBOOK_APP_TOKEN=" in guide:
        _fail(errors, "user guide still shows a build-time token property")
    return errors


def scan_apk(path: Path) -> dict:
    counts = {"app_": set(), "rbuapp_": set()}
    lengths = {"app_": [], "rbuapp_": []}
    entries = {"app_": set(), "rbuapp_": set()}
    with zipfile.ZipFile(path) as archive:
        for info in archive.infolist():
            if info.is_dir():
                continue
            data = archive.read(info)
            blobs = [data]
            for blob in blobs:
                for match in TOKEN_RE.finditer(blob):
                    _record(counts, lengths, entries, match.group(1), info.filename)
                for match in UTF16_RE.finditer(blob):
                    raw = match.group(0).decode("utf-16le").encode("utf-8")
                    _record(counts, lengths, entries, raw, info.filename)
    return {
        "apk": str(path),
        "shared_app_token_count": len(counts["app_"]),
        "user_app_token_count": len(counts["rbuapp_"]),
        "shared_app_token_lengths": sorted(lengths["app_"]),
        "user_app_token_lengths": sorted(lengths["rbuapp_"]),
        "entries": sorted(entries["app_"] | entries["rbuapp_"]),
    }


def _record(counts, lengths, entries, token: bytes, filename: str) -> None:
    kind = "rbuapp_" if token.startswith(b"rbuapp_") else "app_"
    digest = hashlib.sha256(token).hexdigest()
    if digest in counts[kind]:
        return
    counts[kind].add(digest)
    lengths[kind].append(len(token))
    entries[kind].add(filename)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", action="append", default=[], type=Path, help="APK or ZIP to scan")
    parser.add_argument(
        "--allow-apk-tokens",
        action="store_true",
        help="Report APK findings without failing. Use only when assessing an old release.",
    )
    args = parser.parse_args(argv)

    errors = check_source()
    apk_failures = []
    for apk in args.apk:
        report = scan_apk(apk)
        print(
            "apk_scan "
            f"shared_app_token_count={report['shared_app_token_count']} "
            f"user_app_token_count={report['user_app_token_count']} "
            f"shared_lengths={report['shared_app_token_lengths']} "
            f"user_lengths={report['user_app_token_lengths']} "
            f"entries={report['entries']}"
        )
        if report["shared_app_token_count"] or report["user_app_token_count"]:
            apk_failures.append(apk)
    if errors:
        for error in errors:
            print(f"source_error: {error}", file=sys.stderr)
        return 1
    if apk_failures and not args.allow_apk_tokens:
        print(
            "apk_error: token-shaped strings are embedded. Rotate or revoke the live credential. "
            "The values were not printed.",
            file=sys.stderr,
        )
        return 1
    print("source_ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
