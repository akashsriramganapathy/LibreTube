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

class WelcomeActivity : BaseActivity() {

    private val viewModel by viewModels<WelcomeViewModel> { WelcomeViewModel.Factory }

    private val restoreFilePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            viewModel.restoreAdvancedBackup(this, uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.instancesContainer.isGone = true
        binding.operationModeGroup.isGone = true
        binding.localModeInfoContainer.isVisible = true

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
