# TD-H3 nicFW V2.5 Android Channel Editor

Android app for editing the 198 memory channels of a **TIDRADIO TD-H3** running
**nicFW V2.5** firmware. Connects over **Bluetooth Low Energy** (BLE) using the same
service UUID as [nicFWRemoteBT](https://github.com/nicsure/nicfwremotebt), and
implements the same EEPROM protocol and channel layout as the Python CHIRP driver in
the parent repo ([tidradio_h3_nicfw25.py](../tidradio_h3_nicfw25.py)).
Download EEPROM to import so you can test app without a radio: https://drive.google.com/file/d/1b6A2s2pL5FZWGUOK2wS2rodJICGUYH0K/view?usp=sharing


---

## Features

- **BLE scan & connect** — scans for known BLE UART services (nicFW `FF00`, Nordic UART,
  HM-10-style `FFE0`, ISSC, etc.); connects with conservative MTU negotiation for flaky
  adapters; see [CHANGELOG.md](CHANGELOG.md) for v1.1.0 BLE details
- **Classic SPP fallback** *(unconfirmed)* — paired-device picker for older connection methods;
  Classic SPP has not been confirmed to work with nicFW 2.5
- **198-channel list** — shows frequency, name, active group labels, TX/RX tone,
  duplex offset, and bandwidth (W/N) at a glance
- **Band plan TX indicator** — channels whose frequency is blocked for TX by the radio's
  band plan show `(BP)` next to the power level (e.g. `4.9W (BP)` for FM broadcast)
- **Per-channel editor** — frequency, duplex/offset, name, power, modulation, bandwidth,
  TX tone, RX tone (CTCSS in Hz or DCS with polarity), up to 4 group assignments,
  and **Busy Lock** (auto-disabled when a TX offset or duplex mode is configured)
- **Radio Settings Editor** — full editor for the nicFW Settings block (EEPROM 0x1900)
  across 10 sections: Squelch & Audio, Display, TX Settings, RX & Tuning, Scan, VOX,
  Tones & Keys, DTMF, System, Security. Includes RF Gain (AGC/1–42), Power Save (Off/1–20),
  AM AGC Fix, Scrambler IF, TX Sub Tone Deviation, DTMF Volume/Speed/Decode, and more
- **Band Plan Editor** — view and edit all 20 nicFW band plan entries (start/end
  frequency, TX allowed, scan wrap, modulation override, bandwidth override, max power)
- **Scan Presets Editor** — view and edit all 20 spectrum scan preset slots (start/end
  frequency, step, resume, persist, modulation, ultrascan speed, label)
- **Group Label Editor** — edit the 15 group labels (A–O) that are stored in the radio
  EEPROM; labels are shown throughout the app in place of raw letter codes
- **Tune Settings** — edit per-radio calibration values: XTAL 671 correction, VHF/UHF
  power caps (with live watt estimates and ⚠ channel-list advisory when exceeded)
- **Protect Tune Settings** — checkable toggle (overflow menu) that locks calibration
  fields and enables a safe multi-radio write: pre-reads the target radio's 5 calibration
  bytes, patches them into the template copy, then uploads — template is never modified
- **XTAL 671 Calculator** — standalone tool (no EEPROM needed) to compute the nicFW
  XTAL correction value from a measured vs. expected frequency
- **EEPROM dump save / import** — export raw `.bin` for backup or cross-radio cloning;
  import a `.bin` file directly without a Bluetooth connection
- **Multi-select channel operations** — move up/down, move to slot (jump to any position), bulk power, bulk group assign, delete, **export selected channels as CHIRP CSV**
- **Channel search** — toolbar 🔍 icon filters the list live by channel name or group label; **Select All** bulk-selects all visible matches in one tap (great for selecting an entire group)
- **CHIRP CSV import** — fill empty channel slots from a CHIRP-exported CSV (file or clipboard);
  import screen includes group assignment, a **TX Power override spinner** (per-channel CSV value
  or uniform N/T–255), starting-channel picker, and live channel preview; tone parser handles
  blank-mode RepeaterBook exports by falling back to TX-encode-only when `rToneFreq` ≠ 88.5 Hz
- **Context help** — every setting in the Radio Settings, Channel Edit, and Tune Settings
  screens has a lightbulb (?) button that opens an AlertDialog with the setting title,
  value range, factory default, description, and any relevant notes; content is sourced
  from the nicFW V2.5 manual and stored in `docs/help_reference/help_content.yaml`

---

## Build

**Command line:**
```bash
cd AndroidNICFW_CH_EDITOR
./gradlew assembleDebug          # Linux / macOS / Git Bash
gradlew.bat assembleDebug        # Windows CMD / PowerShell
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

**Signed release APK** (keystore entries in `local.properties`; never commit secrets):

```bash
./gradlew assembleRelease    # Linux / macOS / Git Bash
gradlew.bat assembleRelease  # Windows
```

Output: `app/build/outputs/apk/release/app-release.apk`  
Release notes: [CHANGELOG.md](CHANGELOG.md). GitHub **Releases** may attach a renamed copy (e.g. `TD-H3-Editor-1.1.0-release.apk`).

**Android Studio:** Open this folder as a project and click **Run**.

Toolchain: Kotlin 2.0.21 · AGP 8.7.3 · Gradle 9.1 · minSdk 24 · targetSdk 35

### Troubleshooting builds on Windows

If you get "Unable to delete directory" errors during a clean/build, a background
process (antivirus, lingering `java.exe`) is locking the `build/` folder. Fix:

- Exclude the project directory from **Windows Defender real-time scanning**
- Close Android Studio and any other Gradle/Java processes, then retry

---

## Permissions

| Permission | When required |
|---|---|
| `BLUETOOTH_SCAN` | BLE device discovery (Android 12+) |
| `BLUETOOTH_CONNECT` | GATT connection (Android 12+) |
| `BLUETOOTH` + `BLUETOOTH_ADMIN` | Classic BT fallback (Android < 12) |
| `ACCESS_FINE_LOCATION` | Required by Android for BLE scan (Android < 12) |

Grant Bluetooth permissions when prompted after tapping **Connect**.

> **Google Play Protect note:** Some versions of Play Protect flag apps that combine
> `BLUETOOTH_CONNECT` + `ACCESS_FINE_LOCATION` with a "mobile billing" warning. This is
> a false positive — the app contains no SMS, NFC, or billing permissions. Both
> permissions are required solely for BLE scanning on Android < 12.

---

## Connecting to the radio

### BLE (recommended)

1. Enable Bluetooth on the radio (nicFW settings menu).
2. In the app tap **Connect → Scan for Radio (BLE)**.
3. The app shows devices that **advertise** a supported BLE UART service (including nicFW
   `0000ff00-…` and common dongle UUIDs). Tap your radio or adapter to connect.

   If nothing appears, the device may not advertise a known UART UUID in its BLE payload —
   try **Classic SPP** instead, or see [CHANGELOG.md](CHANGELOG.md) under v1.1.0.

### Classic SPP (fallback) *(unconfirmed)*

> **Note:** Classic SPP has not been confirmed to work with the TD-H3 and nicFW 2.5.
> BLE is the recommended and verified connection method.

1. Pair the TD-H3 in **Android Settings → Bluetooth**.
2. In the app tap **Connect → Paired Devices (Classic BT)** and select the radio.

---

## Usage

1. **Connect** — tap Connect and choose a connection method; status shows
   *Connected: \<device name\>*.
2. **Load from radio** — tap **Load**. The app downloads all 256 blocks (8 KB EEPROM)
   and parses channels 1–198.
3. **Browse channels** — scroll the list; each card shows:
   - Channel number
   - RX frequency (MHz)
   - Channel name
   - Active group labels (e.g. "All  MURS" — blank if no groups are assigned)
   - TX / RX tone (CTCSS Hz or DCS code + polarity — blank if no tone is set)
   - Duplex offset (`+600kHz`, `-600kHz`, `Split`, or blank for simplex)
   - Power level — with `(BP)` suffix when TX is blocked by the band plan
   - Bandwidth — `W` (Wide) or `N` (Narrow)
4. **Edit a channel** — tap a card to open the editor:
   - RX frequency (MHz), duplex mode, offset or TX frequency
   - Name (max 12 characters)
   - Power, modulation (Auto / FM / AM / USB), bandwidth (Wide / Narrow)
   - TX Tone and RX Tone — single dropdown with all options:
     *None*, 38 CTCSS tones (67.0 – 250.3 Hz), 104 DCS-N codes, 104 DCS-R codes
   - Group 1–4 — assign up to 4 groups (A–O) from a dropdown showing the group label
   - Frequency validation uses the loaded band plan; FM broadcast (e.g. 88.1 MHz)
     and other RX-only frequencies are accepted as long as they fall within a band
     plan entry
5. **Save to radio** — tap **Save**. The app writes the updated EEPROM back to the
   radio (with a confirmation prompt) and the radio reboots.

### Overflow menu (⋮)

| Item | Notes |
|---|---|
| **Import CHIRP CSV…** | Fill empty channel slots from a CHIRP-exported CSV (requires EEPROM) |
| **Sort Channels by Group…** | Reorder all channels so each group is contiguous (requires EEPROM) |
| **Edit Group Labels…** | Rename groups A–O stored in the EEPROM (requires EEPROM) |
| **Save EEPROM dump…** | Export a raw 8 KB `.bin` backup file (requires EEPROM) |
| **Import EEPROM dump…** | Load a `.bin` file as the active EEPROM (always available) |
| **Edit Band Plan…** | Edit all 20 nicFW band plan entries (requires EEPROM) |
| **Edit Scan Presets…** | Edit all 20 spectrum scan preset slots (requires EEPROM) |
| **Radio Settings…** | Edit the full nicFW Settings block — 56 fields across 10 sections (requires EEPROM) |
| **Tune Settings…** | Edit per-radio calibration: XTAL 671, VHF/UHF power caps (requires EEPROM) |
| **XTAL 671 Calculator…** | Compute XTAL correction from measured vs. expected frequency (always available) |
| **Protect Tune Settings** | Checkable toggle — locks calibration fields and enables safe multi-radio write (always available) |

> For detailed documentation of all overflow menu screens (Radio Settings, Band Plan Editor,
> Scan Presets, Tune Settings, Protect Tune Settings multi-radio workflow, XTAL Calculator,
> and more), see the full [User Guide](../UserGuide.md).

---

## Tone encoding notes

**CTCSS** is stored as a u16 in units of 0.1 Hz (192.8 Hz → `0x0788`).

**DCS** codes are stored as the **decimal integer value of their octal label**:

| CHIRP label | Stored value | EEPROM |
|---|---|---|
| DCS 023 | 19 (= 023 in octal) | `0x8013` |
| DCS 754 | 492 (= 754 in octal) | `0x81EC` |

Bit 15 = DCS flag, bit 14 = polarity R, bits 8–0 = 9-bit code.

---

## References

- Parent repo & Python CHIRP driver: [NICFW-H3-25-CHIRP-ADAPTER](../)
- Protocol and EEPROM layout: [nicsure/nicfw2docs](https://github.com/nicsure/nicfw2docs)
- Bluetooth remote app (BLE UUID source): [nicsure/nicFWRemoteBT](https://github.com/nicsure/nicfwremotebt)
- Full implementation notes: [PLAN.md](PLAN.md)
- User Guide (https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER/blob/main/UserGuide.md)

---

## Tests

Unit tests (protocol checksum, EEPROM parse/write roundtrip) are in
`app/src/test/java/com/nicfw/tdh3editor/`.

```bash
./gradlew test
```

---

## License

Same as the parent repo (GPL v2+). See [../README.md](../README.md).
