#!/usr/bin/env python3
"""
yaml_to_xml.py — Convert help content YAML to Android XML resource
====================================================================

Reads  : docs/help_reference/help_content.yaml
Writes : AndroidNICFW_CH_EDITOR/app/src/main/res/xml/help_content.xml

Usage (run from repo root):
    python scripts/yaml_to_xml.py

Optional overrides:
    python scripts/yaml_to_xml.py --input path/to/other.yaml --output path/to/out.xml

Re-run this script whenever help_content.yaml is updated, then rebuild the app
to pick up the changes.  The res/xml/help_content.xml file is committed to the
repo so a Python/pyyaml environment is only required when regenerating content,
not for normal app builds.

Requirements:
    pip install pyyaml        (one-time — stdlib xml.etree is always available)
"""

import argparse
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

# ---------------------------------------------------------------------------
# Default paths (relative to this script's parent directory = repo root)
# ---------------------------------------------------------------------------
REPO_ROOT  = Path(__file__).resolve().parent.parent
YAML_IN    = REPO_ROOT / "docs" / "help_reference" / "help_content.yaml"
XML_OUT    = (REPO_ROOT / "AndroidNICFW_CH_EDITOR" / "app" / "src"
              / "main" / "res" / "xml" / "help_content.xml")


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--input",  type=Path, default=YAML_IN,
                   help="Path to source YAML file (default: docs/help_reference/help_content.yaml)")
    p.add_argument("--output", type=Path, default=XML_OUT,
                   help="Path to output XML file  (default: app/src/main/res/xml/help_content.xml)")
    return p.parse_args()


def load_yaml(path: Path) -> list:
    try:
        import yaml
    except ImportError:
        print("ERROR: pyyaml is not installed.  Run:  pip install pyyaml", file=sys.stderr)
        sys.exit(1)

    if not path.exists():
        print(f"ERROR: YAML source not found: {path}", file=sys.stderr)
        sys.exit(1)

    with open(path, encoding="utf-8") as f:
        data = yaml.safe_load(f)

    settings = data.get("settings", []) if isinstance(data, dict) else []
    if not settings:
        print(f"WARNING: no 'settings' list found in {path}", file=sys.stderr)
    return settings


def indent_xml(elem: ET.Element, level: int = 0) -> None:
    """Pretty-print indentation without disturbing text content in leaf nodes."""
    pad  = "\n" + "    " * level
    cpad = "\n" + "    " * (level + 1)
    if len(elem):
        if not (elem.text and elem.text.strip()):
            elem.text = cpad
        for i, child in enumerate(elem):
            indent_xml(child, level + 1)
            # separator after every child except the last
            tail = pad if i == len(elem) - 1 else cpad
            if not (child.tail and child.tail.strip()):
                child.tail = tail
    # top-level element gets a trailing newline
    if level == 0:
        elem.tail = "\n"


def build_xml(settings: list) -> ET.Element:
    root = ET.Element("help-content")
    skipped = 0

    for entry in settings:
        key = str(entry.get("key", "")).strip()
        if not key:
            skipped += 1
            continue

        e = ET.SubElement(root, "entry")
        e.set("key", key)

        screen = str(entry.get("screen", "")).strip()
        if screen:
            e.set("screen", screen)

        title = str(entry.get("title", key)).strip()
        e.set("title", title)

        rng = str(entry.get("range", "")).strip()
        if rng:
            e.set("range", rng)

        default = str(entry.get("default", "")).strip()
        if default:
            e.set("default", default)

        desc_el = ET.SubElement(e, "description")
        desc_el.text = str(entry.get("description", "")).strip()

        notes = str(entry.get("notes", "")).strip()
        if notes:
            notes_el = ET.SubElement(e, "notes")
            notes_el.text = notes

    if skipped:
        print(f"WARNING: skipped {skipped} entries with missing 'key' field", file=sys.stderr)

    return root


def write_xml(root: ET.Element, path: Path) -> None:
    indent_xml(root)
    path.parent.mkdir(parents=True, exist_ok=True)

    # Write with explicit UTF-8 declaration at the top
    xml_bytes = ET.tostring(root, encoding="unicode")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write(xml_bytes)
        f.write("\n")


def main() -> None:
    args = parse_args()

    print(f"Reading  : {args.input}")
    settings = load_yaml(args.input)
    print(f"  Loaded : {len(settings)} entries")

    root = build_xml(settings)

    print(f"Writing  : {args.output}")
    write_xml(root, args.output)

    entry_count = len(root)
    print(f"  Written: {entry_count} <entry> elements")
    print("Done.")


if __name__ == "__main__":
    main()
