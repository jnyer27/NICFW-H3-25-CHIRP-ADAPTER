package com.nicfw.tdh3editor

/**
 * Application-level holder for the current EEPROM image so that ChannelEditActivity
 * and GroupLabelEditActivity can read and write the same buffer without passing it
 * through Intent.
 */
object EepromHolder {
    var eeprom: ByteArray? = null

    /**
     * Decoded group labels (A–O, 15 items) parsed from 0x1C90.
     * Empty string means the label is blank in the radio.
     * Populated by MainActivity after each EEPROM load.
     */
    var groupLabels: List<String> = List(15) { "" }
}
