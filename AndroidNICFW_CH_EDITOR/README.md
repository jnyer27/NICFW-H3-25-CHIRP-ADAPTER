# TD-H3 nicFW V2.5 Android Channel Editor

Android app for editing the 198 memory channels of a **TIDRADIO TD-H3** running
**nicFW V2.5** firmware. Connects over **Bluetooth Low Energy** (BLE) using the same
service UUID as [nicFWRemoteBT](https://github.com/nicsure/nicfwremotebt), and
implements the same EEPROM protocol and channel layout as the Python CHIRP driver in
the parent repo ([tidradio_h3_nicfw25.py](../tidradio_h3_nicfw25.py)).
Download APK Here: https://drive.google.com/file/d/15VHP9435Xb3QcM1pZcxp9L_YZdm8Gk0J/view?usp=drive_link

---

## Features

- **BLE scan & connect** — scans for the nicFW BLE service and connects automatically;
  no manual pairing step required in most cases
- **Classic SPP fallback** — paired-device picker for older connection methods
- **198-channel list** — shows frequency, name, active group labels, TX/RX tone, and
  duplex offset at a glance
- **Band plan TX indicator** — channels whose frequency is blocked for TX by the radio's
  band plan show `(BP)` next to the power level (e.g. `4.9W (BP)` for FM broadcast)
- **Per-channel editor** — frequency, duplex/offset, name, power, modulation, bandwidth,
  TX tone, RX tone (CTCSS in Hz or DCS with polarity), and up to 4 group assignments
- **Band Plan Editor** — view and edit all 20 nicFW band plan entries (start/end
  frequency, TX allowed, scan wrap, modulation override, bandwidth override, max power);
  changes are written directly to the EEPROM
- **Group Label Editor** — edit the 15 group labels (A–O) that are stored in the radio
  EEPROM; labels are shown throughout the app in place of raw letter codes
- **EEPROM dump export** — saves raw `.bin` + human-readable tone analysis `.txt` via
  the system share sheet (useful for debugging)

---

## Build

**Command line:**
```bash
cd AndroidNICFW_CH_EDITOR
./gradlew assembleDebug          # Linux / macOS / Git Bash
gradlew.bat assembleDebug        # Windows CMD / PowerShell
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

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
3. The app scans for the nicFW BLE service (`0000ff00-…`) and connects automatically.

### Classic SPP (fallback)

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

| Item | Description |
|---|---|
| **Edit Band Plan…** | Open the Band Plan Editor (enabled after loading EEPROM) |
| **Edit Group Labels…** | Open the Group Label Editor (enabled after loading EEPROM) |
| **Save EEPROM dump…** | Export raw `.bin` + tone analysis `.txt` via the share sheet |

### Band Plan Editor

Tap **⋮ → Edit Band Plan…** to view and edit all 20 nicFW band plan entries.

Each entry covers:

| Field | Description |
|---|---|
| Start / End (MHz) | Frequency range for this entry |
| TX Allowed | Whether the radio permits transmitting in this range |
| Scan Wrap | Whether scan wraps at this band boundary |
| Max Power | 0 = Ignore (no limit); 1–255 = power setting ceiling |
| Modulation | Ignore / FM / AM / USB / Auto / Enforce FM / Enforce AM / Enforce USB |
| Bandwidth | Ignore / Wide / Narrow / FM Tuner (raw 5) / BW(3/4/6/7) placeholders |

Tap a slot to edit it. Tap **Clear Entry** to zero the slot. Tap **Save** to write all
changes back to the EEPROM (the band plan section is updated immediately; tap **Save to
radio** in the main screen to persist to the radio).

### Group Label Editor

Tap **⋮ → Edit Group Labels…** to rename any of the 15 groups (A–O). Labels are stored
directly in the radio EEPROM (6 chars each at offset `0x1C90`) and are updated throughout
the app immediately on save. Tap **Save to radio** in the main screen to persist changes.

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
