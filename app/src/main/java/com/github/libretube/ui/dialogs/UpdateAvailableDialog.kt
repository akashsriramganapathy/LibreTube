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
        // Use external files dir to match logger and make it accessible
        val outputFile = File(requireContext().getExternalFilesDir(null), "LibreTube-Update.apk")
        if (outputFile.exists()) {
            outputFile.delete()
        }
        val updateManager = UpdateManager(requireContext())

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                requireContext().toastFromMainDispatcher(R.string.downloading)
            }
            val downloadResult = updateManager.downloadApk(url, outputFile)
            com.github.libretube.logger.FileLogger.d("UpdateDialog", "Download result: $downloadResult")

            if (downloadResult) {
                withContext(Dispatchers.Main) {
                    val safeContext = context
                    if (safeContext == null) {
                         com.github.libretube.logger.FileLogger.e("UpdateDialog", "Context is null after download")
                         return@withContext
                    }
                    safeContext.toastFromMainDispatcher("Download complete. Preparing install...")
                }

                try {
                    val safeContext = context ?: return@launch
                    // Launch installation intent using FileProvider
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        safeContext,
                        "${safeContext.packageName}.provider",
                        outputFile
                    )
                    
                    com.github.libretube.logger.FileLogger.d("UpdateDialog", "Installing update from: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
                    com.github.libretube.logger.FileLogger.d("UpdateDialog", "Update URI: $uri")

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    withContext(Dispatchers.Main) {
                        try {
                           context?.toastFromMainDispatcher("Launching installer...")
                           startActivity(intent)
                        } catch (e: Exception) {
                            com.github.libretube.logger.FileLogger.e("UpdateDialog", "StartActivity failed", e)
                             context?.toastFromMainDispatcher("Launch failed: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    com.github.libretube.logger.FileLogger.e("UpdateDialog", "Installation setup failed", e)
                    withContext(Dispatchers.Main) {
                         context?.toastFromMainDispatcher("Error: ${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                   context?.toastFromMainDispatcher("Download failed (Logic provided false)")
                }
            }
        }
    }
}
