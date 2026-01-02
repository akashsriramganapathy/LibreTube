package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.logger.FileLogger
import com.github.libretube.extensions.toastFromMainThread
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import android.widget.ScrollView
import android.widget.TextView
import android.view.ViewGroup.LayoutParams
import android.util.TypedValue

class LogViewerDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val logContent = FileLogger.getLogContent()
        val message = if (logContent.isNotEmpty()) logContent else "Log is empty"

        val scrollView = ScrollView(requireContext())
        val textView = TextView(requireContext())
        textView.text = message
        textView.setTextIsSelectable(true)
        
        // Add some padding
        val padding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            16f, 
            requireContext().resources.displayMetrics
        ).toInt()
        textView.setPadding(padding, padding / 2, padding, padding / 2)
        
        scrollView.addView(textView)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.view_logs)
            .setView(scrollView)
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
