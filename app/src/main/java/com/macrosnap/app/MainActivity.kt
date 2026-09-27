package com.macrosnap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.macrosnap.app.ui.MacroSnapNavHost
import com.macrosnap.app.ui.theme.MacroSnapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacroSnapTheme {
                MacroSnapNavHost()
            }
        }
    }
}
