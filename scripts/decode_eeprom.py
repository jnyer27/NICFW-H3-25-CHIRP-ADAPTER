#!/usr/bin/env python3
"""Decode a nicFW 2.5 EEPROM dump and compare against YAML help defaults."""
import sys, struct

dump_path = r"C:\Users\jason\Downloads\TD-H3 EEPROM dump 20260312_174712"
b = open(dump_path, "rb").read()

def u8(off):  return b[off] & 0xFF
def i8(off):  v = b[off] & 0xFF; return v - 256 if v >= 128 else v
def u16(off): return ((b[off] & 0xFF) << 8) | (b[off+1] & 0xFF)

# Offsets from EepromConstants.kt
C = dict(
    SQ=0x1902, SQ_NOISE=0x194D, NOISE_CEIL=0x1973,
    STE=0x194A, MIC_GAIN=0x190E, NOISE_GATE=0x1958,
    RF_GAIN=0x194B, LCD_BRIGHT=0x1919, LCD_TIME=0x191A,
    BREATHE=0x191B, LCD_DIM=0x1956, GAMMA=0x191D,
    LCD_INV=0x195F, SBAR=0x194C, SBAR_PERS=0x1962,
    TX_MOD=0x190D, TX_TO=0x1955, TX_DEV=0x190F,
    PTT=0x190C, TX_CURR=0x1967,
    REP_TONE=0x191E, STEP=0x1906, AF_FILT=0x1960,
    IF_FREQ=0x1961, RFI_COMP=0x196C, AGC0=0x1968,
    AGC1=0x1969, AGC2=0x196A, AGC3=0x196B,
    AM_AGC=0x1974, DW=0x1903, SCAN_RES=0x1916,
    SCAN_RNG=0x1912, SCAN_PERS=0x1914, SCAN_UPD=0x1959,
    ULTRA=0x1917, VOX=0x1952, VOX_TAIL=0x1953,
    TONE_MON=0x1918, KEY_TONES=0x1949, SUB_DEV=0x1966,
    DTMF_DEV=0x191C, DTMF_SPD=0x1957, DTMF_DEC=0x196E,
    DTMF_PAUS=0x1972, BATT=0x1911, BT=0x1947,
    PWR_SAVE=0x1948, DW_DELAY=0x1965, VFO_LOCK=0x1964,
    ASL=0x195A, DIS_FMT=0x195B, SCRAMBLE=0x196D,
    PIN=0x195C, PIN_ACT=0x195E,
    XTAL_LIVE=0x1DFB, MAX_UHF_S=0x1DFD, MAX_VHF_S=0x1DFF,
)

IF_LIST   = ["8.46 kHz","7.25 kHz","6.35 kHz","5.64 kHz","5.08 kHz","4.62 kHz","4.23 kHz"]
STE_LIST  = ["Off","RX","TX","Both"]
SBAR_LIST = ["Segment","Stepped","Solid"]
BATT_LIST = ["Off","Icon","Percent","Volts"]
TONE_LIST = ["Off","On","Clone"]
KEY_LIST  = ["Off","On","Differential","Voice"]
PTT_LIST  = ["Dual","Single","Hybrid"]
ASL_LIST  = ["Off","COS","USB","I-COS"]
PIN_LIST  = ["Off","On","Power On"]
STEP_MAP  = {250:"2.5 kHz",500:"5.0 kHz",625:"6.25 kHz",1250:"12.5 kHz",2500:"25.0 kHz",5000:"50.0 kHz"}
RFGAIN    = ["AGC"]+[str(x) for x in range(1,43)]

def onoff(v): return "On" if v else "Off"

