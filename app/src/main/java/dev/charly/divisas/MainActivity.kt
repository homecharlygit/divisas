package dev.charly.divisas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.charly.divisas.ui.ConverterScreen
import dev.charly.divisas.ui.ConverterViewModel
import dev.charly.divisas.ui.theme.DivisasTheme

class MainActivity : ComponentActivity() {

    private val vm: ConverterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DivisasTheme {
                ConverterScreen(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshIfStale()
    }
}
