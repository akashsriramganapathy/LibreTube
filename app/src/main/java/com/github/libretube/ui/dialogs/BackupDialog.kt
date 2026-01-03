package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BackupDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val options = arrayOf(
            getString(R.string.backup),
            getString(R.string.restore)
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.backup_restore)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Backup
                        // Start the file picker to create a backup file
                        setFragmentResult(
                            BACKUP_DIALOG_REQUEST_KEY,
                            bundleOf(IntentData.backupCreate to true)
                        )
                    }
                    1 -> { // Restore
                         // Start the file picker to open a backup file
                        setFragmentResult(
                            BACKUP_DIALOG_REQUEST_KEY,
                            bundleOf(IntentData.backupRestore to true)
                        )
                    }
                }
                dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    companion object {
        const val BACKUP_DIALOG_REQUEST_KEY = "backup_dialog_request_key"
    }
}
