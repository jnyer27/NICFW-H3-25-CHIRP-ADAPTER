# Changelog — TD-H3 nicFW Android Channel Editor

All notable changes to this app are documented here. Version numbers follow [Semantic Versioning](https://semver.org/).

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
