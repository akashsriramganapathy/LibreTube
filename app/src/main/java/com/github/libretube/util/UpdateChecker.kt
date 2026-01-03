package com.github.libretube.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.IntentData.appUpdateChangelog
import com.github.libretube.constants.IntentData.appUpdateURL
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.obj.update.UpdateInfo
import com.github.libretube.ui.dialogs.UpdateAvailableDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class UpdateChecker(private val context: Context) {
    suspend fun checkUpdate(isManualCheck: Boolean = false) {
        val currentVersionName = BuildConfig.VERSION_NAME


        // Check for updates based on the build type
        val isExperimental = BuildConfig.IS_EXPERIMENTAL
        
        // Extract run number regardless of prefix
        // Regex to extract run number (e.g. "Run 10", "Build-123", "Run10")
        val runPattern = Regex("(?:Run|Build)[\\s-]*(\\d+)", RegexOption.IGNORE_CASE)

        val currentRunNumber = runPattern.find(currentVersionName)?.groupValues?.get(1)?.toIntOrNull()
            ?: currentVersionName.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

        try {
            val response = if (isExperimental) {
                 RetrofitInstance.externalApi.getReleaseByTag("experimental")
            } else {
                 RetrofitInstance.externalApi.getLatestRelease()
            }
            
            // Remote name format: "Nightly Build 9", "Experimental Build 9", "Run 10"
            // We use the same regex to extract the run number safely.
            // If pattern not found, we fallback to 0 to avoid false positives with dates (20250101).
            // Exception: if the name is JUST digits, we accept it.
            val remoteRunNumber = runPattern.find(response.name)?.groupValues?.get(1)?.toIntOrNull()
                ?: if (response.name.all { it.isDigit() }) response.name.toIntOrNull() ?: 0 else 0

            Log.d(TAG(), "Checking update: Mode=${if(isExperimental) "Exp" else "Nightly"}, Local: $currentRunNumber, Remote: $remoteRunNumber")

            if (remoteRunNumber > currentRunNumber) {
                // Find the APK asset
                val apkAsset = response.assets.find { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    withContext(Dispatchers.Main) {
                        showUpdateAvailableDialog(response, apkAsset.browserDownloadUrl, remoteRunNumber.toString())
                    }
                    Log.i(TAG(), "Update found: ${response.name}, URL: ${apkAsset.browserDownloadUrl}")
                } else {
                    Log.w(TAG(), "Update found but no APK asset: ${response.name}")
                    if (isManualCheck) {
                         withContext(Dispatchers.Main) {
                             context.toastFromMainDispatcher("Update found but no APK available.")
                         }
                    }
                }
            } else if (isManualCheck) {
                context.toastFromMainDispatcher(R.string.app_uptodate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showUpdateAvailableDialog(response: UpdateInfo, downloadUrl: String, runNumber: String) {
        if (context is androidx.lifecycle.LifecycleOwner &&
            context.lifecycle.currentState != androidx.lifecycle.Lifecycle.State.RESUMED
        ) {
            return
        }

        val dialog = UpdateAvailableDialog()
        val args =
            Bundle().apply {
                putString(appUpdateChangelog, sanitizeChangelog(response.body))
                putString(appUpdateURL, downloadUrl)
                putString("update_name", response.name)
                putString("run_number", runNumber)
            }
        dialog.arguments = args
        val fragmentManager = (context as? FragmentActivity)?.supportFragmentManager
        fragmentManager?.let {
            if (!it.isStateSaved) {
                dialog.show(it, UpdateAvailableDialog::class.java.simpleName)
            }
        }
    }

    private fun sanitizeChangelog(changelog: String): String {
        return changelog.substringBeforeLast("**Full Changelog**")
            .replace(Regex("in https://github\\.com/\\S+"), "")
            .lines().joinToString("\n") { line ->
                if (line.startsWith("##")) line.uppercase(Locale.ROOT) + " :" else line
            }
            .replace("## ", "")
            .replace(">", "")
            .replace("*", "•")
            .lines()
            .joinToString("\n") { it.trim() }
    }
}
