#!/usr/bin/env python3
"""Patch help_content.yaml default: fields from EEPROM dump values."""
import re, sys

yaml_path = r"C:\Users\jason\Documents\GitHub\NICFW-H3-25-CHIRP-ADAPTER\docs\help_reference\help_content.yaml"

# 20 updates from EEPROM dump (PIN and xtal_671 intentionally excluded)
# Format: (yaml_key, old_default, new_default)
UPDATES = [
    ("sq_noise_lev",   "47",        "38"),
    ("noise_ceiling",  "0",         "55"),
    ("lcd_timeout",    "Off",       "15"),
    ("dim_brightness", "Off",       "3"),
    ("tx_mod_meter",   "Off",       "On"),
    ("tx_timeout",     "Off",       "120"),
    ("step",           "12.5 kHz",  "25.0 kHz"),
    ("rfi_comp",       "Off",       "5"),
    ("dual_watch",     "Off",       "On"),
    ("scan_resume",    "Off",       "10"),
    ("scan_range",     "10 MHz",    "29 MHz"),
    ("ultra_scan",     "Off",       "7"),
    ("tone_monitor",   "Off",       "On"),
    ("dtmf_volume",    "32",        "80"),
    ("dtmf_speed",     "0",         "11"),
    ("dtmf_seq_pause", "0",         "1.0"),
    ("batt_style",     "Icon",      "Percent"),
    ("dw_delay",       "1",         "5"),
]

# Calibration entries (YAML key differs from EEPROM display name)
# (tune) uhf_cap -> yaml key is "uhf_power_cap", default 255 -> 130
# (tune) vhf_cap -> yaml key is "vhf_power_cap", default 255 -> 130
TUNE_UPDATES = [
    ("uhf_power_cap",  "255",  "130"),
    ("vhf_power_cap",  "255",  "130"),
]

ALL_UPDATES = UPDATES + TUNE_UPDATES

text = open(yaml_path, encoding="utf-8").read()
original = text

applied = []
skipped = []

for key, old_val, new_val in ALL_UPDATES:
    # Match the entry block starting at "- key: <key>" and find its "default:" line
    # Pattern: find "- key: <key>" then within the next ~20 lines find "    default: "<old_val>""
    pattern = r'(- key: ' + re.escape(key) + r'\n(?:(?!- key:).)*?    default: ")' + re.escape(old_val) + r'"'
    replacement = r'\g<1>' + new_val + '"'
    new_text, count = re.subn(pattern, replacement, text, count=1, flags=re.DOTALL)
    if count == 1:
        text = new_text
        applied.append((key, old_val, new_val))
    else:
        skipped.append((key, old_val, new_val))

if skipped:
    print("WARNING: Could not find/replace the following entries:")
    for key, old_val, new_val in skipped:
        print(f"  key={key!r}  old={old_val!r}  new={new_val!r}")
    print()

if text == original:
    print("No changes were made.")
    sys.exit(0)

open(yaml_path, "w", encoding="utf-8").write(text)
print(f"Applied {len(applied)} updates:")
for key, old_val, new_val in applied:
    print(f"  {key:<24}  {old_val!r:>12}  ->  {new_val!r}")
print()
print("Done. Run 'python scripts/yaml_to_xml.py' to regenerate XML.")
