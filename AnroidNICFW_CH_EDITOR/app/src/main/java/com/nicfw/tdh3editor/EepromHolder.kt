package com.nicfw.tdh3editor

/**
 * Application-level holder for the current EEPROM image so that ChannelEditActivity
 * can read and write the same buffer without passing it through Intent.
 */
object EepromHolder {
    var eeprom: ByteArray? = null
}
