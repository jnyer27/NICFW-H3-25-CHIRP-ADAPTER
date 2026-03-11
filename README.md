# TIDRADIO TD-H3 nicFW V2.5 CHIRP Adapter

Standalone CHIRP clone-mode driver for the **TIDRADIO TD-H3** running **nicFW V2.5** firmware. This driver is built from the official memory maps and protocol documentation; it is **not** a fork of the older V2.0X driver.

**Target:** nicFW **V2.5X** only. V2.0X codeplugs use little-endian and a different layout and are not supported.

## Documentation

- **Memory maps and protocol:** [nicsure/nicfw2docs](https://github.com/nicsure/nicfw2docs)
  EEPROM layout, `channelInfo`, `settingsBlock`, `bandPlan`, `scanPreset`, and programmer–radio protocol.
- **Protocol summary:** 38400 baud (wired), 8N1. EEPROM read/write by 32-byte blocks (packet IDs `0x30` / `0x31`). Disable radio before R/W, enable after, reboot after write.

## Installation (Option A – standalone)

1. Install [CHIRP](https://chirpmyradio.com/projects/chirp/wiki/Download) and set up a Python 3 environment (see [CHIRP Python 3 setup](https://chirpmyradio.com/projects/chirp/wiki/DevelopersPython3Environment) if building from source).
2. Copy `tidradio_h3_nicfw25.py` into your CHIRP drivers directory:
   - **From source:**
     `cp tidradio_h3_nicfw25.py /path/to/chirp/chirp/drivers/`
   - **Pip/system install:**
     Copy into the same directory where CHIRP's `drivers` package lives (e.g. inside the chirp install).
3. (Optional) Run the unit tests (see below).

## Usage

1. Connect the TD-H3 via the programming cable (or ensure the radio is in programming mode over BLE if using that path).
2. In CHIRP, choose **Radio → Load from radio**.
3. Select **TIDRADIO** / **TD-H3 nicFW 2.5** and the correct serial port.
4. Download (read) or upload (write) the codeplug as usual.

**Warning:** Writing an incorrect or incompatible image can damage the radio or require recovery. Use at your own risk. Prefer a full read backup before any write.

## Tests

From the project root (with CHIRP and pytest available):

```bash
# Optional: install pytest
pip install -r requirements-dev.txt

# Run tests (CHIRP must be on PYTHONPATH, e.g. from a CHIRP source clone or install)
export PYTHONPATH=/path/to/chirp/source/root:$PYTHONPATH   # if using CHIRP source
python -m pytest tests/ -v
```

To generate the sample image file used by one of the tests:

```bash
python3 tests/build_sample_image.py tests/images/TIDRADIO_TD-H3_nicFW25.img
```

Tests use a minimal 8 KB image (valid V2.5 settings magic and one channel) and do not require a radio. If CHIRP is not importable, tests are skipped.

## Layout (V2.5)

- **0x0000–0x001F:** VFO A
- **0x0020–0x003F:** VFO B
- **0x0040–0x18FF:** 198 memory channels (32 bytes each)
- **0x1900–0x19FF:** Settings block (magic `0xD82F`)
- **0x1A00:** Bandplan magic `0xA46D`
- **0x1A02–0x1ACF:** 20 band plan entries (10 bytes each)
- **0x1B00–0x1BFF:** 20 scan presets (20 bytes each)
- **0x1C90–0x1CCF:** 15 group labels (6 bytes each, A–O)
- **0x1DFB:** XTAL (crystal) calibration, then max-power bytes (per-radio)
- **0x1E00 / 0x1F00:** Power tables (see [eeprom.md](https://github.com/nicsure/nicfw2docs/blob/main/eeprom.md)).

## Valid frequency ranges

The driver accepts channels in the following bands:

| Range | Notes |
|---|---|
| 76.0 – 108.0 MHz | FM broadcast (RX only; TX blocked by band plan) |
| 136.0 – 174.0 MHz | VHF (TX/RX) |
| 400.0 – 480.0 MHz | UHF (TX/RX) |

Channels in the FM broadcast range should use **Power = N/T**; the radio's band plan enforces this regardless of the stored power level.

## Band Plan (Settings → Band Plan)

The 20 band plan entries stored at `0x1A02` control per-band TX permission, modulation, bandwidth, and power limits. The driver reads and writes all 20 slots through the CHIRP Settings panel.

### Band plan field encoding (verified from live EEPROM dump, March 2026)

Each 10-byte entry:

| Bytes | Field | Notes |
|---|---|---|
| 0–3 | Start frequency | u32 big-endian, 10 Hz units |
| 4–7 | End frequency | u32 big-endian, 10 Hz units |
| 8 | Max power | 0 = Ignore (no limit); 1–255 = power setting limit |
| 9 bit 0 | TX Allowed | **1 = TX allowed**, 0 = TX blocked |
| 9 bit 1 | Wrap | **1 = scan wrap enabled**, 0 = disabled |
| 9 bits 2–4 | Modulation | Raw value = list index (see table below) |
| 9 bits 5–7 | Bandwidth | Raw value = list index (see table below) |

### Modulation values (raw = list index, no remapping)

| Raw | Label | Confirmed |
|---|---|---|
| 0 | Ignore | ✓ |
| 1 | FM | ✓ |
| 2 | AM | ✓ |
| 3 | USB | — |
| 4 | Auto | — |
| 5 | Enforce FM | — |
| 6 | Enforce AM | — |
| 7 | Enforce USB | — |

### Bandwidth values (raw = list index, no remapping)

| Raw | Label | Confirmed |
|---|---|---|
| 0 | Ignore | ✓ |
| 1 | Wide | ✓ |
| 2 | Narrow | ✓ |
| 3 | BW(3) | — |
| 4 | BW(4) | — |
| 5 | FM Tuner | ✓ |
| 6 | BW(6) | — |
| 7 | BW(7) | — |

> **Note:** Earlier versions of this driver used incorrect label-to-raw mappings for modulation (raw 1 → AM, raw 2 → Enforce FM) and bandwidth (FM Tuner mapped to raw 3 instead of raw 5). These were corrected in March 2026 based on a live EEPROM dump compared with nicFW Programmer.

## Calibration and cloning

In nicFW V2.5 the **XTAL (crystal) calibration** is stored at **EEPROM 0x1DFB**, not at settings offset 0x10 (that byte is defunct in 2.5). The driver exposes it under **Settings → Calibration (per-radio)** along with the max-power fields.

- **Cloning one radio to another:** A full clone copies XTAL and power calibration from the source radio. Those values are hardware-specific. On the target radio, frequency or power may be off; prefer initializing the target with the official nicFW Programmer **Default State 2.5** file, then copy only channels/codeplug data, or re-calibrate (e.g. Advanced Menu / Freq Adjust) on the target.
- **Editing XTAL in CHIRP:** Use the Calibration group only for fine-tuning on the same radio. For large corrections or a fresh 2.5 install, use the nicFW Programmer's Initialize / default file instead.

## Known issues fixed (March 2026)

| # | Issue | Fix |
|---|---|---|
| 1 | Duplex direction inverted: `txf > rxf` produced `"-"` offset instead of `"+"` | Corrected to `"+" if txf > rxf` |
| 2 | Band plan `txAllowed` / `wrap` polarity inverted: bit=1 was treated as "not allowed" | Removed `not`; bit=1 now correctly means allowed/enabled |
| 3 | Band plan modulation labels wrong (raw 1 mapped to AM, raw 2 to Enforce FM) | Simplified to direct raw=index mapping; corrected labels verified from EEPROM dump |
| 4 | Band plan bandwidth labels wrong (FM Tuner mapped to raw 3 instead of raw 5) | Simplified to direct raw=index mapping; FM Tuner now at index/raw 5 |
| 5 | FM broadcast frequencies (e.g. 88.1 MHz) rejected as out of range | Added 76–108 MHz to `valid_bands` |
| 6 | Busy Lock bit written even when a TX offset or duplex mode is configured (radio does not support Busy Lock on repeater channels) | `set_memory()` now forces `_mem.busyLock = 0` when `mem.duplex` is `+`, `−`, or `split` |
| 7 | Same Busy Lock / duplex rule missing from `get_memory()` return path | `busyLock` field cleared in the returned `mem` object when duplex offset is present, so round-trip read–modify–write cannot re-enable Busy Lock on a repeater channel |

## License

GPL v2+. See the header in `tidradio_h3_nicfw25.py`.
