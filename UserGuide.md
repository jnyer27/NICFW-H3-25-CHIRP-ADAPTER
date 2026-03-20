# TD-H3 Channel Editor — User Guide

> **App:** NICFW TD-H3 Channel Editor
> **Radio:** TIDRadio TD-H3 running **nicFW v2.5**
> **Platform:** Android (min SDK 24 / Android 7.0)

**Documentation:** Browse this guide on **[GitHub Pages](https://jnyer27.github.io/NICFW-H3-25-CHIRP-ADAPTER/)** or download the live PDF at **[user-guide.pdf](https://jnyer27.github.io/NICFW-H3-25-CHIRP-ADAPTER/user-guide.pdf)**. Every **[GitHub Release](https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER/releases)** also ships the same PDF as an attachment named `nicfw-td-h3-editor-<release-tag>-userguide.pdf` (CI runs on each published release and when this guide changes).

---

## Table of Contents

1. [Overview](#1-overview)
2. [Connecting to the Radio](#2-connecting-to-the-radio)
3. [Loading & Saving the Radio Memory](#3-loading-saving-the-radio-memory)
4. [Channel List](#4-channel-list)
5. [Editing a Single Channel](#5-editing-a-single-channel)
6. [Multi-Select Mode](#6-multi-select-mode)
   - [Move Up / Down](#61-move-up-down)
   - [Set TX Power (Bulk)](#62-set-tx-power-bulk)
   - [Set Channel Groups (Bulk)](#63-set-channel-groups-bulk)
   - [Delete / Clear Selected](#64-delete-clear-selected)
   - [Move to Slot](#65-move-to-slot)
   - [Export as CHIRP CSV](#66-export-as-chirp-csv)
7. [Channel Search](#7-channel-search)
8. [Overflow Menu Features](#8-overflow-menu-features)
   - [Import CHIRP CSV](#81-import-chirp-csv)
   - [Sort Channels by Group](#82-sort-channels-by-group)
   - [Edit Group Labels](#83-edit-group-labels)
   - [Save / Import EEPROM Dump](#84-save-import-eeprom-dump)
   - [Edit Band Plan](#85-edit-band-plan)
   - [Edit Scan Presets](#86-edit-scan-presets)
   - [Radio Settings](#87-radio-settings)
   - [Tune Settings](#88-tune-settings)
   - [XTAL 671 Calculator](#89-xtal-671-calculator)
9. [CHIRP Adapter (Python Driver)](#9-chirp-adapter-python-driver)
10. [Tips & Troubleshooting](#10-tips-troubleshooting)
11. [Context Help](#11-context-help)

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
│  1 │ GMRS 1     │ 462.5625   │  1.0W │  A  B  │  N  │
│  2 │ GMRS 2     │ 462.5875   │  1.0W │  A  B  │  N  │
│  3 │ MURS 1     │ 151.8200   │  5.0W │  B     │  N  │
│  4 │ WX1        │ 162.4000   │  N/T  │        │  W  │
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
| Power | Estimated output power (N/T = no transmit, 0.5W–5W+). Suffixes: `(BP)` = Band Plan restricts TX on this frequency; `⚠` = stored power exceeds the radio's VHF/UHF power cap and will be clamped at TX time (see [Tune Settings §8.8](#88-tune-settings)) |
| Groups | Active group assignments (up to 4 letters) |
| BW | Bandwidth: `W` = Wide, `N` = Narrow |

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
| **Busy Lock** | On / Off | When On, prevents accidental TX on an occupied channel. Automatically forced Off and disabled whenever a TX offset or duplex mode is set — incompatible with repeater/split channels. |
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

### Power Cap Advisory

If **Tune Settings** has a VHF or UHF power cap configured (see [§8.8](#88-tune-settings)),
a non-blocking warning appears immediately below the Power spinner when the selected value
exceeds the applicable cap:

```
⚠ Exceeds VHF cap (130 ≈ 5.0W) — radio will clamp to cap at TX time
```

The spinner is **not restricted** — you may store any value. The radio enforces the cap
silently at transmit time; the EEPROM byte is unchanged. This lets you retain a high stored
power value and simply raise the cap later without re-editing every channel.

---

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
│ ✓ │ GMRS 1  │ 462.5625  │  1.0W │  A  │ N  ← selected │
│ ✓ │ GMRS 2  │ 462.5875  │  1.0W │  A  │ N  ← selected │
│   │ MURS 1  │ 151.8200  │  5.0W │  B  │ N             │
│ ✓ │ GMRS 3  │ 462.6125  │  1.0W │  A  │ N  ← selected │
│  …                                                       │
├──────────────────────────────────────────────────────────┤
│ 3 selected  │ ↑ │ ↓ │ ⤓ │ 📡 │ 🏷 │ 🗑 │ Done │
└──────────────────────────────────────────────────────────┘
```

**Selection bar buttons:**

| Button | Icon | Action |
|---|---|---|
| **Move Up** | ↑ | Shifts all selected channels one slot up (swaps with the channel above) |
| **Move Down** | ↓ | Shifts all selected channels one slot down |
| **Move to Slot** | ⤓ | Opens a slot picker — moves the selected block to any slot (1–198) in one step |
| **Set TX Power** | 📡 | Opens a power picker — applies chosen power to all selected channels |
| **Set Groups** | 🏷 | Opens a group editor — assigns groups to all selected channels |
| **Export CSV** | ↑□ | Names and exports selected channels as a CHIRP-compatible CSV file |
| **Delete** | 🗑 | Clears (empties) all selected channel slots |
| **Done** | text | Exits multi-select mode, returns to normal browsing |

- Tap additional rows while in multi-select mode to add/remove them from the selection.
- Tap a selected row again to deselect it.
- The selection count in the bar updates in real time.

---

### 6.1 Move Up / Down

Move Up (↑) and Move Down (↓) shift the selected channels one position at a time in the
channel list. The swap is performed in memory; tap **Save to Radio** afterward to persist.

> **Tip:** For large moves, use **Move to Slot** (⤓) to jump the selection directly
> to any slot in one step. For group-based reordering, use **Sort Channels by Group**
> from the overflow menu.

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

#### Power Cap Advisory

If the selected channels include VHF or UHF frequencies and a power cap is set in
**Tune Settings** (see [§8.8](#88-tune-settings)), an advisory appears **below the scroll
wheel** when the chosen value exceeds the applicable cap:

```
⚠ Exceeds VHF cap (130 ≈ 5.0W) — radio will clamp to cap at TX time
```

When the selection spans both bands, the **more restrictive** of the two caps is
used in the advisory. Apply is never blocked — the stored value is saved as-is.

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

### 6.5 Move to Slot

Tap the **⤓** (arrow-to-underline) button while channels are selected to jump the
entire selection to any slot in one action — no need to tap Move Up / Down dozens
of times.

```
┌────────────────────────────────────────────────────┐
│  Move 3 Channel(s) to Slot                         │
├────────────────────────────────────────────────────┤
│                                                    │
│  Move selected channels starting at slot:          │
│                                                    │
│  ┌──────────────────────────────────────────────┐  │
│  │  Ch 10 – WX1                              ▼  │  │
│  └──────────────────────────────────────────────┘  │
│                                                    │
│  3 channel(s) will occupy slot(s) 10–12            │
│                                                    │
│          [ Cancel ]          [ Move ]              │
└────────────────────────────────────────────────────┘
```

**How it works:**

1. Long-press to enter multi-select mode, then tap all channels you want to move.
2. Tap **⤓** — the dialog opens with the Spinner pre-set to the first selected slot.
3. Scroll the Spinner to choose the target **starting slot** (1–198).
4. The hint line updates live: *"N channel(s) will occupy slot(s) X–Y"*.
5. Tap **Move** — the selected channels are removed from their current positions and
   inserted starting at the chosen slot. All remaining channels shift to fill the gaps.

**Selection after move:**

The moved channels remain selected (highlighted) in their new positions so you can
immediately continue editing or move them again.

**Edge cases:**

| Situation | Result |
|---|---|
| Target slot is inside the current selection range | Computed from non-selected channels only — result is predictable |
| All channels selected | Early exit with a toast — nothing to move relative to |
| Target slot > available slots after removals | Channels land at the end of the list |

---

### 6.6 Export as CHIRP CSV

Tap the **↑□** (upload arrow) button while channels are selected to export the selection to a CHIRP-compatible CSV file and share it.

```
┌──────────────────────────────────────────────────────┐
│  Export CHIRP CSV                                    │
│  12 channel(s) will be exported.                     │
│  File name:                                          │
│  ┌────────────────────────────────────────────────┐  │
│  │  chirp_export_20260313_104500                  │  │
│  └────────────────────────────────────────────────┘  │
│          [ Cancel ]      [ Export & Share ]           │
└──────────────────────────────────────────────────────┘
```

1. Long-press to enter multi-select mode and select the channels you want to export.
   - Use **Channel Search** (§7) to quickly select a whole group.
2. Tap the **↑□** icon in the selection bar.
3. Edit the file name if desired (default is `chirp_export_<timestamp>`).
4. Tap **Export & Share** — the Android share sheet opens so you can send the file via email, cloud storage, AirDrop, etc.

The exported CSV:
- Is numbered sequentially from `Location 0` regardless of original slot numbers.
- Preserves frequency, name, duplex/offset, tone (CTCSS/DCS), mode, bandwidth, and power.
- Can be imported directly back into the app (**⋮ → Import CHIRP CSV**) or into the desktop CHIRP software.
- Empty slots in the selection are automatically skipped.

> **Typical use case:** Select all channels in group H–GMRS using Search → Select All, then export to share your repeater list with another user.

---

## 7. Channel Search

Tap the **🔍** (magnifying glass) icon in the top toolbar to open the search bar.

```
┌──────────────────────────────────────────────────────┐
│  TD-H3 Channel Editor                          🔍 ⋮  │
├──────────────────────────────────────────────────────┤
│  [ Search by name, group or frequency… ] [Select All] [✕] │
├──────────────────────────────────────────────────────┤
│  → Ch 31  Towson WRJY6     462.6000 MHz              │
│  → Ch 32  Baltimore WR     462.6750 MHz              │
│  → Ch 35  Lanham WQMM8     462.5500 MHz              │
│  …                                                   │
└──────────────────────────────────────────────────────┘
```

**How it works:**
- Type any part of a **channel name**, **group label**, or **RX frequency** — the list filters live as you type (case-insensitive).
- Matching is on name OR frequency OR any of the channel's group labels:
  - Typing `GMRS` shows channels assigned to groups whose label contains "GMRS".
  - Typing `boat` shows channels with "Boat" in the name or assigned to a group labeled "Boat".
  - Typing `462.5` shows all channels whose RX frequency starts with 462.5 MHz (e.g. 462.5000, 462.5125, 462.5625, …).
  - Typing `462` shows every channel in the 462 MHz band.
- Empty slots are always hidden while a search query is active.
- Tap **✕** to clear the search and restore the full channel list.
- Tap the 🔍 icon again to close the search bar.

**Select All Matches:**
Once you have results, tap **Select All** to enter multi-select mode with every visible (filtered) channel pre-selected. From there you can:
- Tap **Export CSV** (↑□) to export the whole matched set as a CHIRP CSV.
- Tap **Set TX Power** (📡) to change power for the whole group at once.
- Tap **Set Groups** (🏷) to bulk-assign groups to all matches.
- Tap any card to deselect individually, or tap **Done** to exit selection mode.

> **Example workflow — share all GMRS repeaters:**
> 1. Tap 🔍, type `GMRS`.
> 2. Tap **Select All** — all channels in your GMRS group are selected.
> 3. Tap **↑□** → name the file → **Export & Share**.

---

## 8. Overflow Menu Features


Tap the **⋮** (three-dot) menu in the top-right toolbar to access advanced features.

```
┌────────────────────────────────────────┐
│ ⋮                                      │
│  Import CHIRP CSV from File…           │
│  Import CHIRP CSV from Clipboard…      │
│  Sort Channels by Group…               │
│  Edit Group Labels…                    │
│  Save EEPROM dump…                     │
│  Import EEPROM dump…                   │
│  Edit Band Plan…                       │
│  Edit Scan Presets…                    │
│  Radio Settings…                       │
│  Tune Settings…                        │
│  XTAL 671 Calculator…                  │
│  ✓ Protect Tune Settings               │
└────────────────────────────────────────┘
```

> **XTAL 671 Calculator** is always accessible — no EEPROM required.
> **Import EEPROM dump** is always accessible regardless of connection state.
> **Protect Tune Settings** is a checkable toggle — accessible any time; state persists across app restarts.
> All other items require an EEPROM to be loaded first.

---

### 8.1 Import CHIRP CSV

Import a frequency list exported by the CHIRP radio programming software.

Two input methods are supported:

- **⋮ → Import CHIRP CSV from File…** — opens the system file picker to select a `.csv` file.
- **⋮ → Import CHIRP CSV from Clipboard…** — reads a CHIRP CSV copied directly to the clipboard.

#### Steps

1. Export your channel list from CHIRP as a CSV file (`File → Export`).
2. Transfer the CSV to your Android device (email, cloud storage, USB, etc.),
   **or** copy the CSV text to the clipboard on a device where CHIRP is running.
3. In the app, tap **⋮ → Import CHIRP CSV from File…** and pick the file,
   **or** tap **⋮ → Import CHIRP CSV from Clipboard…** to import from the clipboard.
4. The **CHIRP Import** screen opens:

```
┌──────────────────────────────────────────────────────┐
│ ← Import CHIRP CSV                                   │
├──────────────────────────────────────────────────────┤
│  24 channel(s) in CSV · 31 empty slot(s) available   │
│  ✓ All 24 channels will be imported                  │
├──────────────────────────────────────────────────────┤
│  Assign Groups to All Imported Channels              │
│                                                      │
│  Group 1  [ A — GMRS ▼]  Group 2  [ None     ▼]    │
│  Group 3  [ None     ▼]  Group 4  [ None     ▼]    │
├──────────────────────────────────────────────────────┤
│  TX Power (All Imported Channels)                    │
│  [ From CSV                        ▼]               │
├──────────────────────────────────────────────────────┤
│  Starting Channel                                    │
│  [ Ch 1 ▼ ]                                         │
├──────────────────────────────────────────────────────┤
│  Channel Preview                                     │
│  → Ch 1  │ GMRS 1   │ 462.5625 MHz  │ T: 100.0 Hz  │
│  → Ch 2  │ GMRS 2   │ 462.5875 MHz                 │
│  → Ch 3  │ WX1      │ 162.4000 MHz  (CSV #43)      │
│  …                                                  │
├──────────────────────────────────────────────────────┤
│  [ Cancel ]                     [ Import ]           │
└──────────────────────────────────────────────────────┘
```

5. Optionally assign all imported channels to up to 4 groups using the **Group** spinners.
6. Use the **TX Power** spinner to set a uniform power level for every imported channel:
   - **From CSV** *(default)* — keeps whatever power value the CSV row contained.
   - **N/T** — no transmit (useful for receive-only or monitor channels).
   - **1–255** — raw power byte; see the [Power Calibration table](#power-calibration)
     in §5 for approximate watt equivalents.
7. Use the **Starting Channel** spinner to choose which empty slot the first imported
   channel lands in. The preview updates live — the `→ Ch N` labels shift to reflect
   your selection. Only positions where all channels fit without overwriting occupied
   slots are listed.
8. Tap **Import** to write channels into the selected slot range.
9. Tap **Save to Radio** on the main screen to upload.

> ⚠ Imported channels fill **empty slots only** — existing channels are never overwritten.
> If there are more CSV entries than empty slots, a warning is shown and excess entries
> are skipped. The Starting Channel spinner is hidden in this case.

#### Tone / TSQL Handling

The importer follows the CHIRP column spec:

| Tone column | Behaviour |
|---|---|
| `Tone` | TX CTCSS only (`rToneFreq`); RX squelch is carrier-triggered |
| `TSQL` | TX + RX CTCSS using `cToneFreq`; radio opens only when it hears the tone back |
| `DTCS` | TX + RX DCS using `DtcsCode` + `DtcsPolarity` |
| *(blank)* | No tone — **unless** `rToneFreq` ≠ 88.5 Hz (CHIRP's "no-tone" sentinel), in which case the frequency is imported as TX Tone encode-only |

> 💡 **RepeaterBook / database exports** commonly leave the Tone column blank while
> still populating `rToneFreq` with the repeater's access tone. The blank-column fallback
> above ensures that tone is captured as a TX encode rather than silently discarded.
> If you do *not* want the tone imported, zero out the `rToneFreq` column (or set it
> to `88.5`) before importing.

---

### 8.2 Sort Channels by Group

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

### 8.3 Edit Group Labels

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

### 8.4 Save / Import EEPROM Dump

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

### 8.5 Edit Band Plan

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

### 8.6 Edit Scan Presets

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


---

### 8.7 Radio Settings

View and edit the nicFW Settings block stored at EEPROM offset `0x1900`.
Access via **⋮ → Radio Settings…** (requires EEPROM to be loaded).

Tap **Save Settings** to write all changes to the in-memory EEPROM, then tap
**Save to Radio** from the main screen to upload to the device.

> Fields marked **⚠** use EEPROM offsets or encodings that are not fully
> confirmed — treat them with extra caution.

The editor is divided into ten sections:

---

#### Squelch & Audio

| Field | Range | Default | Notes |
|---|---|---|---|
| **Squelch** | 0–9 (spinner) | 2 | Squelch threshold level |
| **Sq Noise Level** | 0–255 | 0 | Raw noise floor threshold |
| **Noise Ceiling** | 0–255 | 0 | Upper noise limit for squelch detection |
| **Sq Tail Elim** | Off / On variants | Off | Squelch tail elimination mode |
| **Mic Gain** | 0–31 | 25 | Microphone preamp gain |
| **Noise Gate** | Off / On | Off | Enables the TX noise gate |
| **RF Gain** | AGC / 1–42 | AGC | Receiver RF gain. AGC = automatic; 1–42 = fixed (lower = less gain) |

---

#### Display

| Field | Range | Default | Notes |
|---|---|---|---|
| **LCD Brightness** | 0–35 | 28 | Backlight level |
| **LCD Timeout (s)** | 0–255 (0 = Off) | Off | Screen off delay in seconds |
| **Heartbeat** | 0–30 (0 = Off) | Off | Idle animation speed |
| **Dim Brightness** | 0–14 (0 = Off) | Off | Reduced brightness level after timeout |
| **LCD Gamma** | 0–255 | 0 | Display gamma correction |
| **LCD Inverted** | Off / On | Off | Inverts the display colors |
| **S-Bar Style** | Spinner | Segment | Signal bar display style |
| **S-Bar Persistent** | Off / On | Off | Keep signal bar visible when idle |

---

#### TX Settings

| Field | Range | Default | Notes |
|---|---|---|---|
| **TX Mod Meter** | Off / On | On | Shows TX modulation level on screen during transmit |
| **TX Timeout (s)** | 0–255 (0 = Off) | 120 | Maximum continuous TX time before automatic cutoff |
| **TX Sub Tone Deviation** | 0–127 | 64 | Sub-audible tone deviation level for CTCSS/DCS |
| **PTT Mode** | Spinner | Dual | PTT button behavior (Dual = toggle + hold) |
| **TX Filter Trans ⚠** | 0–65535 | 280.0 | TX filter transition frequency (offset unconfirmed) |
| **TX Current** | Off / On | Off | Show transmit current on display |
| **Repeater Tone (Hz)** | 0–65535 | 1750 | 1750 Hz burst tone for European repeater access |

---

#### RX & Tuning

| Field | Range | Default | Notes |
|---|---|---|---|
| **Step** | Spinner | 5.0 kHz | VFO tuning step |
| **AF Filters** | Spinner | All | Audio filter combination |
| **IF Freq** | Spinner | 8.46 kHz | Intermediate frequency |
| **RFi Comp** | Spinner | Off | RF interference compensation |
| **AGC 0–3** | 0–63 each | 24 / 32 / 37 / 40 | AGC threshold levels (fine tuning) |
| **AM AGC Fix** | Off / On | Off | Corrects AM mode AGC behavior; recommended On when using AM channels |

---

#### Scan

| Field | Range | Default | Notes |
|---|---|---|---|
| **Dual Watch** | Off / On | On | Monitors two channels simultaneously |
| **Scan Resume (s)** | 0–30 | 10 | Seconds before scan resumes after losing a signal |
| **Scan Range (MHz)** | decimal | 1.00 | Frequency window scanned around current channel |
| **Scan Persist (s)** | decimal (0 = Off) | Off | Hold time when a signal is found |
| **Scan Update (×0.1 s)** | 0–255 | 0 | Display update interval during scan |
| **Ultrascan** | 0–7 (0 = Off) | 7 | Spectrum scan speed (7 = fastest) |

---

#### VOX

| Field | Range | Default | Notes |
|---|---|---|---|
| **VOX Level** | 0–15 (0 = Off) | Off | Voice-activated TX sensitivity |
| **VOX Tail (s)** | decimal | 2.0 | Seconds TX stays open after voice stops |

---

#### Tones & Keys

| Field | Range | Default | Notes |
|---|---|---|---|
| **Tone Monitor** | Spinner | On | Sub-audible tone monitor behavior |
| **Key Tones** | Spinner | Off | Button press beep style |
| **Sub-Tone Deviation** | 0–255 | 74 | Global sub-tone deviation level |

---

#### DTMF

| Field | Range | Default | Notes |
|---|---|---|---|
| **DTMF Volume** | 0–255 | 80 | Received DTMF audio level |
| **DTMF Speed** | 0–15 | 11 | DTMF tone transmit speed |
| **DTMF Decode** | Spinner | Off | DTMF decode / squelch mode |
| **DTMF Seq End Pause (s)** | decimal | 1.0 | Gap between DTMF sequence transmissions |

---

#### System

| Field | Range | Default | Notes |
|---|---|---|---|
| **Battery Style** | Spinner | Percent | Battery indicator format |
| **Bluetooth** | Off / On | On | Enable/disable the radio's Bluetooth module |
| **Power Save** | Off / 1–20 | Off | Battery saving sleep interval. Higher = longer sleep cycles |
| **DW Start Delay (s)** | 0–15 | 5 | Dual Watch activation delay after squelch closes |
| **DW VFO Lock** | Off / On | Off | Lock VFO during Dual Watch mode |
| **AllStar Link** | Spinner | Off | AllStar VoIP repeater link mode |
| **Disable FM Tuner** | Off / On | Off | Hides the FM broadcast band from the tuner |
| **Scrambler IF** | Spinner | Off | Scrambler intermediate frequency; Off = disabled, 2600–3500 Hz in 100 Hz steps |

---

#### Security

| Field | Range | Default | Notes |
|---|---|---|---|
| **PIN** | 0–9999 | 1234 | Radio power-on or key-lock PIN |
| **PIN Action** | Spinner | Off | When PIN is triggered (power-on / key-lock / off) |

---

### 8.8 Tune Settings

View and edit three per-radio calibration values stored at EEPROM offset `0x1DFB`.
Access via **⋮ → Tune Settings…** (requires EEPROM to be loaded).

> ⚠ **These values are calibrated for your specific radio unit.**
> If you cloned another radio's EEPROM, recalibrate XTAL 671 and verify the power
> caps for the target unit before saving to that radio.

#### Screen Layout

```
┌────────────────────────────────────────────────────────┐
│ ← Tune Settings                                        │
├────────────────────────────────────────────────────────┤
│ ╔══════════════════════════════════════════════════════╗ │
│ ║ ⚠  These settings are per-radio calibration values. ║ │
│ ║ If you cloned another radio's EEPROM, recalibrate   ║ │
│ ║ XTAL 671 and verify power caps for this unit.       ║ │
│ ╚══════════════════════════════════════════════════════╝ │
│                                                        │
│  VHF Power Setting Cap                                 │
│  ┌──────────────────────────────────────────────────┐  │
│  │         128                                      │  │
│  │  ─────────────────────────────────────────────   │  │
│  │  ▶    130 (≈ 5.0W)    ◀         ← scroll wheel  │  │
│  │  ─────────────────────────────────────────────   │  │
│  │         132                                      │  │
│  └──────────────────────────────────────────────────┘  │
│  Max TX power on VHF (<300 MHz). Radio silently clamps. │
│                                                        │
│  UHF Power Setting Cap                                 │
│  (same scroll wheel format)                            │
│  Max TX power on UHF (≥300 MHz). Radio silently clamps. │
│                                                        │
│  XTAL 671 Correction                                   │
│  ┌──────────────────────────────────────────────────┐  │
│  │         +28                                      │  │
│  │  ─────────────────────────────────────────────   │  │
│  │  ▶       +29          ◀         ← scroll wheel  │  │
│  │  ─────────────────────────────────────────────   │  │
│  │         +30                                      │  │
│  └──────────────────────────────────────────────────┘  │
│  Crystal oscillator frequency correction (−128…+127).  │
│                                                        │
│  [ Cancel ]             [ Save Tune Settings ]         │
└────────────────────────────────────────────────────────┘
```

#### Field Reference

| Field | EEPROM Offset | Range | Description |
|---|---|---|---|
| **VHF Power Setting Cap** | `0x1DFF` | 0–255 (raw byte) | Maximum TX power for VHF frequencies (<300 MHz). Channels whose stored power exceeds this value are clamped by the radio at transmit time. |
| **UHF Power Setting Cap** | `0x1DFD` | 0–255 (raw byte) | Same cap for UHF frequencies (≥300 MHz). |
| **XTAL 671 Correction** | `0x1DFB` | −128…+127 (signed) | Crystal oscillator correction applied by nicFW. Use the [XTAL 671 Calculator](#79-xtal-671-calculator) to compute the correct value for your unit. |

> The live watt estimate displayed next to each cap picker (e.g., "≈ 5.0W") uses the
> same interpolation table as the channel list power column.

#### How Power Caps Work

```
Per-channel power byte (stored in EEPROM)
        │
        ▼
  if value > cap  ──►  radio transmits at cap value
  if value ≤ cap  ──►  radio transmits at stored value
```

- **The stored EEPROM byte is never modified** — only the transmitted power is clamped.
- When a channel's stored power exceeds the cap, the app shows:
  - A `⚠` suffix in the channel list power column (e.g., `5.0W ⚠`)
  - A non-blocking advisory below the Power spinner in the channel editor
  - A non-blocking advisory below the wheel in the bulk TX power picker
- **Raising the cap later** automatically un-clamps all affected channels without
  requiring any per-channel edits.

#### Channel List Indicators

| Power display | Meaning |
|---|---|
| `5.0W` | Normal — within cap and TX allowed |
| `5.0W ⚠` | Stored power exceeds VHF/UHF cap; radio will clamp at TX time |
| `5.0W (BP)` | Band Plan marks this frequency as TX-restricted |
| `5.0W (BP) ⚠` | Both Band Plan TX-restricted AND power exceeds cap |
| `N/T` | No-transmit (power = 0 or Band Plan TX off) |


---

#### Protect Tune Settings

A checkable toggle in the overflow menu (**⋮ → Protect Tune Settings**) that locks
the per-radio calibration values and enables a safe multi-radio write workflow.

The same toggle is also available inside the Tune Settings screen itself as a switch
at the top of the page.

**When protection is OFF (default):**
- Tune Settings fields (XTAL, VHF cap, UHF cap) are fully editable.
- **Save to Radio** uploads the EEPROM as-is — calibration bytes are whatever is
  in the current template.

**When protection is ON:**
- The XTAL, VHF cap, and UHF cap pickers are greyed out and non-editable.
- The **Save Tune Settings** button is disabled.
- **Save to Radio** performs a two-phase protected write:

```
Phase 1 — Pre-read target radio
  Progress: "Reading target radio calibration…"
  Downloads the connected radio's full EEPROM
  Extracts the 5 calibration bytes (0x1DFB–0x1DFF)

Phase 2 — Patched upload
  Progress: "Writing…"
  Uploads a copy of the template EEPROM with those 5 bytes
  substituted from the target radio's own calibration
  Template in memory is never modified
```

#### Multi-Radio Template Workflow

The typical use case for Protect Tune Settings is writing the same channel/settings
configuration to a collection of radios while preserving each radio's factory
calibration:

1. Connect **Radio 1** → **Load from Radio**.
2. Edit channels, settings, and band plan as desired. Do **not** edit Tune Settings.
3. Enable **Protect Tune Settings** (✓ appears in the menu).
4. Disconnect Radio 1, connect **Radio 2**.
5. Tap **Save to Radio** → confirm dialog shows 🔒 protection note.
6. Progress shows "Reading target radio calibration…" then "Writing…".
7. Radio 2 receives the updated channels/settings but **keeps its own XTAL
   and power caps**.
8. Repeat steps 4–7 for Radio 3, 4, … — each radio keeps its own calibration.

> **Template file workflow:** Save an EEPROM dump (**⋮ → Save EEPROM dump…**) while
> protection is enabled. Load that `.bin` file later and apply it to any number of
> radios — each will retain its own calibration on write.

> **CHIRP note:** The Protect Tune Settings feature is **Android-only**.
> The CHIRP driver does not implement a pre-read, so it always writes whatever
> calibration bytes are in the loaded image. Use the Android app for multi-radio
> template workflows.

#### Workflow

1. Tap **⋮ → Tune Settings…**
2. Scroll the **VHF Power Setting Cap** picker to your desired ceiling raw value.
   - Example: `130` ≈ 5.0 W (matches the field default seen in the NICFW Programmer).
   - Set `255` to disable the cap (allow full power on all channels).
3. Repeat for **UHF Power Setting Cap**.
4. Adjust **XTAL 671** if needed — use the
   [XTAL 671 Calculator](#79-xtal-671-calculator) to derive the correct value first.
5. Tap **Save Tune Settings** → values are written to the in-memory EEPROM.
6. Tap **Save to Radio** on the main screen to upload.

> After saving Tune Settings and returning to the channel list, scroll the list to
> verify that `⚠` indicators appear or disappear as expected.

---

### 8.9 XTAL 671 Calculator

A standalone frequency calibration tool — no EEPROM or radio connection required.
It computes the **nicFW XTAL 671 correction value** needed to compensate for crystal
oscillator error in the TD-H3.

> **Available any time** from the overflow menu, even before loading from the radio.

#### ⚠ Prerequisite — Reset XTAL to 0 First

> **Before measuring the actual RX frequency you must set the radio's XTAL 671
> value to 0.** A non-zero XTAL setting shifts the radio's receive frequency,
> which will corrupt the measurement and produce a wrong correction value.

Steps:
1. On the radio, open Settings → XTAL 671 and set it to **0**.
2. Tune the radio to the target frequency.
3. Measure the actual received frequency with a calibrated reference (SDR, spectrum
   analyzer, or frequency counter).
4. Enter that measured frequency in the calculator.

#### Calculator Screen

```
┌─────────────────────────────────────────────────────────┐
│ ← XTAL 671 Calculator                                  │
├─────────────────────────────────────────────────────────┤
│ ╔═════════════════════════════════════════════════════╗ │
│ ║ ⚠  Before calibrating                              ║ │
│ ║ Set the radio's XTAL 671 value to 0 before         ║ │
│ ║ measuring the actual RX frequency. A non-zero      ║ │
│ ║ XTAL setting will skew the measurement and         ║ │
│ ║ produce an incorrect correction value.             ║ │
│ ╚═════════════════════════════════════════════════════╝ │
│                                                         │
│  Target Frequency (MHz)                                 │
│  ┌───────────────────────────────────────────────────┐  │
│  │  462.7250                                         │  │
│  └───────────────────────────────────────────────────┘  │
│  Expected / license frequency — e.g. 462.7250 GMRS22   │
│                                                         │
│  Actual RX Frequency (MHz)                              │
│  ┌───────────────────────────────────────────────────┐  │
│  │  462.7252                                         │  │
│  └───────────────────────────────────────────────────┘  │
│  Frequency measured by a calibrated reference receiver  │
│                                                         │
│  Common targets:                                        │
│  [GMRS22 462.725] [GMRS1 462.5625] [MURS1 151.820]     │
│  [WX1 162.400]                                          │
│                                                         │
│ ┌───────────────────────────────────────────────────┐   │
│ │        XTAL 671 Correction Value                  │   │
│ │                                                   │   │
│ │                    29                             │   │
│ │                                                   │   │
│ │  Δ = +0.200 kHz  (462.7252 − 462.725 MHz)        │   │
│ └───────────────────────────────────────────────────┘   │
│                                                         │
│  ▲ Radio receives HIGH                                  │
│  Set XTAL 671 to +29                                    │
│  ℹ Measurement must be taken with XTAL 671 set to 0    │
│                                                         │
│  [ Copy Value to Clipboard ]                            │
│                                                         │
│  Formula:                                               │
│  ROUND(((Actual − Target) × 100000) ÷ Target            │
│        × 671.08864, 0)                                  │
└─────────────────────────────────────────────────────────┘
```

#### How to Use

1. Tap **⋮ → XTAL 671 Calculator…** from the main screen (available at any time).
2. Tap a **preset chip** to fill the target frequency, or type it manually.
3. Enter the **Actual RX Frequency** you measured with your reference device.
4. The **correction value** updates live as you type.
5. Tap **Copy Value to Clipboard** then enter it in the radio's XTAL 671 setting.

#### Formula

```
XTAL671 = ROUND( ((Actual − Target) × 100000) ÷ Target × 671.08864, 0 )
```

This matches the Google Sheets reference formula:
`=ROUND(((D2-C2)*100000)/C2 * 671.08864, 0)`
where C2 = Target Freq (MHz), D2 = Actual RX Freq (MHz).

#### Interpreting the Result

| Result | Meaning | Action |
|---|---|---|
| **Positive** (e.g. +29) | Radio receives HIGH — oscillator running fast | Set XTAL 671 to +29 |
| **Negative** (e.g. −35) | Radio receives LOW — oscillator running slow | Set XTAL 671 to −35 |
| **Zero** | Within calibration tolerance | No change needed |

#### Verification Example (from reference spreadsheet)

| Radio | Target (MHz) | Actual RX (MHz) | XTAL 671 |
|---|---|---|---|
| xxx579 | 462.725 | 462.7252 | **29** |
| xxx591 | 462.725 | 462.72524 | **35** |
| xxx866 | 462.725 | 462.7252 | **29** |
| xxx869 | 462.725 | 462.72528 | **41** |
| xxx589 | 462.725 | 462.72532 | **46** |

> Each radio has a unique crystal tolerance; calculate and set individually for
> each unit.

#### Preset Frequencies

| Chip | Frequency | Band |
|---|---|---|
| GMRS22 462.725 | 462.7250 MHz | GMRS channel 22 (most common reference) |
| GMRS1 462.5625 | 462.5625 MHz | GMRS channel 1 |
| MURS1 151.820 | 151.8200 MHz | MURS channel 1 |
| WX1 162.400 | 162.4000 MHz | NOAA Weather channel 1 |

---

## 9. CHIRP Adapter (Python Driver)

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

## 10. Tips & Troubleshooting

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
| **Channel list shows `⚠` next to power** | Stored channel power exceeds the VHF or UHF cap in Tune Settings. Lower the channel power OR raise the cap in **⋮ → Tune Settings…** |
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
| Open context help for a field | Tap the **?** (lightbulb) icon next to the field label |

---

## 11. Context Help

Every setting in the **Radio Settings**, **Channel Editor**, and **Tune Settings**
screens has a built-in help button: a small **?** (lightbulb) icon next to the
field label.

```
┌────────────────────────────────────────────────────┐
│  Squelch Level                              [?]    │
│  ┌──────────────────────────────────────────────┐  │
│  │  5                                        ▼  │  │
│  └──────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────┘
```

Tapping **?** opens an AlertDialog with:

```
┌────────────────────────────────────────────────────┐
│  Squelch Level                                     │
├────────────────────────────────────────────────────┤
│  Range: 0–9  |  Default: 5                         │
│                                                    │
│  Controls the squelch threshold. Lower values open │
│  squelch at weaker signals. 0 = squelch always     │
│  open (carrier squelch disabled).                  │
│                                                    │
│  Note: Applies to the currently selected channel   │
│  memory bank.                                      │
│                            [ OK ]                  │
└────────────────────────────────────────────────────┘
```

**Dialog fields:**

| Field | Content |
|---|---|
| **Title** | Setting name |
| **Range** | Valid value range or options |
| **Default** | Factory default value |
| **Description** | Plain-language explanation of what the setting does |
| **Note** | Additional context, caveats, or interactions with other settings (when applicable) |

**Where context help is available:**

| Screen | Help available on |
|---|---|
| Channel Editor | All fields: Frequency, Duplex, Name, Power, Mode, Bandwidth, Busy Lock, TX Tone, RX Tone, Groups |
| Radio Settings | All 56 settings across all 10 sections |
| Tune Settings | VHF Power Cap, UHF Power Cap, XTAL 671 Correction |

Help content is sourced from the nicFW V2.5 manual and stored in
`docs/help_reference/help_content.yaml` in the repository.

---

*End of User Guide*
