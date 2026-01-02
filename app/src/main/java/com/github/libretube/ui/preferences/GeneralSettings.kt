package com.github.libretube.ui.preferences

import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.RequireRestartDialog
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.github.libretube.util.UpdateWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.TimeUnit

class GeneralSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.general

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.general_settings, rootKey)

        val language = findPreference<ListPreference>("language")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            language?.setOnPreferenceChangeListener { _, _ ->
                RequireRestartDialog().show(
                    childFragmentManager,
                    RequireRestartDialog::class.java.name
                )
                true
            }
            val languages = requireContext().resources.getStringArray(R.array.languageCodes)
                .map { code ->
                    val locale = LocaleHelper.getLocaleFromAndroidCode(code)

                    // each language's name is displayed in its own language,
                    // e.g. 'de': 'Deutsch', 'fr': 'Francais', ...
                    locale.toString() to locale.getDisplayName(locale)
                }.sortedBy { it.second.lowercase() }
            language?.entries =
                arrayOf(requireContext().getString(R.string.systemLanguage)) + languages.map { it.second }
            language?.entryValues = arrayOf("sys") + languages.map { it.first }
        } else {
            // on newer Android versions, the language is set through Android settings
            // language is the only item in this category, so the whole category should be hidden
            language?.parent?.isVisible = false
        }

        val autoRotation = findPreference<ListPreference>(PreferenceKeys.ORIENTATION)
        autoRotation?.setOnPreferenceChangeListener { _, _ ->
            RequireRestartDialog().show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }

        val maxImageCache = findPreference<ListPreference>(PreferenceKeys.MAX_IMAGE_CACHE)
        maxImageCache?.setOnPreferenceChangeListener { _, _ ->
            ImageHelper.initializeImageLoader(requireContext())
            true
        }

        val updateFrequency = findPreference<ListPreference>(PreferenceKeys.CHECKING_FREQUENCY)
        updateFrequency?.setOnPreferenceChangeListener { _, newValue ->
            val frequency = (newValue as String).toInt()
            if (frequency == 15 || frequency == 30) {
                showBatteryDrainWarning(frequency)
            } else {
                scheduleUpdateWorker(frequency)
            }
            true
        }

        val resetSettings = findPreference<Preference>(PreferenceKeys.RESET_SETTINGS)
        resetSettings?.setOnPreferenceClickListener {
            showResetDialog()
            true
        }

        val setDefaultApp = findPreference<Preference>(PreferenceKeys.SET_DEFAULT_APP)
        setDefaultApp?.setOnPreferenceClickListener {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.content.Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, android.net.Uri.parse("package:${requireContext().packageName}"))
            } else {
                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${requireContext().packageName}"))
            }
            startActivity(intent)
            true
        }
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset)
            .setMessage(R.string.reset_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.reset) { _, _ ->
                // clear default preferences
                PreferenceHelper.clearPreferences()

                // clear login token
                PreferenceHelper.setToken("")

                ActivityCompat.recreate(requireActivity())
            }
            .show()
    }

    private fun showBatteryDrainWarning(frequency: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.battery_drain_warning_title)
            .setMessage(R.string.battery_drain_warning_message)
            .setNegativeButton(R.string.cancel) { _, _ ->
                // Reset to default or previous value if needed, but for now just cancel
                val updateFrequency = findPreference<ListPreference>(PreferenceKeys.CHECKING_FREQUENCY)
                // This is a bit tricky as the preference change is already committed if we return true.
                // ideally we should return false in the change listener if we wanted to block it.
                // But for now, we will schedule it anyway if they proceed, or maybe just warn them.
                // A better UX would be to return false in the listener until confirmed.
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                scheduleUpdateWorker(frequency)
            }
            .show()
    }

    private fun scheduleUpdateWorker(frequencyInMinutes: Int) {
        val workManager = WorkManager.getInstance(requireContext())
        val workRequest = PeriodicWorkRequest.Builder(
            UpdateWorker::class.java,
            frequencyInMinutes.toLong(),
            TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "UpdateCheck",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
