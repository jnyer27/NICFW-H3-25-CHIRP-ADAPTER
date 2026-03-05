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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nicfw.tdh3editor.bluetooth.BleManager
import com.nicfw.tdh3editor.bluetooth.BtSerialManager
import com.nicfw.tdh3editor.databinding.ActivityMainBinding
import com.nicfw.tdh3editor.radio.Channel
import com.nicfw.tdh3editor.radio.ChirpCsvImporter
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
import java.util.Collections
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
    private var channelList: List<Channel> = emptyList()

    /**
     * Working copy of the channel list used by [ItemTouchHelper] during drag-to-reorder.
     * Kept in sync with [channelList] outside of drag operations.
     */
    private var dragWorkList: MutableList<Channel> = mutableListOf()

    /** ItemTouchHelper that powers drag-to-reorder of channel cards. */
    private lateinit var touchHelper: ItemTouchHelper

    // ─── Adapter ──────────────────────────────────────────────────────────────

    private val adapter = ChannelAdapter(
        onChannelClick = { channel ->
            if (eeprom != null) {
                startActivity(ChannelEditActivity.intent(this, channel.number, eeprom!!))
            }
        },
        onLongClick = { channel -> enterSelectionMode(channel) },
        onSelectionChanged = { count -> updateSelectionBar(count) },
        onDragStart  = { vh -> touchHelper.startDrag(vh) }
    )

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

    // ─── CHIRP CSV file picker ────────────────────────────────────────────────
    /**
     * Launches the system file picker for a CSV file, reads the content on the IO
     * dispatcher, parses it with [ChirpCsvImporter] to validate it, then starts
     * [ChirpImportActivity] with the raw CSV text so the import screen can present
     * the full preview and group-assignment UI.
     */
    private val csvPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        if (eeprom == null) {
            Toast.makeText(this, "Load EEPROM from radio before importing", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            try {
                // Read the file on the IO thread
                val (csvText, commentsList) = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val text = stream.bufferedReader().readText()
                        // Extract comments in parallel with parse for the preview
                        val comments = extractComments(text)
                        Pair(text, comments)
                    } ?: Pair("", emptyList<String>())
                }

                if (csvText.isBlank()) {
                    Toast.makeText(this@MainActivity, "Could not read file", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val entries = ChirpCsvImporter.parse(csvText)
                if (entries.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No valid CHIRP channels found in selected file",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Launch the import preview / group-assignment screen
                val intent = Intent(this@MainActivity, ChirpImportActivity::class.java).apply {
                    putExtra(ChirpImportActivity.EXTRA_CSV_TEXT, csvText)
                    putExtra(ChirpImportActivity.EXTRA_COMMENTS, ArrayList(commentsList))
                }
                startActivity(intent)

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to read CSV: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Extracts the optional "Comment" column from each data row in the CSV so the
     * import preview can display location descriptions (e.g. "Baltimore, Pigtown").
     * Returns a list parallel to the parsed [ChirpCsvImporter.ChirpEntry] list.
     */
    private fun extractComments(csvText: String): List<String> {
        val lines = csvText.lines()
        val headerIdx = lines.indexOfFirst { it.trimStart().startsWith("Location", ignoreCase = true) }
        if (headerIdx < 0) return emptyList()

        val headers = lines[headerIdx].split(",").map { it.trim().lowercase().trim('"') }
        val commentIdx = headers.indexOf("comment")
        if (commentIdx < 0) return emptyList()

        val result = mutableListOf<String>()
        for (i in (headerIdx + 1) until lines.size) {
            val row = lines[i].trim()
            if (row.isEmpty()) continue
            // Quick split — comments may be quoted
            val cols = buildList {
                var inQ = false; val cur = StringBuilder()
                for (c in row) when {
                    c == '"' -> inQ = !inQ
                    c == ',' && !inQ -> { add(cur.toString()); cur.clear() }
                    else -> cur.append(c)
                }
                add(cur.toString())
            }
            result += if (commentIdx in cols.indices) cols[commentIdx].trim().trim('"') else ""
        }
        return result
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

        setupTouchHelper()
        setupSelectionBar()

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
    // Drag-to-reorder setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupTouchHelper() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            // Drag is started only from the explicit drag handle touch
            override fun isLongPressDragEnabled() = false

            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                val f = from.bindingAdapterPosition
                val t = to.bindingAdapterPosition
                if (f < 0 || t < 0 ||
                    f >= dragWorkList.size || t >= dragWorkList.size) return false
                Collections.swap(dragWorkList, f, t)
                adapter.notifyItemMoved(f, t)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                // No swipe actions
            }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                // Finger lifted — commit the drag order to EEPROM
                applyDragReorder()
            }
        }

        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerChannels)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // In-app selection bar (replaces ActionMode/CAB)
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupSelectionBar() {
        binding.btnMoveUp.setOnClickListener      { moveSelectedUp() }
        binding.btnMoveDown.setOnClickListener    { moveSelectedDown() }
        binding.btnClearSelected.setOnClickListener { clearSelectedChannels() }
        binding.btnSelectionDone.setOnClickListener {
            adapter.exitSelectionMode()
            // onSelectionChanged(0) will be called → hides the bar
        }
    }

    /**
     * Shows/hides the selection bar and keeps the count label up to date.
     * Called from [ChannelAdapter.onSelectionChanged] on every selection change.
     */
    private fun updateSelectionBar(count: Int) {
        if (count == 0) {
            binding.selectionBar.visibility = View.GONE
        } else {
            binding.selectionBar.visibility = View.VISIBLE
            binding.selectionCount.text = "$count selected"
        }
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
        menu.findItem(R.id.action_import_chirp)?.isEnabled = hasEeprom
        menu.findItem(R.id.action_sort_by_group)?.isEnabled = hasEeprom
        menu.findItem(R.id.action_save_dump)?.isEnabled = hasEeprom
        menu.findItem(R.id.action_edit_group_labels)?.isEnabled = hasEeprom
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import_chirp -> {
                // Open system file picker — accept CSV and plain-text MIME types
                csvPickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                true
            }
            R.id.action_sort_by_group -> {
                startActivity(Intent(this, ChannelSortActivity::class.java))
                true
            }
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

            val isEmpty = (eep[off].toInt() and 0xFF) == 0xFF &&
                          (eep[off+1].toInt() and 0xFF) == 0xFF &&
                          (eep[off+2].toInt() and 0xFF) == 0xFF &&
                          (eep[off+3].toInt() and 0xFF) == 0xFF
            val rxToneWord = ((eep[off+8].toInt() and 0xFF) shl 8) or (eep[off+9].toInt() and 0xFF)
            val txToneWord = ((eep[off+10].toInt() and 0xFF) shl 8) or (eep[off+11].toInt() and 0xFF)

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
    // Channel selection — in-app bar replaces the system ActionMode/CAB
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Enters multi-select mode for [channel] as the first selection and reveals
     * the in-app selection bar at the bottom of the screen.
     */
    private fun enterSelectionMode(channel: Channel) {
        adapter.enterSelectionMode(channel.number)
        // updateSelectionBar() is called automatically via onSelectionChanged callback
    }

    /**
     * Moves every selected channel up by one slot, keeping contiguous groups
     * together as a unit.
     *
     * Algorithm: build a [BooleanArray] of which positions are selected, then
     * scan top-to-bottom for contiguous selected blocks.  For each block that
     * has a free (non-selected) slot immediately above it, rotate that slot to
     * just below the block — equivalent to the whole block sliding up by one.
     * Uses [Collections.rotate] on a sub-list view for an O(n) in-place move.
     */
    private fun moveSelectedUp() {
        val eep = eeprom ?: return
        val selected = adapter.selectedChannelNumbers
        if (selected.isEmpty()) return

        val channels = EepromParser.parseAllChannels(eep).toMutableList()

        // Parallel boolean array — true if channels[i] is currently selected.
        val sel = BooleanArray(channels.size) { i -> (i + 1) in selected }

        var i = 0
        while (i < channels.size) {
            if (sel[i]) {
                // Find end of this contiguous selected block
                var j = i
                while (j + 1 < channels.size && sel[j + 1]) j++

                // Block = [i..j].  Can we move it up?
                if (i > 0 && !sel[i - 1]) {
                    // Rotate [i-1 .. j] left by 1:
                    //   [above, blk0, blk1, …, blkN] → [blk0, blk1, …, blkN, above]
                    Collections.rotate(channels.subList(i - 1, j + 1), -1)
                    sel[j]     = false   // "above" channel now sits at j
                    sel[i - 1] = true    // block now occupies [i-1 .. j-1]
                }
                i = j + 1
            } else {
                i++
            }
        }

        // Renumber and build new selected set
        channels.forEachIndexed { idx, ch -> channels[idx] = ch.copy(number = idx + 1) }
        val newSelected = channels.indices.filter { sel[it] }.map { it + 1 }.toSet()

        applyChannelReorder(eep, channels, newSelected)
    }

    /**
     * Moves every selected channel down by one slot, keeping contiguous groups
     * together as a unit.
     *
     * Mirror of [moveSelectedUp]: scans bottom-to-top for contiguous blocks and
     * rotates the slot immediately below each block to just above it.
     */
    private fun moveSelectedDown() {
        val eep = eeprom ?: return
        val selected = adapter.selectedChannelNumbers
        if (selected.isEmpty()) return

        val channels = EepromParser.parseAllChannels(eep).toMutableList()

        val sel = BooleanArray(channels.size) { i -> (i + 1) in selected }

        var j = channels.size - 1
        while (j >= 0) {
            if (sel[j]) {
                // Find start of this contiguous selected block
                var i = j
                while (i - 1 >= 0 && sel[i - 1]) i--

                // Block = [i..j].  Can we move it down?
                if (j < channels.size - 1 && !sel[j + 1]) {
                    // Rotate [i .. j+1] right by 1:
                    //   [blk0, blk1, …, blkN, below] → [below, blk0, blk1, …, blkN]
                    Collections.rotate(channels.subList(i, j + 2), 1)
                    sel[i]     = false   // "below" channel now sits at i
                    sel[j + 1] = true    // block now occupies [i+1 .. j+1]
                }
                j = i - 1
            } else {
                j--
            }
        }

        channels.forEachIndexed { idx, ch -> channels[idx] = ch.copy(number = idx + 1) }
        val newSelected = channels.indices.filter { sel[it] }.map { it + 1 }.toSet()

        applyChannelReorder(eep, channels, newSelected)
    }

    /** Writes reordered channels back to the EEPROM, updates state, and refreshes the list. */
    private fun applyChannelReorder(
        eep: ByteArray,
        channels: MutableList<Channel>,
        newSelected: Set<Int>
    ) {
        for (ch in channels) EepromParser.writeChannel(eep, ch)
        eeprom = eep
        EepromHolder.eeprom = eep
        channelList = channels.toList()
        dragWorkList = channels.toMutableList()
        adapter.submitList(channelList)
        adapter.updateSelection(newSelected)
        // updateSelectionBar called via onSelectionChanged
    }

    /**
     * Called by [ItemTouchHelper] after the user drops a dragged card.
     * Commits the drag order in [dragWorkList] to the EEPROM, renumbering
     * channels to match their new positions. The dragged channel stays selected.
     */
    private fun applyDragReorder() {
        val eep = eeprom ?: return
        if (dragWorkList.isEmpty()) return

        // Capture which original numbers were selected before renumbering
        val oldSelected = adapter.selectedChannelNumbers
        val newSelected = mutableSetOf<Int>()

        // Renumber channels in their new drag order (1-based) and track selection
        dragWorkList.forEachIndexed { idx, ch ->
            if (ch.number in oldSelected) newSelected.add(idx + 1)
            dragWorkList[idx] = ch.copy(number = idx + 1)
        }

        // Write the renumbered channels to EEPROM
        for (ch in dragWorkList) EepromParser.writeChannel(eep, ch)
        eeprom = eep
        EepromHolder.eeprom = eep
        channelList = dragWorkList.toList()

        // Update the ListAdapter — DiffUtil detects position changes
        adapter.submitList(channelList)
        adapter.updateSelection(newSelected)
        // updateSelectionBar called via onSelectionChanged
    }

    /**
     * Prompts the user to confirm, then erases the data from each selected channel slot
     * (sets it to empty/unused). The slot numbers themselves are unchanged.
     */
    private fun clearSelectedChannels() {
        val eep = eeprom ?: return
        val selected = adapter.selectedChannelNumbers
        if (selected.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Clear Channels")
            .setMessage(
                "Clear ${selected.size} selected channel(s)?\n\n" +
                "This erases the stored data and marks the slot(s) as empty. " +
                "Slot numbers are not affected."
            )
            .setPositiveButton("Clear") { _, _ ->
                for (num in selected) {
                    EepromParser.writeChannel(eep, Channel(number = num, empty = true))
                }
                eeprom = eep
                EepromHolder.eeprom = eep
                channelList = EepromParser.parseAllChannels(eep)
                dragWorkList = channelList.toMutableList()
                adapter.submitList(channelList)
                adapter.exitSelectionMode()
                // updateSelectionBar(0) called via onSelectionChanged → hides bar
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

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
        channelList  = EepromParser.parseAllChannels(data)
        dragWorkList = channelList.toMutableList()
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
