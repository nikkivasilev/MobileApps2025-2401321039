package com.macrosnap.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.macrosnap.app.ui.MacroSnapNavHost
import com.macrosnap.app.ui.theme.MacroSnapTheme

// AppCompatActivity, not ComponentActivity: it applies the in-app language on Android 12 and below.
class MainActivity : AppCompatActivity() {

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
