package dev.charly.divisas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import dev.charly.divisas.ui.i18n.LocalAppLocale
import dev.charly.divisas.ui.i18n.LocalStrings
import dev.charly.divisas.ui.i18n.stringsFor
import dev.charly.divisas.ui.theme.DivisasTheme

enum class Screen { HOME, SETTINGS, ABOUT }

@Composable
fun AppRoot(vm: ConverterViewModel) {
    val settings = vm.settings
    CompositionLocalProvider(
        LocalStrings provides stringsFor(settings.language),
        LocalAppLocale provides settings.language.locale,
        LocalBlurType provides settings.blur,
    ) {
        DivisasTheme(palette = settings.palette, dark = settings.dark) {
            var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
            BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val slide = spring<IntOffset>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                        val scale = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
                        if (targetState.ordinal > initialState.ordinal) {
                            // Hacia adelante: la nueva pantalla entra desde la derecha creciendo
                            (slideInHorizontally(slide) { it / 2 } + scaleIn(scale, initialScale = 0.85f) + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(slide) { -it / 5 } + scaleOut(targetScale = 0.92f) + fadeOut(tween(180)))
                        } else {
                            // Regreso: la pantalla actual se va a la derecha encogiéndose
                            (slideInHorizontally(slide) { -it / 5 } + scaleIn(scale, initialScale = 0.92f) + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(slide) { it / 2 } + scaleOut(targetScale = 0.85f) + fadeOut(tween(180)))
                        }
                    },
                    label = "screens",
                ) { target ->
                    // La pantalla que entra se enfoca y la que sale se desenfoca
                    val blur by transition.animateDp(
                        transitionSpec = { spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow) },
                        label = "screenBlur",
                    ) { state -> if (state == EnterExitState.Visible) 0.dp else 24.dp }
                    Box(Modifier.fillMaxSize().appBlur(blur)) {
                    when (target) {
                        Screen.HOME -> ConverterScreen(
                            vm = vm,
                            onOpenSettings = { screen = Screen.SETTINGS },
                            onOpenAbout = { screen = Screen.ABOUT },
                        )
                        Screen.SETTINGS -> SettingsScreen(vm = vm, onBack = { screen = Screen.HOME })
                        Screen.ABOUT -> AboutScreen(onBack = { screen = Screen.HOME })
                    }
                    }
                }
            }
        }
    }
}
