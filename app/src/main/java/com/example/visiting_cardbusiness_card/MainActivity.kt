package com.example.visiting_cardbusiness_card

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visiting_cardbusiness_card.ui.screens.CameraScreen
import com.example.visiting_cardbusiness_card.ui.screens.HomeScreen
import com.example.visiting_cardbusiness_card.ui.screens.ResultsScreen
import com.example.visiting_cardbusiness_card.ui.theme.Visiting_cardBusiness_CardTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import androidx.compose.material3.Text
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Visiting_cardBusiness_CardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = mainViewModel,
                onScanClick = {
                    if (cameraPermissionState.status.isGranted) {
                        navController.navigate("camera")
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                onCardClick = { card ->
                    mainViewModel.currentImageUri.value = card.imageUri
                    mainViewModel.currentResults.value = card.results
                    navController.navigate("results")
                }
            )
        }
        composable("camera") {
            CameraScreen(onImageCaptured = { uri ->
                mainViewModel.processImage(navController.context, uri)
                navController.navigate("results") {
                    popUpTo("home")
                }
            })
        }
        composable("results") {
            ResultsScreen(
                viewModel = mainViewModel,
                onBack = { navController.popBackStack("home", false) }
            )
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && navController.currentDestination?.route == "camera_permission") {
            navController.navigate("camera")
        }
    }
}
