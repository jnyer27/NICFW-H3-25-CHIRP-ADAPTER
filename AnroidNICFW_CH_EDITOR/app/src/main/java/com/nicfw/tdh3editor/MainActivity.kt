package com.nicfw.tdh3editor

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nicfw.tdh3editor.bluetooth.BleManager
import com.nicfw.tdh3editor.bluetooth.BtSerialManager
import com.nicfw.tdh3editor.databinding.ActivityMainBinding
import com.nicfw.tdh3editor.radio.EepromConstants
import com.nicfw.tdh3editor.radio.EepromParser
import com.nicfw.tdh3editor.radio.Protocol
import com.nicfw.tdh3editor.radio.RadioStream
import com.nicfw.tdh3editor.radio.ToneCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Classic Bluetooth SPP (fallback for older radios / already-paired devices)
    private lateinit var btManager: BtSerialManager

    // BLE — primary connection method for nicFW TD-H3
    private lateinit var bleManager: BleManager

    /** Whichever connection (BLE or SPP) is currently active. Null when disconnected. */
    private var activeStream: RadioStream? = null
    private var activeDeviceName: String? = null

    private var eeprom: ByteArray? = null
    private var channelList: List<com.nicfw.tdh3editor.radio.Channel> = emptyList()

    private val adapter = ChannelAdapter { channel ->
        if (eeprom != null) {
            startActivity(ChannelEditActivity.intent(this, channel.number, eeprom!!))
        }
    }

    // ─── BLE scan state ───────────────────────────────────────────────────────
    private var scanDialog: AlertDialog? = null
    private val scanDevices = mutableListOf<BluetoothDevice>()
    private val scanDeviceRssi = mutableListOf<Int>()
    private lateinit var scanListAdapter: ArrayAdapter<String>
    private val scanTimeoutHandler = Handler(Looper.getMainLooper())
    private val stopScanRunnable = Runnable { stopBleScan() }

    // ─── Permission launcher ──────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { !it }) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        btManager = BtSerialManager(this)
        bleManager = BleManager(this)

        binding.recyclerChannels.layoutManager = LinearLayoutManager(this)
        binding.recyclerChannels.adapter = adapter

        requestBluetoothPermissions()

        binding.btnConnect.setOnClickListener {
            if (isAnyConnected()) {
                disconnectAll()
                updateConnectionUi()
            } else {
                showConnectPicker()
            }
        }

        binding.btnLoad.setOnClickListener { loadFromRadio() }
        binding.btnSave.setOnClickListener { showSaveConfirm() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Options menu
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val hasEeprom = (eeprom != null)
        menu.findItem(R.id.action_save_dump)?.isEnabled = hasEeprom
        menu.findItem(R.id.action_edit_group_labels)?.isEnabled = hasEeprom
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save_dump -> { saveEepromDump(); true }
            R.id.action_edit_group_labels -> {
                startActivity(Intent(this, GroupLabelEditActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EEPROM dump (debug)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves two files into the app's private external-files directory then shares them:
     *
     *  • `tdh3_eeprom_<timestamp>.bin`  — raw 8 KB EEPROM image
     *  • `tdh3_tones_<timestamp>.txt`   — human-readable hex dump of every channel's
     *                                     rxSubTone / txSubTone words plus decoded values,
     *                                     making it easy to spot DCS/CTCSS mapping bugs
     *
     * No WRITE_EXTERNAL_STORAGE permission required on API 29+.
     */
    private fun saveEepromDump() {
        val eep = eeprom ?: run {
            Toast.makeText(this, "No EEPROM data loaded — load from radio first", Toast.LENGTH_SHORT).show()
            return
        }

        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = getExternalFilesDir(null) ?: filesDir

        // ── Binary dump ───────────────────────────────────────────────────────
        val binFile = File(dir, "tdh3_eeprom_$ts.bin")
        binFile.writeBytes(eep)

        // ── Tone analysis text ────────────────────────────────────────────────
        val txtFile = File(dir, "tdh3_tones_$ts.txt")
        buildToneAnalysis(eep, ts).let { txtFile.writeText(it) }

        // ── Share both files ──────────────────────────────────────────────────
        val authority = "${packageName}.fileprovider"
        val binUri  = FileProvider.getUriForFile(this, authority, binFile)
        val txtUri  = FileProvider.getUriForFile(this, authority, txtFile)

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                arrayListOf(binUri, txtUri)
            )
            putExtra(Intent.EXTRA_SUBJECT, "TD-H3 EEPROM dump $ts")
            putExtra(
                Intent.EXTRA_TEXT,
                "TD-H3 EEPROM dump\n" +
                "Binary: ${binFile.name} (${eep.size} bytes)\n" +
                "Tone analysis: ${txtFile.name}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share EEPROM dump"))
    }

    /**
     * Builds a human-readable text report of every channel's raw tone sub-tone words
     * and how [ToneCodec] currently decodes them — essential for diagnosing DCS mapping issues.
     *
     * Example line:
     *   Ch  2  off=0x0060  RX=0x81EC → DTCS 492 (raw9) → decoded 754  TX=0x0000 → None
     */
    private fun buildToneAnalysis(eep: ByteArray, ts: String): String {
        val sb = StringBuilder()
        sb.appendLine("TD-H3 EEPROM Tone Debug  $ts")
        sb.appendLine("Format: Ch N  offset  RX=0xWWWW → raw9=DDD → decoded VAL  TX=0xWWWW → …")
        sb.appendLine("=".repeat(80))

        for (ch in 1..EepromConstants.NUM_CHANNELS) {
            val idx = ch - 1
            val off = EepromConstants.CHANNEL_BASE + idx * EepromConstants.CHANNEL_STRUCT_SIZE
            if (off + 12 > eep.size) break

            // Check empty marker (all 0xFF for first 4 bytes)
            val isEmpty = (eep[off].toInt() and 0xFF) == 0xFF &&
                          (eep[off+1].toInt() and 0xFF) == 0xFF &&
                          (eep[off+2].toInt() and 0xFF) == 0xFF &&
                          (eep[off+3].toInt() and 0xFF) == 0xFF
            val rxToneWord = ((eep[off+8].toInt() and 0xFF) shl 8) or (eep[off+9].toInt() and 0xFF)
            val txToneWord = ((eep[off+10].toInt() and 0xFF) shl 8) or (eep[off+11].toInt() and 0xFF)

            // Skip empty channels with no tones
            if (isEmpty && rxToneWord == 0 && txToneWord == 0) continue

            val (rxMode, rxVal, rxPol) = ToneCodec.decode(rxToneWord)
            val (txMode, txVal, txPol) = ToneCodec.decode(txToneWord)

            fun toneDesc(word: Int, mode: String?, value: Double?, pol: String?): String {
                val hex = "0x${word.toString(16).padStart(4, '0').uppercase()}"
                val raw9 = word and 0x01FF
                return when {
                    word == 0     -> "$hex → None"
                    word in 1..3000 -> "$hex → CTCSS ${value} Hz"
                    (word and 0x8000) != 0 ->
                        "$hex → raw9=$raw9 (0x${raw9.toString(16)}) → decoded $mode ${value} pol=$pol"
                    else -> "$hex → unknown"
                }
            }

            sb.appendLine(
                "Ch %3d  off=0x%04X  %s  RX: %-50s  TX: %s".format(
                    ch, off,
                    if (isEmpty) "[empty]" else "       ",
                    toneDesc(rxToneWord, rxMode, rxVal, rxPol),
                    toneDesc(txToneWord, txMode, txVal, txPol)
                )
            )
        }
        sb.appendLine("=".repeat(80))
        sb.appendLine("Done. Channels with all-zero tones omitted.")
        return sb.toString()
    }

    override fun onResume() {
        super.onResume()
        updateConnectionUi()
        EepromHolder.eeprom?.let { data ->
            eeprom = data
            EepromHolder.groupLabels = EepromParser.parseGroupLabels(data)
            refreshChannelList(data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanTimeoutHandler.removeCallbacks(stopScanRunnable)
        bleManager.stopScan()
        bleManager.disconnect()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permissions
    // ─────────────────────────────────────────────────────────────────────────

    private fun requestBluetoothPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+  — neverForLocation flag means no location permission needed
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN))
                perms.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            // Android < 12 — ACCESS_FINE_LOCATION is required for BLE scanning
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    // ─────────────────────────────────────────────────────────────────────────
    // Connection management
    // ─────────────────────────────────────────────────────────────────────────

    private fun isAnyConnected() = activeStream != null

    private fun disconnectAll() {
        bleManager.disconnect()
        btManager.disconnect()
        activeStream = null
        activeDeviceName = null
    }

    /**
     * Shows the top-level connection picker: BLE scan (primary) or Classic SPP (fallback).
     */
    private fun showConnectPicker() {
        AlertDialog.Builder(this)
            .setTitle("Connect to radio")
            .setItems(arrayOf("Scan for Radio  (BLE)", "Paired Devices  (Classic BT)")) { _, which ->
                when (which) {
                    0 -> startBleScan()
                    1 -> showPairedDevicePicker()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLE scan flow
    // ─────────────────────────────────────────────────────────────────────────

    private fun startBleScan() {
        scanDevices.clear()
        scanDeviceRssi.clear()

        scanListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        scanListAdapter.add("Scanning…  (up to 8 seconds)")

        scanDialog = AlertDialog.Builder(this)
            .setTitle("Select Radio  (BLE)")
            .setAdapter(scanListAdapter) { _, which ->
                if (which < scanDevices.size) {
                    scanTimeoutHandler.removeCallbacks(stopScanRunnable)
                    bleManager.stopScan()
                    scanDialog?.dismiss()
                    scanDialog = null
                    connectBleDevice(scanDevices[which])
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                scanTimeoutHandler.removeCallbacks(stopScanRunnable)
                bleManager.stopScan()
            }
            .create()
        scanDialog?.show()

        bleManager.startScan(
            onFound = { device, rssi -> runOnUiThread { addScanResult(device, rssi) } },
            onError = { code ->
                runOnUiThread {
                    scanDialog?.dismiss()
                    scanDialog = null
                    Toast.makeText(
                        this,
                        "BLE scan failed (code $code). Check Bluetooth is on and permissions are granted.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        // Auto-stop scan after 8 seconds
        scanTimeoutHandler.postDelayed(stopScanRunnable, 8_000)
    }

    @Suppress("MissingPermission")
    private fun addScanResult(device: BluetoothDevice, rssi: Int) {
        scanDevices.add(device)
        scanDeviceRssi.add(rssi)
        scanListAdapter.clear()
        scanDevices.forEachIndexed { i, d ->
            val name = d.name?.takeIf { it.isNotBlank() } ?: d.address
            scanListAdapter.add("$name  (${scanDeviceRssi[i]} dBm)")
        }
        scanListAdapter.notifyDataSetChanged()
    }

    private fun stopBleScan() {
        bleManager.stopScan()
        if (scanDevices.isEmpty()) {
            scanDialog?.dismiss()
            scanDialog = null
            Toast.makeText(
                this,
                "No BLE devices found. Make sure the radio is powered on and in range.",
                Toast.LENGTH_LONG
            ).show()
        }
        // If devices were found the dialog stays open for the user to pick
    }

    @Suppress("MissingPermission")
    private fun connectBleDevice(device: BluetoothDevice) {
        val label = device.name?.takeIf { it.isNotBlank() } ?: device.address
        Toast.makeText(this, "Connecting to $label…", Toast.LENGTH_SHORT).show()

        bleManager.connect(device) { result ->
            runOnUiThread {
                result.fold(
                    onSuccess = { stream ->
                        activeStream = stream
                        activeDeviceName = bleManager.deviceName()
                        updateConnectionUi()
                        Toast.makeText(this, "Connected via BLE", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        updateConnectionUi()
                        Toast.makeText(this, "BLE connect failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Classic SPP flow (fallback)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showPairedDevicePicker() {
        val devices = btManager.pairedDevices()
        if (devices.isEmpty()) {
            Toast.makeText(
                this,
                "No paired devices. Pair the radio in System Bluetooth settings first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        @Suppress("MissingPermission")
        val names = devices.map { it.name ?: it.address }
        AlertDialog.Builder(this)
            .setTitle("Select radio  (Classic BT)")
            .setItems(names.toTypedArray()) { _, which ->
                connectSppDevice(devices[which])
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun connectSppDevice(device: BluetoothDevice) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { btManager.connect(device) }
            runOnUiThread {
                result.fold(
                    onSuccess = { stream ->
                        activeStream = stream
                        activeDeviceName = btManager.connectedDeviceName()
                        updateConnectionUi()
                        Toast.makeText(this@MainActivity, "Connected via SPP", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(this@MainActivity, "SPP connect failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun updateConnectionUi() {
        val connected = isAnyConnected()
        binding.statusText.text = if (connected) {
            getString(R.string.status_connected, activeDeviceName ?: "Radio")
        } else {
            getString(R.string.status_disconnected)
        }
        binding.btnConnect.text =
            if (connected) getString(R.string.disconnect) else getString(R.string.connect)
        binding.btnLoad.isEnabled = connected
        binding.btnSave.isEnabled = connected && eeprom != null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Radio operations (protocol unchanged — works over BLE and classic SPP)
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadFromRadio() {
        val stream = activeStream ?: return
        stream.readTimeoutMs = 500
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.btnLoad.isEnabled = false
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    Protocol.download(stream) { cur, total ->
                        runOnUiThread {
                            binding.progressBar.progress = cur
                            binding.progressText.text = getString(R.string.cloning, cur, total)
                        }
                    }
                }
                eeprom = data
                EepromHolder.eeprom = data
                EepromHolder.groupLabels = EepromParser.parseGroupLabels(data)
                refreshChannelList(data)
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                    updateConnectionUi()
                    Toast.makeText(this@MainActivity, "Loaded ${data.size} bytes", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                    updateConnectionUi()
                    Toast.makeText(this@MainActivity, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun refreshChannelList(data: ByteArray) {
        if (data.size < Protocol.EEPROM_SIZE) return
        channelList = EepromParser.parseAllChannels(data)
        adapter.submitList(channelList)
    }

    private fun showSaveConfirm() {
        val eep = eeprom ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.save_confirm_title)
            .setMessage(R.string.save_confirm_message)
            .setPositiveButton(R.string.ok) { _, _ -> saveToRadio(eep) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveToRadio(data: ByteArray) {
        val stream = activeStream ?: run {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            return
        }
        stream.readTimeoutMs = 500
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Protocol.upload(stream, data) { cur, total ->
                        runOnUiThread {
                            binding.progressBar.progress = cur
                            binding.progressText.text = getString(R.string.cloning, cur, total)
                        }
                    }
                }
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                    updateConnectionUi()
                    Toast.makeText(this@MainActivity, "Saved to radio", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                    updateConnectionUi()
                    Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
