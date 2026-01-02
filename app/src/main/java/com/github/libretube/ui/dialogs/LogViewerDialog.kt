package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.logger.FileLogger
import com.github.libretube.extensions.toastFromMainThread
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LogViewerDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val logContent = FileLogger.getLogContent()
        val message = if (logContent.isNotEmpty()) logContent else "Log is empty"

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.view_logs)
            .setMessage(message)
            .setPositiveButton(androidx.preference.R.string.copy) { _, _ ->
                ClipboardHelper.save(requireContext(), text = logContent, notify = true)
            }
            .setNeutralButton(R.string.clear_logs) { _, _ ->
                FileLogger.clearLog()
                requireContext().toastFromMainThread("Logs cleared")
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }
}
