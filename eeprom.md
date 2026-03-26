# EEPROM Layout — TIDRADIO TD-H3 nicFW V2.5

This document describes the EEPROM memory map used by the TIDRADIO TD-H3 running nicFW V2.5, as implemented in the CHIRP driver (`tidradio_h3_nicfw25.py`).

---

## Overview

| Property | Value |
|----------|-------|
| Total size | 8,192 bytes (8 KB) |
| Block size | 32 bytes |
| Total blocks | 256 |
| Byte order | **Big-endian** |
| Firmware magic | `0xD82F` at offset `0x1900` |

---

## Memory Map

| Start | End | Size | Description |
|-------|-----|------|-------------|
| `0x0000` | `0x001F` | 32 B | VFO A channel info |
| `0x0020` | `0x003F` | 32 B | VFO B channel info |
| `0x0040` | `0x18FF` | 6,336 B | 198 memory channels (32 bytes each) |
| `0x1900` | `0x19FF` | 256 B | Settings block |
| `0x1A00` | `0x1A01` | 2 B | Band plan magic (`0xA46D`) |
| `0x1A02` | `0x1ACF` | 200 B | 20 band plan entries (10 bytes each) |
| `0x1B00` | `0x1BFF` | 256 B | 20 scan preset entries (20 bytes each, with 16 bytes spare at end) |
| `0x1C90` | `0x1CCF` | 64 B | 15 group labels (6 bytes each, groups A–O) |
| `0x1DFB` | `0x1DFF` | 5 B | Per-radio calibration data |

---

## Channel Info Structure (32 bytes)

Applies to VFO A (`0x0000`), VFO B (`0x0020`), and memory channels `0x0040 + (index × 32)`.

```
Offset  Size  Field        Description
------  ----  -----------  ---------------------------------------------------
0x00    u32   rxFreq       Receive frequency in 10 Hz units (big-endian)
0x04    u32   txFreq       Transmit frequency in 10 Hz units (big-endian)
0x08    u16   rxSubTone    RX tone/code (see Tone Encoding below)
0x0A    u16   txSubTone    TX tone/code (see Tone Encoding below)
0x0C    u8    txPower      0 = No Transmit, 1–255 = power setting number
0x0D    u16   groups       4× 4-bit group slots (see Groups below)
0x0F    u8    flags        Packed bit field (see Flags below)
0x10    u8[4] reserved     Unused
0x14    char[12] name      Channel name, ASCII, left-aligned, space/0xFF padded
```

### Frequency Encoding

Frequencies are stored as unsigned 32-bit big-endian integers in units of **10 Hz**.

```
Stored value = frequency_Hz / 10
```

Example: 146.520 MHz → `14,652,000` → `0x00DF0C80`

### Tone Encoding

Both `rxSubTone` and `txSubTone` use the same 16-bit word format:

| Condition | Meaning | Format |
|-----------|---------|--------|
| `word == 0` | Off (no tone/code) | — |
| `0 < word ≤ 3000` | CTCSS | Value in 0.1 Hz units (e.g., 1000 = 100.0 Hz) |
| `word & 0x8000` | DCS | Bits 8:0 = code (1–511); bit 14 = polarity (0=Normal, 1=Reversed) |

DCS word construction:
```
word = 0x8000 | dcs_code | (0x4000 if reversed else 0)
```

### Channel Flags

The `flags` byte packs several fields (LSB first as declared in the bitwise structure):

| Bits | Field | Values |
|------|-------|--------|
| 0 | bandwidth | 0 = Wide, 1 = Narrow |
| 2:1 | modulation | 0 = Auto, 1 = FM, 2 = AM, 3 = USB |
| 3 | position | (reserved/internal use) |
| 5:4 | pttID | PTT ID mode bits |
| 6 | reversed | (internal) |
| 7 | busyLock | 1 = enabled (incompatible with duplex/split; driver clears automatically) |

### Groups

The `groups` field is a 16-bit word containing four 4-bit slots:

| Bits | Slot |
|------|------|
| 3:0 | Group 1 |
| 7:4 | Group 2 |
| 11:8 | Group 3 |
| 15:12 | Group 4 |

Values: `0` = None, `1`–`15` = Groups A–O.

---

## Settings Block (256 bytes at `0x1900`)

The settings block is identified by the magic value `0xD82F` at offset `+0x00`.

