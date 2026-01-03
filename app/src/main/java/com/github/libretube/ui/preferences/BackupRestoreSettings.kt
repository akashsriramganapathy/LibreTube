package com.github.libretube.ui.preferences

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.DialogImportExportFormatChooserBinding
import com.github.libretube.enums.ImportFormat
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.helpers.AutoBackupHelper
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.helpers.ImportHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.BackupFile
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.BackupDialog
import com.github.libretube.ui.dialogs.BackupDialog.Companion.BACKUP_DIALOG_REQUEST_KEY
import com.github.libretube.ui.dialogs.RequireRestartDialog
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BackupRestoreSettings : BasePreferenceFragment() {
    private var backupFile = BackupFile()
    private var importFormat: ImportFormat = ImportFormat.NEWPIPE

    override val titleResourceId: Int = R.string.backup_restore

    // backup and restore database
    private val getBackupFile =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            CoroutineScope(Dispatchers.IO).launch {
                BackupHelper.restoreAdvancedBackup(requireContext().applicationContext, uri)
                withContext(Dispatchers.Main) {
                    // could fail if fragment is already closed
                    runCatching {
                        RequireRestartDialog().show(childFragmentManager, this::class.java.name)
                    }
                }
            }
        }
    private val createBackupFile = registerForActivityResult(CreateDocument(FILETYPE_ANY)) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.IO) {
            BackupHelper.createAdvancedBackup(requireContext().applicationContext, uri, backupFile)
        }
    }

    /**
     * result listeners for importing and exporting subscriptions
     */
    private val getSubscriptionsFile = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        CoroutineScope(Dispatchers.IO).launch {
            ImportHelper.importSubscriptions(requireContext().applicationContext, uri, importFormat)
        }
    }

    private val createSubscriptionsFile =
        registerForActivityResult(CreateDocument(FILETYPE_ANY)) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch(Dispatchers.IO) {
                ImportHelper.exportSubscriptions(
                    requireContext().applicationContext,
                    uri,
                    importFormat
                )
            }
        }


    // result listeners for importing and exporting playlists
    private val getPlaylistsFile =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { files ->
            for (file in files) {
                CoroutineScope(Dispatchers.IO).launch {
                    ImportHelper.importPlaylists(
                        requireContext().applicationContext,
                        file,
                        importFormat
                    )
                }
            }
        }

    private val getWatchHistoryFile =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { files ->
            for (file in files) {
                CoroutineScope(Dispatchers.IO).launch {
                    ImportHelper.importWatchHistory(
                        requireContext().applicationContext,
                        file,
                        importFormat
                    )
                }
            }
        }

    private val createPlaylistsFile =
        registerForActivityResult(CreateDocument(FILETYPE_ANY)) { uri ->
            uri?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    ImportHelper.exportPlaylists(
                        requireContext().applicationContext,
                        uri,
                        importFormat
                    )
                }
            }
        }

    private val openDocumentTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        
        // Persist permissions
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        PreferenceHelper.putString(PreferenceKeys.AUTO_BACKUP_PATH, uri.toString())
        updateBackupPathSummary(uri.toString())
        
        // If enabled, reschedule to ensure everything is correct
        AutoBackupHelper.scheduleBackup(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.import_export_settings, rootKey)

        // ... existing listeners ...

        val importSubscriptions = findPreference<Preference>("import_subscriptions")
        importSubscriptions?.setOnPreferenceClickListener {
            createImportFormatDialog(
                requireContext(),
                R.string.import_subscriptions_from,
                importSubscriptionFormatList
            ) { format, _ ->
                importFormat = format
                getSubscriptionsFile.launch("*/*")
            }
            true
        }

        val exportSubscriptions = findPreference<Preference>("export_subscriptions")
        exportSubscriptions?.setOnPreferenceClickListener {
            createImportFormatDialog(
                requireContext(),
                R.string.export_subscriptions_to,
                exportSubscriptionFormatList,
                isExport = true
            ) { format, includeTimestamp ->
                importFormat = format
                createSubscriptionsFile.launch(
                    getExportFileName(requireContext(), format, "subscriptions", includeTimestamp)
                )
            }
            true
        }

        val importPlaylists = findPreference<Preference>("import_playlists")
        importPlaylists?.setOnPreferenceClickListener {
            createImportFormatDialog(
                requireContext(),
                R.string.import_playlists_from,
                importPlaylistFormatList
            ) { format, _ ->
                importFormat = format
                getPlaylistsFile.launch(arrayOf("*/*"))
            }
            true
        }

        val exportPlaylists = findPreference<Preference>("export_playlists")
        exportPlaylists?.setOnPreferenceClickListener {
            createImportFormatDialog(
                requireContext(),
                R.string.export_playlists_to,
                exportPlaylistFormatList,
                isExport = true
            ) { format, includeTimestamp ->
                importFormat = format
                createPlaylistsFile.launch(
                    getExportFileName(requireContext(), format, "playlists", includeTimestamp)
                )
            }
            true
        }

        val importWatchHistory = findPreference<Preference>("import_watch_history")
        importWatchHistory?.setOnPreferenceClickListener {
            createImportFormatDialog(
                requireContext(),
                R.string.import_watch_history,
                importWatchHistoryFormatList
            ) { format, _ ->
                importFormat = format
                getWatchHistoryFile.launch(arrayOf("*/*"))
            }
            true
        }

        childFragmentManager.setFragmentResultListener(
            BACKUP_DIALOG_REQUEST_KEY,
            this
        ) { _, resultBundle ->
            val encodedBackupFile = resultBundle.getString(IntentData.backupFile)!!
            backupFile = Json.decodeFromString(encodedBackupFile)
            val timestamp = TextUtils.getFileSafeTimeStampNow()
            createBackupFile.launch("libretube-backup-${timestamp}.json")
        }
        val advancedBackup = findPreference<Preference>("backup")
        advancedBackup?.setOnPreferenceClickListener {
            BackupDialog().show(childFragmentManager, null)
            true
        }

        val restoreAdvancedBackup = findPreference<Preference>("restore")
        restoreAdvancedBackup?.setOnPreferenceClickListener {
            getBackupFile.launch(JSON)
            true
        }

        // Auto Backup
        val autoBackupEnabled = findPreference<androidx.preference.SwitchPreferenceCompat>(PreferenceKeys.AUTO_BACKUP_ENABLED)
        autoBackupEnabled?.setOnPreferenceChangeListener { _, newValue ->
            val isChecked = newValue as Boolean
            if (isChecked) {
                val path = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_PATH, "")
                if (path.isEmpty()) {
                    requireContext().toastFromMainThread(R.string.auto_backup_no_path_selected)
                }
            }
            PreferenceHelper.putBoolean(PreferenceKeys.AUTO_BACKUP_ENABLED, isChecked)
            AutoBackupHelper.scheduleBackup(requireContext())
            true
        }

        val autoBackupPath = findPreference<Preference>(PreferenceKeys.AUTO_BACKUP_PATH)
        autoBackupPath?.setOnPreferenceClickListener {
            openDocumentTree.launch(null)
            true
        }
        updateBackupPathSummary(PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_PATH, ""))

        val autoBackupInterval = findPreference<androidx.preference.ListPreference>(PreferenceKeys.AUTO_BACKUP_INTERVAL)
        autoBackupInterval?.setOnPreferenceChangeListener { _, _ ->
            lifecycleScope.launch(Dispatchers.Main) { 
                 AutoBackupHelper.scheduleBackup(requireContext())
            }
            true
        }

        val autoBackupTime = findPreference<Preference>(PreferenceKeys.AUTO_BACKUP_TIME)
        autoBackupTime?.setOnPreferenceClickListener {
            val now = java.util.Calendar.getInstance()
            val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(java.util.Calendar.MINUTE)
            val defaultTime = String.format("%02d:%02d", currentHour, currentMinute)

            val currentTime = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_TIME, defaultTime)
            val parts = currentTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: currentHour
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: currentMinute

            android.app.TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                PreferenceHelper.putString(PreferenceKeys.AUTO_BACKUP_TIME, newTime)
                updateBackupTimeSummary(newTime)
                AutoBackupHelper.scheduleBackup(requireContext())
            }, hour, minute, android.text.format.DateFormat.is24HourFormat(requireContext())).show()
            true
        }
        
        val initialNow = java.util.Calendar.getInstance()
        val initialDefaultTime = String.format("%02d:%02d", initialNow.get(java.util.Calendar.HOUR_OF_DAY), initialNow.get(java.util.Calendar.MINUTE))
        updateBackupTimeSummary(PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_TIME, initialDefaultTime))

        val autoBackupMaxKeep = findPreference<androidx.preference.ListPreference>(PreferenceKeys.AUTO_BACKUP_MAX_KEEP)
        autoBackupMaxKeep?.setOnPreferenceChangeListener { _, newValue ->
            autoBackupMaxKeep.summary = getString(R.string.auto_backup_max_keep_summary, newValue as String)
            true
        }
        autoBackupMaxKeep?.summary = getString(R.string.auto_backup_max_keep_summary, 
            PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_MAX_KEEP, "25"))
        
        val backupNow = findPreference<Preference>("auto_backup_backup_now")
        backupNow?.setOnPreferenceClickListener {
            androidx.work.WorkManager.getInstance(requireContext())
                .enqueue(androidx.work.OneTimeWorkRequest.from(com.github.libretube.workers.AutoBackupWorker::class.java))
            requireContext().toastFromMainThread(R.string.backup_creation_success) // Just a feedback that it started?
            true
        }
    }

    private fun updateBackupTimeSummary(time: String) {
        val autoBackupTime = findPreference<Preference>(PreferenceKeys.AUTO_BACKUP_TIME)
        autoBackupTime?.summary = getString(R.string.auto_backup_time_summary, time)
    }

    private fun updateBackupPathSummary(path: String?) {
        val autoBackupPath = findPreference<Preference>(PreferenceKeys.AUTO_BACKUP_PATH)
        if (!path.isNullOrBlank()) {
            try {
                val uri = android.net.Uri.parse(path)
                // Try to make it readable
                autoBackupPath?.summary = androidx.documentfile.provider.DocumentFile.fromTreeUri(requireContext(), uri)?.name ?: path
            } catch (e: Exception) {
                autoBackupPath?.summary = path
            }
        } else {
            autoBackupPath?.summary = getString(R.string.auto_backup_no_path_selected)
        }
    }

    companion object {
        const val JSON = "application/json"
    // ... rest of companion object ...

        /**
         * Mimetype to use to create new files when setting extension manually
         */
        const val FILETYPE_ANY = "application/octet-stream"

        val importSubscriptionFormatList = listOf(
            ImportFormat.NEWPIPE,
            ImportFormat.FREETUBE,
            ImportFormat.YOUTUBECSV
        )
        val exportSubscriptionFormatList = listOf(
            ImportFormat.NEWPIPE,
            ImportFormat.FREETUBE
        )
        val importPlaylistFormatList = listOf(
            ImportFormat.PIPED,
            ImportFormat.FREETUBE,
            ImportFormat.YOUTUBECSV,
            ImportFormat.URLSORIDS
        )
        val exportPlaylistFormatList = listOf(
            ImportFormat.PIPED,
            ImportFormat.FREETUBE
        )
        val importWatchHistoryFormatList = listOf(ImportFormat.YOUTUBEJSON)

        fun createImportFormatDialog(
            context: Context,
            @StringRes titleStringId: Int,
            formats: List<ImportFormat>,
            isExport: Boolean = false,
            onConfirm: (ImportFormat, Boolean) -> Unit
        ) {
            var selectedIndex = 0

            val dialog = MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(titleStringId))
                .setSingleChoiceItems(
                    formats.map { context.getString(it.value) }.toTypedArray(),
                    selectedIndex
                ) { _, i ->
                    selectedIndex = i
                }

            val layoutInflater = LayoutInflater.from(context)
            val binding = DialogImportExportFormatChooserBinding.inflate(layoutInflater)
            binding.includeTimestamp.isChecked = PreferenceHelper.getBoolean(
                PreferenceKeys.INCLUDE_TIMESTAMP_IN_BACKUP_FILENAME,
                false
            )
            if (isExport) {
                dialog.setView(binding.root)
            }

            dialog.setPositiveButton(R.string.okay) { _, _ ->
                if (isExport) PreferenceHelper.putBoolean(
                    PreferenceKeys.INCLUDE_TIMESTAMP_IN_BACKUP_FILENAME,
                    binding.includeTimestamp.isChecked
                )

                onConfirm(formats[selectedIndex], binding.includeTimestamp.isChecked)
            }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        fun getExportFileName(
            context: Context,
            format: ImportFormat,
            type: String,
            includeTimestamp: Boolean
        ): String {
            var baseString = context.getString(format.value).lowercase()
            baseString += "-${type}"

            if (includeTimestamp) {
                baseString += "-${TextUtils.getFileSafeTimeStampNow()}"
            }

            return "${baseString}.${format.fileExtension}"
        }
    }
}
