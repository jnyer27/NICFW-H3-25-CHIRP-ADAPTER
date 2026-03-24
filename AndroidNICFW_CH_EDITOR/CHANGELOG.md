# Changelog — TD-H3 nicFW Android Channel Editor

All notable changes to this app are documented here. Version numbers follow [Semantic Versioning](https://semver.org/).

## Unreleased

## [2.3.0] — 2026-03-24

### Documentation

- **User Guide PDF** — Regenerated via MkDocs Material and a print stylesheet (Playwright), aligned with the in-repo guide.
- **Connection clarity** — Explains **USB‑C + CHIRP on PC** vs **BLE on Android**, and labels **Classic Bluetooth (SPP)** as *unconfirmed* for nicFW 2.5 (prefer BLE).

### Android project notes

- **README** — BLE scan/MTU notes and Classic SPP caveat to match the guide.

## [2.2.0] — 2026-03-24

### Search RepeaterBook

- **Scrollable screen** — Form, results, filter, and actions share one vertical scroll so tall forms don’t push results off-screen.
- **Select all visible** — Toolbar item and on-screen control select every row that matches the **Filter results** box (after search).
- **US amateur proximity** — When using **Amateur** + US coordinates + distance, optional **band**, **mode**, **frequency**, and **More search options** (features, operational status, simplex nodes) align with RepeaterBook **Proximity 2.0**.
- **State / province required (export API)** — For **United States**, **Canada**, and **Mexico**, **State / province** must not be **All** when the app uses the JSON **export** path. Use a real jurisdiction, or enter **latitude**, **longitude**, and **distance** for **US proximity** (GMRS or amateur), which does not require state selection.

### CHIRP import

- **Channel preview** — Preview rows use the same **chip** styling and content as the main channel list (groups, power, mode, duplex/offset, bandwidth, TX/RX tones), with shared logic in **ChannelListChips**.

## [2.1.0] — 2026-03-24

### UI and workflow polish

- **Main channel offset chips** now show signed kHz integers with no units (for example: `+5000`, `+600`, `-600`) to match the channel editor TX/Offset entry style.
- **Protect Tune Settings default** is now **ON** for fresh installs; existing users keep their stored preference.
- **Proximity location helper** now pre-fills distance to **30 miles** when using **Use my location** and the distance field is blank/zero.
- **Documentation refresh** expands RepeaterBook guidance, including API vs proximity behavior and setup notes.

## [2.0.0] — 2026-03-24

### RepeaterBook support

- **Major feature release: "Repeaterbook Support!"** — Adds integrated RepeaterBook search/import flow with CHIRP-style filtering and direct handoff to CHIRP import preview.
- **Proximity workflows** — Supports US proximity HTML flows for both GMRS (`prox_result.php`) and amateur (`prox2_result.php`), with miles default (`Dunit=m`) and optional location-based search.
- **Tone enrichment** — Fetches repeater detail pages to populate uplink/downlink tones (mapped to `PL`/`TSQ`) when available.
- **UI polish** — Simplifies menu to one RepeaterBook search entry and improves readability/styling consistency for action buttons and chips in dark mode.

## [1.2.0] — 2026-03-23

### BLE

- **Setup failures** — Service discovery errors, missing UART service/characteristics, or a failed CCCD (notification enable) write now report failure to the caller and **disconnect + close GATT on the main thread**, instead of leaving a half-open connection or treating a bad descriptor write as success.
- **Stream teardown** — `BleRadioStream` clears its GATT and write-characteristic references on `close()` to avoid use-after-close writes.

## [1.1.0] — 2026-03-20

### BLE (Bluetooth Low Energy)

- **Scan filters** — Discovery uses Android scan filters for known BLE UART service UUIDs (HM-10/TI `FFE0`, Nordic UART, Microchip/ISSC, nicFW `FF00`). Only devices that **advertise** one of these services appear in the list, reducing clutter from unrelated peripherals.
- **Safer MTU negotiation** — Replaces a fixed `requestMtu(512)` with a **247-byte** MTU request and **20-byte** fallback. Many cheap BLE UART adapters disconnect or misbehave on aggressive 512-byte MTU; chunk size is clamped after negotiation and defaults to 20 bytes if MTU fails.
- **MTU timeout fallback** — If `onMtuChanged` never runs (some stacks/devices), service discovery still starts after **800 ms** so connect does not hang indefinitely.
- **Multi-service GATT** — After connect, the first matching supported UART service on the GATT is selected (same UUID ordering as above), improving compatibility with dongles that expose more than one profile.

`BleRadioStream` write buffering and notification handling are unchanged; EEPROM protocol traffic is still byte-identical to serial.

### Notes

- If your radio or adapter **does not include a UART service UUID in its advertisement**, it may no longer appear in the BLE scan list. In that case use **Classic SPP** (paired devices) or report the advertising data so a filter adjustment can be considered.

[1.1.0]: https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER/releases/tag/android-editor-v1.1.0
[1.2.0]: https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER/releases/tag/android-editor-v1.2.0
[2.0.0]: https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER/releases/tag/android-editor-v2.0.0
[2.1.0]: https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER/releases/tag/android-editor-v2.1.0
[2.2.0]: https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER/releases/tag/android-editor-v2.2.0
[2.3.0]: https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER/releases/tag/android-editor-v2.3.0
