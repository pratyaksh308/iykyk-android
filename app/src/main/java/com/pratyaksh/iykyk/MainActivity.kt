package com.pratyaksh.iykyk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.pratyaksh.iykyk.ui.HomeScreen
import com.pratyaksh.iykyk.ui.theme.IykykTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            IykykTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { contentPadding ->
                    HomeScreen(contentPadding)
                }
            }
        }
    }
}