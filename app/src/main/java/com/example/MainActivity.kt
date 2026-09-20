package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AgendaMainScreen
import com.example.ui.AgendaViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AgendaViewModel = viewModel()

            // Auto-start/stop FloatingCoachService if enabled/disabled and has permission
            androidx.compose.runtime.LaunchedEffect(viewModel.isFloatingCoachBubbleEnabled) {
                val intent = Intent(this@MainActivity, FloatingCoachService::class.java)
                if (viewModel.isFloatingCoachBubbleEnabled) {
                    if (Settings.canDrawOverlays(this@MainActivity)) {
                        try {
                            startService(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    try {
                        stopService(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            MyApplicationTheme(darkTheme = viewModel.isDarkTheme) {
                AgendaMainScreen(viewModel = viewModel)
            }
        }
    }
}
