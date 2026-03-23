package com.nicfw.tdh3editor

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nicfw.tdh3editor.BuildConfig
import com.nicfw.tdh3editor.databinding.ActivityRepeaterbookSearchBinding
import com.nicfw.tdh3editor.radio.ChirpCsvExporter
import com.nicfw.tdh3editor.radio.repeaterbook.RepeaterBookHttp
import com.nicfw.tdh3editor.radio.repeaterbook.RepeaterBookJsonParser
import com.nicfw.tdh3editor.radio.repeaterbook.RepeaterBookQuery
import com.nicfw.tdh3editor.radio.repeaterbook.RepeaterBookToChannelMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Search RepeaterBook via export.php / exportROW, multi-select results, then hand off to
 * [ChirpImportActivity] with generated CHIRP CSV (same path as file/clipboard import).
 */
class RepeaterBookSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRepeaterbookSearchBinding

    /** Full result set from last successful search. */
    private val allRows = mutableListOf<RepeaterRow>()

    /** Filtered view for the adapter. */
    private val visibleRows = mutableListOf<RepeaterRow>()

    private val adapter = RepeaterBookResultsAdapter(
        visibleRows,
        onToggle = { row, checked ->
            row.selected = checked
            updateImportButton()
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (EepromHolder.eeprom == null) {
            Toast.makeText(this, R.string.repeaterbook_need_eeprom, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        binding = ActivityRepeaterbookSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.textAttribution.text = HtmlCompat.fromHtml(
            getString(R.string.repeaterbook_attribution_html),
            HtmlCompat.FROM_HTML_MODE_LEGACY,
        )
        binding.textAttribution.movementMethod = LinkMovementMethod.getInstance()

        binding.recyclerResults.layoutManager = LinearLayoutManager(this)
        binding.recyclerResults.adapter = adapter

        binding.radioRegion.setOnCheckedChangeListener { _, _ -> updateRegionFields() }
        updateRegionFields()

        binding.btnSearch.setOnClickListener { runSearch() }
        binding.btnExit.setOnClickListener { finish() }
        binding.btnImport.setOnClickListener { importSelected() }

        binding.editFilter.doAfterTextChanged { applyFilter() }

        if (!hasRepeaterBookConfig()) {
            Toast.makeText(this, R.string.repeaterbook_configure_token, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_repeaterbook_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.repeaterbook_menu_select_all -> {
                visibleRows.forEach { it.selected = true }
                adapter.notifyDataSetChanged()
                updateImportButton()
                return true
            }
            R.id.repeaterbook_menu_clear -> {
                allRows.forEach { it.selected = false }
                adapter.notifyDataSetChanged()
                updateImportButton()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hasRepeaterBookConfig(): Boolean =
        BuildConfig.REPEATERBOOK_APP_TOKEN.isNotBlank() &&
            BuildConfig.REPEATERBOOK_CONTACT_EMAIL.isNotBlank()

    private fun updateRegionFields() {
        val na = binding.radioNa.isChecked
        binding.layoutStateId.isVisible = na
        binding.layoutCounty.isVisible = na
        binding.layoutRegionRow.isVisible = !na
    }

    private fun buildQuery(): RepeaterBookQuery {
        val na = binding.radioNa.isChecked
        return RepeaterBookQuery(
            northAmerica = na,
            country = binding.editCountry.text?.toString().orEmpty(),
            stateId = binding.editStateId.text?.toString().orEmpty(),
            region = binding.editRegionRow.text?.toString().orEmpty(),
            county = binding.editCounty.text?.toString().orEmpty(),
            city = binding.editCity.text?.toString().orEmpty(),
            landmark = binding.editLandmark.text?.toString().orEmpty(),
            callsign = binding.editCallsign.text?.toString().orEmpty(),
            frequency = binding.editFrequency.text?.toString().orEmpty(),
            mode = binding.editMode.text?.toString().orEmpty(),
            emcomm = binding.editEmcomm.text?.toString().orEmpty(),
            stype = binding.editStype.text?.toString().orEmpty(),
        )
    }

    private fun runSearch() {
        if (!hasRepeaterBookConfig()) {
            Toast.makeText(this, R.string.repeaterbook_configure_token, Toast.LENGTH_LONG).show()
            return
        }
        binding.btnSearch.isEnabled = false
        binding.progressSearch.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = RepeaterBookHttp.fetchRepeaters(buildQuery())
                    val objs = RepeaterBookJsonParser.parseResults(json)
                    objs.map { RepeaterRow(it, selected = false) }
                }
            }
            binding.progressSearch.visibility = View.GONE
            binding.btnSearch.isEnabled = true
            result.onSuccess { rows ->
                allRows.clear()
                allRows.addAll(rows)
                binding.layoutFilter.isVisible = rows.isNotEmpty()
                binding.editFilter.setText("")
                applyFilter()
                if (rows.isEmpty()) {
                    binding.textEmptyResults.isVisible = true
                    binding.recyclerResults.isVisible = false
                } else {
                    binding.textEmptyResults.isVisible = false
                    binding.recyclerResults.isVisible = true
                }
                updateImportButton()
            }.onFailure { e ->
                Toast.makeText(
                    this@RepeaterBookSearchActivity,
                    getString(R.string.repeaterbook_search_failed, e.message ?: e.toString()),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun applyFilter() {
        val q = binding.editFilter.text?.toString()?.trim()?.lowercase().orEmpty()
        visibleRows.clear()
        if (q.isEmpty()) {
            visibleRows.addAll(allRows)
        } else {
            allRows.filterTo(visibleRows) { row ->
                row.matchesFilter(q)
            }
        }
        adapter.notifyDataSetChanged()
        binding.textEmptyResults.isVisible = visibleRows.isEmpty() && allRows.isNotEmpty()
        binding.recyclerResults.isVisible = visibleRows.isNotEmpty()
    }

    private fun updateImportButton() {
        val n = allRows.count { it.selected }
        binding.btnImport.isEnabled = n > 0
        binding.btnImport.text = if (n > 0) {
            getString(R.string.repeaterbook_import_selected) + " ($n)"
        } else {
            getString(R.string.repeaterbook_import_selected)
        }
    }

    private fun importSelected() {
        val selected = allRows.filter { it.selected }
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.repeaterbook_select_one, Toast.LENGTH_SHORT).show()
            return
        }
        val pairs = selected.mapNotNull { row ->
            val ch = RepeaterBookToChannelMapper.fromJson(row.json) ?: return@mapNotNull null
            row to ch
        }
        if (pairs.isEmpty()) {
            Toast.makeText(this, R.string.repeaterbook_no_results, Toast.LENGTH_LONG).show()
            return
        }
        val channels = pairs.map { it.second }
        val comments = pairs.map { RepeaterBookToChannelMapper.commentLine(it.first.json) }
        val csv = ChirpCsvExporter.export(channels)
        startActivity(
            Intent(this, ChirpImportActivity::class.java).apply {
                putExtra(ChirpImportActivity.EXTRA_CSV_TEXT, csv)
                putExtra(ChirpImportActivity.EXTRA_COMMENTS, ArrayList(comments))
            },
        )
        finish()
    }

    data class RepeaterRow(val json: JSONObject, var selected: Boolean) {
        fun matchesFilter(q: String): Boolean {
            val call = json.optString("Callsign", "").lowercase()
            val city = json.optString("Nearest City", "").lowercase()
            val freq = json.optString("Frequency", "") + json.opt("Frequency")?.toString().orEmpty()
            val line = "$call $city $freq".lowercase()
            return line.contains(q)
        }
    }
}

private class RepeaterBookResultsAdapter(
    private val rows: List<RepeaterBookSearchActivity.RepeaterRow>,
    private val onToggle: (RepeaterBookSearchActivity.RepeaterRow, Boolean) -> Unit,
) : androidx.recyclerview.widget.RecyclerView.Adapter<RepeaterBookResultsAdapter.VH>() {

    class VH(val binding: com.nicfw.tdh3editor.databinding.ItemRepeaterbookResultBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val binding = com.nicfw.tdh3editor.databinding.ItemRepeaterbookResultBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        val j = row.json
        val call = j.optString("Callsign", "").ifBlank { "—" }
        val freq = j.opt("Frequency")?.toString()?.trim().orEmpty().ifBlank { j.optString("Frequency") }
        val title = "$call  $freq MHz".trim()
        val pl = j.optString("PL", "")
        val tsq = j.optString("TSQ", "")
        val tone = when {
            pl.isNotBlank() && tsq.isNotBlank() -> "PL $pl  TSQ $tsq"
            pl.isNotBlank() -> "PL $pl"
            tsq.isNotBlank() -> "TSQ $tsq"
            else -> ""
        }
        val sub = buildString {
            append(RepeaterBookToChannelMapper.commentLine(j))
            if (tone.isNotEmpty()) {
                if (isNotEmpty()) append(" · ")
                append(tone)
            }
        }
        holder.binding.textTitle.text = title
        holder.binding.textSubtitle.text = sub.ifBlank { "—" }
        holder.binding.checkSelected.setOnCheckedChangeListener(null)
        holder.binding.checkSelected.isChecked = row.selected
        holder.binding.checkSelected.setOnCheckedChangeListener { _, checked ->
            onToggle(row, checked)
        }
        holder.itemView.setOnClickListener {
            holder.binding.checkSelected.isChecked = !holder.binding.checkSelected.isChecked
        }
    }
}
