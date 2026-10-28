package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.JarvisScreen
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisTheme
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory App Check initialization with launch intent before UI/Firebase
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            JarvisTheme {
                JarvisScreen(viewModel = viewModel)
            }
        }
    }
}
