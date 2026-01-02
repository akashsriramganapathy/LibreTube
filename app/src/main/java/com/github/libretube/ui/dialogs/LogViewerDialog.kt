package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
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

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_log_viewer, null)
        
        val logTextView = view.findViewById<TextView>(R.id.logTextView)
        logTextView.text = message
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.view_logs)
            .setView(view)
            .show()

        view.findViewById<Button>(R.id.btnCopy).setOnClickListener {
            ClipboardHelper.save(requireContext(), text = logContent, notify = true)
        }

        view.findViewById<Button>(R.id.btnClear).setOnClickListener {
            FileLogger.clearLog()
            requireContext().toastFromMainThread("Logs cleared")
            logTextView.text = "Log is empty"
        }

        view.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        
        return dialog
    }
}
