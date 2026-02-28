"""Minimal 8 KB EEPROM image for TIDRADIO TD-H3 nicFW V2.5.

- Settings magic 0xD82F at 0x1900 (big-endian).
- Channel 0 at 0x0040: 146.52 MHz simplex, 32 bytes channelInfo.
- Rest zeros (empty channels / defaults).
"""

import struct

SIZE = 8192


def build_minimal_image():
    data = bytearray(SIZE)
    # Settings block magic at 0x1900 (V2.5)
    struct.pack_into(">H", data, 0x1900, 0xD82F)
    # Channel 0: 146.52 MHz = 14652000 in 10 Hz units (big-endian u32)
    freq_10hz = 14652000  # 146.52 * 1e6 / 10
    struct.pack_into(">I", data, 0x0040, freq_10hz)  # rxFreq
    struct.pack_into(">I", data, 0x0044, freq_10hz)  # txFreq
    # txPower = 1 (low), name "CH0" at 0x0040 + 20 = 0x0054 for name[12]
    data[0x0048] = 1  # txPower
    data[0x0054 : 0x0054 + 3] = b"CH0"
    return bytes(data)


if __name__ == "__main__":
    import sys

    out = build_minimal_image()
    if len(sys.argv) > 1:
        with open(sys.argv[1], "wb") as f:
            f.write(out)
        print("Wrote %d bytes to %s" % (len(out), sys.argv[1]))
    else:
        sys.stdout.buffer.write(out)
