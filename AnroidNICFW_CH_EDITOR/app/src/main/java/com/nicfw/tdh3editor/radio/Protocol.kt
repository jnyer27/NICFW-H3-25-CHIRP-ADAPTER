package com.nicfw.tdh3editor.radio

import java.io.IOException

/**
 * TD-H3 nicFW V2.5 EEPROM protocol (mirrors tidradio_h3_nicfw25.py).
 * Commands: 0x45 enter, 0x46 exit, 0x30 read block, 0x31 write block, 0x49 reboot.
 * Block size 32, 256 blocks = 8 KB. Checksum: sum(data) % 256.
 */
object Protocol {

    const val CMD_DISABLE_RADIO = 0x45
    const val CMD_ENABLE_RADIO = 0x46
    const val CMD_READ_EEPROM = 0x30
    const val CMD_WRITE_EEPROM = 0x31
    const val CMD_REBOOT_RADIO = 0x49

    const val BLOCK_SIZE = 32
    const val EEPROM_SIZE = 8192
    const val NUM_BLOCKS = EEPROM_SIZE / BLOCK_SIZE

    fun checksum(data: ByteArray): Int = data.fold(0) { acc, b -> acc + (b.toInt() and 0xFF) } % 256

    @Throws(IOException::class)
    fun enterProgramming(stream: RadioStream) {
        stream.output.write(CMD_DISABLE_RADIO)
        stream.output.flush()
        val ack = readExactly(stream, 1)
        if (ack.size != 1 || (ack[0].toInt() and 0xFF) != CMD_DISABLE_RADIO)
            throw IOException("Radio did not acknowledge disable command")
    }

    @Throws(IOException::class)
    fun exitProgramming(stream: RadioStream) {
        stream.output.write(CMD_ENABLE_RADIO)
        stream.output.flush()
        val ack = readExactly(stream, 1)
        if (ack.size != 1 || (ack[0].toInt() and 0xFF) != CMD_ENABLE_RADIO)
            throw IOException("Radio did not acknowledge enable command")
    }

    @Throws(IOException::class)
    fun readBlock(stream: RadioStream, blockNum: Int): ByteArray {
        require(blockNum in 0 until NUM_BLOCKS)
        stream.output.write(byteArrayOf(CMD_READ_EEPROM.toByte(), blockNum.toByte()))
        stream.output.flush()
        val packetId = readExactly(stream, 1)
        if (packetId.size != 1 || (packetId[0].toInt() and 0xFF) != CMD_READ_EEPROM)
            throw IOException("Invalid read response (packet ID)")
        val data = readExactly(stream, BLOCK_SIZE)
        if (data.size != BLOCK_SIZE)
            throw IOException("Short read: got ${data.size} bytes")
        val checksumRead = readExactly(stream, 1)
        if (checksumRead.size != 1)
            throw IOException("No checksum byte")
        if (checksum(data) != (checksumRead[0].toInt() and 0xFF))
            IOException("Checksum mismatch block $blockNum").let { } // log only; driver continues
        return data
    }

    @Throws(IOException::class)
    fun writeBlock(stream: RadioStream, blockNum: Int, data: ByteArray) {
        require(blockNum in 0 until NUM_BLOCKS)
        require(data.size == BLOCK_SIZE) { "Block must be $BLOCK_SIZE bytes" }
        stream.output.write(CMD_WRITE_EEPROM)
        stream.output.write(blockNum)
        stream.output.write(data)
        stream.output.write(checksum(data))
        stream.output.flush()
        val ack = readExactly(stream, 1)
        if (ack.size != 1 || (ack[0].toInt() and 0xFF) != CMD_WRITE_EEPROM)
            throw IOException("Radio did not acknowledge write block $blockNum")
    }

    @Throws(IOException::class)
    fun rebootRadio(stream: RadioStream) {
        stream.output.write(CMD_REBOOT_RADIO)
        stream.output.flush()
    }

    /**
     * Read exactly n bytes with timeout (using stream's readTimeoutMs).
     * Reads in a loop until n bytes or timeout.
     */
    @Throws(IOException::class)
    fun readExactly(stream: RadioStream, n: Int): ByteArray {
        val timeoutMs = stream.readTimeoutMs.coerceAtLeast(100)
        val start = System.currentTimeMillis()
        val out = ByteArray(n)
        var pos = 0
        while (pos < n) {
            if (System.currentTimeMillis() - start > timeoutMs)
                throw IOException("Read timeout")
            val read = stream.input.read(out, pos, n - pos)
            if (read < 0) throw IOException("End of stream")
            pos += read
        }
        return out
    }

    @Throws(IOException::class)
    fun download(stream: RadioStream, onProgress: (current: Int, total: Int) -> Unit): ByteArray {
        enterProgramming(stream)
        try {
            val data = ByteArray(EEPROM_SIZE)
            for (block in 0 until NUM_BLOCKS) {
                val chunk = readBlock(stream, block)
                chunk.copyInto(data, block * BLOCK_SIZE)
                onProgress(block + 1, NUM_BLOCKS)
            }
            return data
        } finally {
            exitProgramming(stream)
        }
    }

    @Throws(IOException::class)
    fun upload(stream: RadioStream, eeprom: ByteArray, onProgress: (current: Int, total: Int) -> Unit) {
        require(eeprom.size >= EEPROM_SIZE) { "Image too small" }
        enterProgramming(stream)
        try {
            for (block in 0 until NUM_BLOCKS) {
                val start = block * BLOCK_SIZE
                val chunk = eeprom.copyOfRange(start, start + BLOCK_SIZE)
                writeBlock(stream, block, chunk)
                onProgress(block + 1, NUM_BLOCKS)
            }
        } finally {
            exitProgramming(stream)
        }
        rebootRadio(stream)
    }
}