```
Offset  Size  Field              Description
------  ----  -----------------  -------------------------------------------
0x00    u16   magic              0xD82F (V2.5 identifier)
0x02    u8    squelch            0–9
0x03    u8    dualWatch          0/1
0x04    u8    autoFloor          0/1
0x05    u8    activeVfo          0 = VFO-A, 1 = VFO-B
0x06    u16   step               Tuning step in Hz (see Step Values)
0x08    u16   rxSplit            RX split in 10 Hz units
0x0A    u16   txSplit            TX split in 10 Hz units
0x0C    u8    pttMode            PTT mode index
0x0D    u8    txModMeter         0/1
0x0E    u8    micGain            0–31
0x0F    u8    txDeviation        TX deviation
0x10    i8    (obsolete)         Not used in V2.5; XTAL calibration moved to 0x1DFB
0x11    u8    battStyle          0=Off, 1=Icon, 2=Percentage, 3=Voltage
0x12    u16   scanRange          Scan range in 10 Hz units
0x14    u16   scanPersist        Scan persist
0x16    u8    scanResume         0=Time, 1=Hold, 2=Seek
0x17    u8    ultraScan          0–7
0x18    u8    toneMonitor        0=Off, 1=On, 2=Clone
0x19    u8    lcdBrightness      0–28
0x1A    u8    lcdTimeout         0=Off, 1–200 (seconds)
0x1B    u8    breathe            Heartbeat/breathe effect
0x1C    u8    dtmfDev            DTMF deviation
0x1D    u8    gamma              LCD gamma correction
0x1E    u16   repeaterTone       Repeater tone in Hz
0x20    ...   (VFO A state)      See VFO State below
0x27    ...   (VFO B state)      See VFO State below
0x2E    u8    keyLock            0/1
0x2F    u8    bluetooth          0/1
0x30    u8    powerSave          0/1
0x31    u8    keyTones           0/1
0x32    u8    ste                Squelch tail elimination
0x33    u8    rfGain             0 = AGC, 1–42 = manual level
0x37    u32   lastFmtFreq        Last FM tuner frequency
0x3B    u8    vox                0=Off, 1–15=level
0x3C    u16   voxTail            VOX tail in 10 ms units
0x3E    u8    txTimeout          TX timeout in seconds
0x3F    u8    dimmer             LCD dimmer
0x40    u8    dtmfSpeed          DTMF speed
0x41    u8    noiseGate          Noise gate
0x42    u8    asl                0=Off, 1=COS, 2=USB, 3=I-COS
0x43    u8    disableFmt         0/1 (disable FM tuner)
0x44    u16   pin                Security PIN
0x46    u8    pinAction          0=Off, 1=Lock, 2=Unlock
0x47    u8    lcdInverted        0/1
0x48    u8    afFilters          AF filter index (0–7)
0x49    u8    ifFreq             IF frequency index (0–6)
0x4A    u8    sBarStyle          Signal bar style
0x4B    u8    sqNoiseLev         Squelch noise level
0x65    u8    dualWatchDelay     Dual watch delay
0x66    u8    subToneDeviation   Sub-tone deviation
0x74    u8    amAgcFix           0=Off, 1=On
```

### Step Values

The `step` field stores the tuning step in Hz:

| Hz | Display |
|----|---------|
| 2500 | 2.5 kHz |
| 5000 | 5.0 kHz |
| 6250 | 6.25 kHz |
| 12500 | 12.5 kHz |
| 25000 | 25 kHz |
| 50000 | 50 kHz |

### VFO State

VFO A state starts at `+0x20`; VFO B at `+0x27`. Each occupies 7 bytes:

```
+0  u8   activeGroup       Currently selected group (0–15)
+1  u8   lastGroup         Previously active group
+2  u8[5] groupChannels    Channel index per group slot (up to 5 group slots)
```

The driver also tracks `groupModeChannels[16]` (one channel index per group) and `mode` (0 = VFO, 1 = Channel/Group) within the VFO state region.

---

## Band Plan Block (at `0x1A00`)

| Offset | Size | Field | Description |
|--------|------|-------|-------------|
| `0x1A00` | u16 | magic | `0xA46D` |
| `0x1A02` | 10×20 B | entries | 20 band plan entries |

### Band Plan Entry (10 bytes each)

```
Offset  Size  Field      Description
------  ----  ---------  ----------------------------------------
0x00    u32   startFreq  Start frequency in 10 Hz units
0x04    u32   endFreq    End frequency in 10 Hz units
0x08    u8    maxPower   0 = Ignore, 1–255 = maximum power setting
0x09    u8    flags      Packed bit field (see below)
```

### Band Plan Flags

| Bits | Field | Values |
|------|-------|--------|
| 0 | txAllowed | 1 = TX allowed, 0 = TX blocked |
| 1 | wrap | 1 = scan wrap enabled, 0 = disabled |
| 4:2 | modulation | 3-bit index (see Band Plan Modulation) |
| 7:5 | bandwidth | 3-bit index (see Band Plan Bandwidth) |

### Band Plan Modulation

