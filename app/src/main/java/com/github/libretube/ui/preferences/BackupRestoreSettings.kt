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
import com.github.libretube.extensions.toastFromMainThread

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.import_export_settings, rootKey)

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

        setupAutoBackupPreferences()
    }

    private val selectAutoBackupLocation = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult

        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val path = uri.toString()
            PreferenceHelper.putString(PreferenceKeys.AUTO_BACKUP_PATH, path)
            findPreference<Preference>(PreferenceKeys.AUTO_BACKUP_PATH)?.summary = path
        } catch (e: Exception) {
            requireContext().toastFromMainThread(R.string.auto_backup_permission_error)
        }
    }

    private fun setupAutoBackupPreferences() {
        val enableAutoBackup = findPreference<androidx.preference.SwitchPreferenceCompat>(PreferenceKeys.AUTO_BACKUP_ENABLED)
        val backupLocation = findPreference<Preference>(PreferenceKeys.AUTO_BACKUP_PATH)
        val backupTime = findPreference<Preference>(PreferenceKeys.AUTO_BACKUP_TIME)
        val maxFiles = findPreference<androidx.preference.EditTextPreference>(PreferenceKeys.AUTO_BACKUP_MAX_FILES)

        // Set initial state
        val savedPath = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_PATH, "")
        if (savedPath.isNotEmpty()) {
            backupLocation?.summary = savedPath
        }

        val savedTime = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_TIME, "02:00") // Default to 2 AM
        backupTime?.summary = savedTime

        // Location click listener
        backupLocation?.setOnPreferenceClickListener {
            selectAutoBackupLocation.launch(null)
            true
        }
        
        // Time click listener
        backupTime?.setOnPreferenceClickListener {
            val currentTime = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_TIME, "02:00").split(":")
            val hour = currentTime[0].toInt()
            val minute = currentTime[1].toInt()

            android.app.TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                PreferenceHelper.putString(PreferenceKeys.AUTO_BACKUP_TIME, formattedTime)
                backupTime.summary = formattedTime
                
                // Reschedule worker if enabled
                if (PreferenceHelper.getBoolean(PreferenceKeys.AUTO_BACKUP_ENABLED, false)) {
                     com.github.libretube.workers.AutoBackupWorker.enqueueWork(requireContext().applicationContext)
                }
            }, hour, minute, true).show() // true for 24h view
            true
        }

        // Enable toggle listener (check if path is set)
        enableAutoBackup?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                val path = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_PATH, "")
                if (path.isEmpty()) {
                    requireContext().toastFromMainThread(R.string.auto_backup_permission_error)
                    // Launch picker if enabling without path, or just toast.
                    selectAutoBackupLocation.launch(null)
                    false // Don't enable yet
                } else {
                    // Reschedule immediately when enabling
                    com.github.libretube.workers.AutoBackupWorker.enqueueWork(requireContext().applicationContext)
                    true
                }
            } else {
                // Determine if we should cancel work? 
                // PeriodicWork "UPDATE" policy with existing work usually keeps it running but we check "enabled" flag inside doWork too.
                // Re-enqueuing isn't strictly necessary on disable if doWork checks the flag, but good practice if we wanted to cancel.
                // For now, doWork checking the flag is sufficient.
                true
            }
        }
        
        maxFiles?.setOnPreferenceChangeListener { _, newValue ->
             runCatching {
                 (newValue as String).toInt()
                 true
             }.getOrElse { false }
        }
    }

    companion object {
        const val JSON = "application/json"

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
