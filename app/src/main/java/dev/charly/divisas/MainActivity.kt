package dev.charly.divisas

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import dev.charly.divisas.ui.AppRoot
import dev.charly.divisas.ui.ConverterViewModel

class MainActivity : ComponentActivity() {

    private val vm: ConverterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBars(vm.settings.dark)
        setContent {
            val dark = vm.settings.dark
            // Íconos de la barra de estado según el modo de la app, no el del sistema
            LaunchedEffect(dark) { applySystemBars(dark) }
            AppRoot(vm)
        }
    }

    private fun applySystemBars(dark: Boolean) {
        val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onResume() {
        super.onResume()
        vm.refreshIfStale()
    }
}