| Raw value | Label |
|-----------|-------|
| 0 | Ignore |
| 1 | FM |
| 2 | AM |
| 3 | USB |
| 4 | Auto |
| 5 | Enforce FM |
| 6 | Enforce AM |
| 7 | Enforce USB |

### Band Plan Bandwidth

| Raw value | Label |
|-----------|-------|
| 0 | Ignore |
| 1 | Wide |
| 2 | Narrow |
| 3 | BW(3) |
| 4 | BW(4) |
| 5 | FM Tuner |
| 6 | BW(6) |
| 7 | BW(7) |

---

## Scan Preset Block (at `0x1B00`)

20 entries × 20 bytes = 400 bytes. Remaining 112 bytes of the 512-byte block are unused.

### Scan Preset Entry (20 bytes each)

```
Offset  Size    Field      Description
------  ------  ---------  -----------------------------------------------
0x00    u32     startFreq  Start frequency in 10 Hz units
0x04    u16     range      Range in 10 kHz units; end = start + (range × 10 kHz)
0x06    u16     step       Step in 10 Hz units
0x08    u8      resume     Scan resume mode
0x09    u8      persist    Scan persist
0x0A    u8      flags      Packed bit field (see below)
0x0B    char[9] label      Label text (8 chars + null terminator)
```

### Scan Preset Flags

| Bits | Field | Values |
|------|-------|--------|
| 1:0 | modulation | 0=FM, 1=AM, 2=USB, 3=Auto |
| 4:2 | ultraScan | Ultrascan level 0–7 |
| 7:5 | (unused) | — |

> **Note:** Scan preset modulation uses a different index order than channel modulation.

---

## Group Labels Block (at `0x1C90`)

15 entries × 6 bytes = 90 bytes (groups A–O).

Each entry is a 6-byte field: up to 5 ASCII characters followed by a null terminator or `0xFF` padding.

```
Entry 0 → Group A
Entry 1 → Group B
...
Entry 14 → Group O
```

---

## Calibration Block (at `0x1DFB`)

5 bytes of per-radio hardware calibration. This data is radio-specific and should be preserved when cloning.

```
Offset  Size  Field                Description
------  ----  -------------------  ----------------------------------------
0x00    i8    xtal671              Crystal oscillator calibration (-128 to +127)
0x01    u8    maxPowerWattsUHF     Max UHF power in 0.1 W units
0x02    u8    maxPowerSettingUHF   Max UHF power setting number
0x03    u8    maxPowerWattsVHF     Max VHF power in 0.1 W units
0x04    u8    maxPowerSettingVHF   Max VHF power setting number
```

The XTAL calibration field at `0x1DFB` is live in V2.5. The equivalent field in the settings block at `+0x10` is **obsolete** and unused.

> **Cloning note:** Cloning the full EEPROM transfers calibration data. If cloning to a different physical radio, re-run calibration via the radio's Advanced Menu or reinitialize with a Default State image before writing channels.

---

## Serial Protocol

Communication is at **38400 baud, 8N1, no flow control**.

### Commands

| Command | Byte | Description |
|---------|------|-------------|
| `CMD_DISABLE_RADIO` | `0x45` | Suspend radio operation before read/write |
| `CMD_ENABLE_RADIO` | `0x46` | Resume radio operation after read/write |
| `CMD_READ_EEPROM` | `0x30` | Read a 32-byte block |
| `CMD_WRITE_EEPROM` | `0x31` | Write a 32-byte block |
| `CMD_REBOOT_RADIO` | `0x49` | Reboot the radio |

### Read Block

1. Send: `[0x30, block_number]`
2. Receive: `[0x30]` (ACK)
3. Receive: 32 bytes of data
4. Receive: 1 byte checksum (sum of the 32 data bytes, mod 256)

### Write Block

1. Send: `[0x31, block_number]`
2. Send: 32 bytes of data
3. Send: 1 byte checksum (sum of the 32 data bytes, mod 256)
4. Receive: `[0x31]` (ACK)

Block numbers run from `0` (address `0x0000`) to `255` (address `0x1FE0`).

---

## Driver Implementation Notes

### Busy Lock Constraint

The radio does not support busy lock simultaneously with repeater or split duplex. The driver enforces this:

- On `set_memory()`: if `duplex` is `+`, `-`, or `split`, `busyLock` is forced to `0`.
- On `get_memory()`: if a TX/RX offset is present, `busyLock` is cleared in the returned object.

### Valid Frequency Bands

```
76.000–108.000 MHz   FM broadcast (RX only; TX blocked by band plan)
136.000–174.000 MHz  VHF
400.000–480.000 MHz  UHF
```

### Channel Numbering

| CHIRP number | EEPROM index | Address |
|-------------|-------------|---------|
| 1–198 | 0–197 | `0x0040 + (index × 32)` |
| VFO A | — | `0x0000` |
| VFO B | — | `0x0020` |
