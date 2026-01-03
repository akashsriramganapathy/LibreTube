package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.obj.BackupFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupDialog : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val backupOptions = listOf(
            BackupHelper.BackupOption.WatchHistory,
            BackupHelper.BackupOption.WatchPositions,
            BackupHelper.BackupOption.SearchHistory,
            BackupHelper.BackupOption.LocalSubscriptions,
            BackupHelper.BackupOption.CustomInstances,
            BackupHelper.BackupOption.PlaylistBookmarks,
            BackupHelper.BackupOption.LocalPlaylists,
            BackupHelper.BackupOption.SubscriptionGroups,
            BackupHelper.BackupOption.Preferences
        )

        val backupItems = backupOptions.map { context?.getString(it.nameRes)!! }.toTypedArray()

        val selected = BooleanArray(backupOptions.size) { true }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.backup)
            .setMultiChoiceItems(backupItems, selected) { _, index, newValue ->
                selected[index] = newValue
            }
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.backup, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    requireDialog().hide()

                    lifecycleScope.launch(Dispatchers.IO) {
                        val selectedOptions = backupOptions.filterIndexed { index, _ -> selected[index] }
                        val backupFile = BackupHelper.createBackup(selectedOptions)

                        val encodedBackupFile = Json.encodeToString(backupFile)
                        setFragmentResult(
                            BACKUP_DIALOG_REQUEST_KEY,
                            bundleOf(IntentData.backupFile to encodedBackupFile)
                        )

                        dialog?.dismiss()
                    }
                }
            }
    }

    companion object {
        const val BACKUP_DIALOG_REQUEST_KEY = "backup_dialog_request_key"
    }
}