rows = [
  ("squelch",        "squelch",           str(u8(C["SQ"])),                                          "2"),
  ("sq_noise_lev",   "sqNoiseLev",        str(u8(C["SQ_NOISE"])),                                    "47"),
  ("noise_ceiling",  "noiseCeiling",      str(u8(C["NOISE_CEIL"])),                                  "0"),
  ("sq_tail_elim",   "ste",               STE_LIST[min(u8(C["STE"]),3)],                             "Off"),
  ("mic_gain",       "micGain",           str(u8(C["MIC_GAIN"])),                                    "25"),
  ("noise_gate",     "noiseGate",         onoff(u8(C["NOISE_GATE"])),                                "Off"),
  ("rf_gain",        "rfGain",            RFGAIN[min(u8(C["RF_GAIN"]),42)],                          "AGC"),
  ("lcd_brightness", "lcdBrightness",     str(u8(C["LCD_BRIGHT"])),                                  "28"),
  ("lcd_timeout",    "lcdTimeout",        str(u8(C["LCD_TIME"])) if u8(C["LCD_TIME"]) else "Off",    "Off"),
  ("heartbeat",      "breathe",           str(u8(C["BREATHE"])) if u8(C["BREATHE"]) else "Off",      "Off"),
  ("dim_brightness", "lcdDim",            str(u8(C["LCD_DIM"])) if u8(C["LCD_DIM"]) else "Off",      "Off"),
  ("lcd_gamma",      "lcdGamma",          str(u8(C["GAMMA"])),                                        "0"),
  ("lcd_inverted",   "lcdInverted",       onoff(u8(C["LCD_INV"])),                                   "Off"),
  ("sbar_style",     "sBarStyle",         SBAR_LIST[min(u8(C["SBAR"]),2)],                           "Segment"),
  ("sbar_always_on", "sBarPersistent",    onoff(u8(C["SBAR_PERS"])),                                 "Off"),
  ("tx_mod_meter",   "txModMeter",        onoff(u8(C["TX_MOD"])),                                    "Off"),
  ("tx_timeout",     "txTimeout",         str(u8(C["TX_TO"])) if u8(C["TX_TO"]) else "Off",          "Off"),
  ("tx_deviation",   "txDeviation",       str(u8(C["TX_DEV"])),                                      "64"),
  ("ptt_mode",       "pttMode",           PTT_LIST[min(u8(C["PTT"]),2)],                             "Dual"),
  ("tx_current",     "showXmitCurrent",   onoff(u8(C["TX_CURR"])),                                   "Off"),
  ("repeater_tone",  "repeaterTone",      str(u16(C["REP_TONE"]))+" Hz",                             "1750 Hz"),
  ("step",           "step",              STEP_MAP.get(u16(C["STEP"]), str(u16(C["STEP"]))+" raw"),  "12.5 kHz"),
  ("af_filters",     "afFilters",         str(u8(C["AF_FILT"])),                                     "0"),
  ("if_freq",        "ifFreq",            IF_LIST[u8(C["IF_FREQ"])] if u8(C["IF_FREQ"])<len(IF_LIST) else str(u8(C["IF_FREQ"])), "8.46 kHz"),
  ("rfi_comp",       "rfiComp",           str(u8(C["RFI_COMP"])) if u8(C["RFI_COMP"]) else "Off",   "Off"),
  ("agc0 (Tbl 1)",   "agc0",              str(u8(C["AGC0"])),                                        "24"),
  ("agc1 (Tbl 2)",   "agc1",              str(u8(C["AGC1"])),                                        "32"),
  ("agc2 (Tbl 3)",   "agc2",              str(u8(C["AGC2"])),                                        "37"),
  ("agc3 (Tbl 4)",   "agc3",              str(u8(C["AGC3"])),                                        "40"),
  ("am_agc_fix",     "amAgcFix",          onoff(u8(C["AM_AGC"])),                                    "Off"),
  ("dual_watch",     "dualWatch",         onoff(u8(C["DW"])),                                        "Off"),
  ("scan_resume",    "scanResume",        str(u8(C["SCAN_RES"])) if u8(C["SCAN_RES"]) else "Off",    "Off"),
  ("scan_range",     "scanRange",         "%d MHz" % (u16(C["SCAN_RNG"])*10//1000),                  "10 MHz"),
  ("scan_persist",   "scanPersist",       "%.1f" % (u16(C["SCAN_PERS"])/10.0),                       "0"),
  ("scan_update",    "scanUpdate",        str(u8(C["SCAN_UPD"])) if u8(C["SCAN_UPD"]) else "Off",    "Off"),
  ("ultra_scan",     "ultraScan",         str(u8(C["ULTRA"])) if u8(C["ULTRA"]) else "Off",          "Off"),
  ("vox",            "vox",               str(u8(C["VOX"])) if u8(C["VOX"]) else "Off",              "Off"),
  ("vox_tail",       "voxTail",           "%.1f sec" % (u16(C["VOX_TAIL"])/10.0),                    "2.0 sec"),
  ("tone_monitor",   "toneMonitor",       TONE_LIST[min(u8(C["TONE_MON"]),2)],                       "Off"),
  ("key_tones",      "keyTones",          KEY_LIST[min(u8(C["KEY_TONES"]),3)],                       "Off"),
  ("sub_tone_dev",   "subToneDev",        str(u8(C["SUB_DEV"])),                                     "74"),
  ("dtmf_volume",    "dtmfDev",           str(u8(C["DTMF_DEV"])),                                    "32"),
  ("dtmf_speed",     "dtmfSpeed",         str(u8(C["DTMF_SPD"])),                                    "0"),
  ("dtmf_decode",    "dtmfDecode",        str(u8(C["DTMF_DEC"])) if u8(C["DTMF_DEC"]) else "Off",   "Off"),
  ("dtmf_seq_pause", "dtmfSeqPause",      "%.1f" % (u8(C["DTMF_PAUS"])/10.0),                       "0"),
  ("batt_style",     "battStyle",         BATT_LIST[min(u8(C["BATT"]),3)],                           "Icon"),
  ("bluetooth",      "bluetooth",         onoff(u8(C["BT"])),                                        "On"),
  ("power_save",     "powerSave",         str(u8(C["PWR_SAVE"])) if u8(C["PWR_SAVE"]) else "Off",    "Off"),
  ("dw_delay",       "dwDelay",           str(u8(C["DW_DELAY"])),                                    "1"),
  ("vfo_dw_lock",    "vfoLockActive",     onoff(u8(C["VFO_LOCK"])),                                  "Off"),
  ("asl",            "asl",               ASL_LIST[min(u8(C["ASL"]),3)],                             "Off"),
  ("disable_fmt",    "disableFmt",        "Yes" if u8(C["DIS_FMT"]) else "No",                       "No"),
  ("scrambler_if",   "scramblerIf",       str(u8(C["SCRAMBLE"])) if u8(C["SCRAMBLE"]) else "Off",    "Off"),
  ("PIN (skip)",     "pin",               str(u16(C["PIN"])),                                        "9999"),
  ("pin_action",     "pinAction",         PIN_LIST[min(u8(C["PIN_ACT"]),2)],                         "Off"),
  ("xtal_671 (skip)","xtal671 live",      str(i8(C["XTAL_LIVE"])),                                   "0"),
  ("(tune) uhf_cap", "maxPowerSettingUHF",str(u8(C["MAX_UHF_S"])),                                   "255"),
  ("(tune) vhf_cap", "maxPowerSettingVHF",str(u8(C["MAX_VHF_S"])),                                   "255"),
]

print(f"{'YAML key':<24} {'Radio value':<18} {'YAML default':<18} Match")
print("-"*75)
mismatches = []
for yaml_key, field, radio_val, yaml_def in rows:
    match = "OK" if radio_val == yaml_def else "DIFF"
    flag = " <<<" if match == "DIFF" else ""
    print(f"{yaml_key:<24} {radio_val:<18} {yaml_def:<18} {match}{flag}")
    if match == "DIFF":
        mismatches.append((yaml_key, field, radio_val, yaml_def))

print()
print(f"=== {len(mismatches)} MISMATCHES (radio value != YAML default) ===")
for k, f, rv, yd in mismatches:
    skip = " *** DO NOT UPDATE (personal/calibration)" if "(skip)" in k else ""
    print(f"  {k:<24}  radio={rv:<14} yaml_default={yd}{skip}")
