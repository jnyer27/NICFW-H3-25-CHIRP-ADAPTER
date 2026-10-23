package com.nicfw.tdh3editor

import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.textfield.TextInputEditText
import com.nicfw.tdh3editor.radio.repeaterbook.RepeaterBookApiException
import com.nicfw.tdh3editor.radio.repeaterbook.RepeaterBookCredentialStore
import com.nicfw.tdh3editor.radio.repeaterbook.RepeaterBookTokenPolicy

object RepeaterBookTokenDialog {

    fun show(activity: AppCompatActivity, onChanged: () -> Unit = {}) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_repeaterbook_token, null)
        val status = view.findViewById<TextView>(R.id.textRepeaterBookTokenStatus)
        val help = view.findViewById<TextView>(R.id.textRepeaterBookTokenHelp)
        val input = view.findViewById<TextInputEditText>(R.id.editRepeaterBookToken)
        status.setText(
            if (RepeaterBookCredentialStore.hasToken()) {
                R.string.repeaterbook_token_status_saved
            } else {
                R.string.repeaterbook_token_status_empty
            },
        )
        help.text = HtmlCompat.fromHtml(
            activity.getString(R.string.repeaterbook_token_help_html),
            HtmlCompat.FROM_HTML_MODE_LEGACY,
        )
        help.movementMethod = LinkMovementMethod.getInstance()

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.repeaterbook_token_title)
            .setView(view)
            .setPositiveButton(R.string.repeaterbook_token_save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.repeaterbook_token_clear) { _, _ ->
                RepeaterBookCredentialStore.clear(activity)
                Toast.makeText(activity, R.string.repeaterbook_token_cleared, Toast.LENGTH_LONG).show()
                onChanged()
            }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                when (val decision = RepeaterBookCredentialStore.save(activity, input.text?.toString().orEmpty())) {
                    is RepeaterBookTokenPolicy.Decision.Accepted -> {
                        Toast.makeText(activity, R.string.repeaterbook_token_saved, Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        onChanged()
                    }
                    is RepeaterBookTokenPolicy.Decision.Rejected -> {
                        Toast.makeText(activity, messageFor(decision.reason), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    fun showSearchFailure(activity: AppCompatActivity, error: Throwable) {
        val api = error as? RepeaterBookApiException
        if (api != null && api.statusCode == 429) {
            AlertDialog.Builder(activity)
                .setTitle(R.string.repeaterbook_error_429_title)
                .setMessage(RepeaterBookTokenPolicy.redact(api.message ?: ""))
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        if (api != null && (api.terminal || api.statusCode == 401 || api.statusCode == 403)) {
            val detail = buildString {
                append(activity.getString(R.string.repeaterbook_search_failed, api.message ?: ""))
                val body = RepeaterBookTokenPolicy.redact(api.errorBody?.trim().orEmpty())
                if (body.isNotEmpty()) {
                    append("\n\n")
                    append(body.take(400))
                }
                append("\n\n")
                append(activity.getString(R.string.repeaterbook_error_401_hint))
            }
            AlertDialog.Builder(activity)
                .setTitle(R.string.repeaterbook_error_401_title)
                .setMessage(detail)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        Toast.makeText(
            activity,
            activity.getString(
                R.string.repeaterbook_search_failed,
                RepeaterBookTokenPolicy.redact(error.message ?: error.toString()),
            ),
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun messageFor(reason: RepeaterBookTokenPolicy.Reason): Int = when (reason) {
        RepeaterBookTokenPolicy.Reason.BLANK -> R.string.repeaterbook_token_rejected_blank
        RepeaterBookTokenPolicy.Reason.SHARED_APP_TOKEN -> R.string.repeaterbook_token_rejected_shared
        RepeaterBookTokenPolicy.Reason.URL_OR_QUERY -> R.string.repeaterbook_token_rejected_url
        RepeaterBookTokenPolicy.Reason.MALFORMED -> R.string.repeaterbook_token_rejected_malformed
        RepeaterBookTokenPolicy.Reason.STORAGE -> R.string.repeaterbook_token_rejected_storage
    }
}
