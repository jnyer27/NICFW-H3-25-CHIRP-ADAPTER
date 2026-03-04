# TD-H3 nicFW V2.5 Android Channel Editor

Android app for editing the 198 memory channels of a **TIDRADIO TD-H3** running
**nicFW V2.5** firmware. Connects over **Bluetooth Low Energy** (BLE) using the same
service UUID as [nicFWRemoteBT](https://github.com/nicsure/nicfwremotebt), and
implements the same EEPROM protocol and channel layout as the Python CHIRP driver in
the parent repo ([tidradio_h3_nicfw25.py](../tidradio_h3_nicfw25.py)).

---

## Features

- **BLE scan & connect** — scans for the nicFW BLE service and connects automatically;
  no manual pairing step required in most cases
- **Classic SPP fallback** — paired-device picker for older connection methods
- **198-channel list** — shows frequency, name, TX/RX tone, and duplex offset at a glance
- **Per-channel editor** — frequency, duplex/offset, name, power, modulation, bandwidth,
  TX tone, and RX tone (CTCSS in Hz or DCS with polarity)
- **EEPROM dump export** — saves raw `.bin` + human-readable tone analysis `.txt` via
  the system share sheet (useful for debugging)

---

## Build

**Command line:**
```bash
cd AnroidNICFW_CH_EDITOR
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
   - TX / RX tone (CTCSS Hz or DCS code + polarity — blank if no tone is set)
   - Duplex offset (`+600kHz`, `-600kHz`, `Split`, or blank for simplex)
4. **Edit a channel** — tap a card to open the editor:
   - RX frequency (MHz), duplex mode, offset or TX frequency
   - Name (max 12 characters)
   - Power, modulation (Auto / FM / AM / USB), bandwidth (Wide / Narrow)
   - TX Tone and RX Tone — single dropdown with all options:
     *None*, 38 CTCSS tones (67.0 – 250.3 Hz), 104 DCS-N codes, 104 DCS-R codes
5. **Save to radio** — tap **Save**. The app writes the updated EEPROM back to the
   radio (with a confirmation prompt) and the radio reboots.

### EEPROM dump (debugging)

Tap the **⋮ overflow menu → Save EEPROM dump…** to export:
- `tdh3_eeprom_<timestamp>.bin` — raw 8 KB image
- `tdh3_tones_<timestamp>.txt` — per-channel tone word breakdown (hex, 9-bit, decoded)

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
