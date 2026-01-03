package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.NavBarOptionsDialog
import com.github.libretube.ui.dialogs.RequireRestartDialog

class AppearanceSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.appearance
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.appearance_settings, rootKey)



        val labelVisibilityMode = findPreference<ListPreference>(PreferenceKeys.LABEL_VISIBILITY)
        labelVisibilityMode?.setOnPreferenceChangeListener { _, _ ->
            RequireRestartDialog().show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }

        val navBarOptions = findPreference<Preference>(PreferenceKeys.NAVBAR_ITEMS)
        navBarOptions?.setOnPreferenceClickListener {
            NavBarOptionsDialog().show(childFragmentManager, null)
            true
        }
    }


}
