# TD-H3 Channel Editor — User Guide

> **App:** NICFW TD-H3 Channel Editor
> **Radio:** TIDRadio TD-H3 running **nicFW v2.5**
> **Platform:** Android (min SDK 24 / Android 7.0)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Connecting to the Radio](#2-connecting-to-the-radio)
3. [Loading & Saving the Radio Memory](#3-loading--saving-the-radio-memory)
4. [Channel List](#4-channel-list)
5. [Editing a Single Channel](#5-editing-a-single-channel)
6. [Multi-Select Mode](#6-multi-select-mode)
   - [Move Up / Down](#61-move-up--down)
   - [Set TX Power (Bulk)](#62-set-tx-power-bulk)
   - [Set Channel Groups (Bulk)](#63-set-channel-groups-bulk)
   - [Delete / Clear Selected](#64-delete--clear-selected)
7. [Overflow Menu Features](#7-overflow-menu-features)
   - [Import CHIRP CSV](#71-import-chirp-csv)
   - [Sort Channels by Group](#72-sort-channels-by-group)
   - [Edit Group Labels](#73-edit-group-labels)
   - [Save / Import EEPROM Dump](#74-save--import-eeprom-dump)
   - [Edit Band Plan](#75-edit-band-plan)
   - [Edit Scan Presets](#76-edit-scan-presets)
8. [CHIRP Adapter (Python Driver)](#8-chirp-adapter-python-driver)
9. [Tips & Troubleshooting](#9-tips--troubleshooting)

---

## 1. Overview

The **TD-H3 Channel Editor** is an Android companion app for the TIDRadio TD-H3 running
nicFW v2.5 firmware. It communicates with the radio over **Bluetooth** (BLE or Classic SPP)
to read and write the full 8 KB EEPROM — the same memory block that stores all channels,
band plan entries, scan presets, and group labels.

```
┌────────────────────────────────────────────┐
│          TD-H3 Channel Editor              │
├────────────────────────────────────────────┤
│  Status: Connected — TD-H3 (nicFW 2.5)    │
│                              [ Connect ]   │
├────────────────────────────────────────────┤
│  [ Load from Radio ]   [ Save to Radio ]   │
├────────────────────────────────────────────┤
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░  Reading…  72/256 │
├────────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐    │
│ │ 1 │ GMRS 1   │ 462.5625  │ 1.0W  A │    │
│ │ 2 │ GMRS 2   │ 462.5875  │ 1.0W  A │    │
│ │ 3 │ MURS 1   │ 151.8200  │ 5.0W  B │    │
│ │ …                                    │    │
│ └─────────────────────────────────────┘    │
│                               ⋮ (198 ch)  │
└────────────────────────────────────────────┘
```

**Key concept:** All edits are made in memory only. Nothing changes on the radio until
you tap **Save to Radio**. This lets you make multiple changes and upload them all at once.

---

## 2. Connecting to the Radio

Tap **[ Connect ]** on the main screen. A dialog appears with two options:

```
┌──────────────────────────────────┐
│      Connect to Radio            │
├──────────────────────────────────┤
│                                  │
│  [ Scan for Radio (BLE) ]        │
│                                  │
│  [ Paired Devices (Classic BT) ] │
│                                  │
│              [ Cancel ]          │
└──────────────────────────────────┘
```

### Option A — BLE (Recommended)

- Tap **Scan for Radio (BLE)**.
- The app scans for nearby BLE devices. The TD-H3 broadcasts as **TD-H3** or similar.
- Tap the device name in the scan list to connect.
- Android 12+ requires the **Nearby Devices** permission; Android 11 and below requires
  **Location** permission. Grant when prompted.

### Option B — Classic Bluetooth (SPP)

- Pair the radio to your phone first via Android's system Bluetooth settings.
- Tap **Paired Devices (Classic BT)** and select the radio from the list.

### Connection Status

| Status indicator | Meaning |
|---|---|
| `Status: Disconnected` | No radio connected |
| `Status: Connecting…` | Handshake in progress |
| `Status: Connected — <name>` | Ready to load/save |

> **Tip:** The BLE connection uses nicFW's remote-control protocol (0x4A enable /
> 0x4B disable). If the radio firmware does not support remote mode, use Classic BT.

---

## 3. Loading & Saving the Radio Memory

### Load from Radio

Tap **[ Load from Radio ]** (only enabled when connected). The app reads the full 8 KB
EEPROM in 256 × 32-byte blocks. A progress bar and block counter are shown during
the transfer (about 10–30 seconds depending on connection speed).

```
  [ Load from Radio ]   [ Save to Radio ]
  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░
  Reading block 142 / 256…
```

Once loaded, the channel list populates automatically with all 198 channel slots.

### Save to Radio

Tap **[ Save to Radio ]** to write all pending changes back to the radio. The same
progress display is shown for the write pass. After save completes, the radio immediately
uses the new channel data.

> ⚠ **Important:** Saving rewrites the complete EEPROM. Do not disconnect the radio
> or leave the app during a save operation.

---

## 4. Channel List

After loading, the main screen shows all 198 channel slots in a scrollable list.

```
┌──────────────────────────────────────────────────────┐
│ #  │ Name       │  Freq (MHz) │ Power │ Groups │ BW  │
├──────────────────────────────────────────────────────┤
│  1 │ GMRS 1     │ 462.5625   │  1.0W │  A  B  │ Nar │
│  2 │ GMRS 2     │ 462.5875   │  1.0W │  A  B  │ Nar │
│  3 │ MURS 1     │ 151.8200   │  5.0W │  B     │ Nar │
│  4 │ WX1        │ 162.4000   │  N/T  │        │ Wid │
│  5 │ (empty)    │            │       │        │     │
│  …                                               …   │
│ 198│ (empty)    │            │       │        │     │
└──────────────────────────────────────────────────────┘
```

**Column descriptions:**

| Column | Description |
|---|---|
| `#` | Channel slot number (1–198, fixed) |
| Name | Up to 12-character label |
| Freq | RX frequency in MHz (TX offset shown on edit screen) |
| Power | Estimated output power (N/T = no transmit, 0.5W–5W+) |
| Groups | Active group assignments (up to 4 letters) |
| BW | Bandwidth: `Wid` = Wide, `Nar` = Narrow |

- **Tap** a channel row to open the channel editor.
- **Long-press** a channel row to enter [Multi-Select Mode](#6-multi-select-mode).
- **Drag** a channel by its drag handle (≡) to reorder within the list. Drag
  reordering writes new slot numbers into the EEPROM immediately in memory.

---

## 5. Editing a Single Channel

Tap any channel row to open the **Channel Editor**.

```
┌────────────────────────────────────────────────┐
│ ← Edit Channel 3                               │
├────────────────────────────────────────────────┤
│                                                │
│  RX Frequency (MHz)   [ 151.8200             ] │
│  TX Offset / Freq     [ 0                    ] │
│  Duplex               [ (none)               ] │
│  Name                 [ MURS 1               ] │
│                                                │
│  Power     [ 130 ▼]   Mode  [ FM ▼]           │
│  Bandwidth [ Wide ▼]                           │
│                                                │
│  TX Tone   [ None ▼]                           │
│  RX Tone   [ None ▼]                           │
│                                                │
│  Group 1   [ B – MURS ▼]                      │
│  Group 2   [ None ▼]                          │
│  Group 3   [ None ▼]                          │
│  Group 4   [ None ▼]                          │
│                                                │
│  [  Cancel  ]              [  Save  ]          │
└────────────────────────────────────────────────┘
```

### Field Reference

| Field | Valid Values | Notes |
|---|---|---|
| **RX Frequency** | MHz, 4 decimal places | Leave blank to mark slot as empty |
| **TX Offset / Freq** | kHz (duplex ±) or MHz (split) | Depends on Duplex setting |
| **Duplex** | (blank), `+`, `-`, `split` | `+` adds offset, `-` subtracts, `split` sets independent TX |
| **Name** | Up to 12 characters | Stored as ASCII in EEPROM |
| **Power** | N/T, 1–255 (raw byte) | See power-to-watt table below |
| **Mode** | Auto, FM, AM, USB | |
| **Bandwidth** | Wide, Narrow | |
| **TX Tone** | None, CTCSS 67.0–250.3 Hz, DCS 023–754 N/R | Flat picker — all tones in one list |
| **RX Tone** | Same as TX Tone | Squelch decode tone |
| **Group 1–4** | None, A–O (with custom labels) | Up to 4 groups per channel |

### Power Calibration

The power spinner stores a raw byte (0–255). Approximate watt equivalents:

| Raw value | Approx. watts |
|---|---|
| 0 / N/T | No transmit |
| 29 | 0.5 W |
| 58 | 2.0 W |
| 130 | 5.0 W |
| 255 | ~8.7 W (extrapolated) |

> Values between calibration points are linearly interpolated. The watt label shown
> in the channel list is an estimate; actual output depends on battery and frequency.

### Tone Picker Format

The flat tone spinner uses these formats:

| Display | Type |
|---|---|
| `None` | No tone |
| `CTCSS 100.0 Hz` | CTCSS sub-audible tone |
| `DCS 023 N` | DCS code 023, Normal polarity |
| `DCS 023 R` | DCS code 023, Reverse polarity |

### Saving / Clearing a Channel

- Tap **Save** to write changes into the in-memory EEPROM buffer.
- To **clear** a channel slot (make it empty), delete the RX Frequency field and tap Save.
- Changes take effect on the radio only after **Save to Radio** from the main screen.

---

## 6. Multi-Select Mode

**Long-press** any channel row to activate multi-select mode. A colored action bar
appears at the bottom of the screen:

```
┌──────────────────────────────────────────────────────────┐
│  Channel List                                            │
│  …                                                       │
│ ✓ │ GMRS 1  │ 462.5625  │  1.0W │  A  │ Nar  ← selected │
│ ✓ │ GMRS 2  │ 462.5875  │  1.0W │  A  │ Nar  ← selected │
│   │ MURS 1  │ 151.8200  │  5.0W │  B  │ Nar              │
│ ✓ │ GMRS 3  │ 462.6125  │  1.0W │  A  │ Nar  ← selected │
│  …                                                       │
├──────────────────────────────────────────────────────────┤
│ 3 selected  │ ↑ │ ↓ │ 📡 │ 🏷 │ 🗑 │ Done │
└──────────────────────────────────────────────────────────┘
```

**Selection bar buttons:**

| Button | Icon | Action |
|---|---|---|
| **Move Up** | ↑ | Shifts all selected channels one slot up (swaps with the channel above) |
| **Move Down** | ↓ | Shifts all selected channels one slot down |
| **Set TX Power** | 📡 | Opens a power picker — applies chosen power to all selected channels |
| **Set Groups** | 🏷 | Opens a group editor — assigns groups to all selected channels |
| **Delete** | 🗑 | Clears (empties) all selected channel slots |
| **Done** | text | Exits multi-select mode, returns to normal browsing |

- Tap additional rows while in multi-select mode to add/remove them from the selection.
- Tap a selected row again to deselect it.
- The selection count in the bar updates in real time.

---

### 6.1 Move Up / Down

Move Up (↑) and Move Down (↓) shift the selected channels one position at a time in the
channel list. The swap is performed in memory; tap **Save to Radio** afterward to persist.

> **Tip:** For large reorders, use **Sort Channels by Group** from the overflow menu
> instead of manually moving channels one by one.

---

### 6.2 Set TX Power (Bulk)

Tap the **📡** (RF transmit / antenna) button while channels are selected:

```
┌─────────────────────────────┐
│  Set TX Power               │
│                             │
│  ┌───────────────────────┐  │
│  │         126           │  │
│  │   ─────────────────   │  │
│  │  ▶      130      ◀   │  │   ← scroll wheel
│  │   ─────────────────   │  │
│  │         131           │  │
│  └───────────────────────┘  │
│                             │
│     [ Cancel ]  [ Apply ]   │
└─────────────────────────────┘
```

- The scroll wheel (NumberPicker) is pre-seeded to the current power of the **first
  selected non-empty channel**.
- Scroll the wheel to pick any value: `N/T`, `1`–`255`.
- Tap **Apply** to write the chosen power to every selected non-empty channel.
- Empty slots in the selection are skipped.

**Power values at a glance:**

```
N/T ─── 29 ─────── 58 ──────────── 130 ────────── 255
 0W    0.5W        2W              5W            ~8.7W
```

---

### 6.3 Set Channel Groups (Bulk)

Tap the **🏷** (label/tag) button while channels are selected:

```
┌──────────────────────────────────────────┐
│  Set Groups for Selected Channels        │
├──────────────────────────────────────────┤
│                                          │
│  Group Slot 1  [ — Keep — ▼]            │
│  Group Slot 2  [ A — GMRS ▼]            │
│  Group Slot 3  [ — Keep — ▼]            │
│  Group Slot 4  [ — Keep — ▼]            │
│                                          │
│          [ Cancel ]  [ Apply ]           │
└──────────────────────────────────────────┘
```

**Spinner options for each group slot:**

| Option | Meaning |
|---|---|
| `— Keep —` | Do not change this slot on the selected channels |
| `None` | Clear this slot (remove group assignment) |
| `A` through `O` (with labels) | Assign the displayed group |

- Each slot spinner is **independent** — you can set slot 2 to `A — GMRS` while leaving
  slots 1, 3, and 4 unchanged (keep their current values).
- Custom group names (e.g., "GMRS", "MURS") are shown inline from the **Group Labels** you
  defined in Edit Group Labels.
- Tap **Apply** to bulk-assign groups to all selected non-empty channels.

---

### 6.4 Delete / Clear Selected

Tap the **🗑** (delete) button to clear all selected channels. A confirmation dialog
appears showing the count. Clearing sets those slots to empty (all bytes zeroed).

> Empty slots appear as `(empty)` in the channel list and can be reused by importing
> CHIRP CSV data or by editing them manually.

---

## 7. Overflow Menu Features

Tap the **⋮** (three-dot) menu in the top-right toolbar to access advanced features.

```
┌──────────────────────────┐
│ ⋮                        │
│  Import CHIRP CSV…       │
│  Sort Channels by Group… │
│  Edit Group Labels…      │
│  Save EEPROM dump…       │
│  Import EEPROM dump…     │
│  Edit Band Plan…         │
│  Edit Scan Presets…      │
└──────────────────────────┘
```

> All items except **Import EEPROM dump** require an EEPROM to be loaded first.

---

### 7.1 Import CHIRP CSV

Import a frequency list exported by the CHIRP radio programming software.

#### Steps

1. Export your channel list from CHIRP as a CSV file (`File → Export`).
2. Transfer the CSV to your Android device (email, cloud storage, USB, etc.).
3. In the app, tap **⋮ → Import CHIRP CSV…** and pick the file.
4. The **CHIRP Import** screen opens:

```
┌──────────────────────────────────────────────────────┐
│ ← Import CHIRP CSV                                   │
├──────────────────────────────────────────────────────┤
│  24 channel(s) in CSV · 31 empty slot(s) available   │
│  ✓ All 24 channels will be imported                  │
├──────────────────────────────────────────────────────┤
│  Assign groups to imported channels (optional):      │
│                                                      │
│  Group Slot 1  [ A — GMRS ▼]                        │
│  Group Slot 2  [ None     ▼]                        │
│  Group Slot 3  [ None     ▼]                        │
│  Group Slot 4  [ None     ▼]                        │
├──────────────────────────────────────────────────────┤
│  Preview (24 channels):                              │
│  → Ch 8  │ GMRS 1   │ 462.5625 MHz  │ T: 100.0 Hz   │
│  → Ch 9  │ GMRS 2   │ 462.5875 MHz                  │
│  → Ch 10 │ WX1      │ 162.4000 MHz  (CSV #43)       │
│  …                                                   │
├──────────────────────────────────────────────────────┤
│  [ Cancel ]                     [ Import ]           │
└──────────────────────────────────────────────────────┘
```

5. Optionally assign all imported channels to up to 4 groups using the spinners.
6. Tap **Import** to write channels into the first available empty slots.
7. Tap **Save to Radio** on the main screen to upload.

> ⚠ Imported channels fill **empty slots only** — existing channels are never overwritten.
> If there are more CSV entries than empty slots, a warning is shown and excess entries
> are skipped.

---

### 7.2 Sort Channels by Group

Automatically reorganize all channels so that each group's channels are contiguous in
the channel list (useful for radios that scan group-by-group).

```
┌────────────────────────────────────────────────────┐
│ ← Sort Channels by Group                          │
├────────────────────────────────────────────────────┤
│  Drag groups into your preferred order.            │
│  Channels will be reorganized so each group's      │
│  channels appear together.                         │
│                                                    │
│  48 grouped across 5 group(s) · 12 ungrouped       │
│  · 138 empty slots                                 │
│                                                    │
│  ≡  A  GMRS           (24 ch)                     │
│  ≡  B  MURS            (5 ch)                     │
│  ≡  C  Weather         (7 ch)                     │
│  ≡  D  Local          (12 ch)                     │
│  ≡  E  Simplex         (3 ch)  ← drag handle (≡)  │
│  ≡  F  (no label)      (0 ch)                     │
│  …                                                 │
│                                                    │
│  Ungrouped channels will be placed after           │
│  all group blocks. Empty slots always moved last.  │
│                                                    │
│  [ Cancel ]              [ Sort ]                  │
└────────────────────────────────────────────────────┘
```

#### How it works

1. Drag group rows by their **≡** handle to set priority order.
2. Groups higher in the list get lower channel numbers after sorting.
3. Tap **Sort** — a confirmation dialog shows the final order before committing.
4. The sort is **stable**: channels within the same group keep their relative order.
5. Channels with multiple group assignments are sorted under their
   **highest-priority group** (the one earliest in your drag order).
6. Ungrouped channels are appended after all group blocks; empty slots go last.
7. Tap **Save to Radio** afterward.

> **Example sort order:** A (GMRS) → B (MURS) → C (Weather) → Ungrouped → Empty
> Result: channels 1–24 = GMRS, 25–29 = MURS, 30–36 = Weather, 37–48 = ungrouped.

---

### 7.3 Edit Group Labels

Assign human-readable names to the 15 group letters (A–O). Labels are stored in the
EEPROM at offset `0x1C90` (6 bytes each, max 5 characters + null).

```
┌──────────────────────────────────────┐
│ ← Edit Group Labels                 │
├──────────────────────────────────────┤
│  A  │ [ GMRS        ]               │
│  B  │ [ MURS        ]               │
│  C  │ [ Weather     ]               │
│  D  │ [ Local       ]               │
│  E  │ [ Simplex     ]               │
│  F  │ [             ]               │
│  G  │ [             ]               │
│  H  │ [             ]               │
│  I  │ [             ]               │
│  J  │ [             ]               │
│  K  │ [             ]               │
│  L  │ [             ]               │
│  M  │ [             ]               │
│  N  │ [             ]               │
│  O  │ [             ]               │
├──────────────────────────────────────┤
│  [ Cancel ]           [ Save ]      │
└──────────────────────────────────────┘
```

- Max **5 characters** per label (enforced by the text field).
- Labels appear throughout the app (channel editor group spinners, bulk group dialog,
  CHIRP import group spinners, sort screen).
- Tap **Save** — labels are written into the in-memory EEPROM and take effect in the
  app immediately. Upload to radio with **Save to Radio**.

---

### 7.4 Save / Import EEPROM Dump

#### Save EEPROM dump

Saves the currently loaded EEPROM as a raw `.bin` file to your device storage.

- The file is named `nicfw25_YYYYMMDD_HHmmss.bin`.
- Use this as a backup before making changes, or to share an EEPROM snapshot.

#### Import EEPROM dump

Loads a previously saved `.bin` file as the active EEPROM (bypasses Bluetooth).

- The file must be exactly 8192 bytes (8 KB).
- After import, the channel list refreshes as if you had loaded from the radio.
- Use **Save to Radio** to push the loaded dump to a connected radio.

> **Workflow example:** Clone one radio's configuration to another:
> 1. Connect Radio A → **Load from Radio** → **Save EEPROM dump**.
> 2. Connect Radio B → **Import EEPROM dump** → **Save to Radio**.

---

### 7.5 Edit Band Plan

Define up to 20 frequency band entries that control which frequencies are available
for TX on the radio. nicFW 2.5 stores the Band Plan at EEPROM offset `0x1A00`.

```
┌────────────────────────────────────────────────────────────┐
│ ← Edit Band Plan                                          │
├────────────────────────────────────────────────────────────┤
│  Band plan controls which frequencies allow TX.           │
│  The radio's Band Plan overrides per-channel power.       │
├────────────────────────────────────────────────────────────┤
│  #1  │  136.000 – 174.000 MHz  │ TX ✓  FM  Wide  100      │
│  #2  │  400.000 – 480.000 MHz  │ TX ✓  FM  Wide  100      │
│  #3  │  108.000 – 136.000 MHz  │ TX ✗  AM  Narrow  0      │
│  #4  │  151.820 – 154.600 MHz  │ TX ✓  FM  Narrow  50     │  ← MURS
│  …                                                         │
│  #20 │  (empty)                                            │
├────────────────────────────────────────────────────────────┤
│  [ Cancel ]                     [ Save ]                   │
└────────────────────────────────────────────────────────────┘
```

Tap any row to edit that band plan entry:

```
┌────────────────────────────────────────┐
│ ← Band Plan Entry #4                  │
├────────────────────────────────────────┤
│  Start Freq (MHz)  [ 151.8200       ] │
│  End Freq (MHz)    [ 154.6000       ] │
│  Max Power         [ 50             ] │
│  TX Allowed        [ ✓ Yes         ] │
│  Modulation        [ FM  ▼         ] │
│  Bandwidth         [ Narrow ▼      ] │
│                                        │
│  [ Clear Entry ]                       │
│  [ Cancel ]          [ Save Entry ]    │
└────────────────────────────────────────┘
```

**Band Plan Field Reference:**

| Field | Description |
|---|---|
| Start / End Freq | Frequency range in MHz |
| Max Power | Raw power cap (0–255); 0 = Ignore (no cap applied) |
| TX Allowed | When off, the radio treats this range as RX-only |
| Modulation | Ignore / FM / AM / USB / Auto / Enforce variants |
| Bandwidth | Ignore / Wide / Narrow / FM Tuner |

> The radio applies Band Plan entries in order — the first matching entry wins.
> A catch-all entry (wide range, TX off) placed last covers all unlicensed frequencies.

---

### 7.6 Edit Scan Presets

Configure up to 20 scan presets stored at EEPROM offset `0x1B00`. Each preset defines
a frequency range for the radio's spectrum scanner.

```
┌─────────────────────────────────────────────────────────────────┐
│ ← Edit Scan Presets                                            │
├─────────────────────────────────────────────────────────────────┤
│  Scan presets define frequency ranges for the built-in        │
│  spectrum scanner. Up to 20 slots available.                  │
├─────────────────────────────────────────────────────────────────┤
│  #1  │ 108.00000 – 137.00000 MHz  "Air Band"                  │
│      │  Step: 25.00 kHz  Resume: 10  Persist: 0  AM  Ultra: 7 │
│  #2  │ 137.00000 – 144.00000 MHz  "Satellite"                 │
│      │  Step: 25.00 kHz  Resume: 5   Persist: 0  FM  Ultra: 7 │
│  …                                                             │
│  #13 │ 462.55000 – 462.75000 MHz  "GMRS"                      │
│      │  Step: 12.50 kHz  Resume: 0  Persist: 0  FM  Ultra: 7  │
│  …                                                             │
│  #20 │ Empty — Tap to configure this slot                     │
├─────────────────────────────────────────────────────────────────┤
│  [ Cancel ]                              [ Save ]              │
└─────────────────────────────────────────────────────────────────┘
```

Tap any row to edit that scan preset slot:

```
┌────────────────────────────────────────────────┐
│ ← Scan Preset #1                              │
├────────────────────────────────────────────────┤
│  Start Freq (MHz)  [ 108.0000             ]   │
│  End Freq (MHz)    [ 137.0000             ]   │
│  Step (kHz)        [ 25.00               ]   │
│  Scan Resume       [ 10                  ]   │
│  Scan Persist      [ 0                   ]   │
│  Modulation        [ AM ▼]                   │
│  Ultrascan Speed   [ 7 ▼]                    │
│  Label (8 chars)   [ Air Band            ]   │
│                                               │
│  [ Clear Slot ]                               │
│  [ Cancel ]         [ Save Entry ]            │
└────────────────────────────────────────────────┘
```

**Scan Preset Field Reference:**

| Field | Description |
|---|---|
| Start / End Freq | MHz — defines the scan range |
| Step | Scan step in kHz (e.g., 25.00 = standard FM, 12.50 = GMRS) |
| Scan Resume | Dwell count after signal lost before resuming scan |
| Scan Persist | Hold count when signal found |
| Modulation | FM / AM / USB / Auto (encoding matches EEPROM, not channel list) |
| Ultrascan Speed | 0–7 (0 = slowest, 7 = fastest) |
| Label | Up to 8 ASCII characters, displayed on the radio screen |

> **Empty slot detection:** A slot is empty when `startFreq == 0`. Tapping
> **Clear Slot** zeros the entry. The EEPROM block has no magic header — each slot
> stands alone.

---

## 8. CHIRP Adapter (Python Driver)

The repository also includes a CHIRP radio driver:
**`tidradio_h3_nicfw25.py`**

This driver lets you use **CHIRP** (the desktop radio programming software) to program
the TD-H3 over a USB serial or Bluetooth serial connection.

### Connection

1. Open CHIRP.
2. Select `Radio → Download from Radio`.
3. Choose **TIDRadio TD-H3 (nicFW 2.5)** from the radio list.
4. Select the correct COM port / serial device.
5. Click OK — CHIRP reads the full EEPROM via the same 0x45/0x30/0x46 protocol.

### CHIRP Tab Overview

The driver exposes three extra setting tabs alongside the standard channel grid:

#### Band Plan Tab

Mirrors the Android **Edit Band Plan** screen. Each of the 20 band plan entries
is shown as a CHIRP settings group with fields for start/end frequency, max power,
TX allowed, modulation, and bandwidth.

#### Scan Presets Tab

Mirrors the Android **Edit Scan Presets** screen. All 20 scan preset slots
are editable with the same fields: start/end frequency, step, resume, persist,
modulation, ultrascan speed, and label.

#### Group Labels Tab

Mirrors the Android **Edit Group Labels** screen. Displays 15 text fields (A–O)
for assigning human-readable names to channel groups.

### Supported Channel Fields

| CHIRP Field | EEPROM Field |
|---|---|
| Frequency | freqRxHz |
| Offset | offsetHz (duplex mode) |
| Duplex | duplex (+/−/split/none) |
| Name | name (12 chars) |
| Power | power (raw 0–255, mapped to Low/Medium/High/Turbo) |
| Mode | mode (FM/AM/USB) |
| Tone Mode | txToneMode (Tone/DTCS/none) |
| CTCSS / DCS | txToneVal + txTonePolarity |

---

## 9. Tips & Troubleshooting

### General Workflow

```
Load from Radio
      │
      ▼
  Edit channels / Band Plan / Scan Presets / Groups
      │
      ▼
  [Save EEPROM dump]  ← optional backup before upload
      │
      ▼
Save to Radio
```

Always back up your EEPROM dump before making major changes.

---

### Common Issues

| Issue | Resolution |
|---|---|
| **Connect button grayed out** | Not connected — tap Connect first |
| **Load / Save buttons grayed out** | No Bluetooth connection established |
| **"Frequency out of range" on save** | Frequency is outside Band Plan entries; verify band plan or use a supported frequency |
| **"No empty slots" on CHIRP import** | All 198 channels are used; clear some channels first |
| **Channel list shows nothing after load** | EEPROM read may have failed; try disconnecting and reconnecting, then load again |
| **Scan Preset entry shows 1.0 MHz** | Slot is empty (`startFreq == 0`); this is a display artifact in some tools — empty slots are handled correctly by nicFW |
| **BLE scan finds no devices** | Ensure the radio is powered on and not already connected to another device; on Android 12+ grant Nearby Devices permission |
| **Classic BT connection fails** | Pair the radio in Android Bluetooth settings first, then retry |

---

### Power Estimate Reference

Use this quick table when setting power in bulk:

```
Target   Raw Value (approx.)
────────────────────────────
 N/T          0  (N/T)
 0.5 W        29
 1.0 W        44
 2.0 W        58
 3.0 W        86
 5.0 W       130
 Max          255
```

---

### Group Design Tips

- Use **Group A–D** for operational categories (e.g., GMRS, MURS, Weather, Simplex).
- Assign channels to **multiple groups** when they logically belong to more than one
  category (e.g., a GMRS repeater might be in Group A *and* Group D for local repeaters).
- After organizing groups, run **Sort Channels by Group** to pack related channels into
  contiguous slot numbers for cleaner scanning.
- Use **Bulk Group Edit** (🏷 in multi-select mode) to assign a group to dozens of
  channels at once instead of editing each one individually.

---

### Keyboard / Navigation

| Action | How |
|---|---|
| Enter multi-select | Long-press a channel row |
| Exit multi-select | Tap Done in the action bar |
| Scroll channel list | Swipe up/down on the list |
| Reorder a channel | Drag the ≡ handle (outside multi-select mode) |
| Go back from any editor | ← toolbar back arrow or Android back gesture |

---

*End of User Guide*
