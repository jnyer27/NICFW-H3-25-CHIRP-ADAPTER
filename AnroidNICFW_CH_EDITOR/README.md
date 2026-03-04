# AnroidNICFW_CH_EDITOR

Android channel editor for **TIDRADIO TD-H3** with **nicFW V2.5**. Connects to the radio over **Bluetooth serial** (same approach as [nicFWRemoteBT](https://github.com/nicsure/nicfwremotebt)) and uses the same EEPROM protocol and channel layout as the Python CHIRP driver in the parent repo.

## Build

- Open the project in **Android Studio** (or use Gradle from the command line).
- Minimum SDK 24, target SDK 35. Kotlin 2.0.21, Android Gradle Plugin 8.7.3.
- The project includes the Gradle wrapper. Build: **Build → Make Project** or run `./gradlew assembleDebug` (Unix/macOS) or `gradlew.bat assembleDebug` (Windows). No need to run `gradle wrapper` first.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.

### Troubleshooting

- **Windows: "Unable to delete directory" / file lock when cleaning or building**  
  A process (e.g. real-time anti-virus or a lingering `java.exe`) may be holding handles on `app/build` or the project `build/` directory. To avoid this:
  - Exclude the project directory (or at least `build/` and `app/build/`) from **real-time anti-virus scanning** (e.g. Windows Defender).
  - Close other Gradle/Java processes and Android Studio, then run **Invalidate Caches / Restart** or retry the build.
  See also [CONTRIBUTING.md](CONTRIBUTING.md) for development environment tips.

## Permissions

The app requests:

- **Bluetooth Connect** (Android 12+) — to connect to the paired TD-H3.
- **Bluetooth Scan** (optional) — for discovery; can be denied if you only use paired devices.

Grant these when prompted so the app can open the SPP link to the radio.

## Pairing the TD-H3

1. On the radio, enable **Bluetooth** (nicFW settings).
2. On the phone, open **Settings → Bluetooth** and pair with the device (e.g. "TD-H3" or the name shown by the radio).
3. In the app, tap **Connect** and choose the paired radio from the list.

## Usage

1. **Connect** — Tap Connect and select the paired TD-H3. Status shows "Connected: &lt;name&gt;".
2. **Load from radio** — Downloads the full 8 KB EEPROM and parses channels 1–198. Progress shows "Cloning… 1/256 … 256/256".
3. **Channel list** — Scroll the list; tap a channel to edit.
4. **Edit channel** — Set RX frequency (MHz), duplex/offset, name (max 12 chars), power, mode, bandwidth. Save with OK.
5. **Save to radio** — Uploads the current EEPROM image to the radio (with confirmation). The radio reboots after write.

The app uses the same protocol and EEPROM layout as the parent repo’s [tidradio_h3_nicfw25.py](../tidradio_h3_nicfw25.py) and nicFWRemoteBT-style Bluetooth SPP.

## Plan

The full implementation plan is in [PLAN.md](PLAN.md). It covers:

- Bluetooth SPP connection (nicFWRemoteBT-style)
- Protocol layer (clone download/upload, 32-byte blocks)
- EEPROM and channel model (198 channels, V2.5 layout)
- Channel editor UI (connection, load/save, list, edit)
- Testing and alignment with the Python driver

## References

- Parent repo: [NICFW-H3-25-CHIRP-ADAPTER](../) — Python CHIRP driver ([tidradio_h3_nicfw25.py](../tidradio_h3_nicfw25.py))
- Protocol and EEPROM layout: [nicsure/nicfw2docs](https://github.com/nicsure/nicfw2docs)
- Bluetooth remote app: [nicsure/nicFWRemoteBT](https://github.com/nicsure/nicfwremotebt)

## Tests

Unit tests (protocol checksum, EEPROM parse/roundtrip) are in `app/src/test/java/com/nicfw/tdh3editor/`. Run with **Run → Run 'Tests in app'** or `./gradlew test`.

## License

Same as the parent repo (GPL v2+). See the parent [README](../README.md).
