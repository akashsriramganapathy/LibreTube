package com.github.libretube.ui.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.github.libretube.databinding.ActivityWelcomeBinding

import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.models.WelcomeViewModel
import com.github.libretube.ui.preferences.BackupRestoreSettings

import com.github.libretube.R

class WelcomeActivity : BaseActivity() {

    private val viewModel by viewModels<WelcomeViewModel> { WelcomeViewModel.Factory }

    private val restoreFilePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            viewModel.restoreAdvancedBackup(this, uri)
        }

    private val selectBackupLocation = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val path = uri.toString()
            com.github.libretube.helpers.PreferenceHelper.putString(com.github.libretube.constants.PreferenceKeys.AUTO_BACKUP_PATH, path)
            com.github.libretube.helpers.PreferenceHelper.putBoolean(com.github.libretube.constants.PreferenceKeys.AUTO_BACKUP_ENABLED, true)
            
            // Schedule worker immediately
            com.github.libretube.workers.AutoBackupWorker.enqueueWork(applicationContext)
            
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.auto_backup_permission_error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.localModeInfoContainer.isVisible = true

        binding.selectBackupLocation.setOnClickListener {
            selectBackupLocation.launch(null)
        }

        binding.setDefaultApp.setOnClickListener {
            val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, android.net.Uri.parse("package:$packageName"))
            } else {
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))
            }
            startActivity(intent)
        }

        binding.okay.setOnClickListener {
            viewModel.onConfirmSettings()
        }

        binding.restore.setOnClickListener {
            restoreFilePicker.launch(BackupRestoreSettings.JSON)
        }

        viewModel.uiState.observe(this) { (error, navigateToMain) ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }

            navigateToMain?.let {
                val mainActivityIntent = Intent(this, MainActivity::class.java)
                startActivity(mainActivityIntent)
                finish()
                viewModel.onNavigated()
            }
        }
    }

    override fun requestOrientationChange() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
    }
}
