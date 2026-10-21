package com.epicmems.stride

import android.Manifest
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
import com.epicmems.stride.track.StepTrackingService
import com.epicmems.stride.ui.StrideRoot
import com.epicmems.stride.ui.StrideViewModel
import com.epicmems.stride.ui.StrideViewModelFactory
import com.epicmems.stride.ui.theme.StrideTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
            StepTrackingService.start(this)
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
                    if (!has(Manifest.permission.ACTIVITY_RECOGNITION)) {
                        missing += Manifest.permission.ACTIVITY_RECOGNITION
                    }
                    // Routes are recorded automatically now, so location is requested up front.
                    if (!has(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        missing += Manifest.permission.ACCESS_FINE_LOCATION
                        missing += Manifest.permission.ACCESS_COARSE_LOCATION
                    }
                    if (Build.VERSION.SDK_INT >= 33 && !has(Manifest.permission.POST_NOTIFICATIONS)) {
                        missing += Manifest.permission.POST_NOTIFICATIONS
                    }
                    if (missing.isEmpty()) {
                        StepTrackingService.start(this@MainActivity)
                    } else {
                        permissionLauncher.launch(missing.toTypedArray())
                    }
                }

                StrideRoot(
                    viewModel = vm,
                    onRequestTrackingPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACTIVITY_RECOGNITION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        }
    }

    private fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
