package com.epicmems.stride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epicmems.stride.step.StepCounterService
import com.epicmems.stride.ui.StrideRoot
import com.epicmems.stride.ui.StrideViewModel
import com.epicmems.stride.ui.StrideViewModelFactory
import com.epicmems.stride.ui.theme.StrideTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
            startTracking()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrideTheme {
                val container = (application as StrideApp).container
                val vm: StrideViewModel = viewModel(factory = StrideViewModelFactory(container))

                LaunchedEffect(Unit) {
                    val missing = mutableListOf<String>()
                    if (!hasActivityPermission()) {
                        missing += Manifest.permission.ACTIVITY_RECOGNITION
                    }
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        missing += Manifest.permission.POST_NOTIFICATIONS
                    }
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    } else {
                        startTracking()
                    }
                }

                StrideRoot(
                    viewModel = vm,
                    onRequestPermission = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
                    }
                )
            }
        }
    }

    private fun hasActivityPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

    private fun startTracking() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, StepCounterService::class.java)
        )
    }
}
