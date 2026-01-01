package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.constants.IntentData.appUpdateChangelog
import com.github.libretube.constants.IntentData.appUpdateURL
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.util.UpdateManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UpdateAvailableDialog : DialogFragment() {
    private var changelog: String? = null
    private var releaseUrl: String? = null
    private var updateName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            changelog = getString(appUpdateChangelog)
            releaseUrl = getString(appUpdateURL)
            updateName = getString("update_name")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.update_available)
            .setMessage(changelog)
            .setPositiveButton(R.string.update) { _, _ ->
                installUpdate()
            }
            .setNeutralButton(R.string.download) { _, _ ->
                releaseUrl?.let {
                    startActivity(Intent(Intent.ACTION_VIEW, it.toUri()))
                }
            }
            .setNegativeButton(R.string.tooltip_dismiss, null)
            .setCancelable(false)
            .show()
    }

    private fun installUpdate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!requireContext().packageManager.canRequestPackageInstalls()) {
                android.widget.Toast.makeText(requireContext(), R.string.toast_install_permission_required, android.widget.Toast.LENGTH_LONG).show()
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = ("package:" + requireContext().packageName).toUri()
                })
                return
            }
        }

        val url = "https://github.com/akashsriramganapathy/LibreTube/releases/download/nightly/LibreTube-Nightly.apk"
        val outputFile = File(requireContext().cacheDir, "update.apk")
        val updateManager = UpdateManager(requireContext())

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                requireContext().toastFromMainDispatcher(R.string.downloading)
            }
            if (updateManager.downloadApk(url, outputFile)) {
                withContext(Dispatchers.Main) {
                    updateManager.installApk(outputFile)
                }
            } else {
                withContext(Dispatchers.Main) {
                    requireContext().toastFromMainDispatcher(R.string.downloadfailed)
                }
            }
        }
    }
}
