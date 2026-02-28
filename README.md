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
     Copy into the same directory where CHIRP’s `drivers` package lives (e.g. inside the chirp install).
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
- **0x1A02–:** Band plans, scan presets, group labels, DTMF presets  
- **0x1DFB:** XTAL (crystal) calibration, then max-power bytes (per-radio)  
- **0x1E00 / 0x1F00:** Power tables (see [eeprom.md](https://github.com/nicsure/nicfw2docs/blob/main/eeprom.md)).

## Calibration and cloning

In nicFW V2.5 the **XTAL (crystal) calibration** is stored at **EEPROM 0x1DFB**, not at settings offset 0x10 (that byte is defunct in 2.5). The driver exposes it under **Settings → Calibration (per-radio)** along with the max-power fields.

- **Cloning one radio to another:** A full clone copies XTAL and power calibration from the source radio. Those values are hardware-specific. On the target radio, frequency or power may be off; prefer initializing the target with the official nicFW Programmer **Default State 2.5** file, then copy only channels/codeplug data, or re-calibrate (e.g. Advanced Menu / Freq Adjust) on the target.
- **Editing XTAL in CHIRP:** Use the Calibration group only for fine-tuning on the same radio. For large corrections or a fresh 2.5 install, use the nicFW Programmer’s Initialize / default file instead.

## License

GPL v2+. See the header in `tidradio_h3_nicfw25.py`.
