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
        var isExperimental = false

        if (currentVersionName.startsWith("Nightly-Run")) {
            currentRunNumber = currentVersionName.substringAfter("Nightly-Run").toIntOrNull() ?: 0
        } else if (currentVersionName.startsWith("Experimental-Run")) {
            currentRunNumber = currentVersionName.substringAfter("Experimental-Run").toIntOrNull() ?: 0
            isExperimental = true
        }

        try {
            val response = if (isExperimental) {
                 RetrofitInstance.externalApi.getReleaseByTag("experimental")
            } else {
                 RetrofitInstance.externalApi.getLatestRelease()
            }
            
            // Remote name format: "Nightly Build 9" or "Experimental Build 9"
            val remoteRunNumber = response.name.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

            Log.d(TAG(), "Checking update: Mode=${if(isExperimental) "Exp" else "Nightly"}, Local: $currentRunNumber, Remote: $remoteRunNumber")

            if (remoteRunNumber >= currentRunNumber) {
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
