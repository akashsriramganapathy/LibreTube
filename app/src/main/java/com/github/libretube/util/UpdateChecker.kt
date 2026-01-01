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
        var currentRunNumber = 0

        if (currentVersionName.startsWith("Nightly-Run")) {
            currentRunNumber = currentVersionName.substringAfter("Nightly-Run").toIntOrNull() ?: 0
        } else {
             // Fallback for stable versions or if format changes, but since valid check is digits only which is bad
             // We assume running locally (stable) we always want to check against nightly if available
             // But user said current version is v0.29.01. "291".
             // Let's assume stable versions are "older" than nightlies for this specific use case, or just checking numbers.
             // If manual check, user expects update.
        }

        try {
            val response = RetrofitInstance.externalApi.getLatestRelease()
            // Remote name format: "Nightly Build 9"
            val remoteRunNumber = response.name.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

            Log.d(TAG(), "Checking update: Local Run: $currentRunNumber, Remote Run: $remoteRunNumber")

            if (remoteRunNumber > currentRunNumber) {
                withContext(Dispatchers.Main) {
                    showUpdateAvailableDialog(response)
                }
                Log.i(TAG(), "Update found: $response")
            } else if (isManualCheck) {
                context.toastFromMainDispatcher(R.string.app_uptodate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showUpdateAvailableDialog(response: UpdateInfo) {
        if (context is androidx.lifecycle.LifecycleOwner &&
            context.lifecycle.currentState != androidx.lifecycle.Lifecycle.State.RESUMED
        ) {
            return
        }

        val dialog = UpdateAvailableDialog()
        val args =
            Bundle().apply {
                putString(appUpdateChangelog, sanitizeChangelog(response.body))
                putString(appUpdateURL, response.htmlUrl)
                putString("update_name", response.name)
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
