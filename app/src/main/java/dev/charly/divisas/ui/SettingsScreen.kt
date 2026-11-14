package dev.charly.divisas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.charly.divisas.R
import dev.charly.divisas.ui.i18n.AppLanguage
import dev.charly.divisas.ui.i18n.LocalStrings
import dev.charly.divisas.ui.theme.PALETTE_DYNAMIC
import dev.charly.divisas.ui.theme.PALETTE_HUES
import dev.charly.divisas.ui.theme.supportsDynamicColor
import dev.charly.divisas.ui.theme.swatchColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(vm: ConverterViewModel, onBack: () -> Unit) {
    val str = LocalStrings.current
    val st = vm.settings
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(str.settings, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = str.back)
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding(),
                bottom = pad.calculateBottomPadding() + 32.dp,
            ),
        ) {
            // ---------- Color del tema ----------
            item { SectionHeader(str.themeColor, Modifier.staggeredEnter(0)) }
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().staggeredEnter(1),
                ) {
                    Column(Modifier.padding(vertical = 18.dp, horizontal = 8.dp)) {
                        Text(
                            str.themeColorDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, bottom = 14.dp),
                        )

                        // (índice, relleno, nombre) — el primero es el del wallpaper si el sistema lo soporta
                        val options = buildList {
                            if (supportsDynamicColor) {
                                val d = if (st.dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                                add(Triple(PALETTE_DYNAMIC, Brush.sweepGradient(listOf(d.primary, d.tertiary, d.secondary, d.primary)), str.wallpaper))
                            }
                            PALETTE_HUES.forEachIndexed { i, hue ->
                                add(Triple(i, Brush.linearGradient(listOf(swatchColor(hue, st.dark), swatchColor(hue, st.dark))), str.colorNames[i]))
                            }
                        }
                        options.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                row.forEach { (index, fill, name) ->
                                    Swatch(
                                        fill = fill,
                                        sample = if (index == PALETTE_DYNAMIC) colors.primary else swatchColor(PALETTE_HUES[index], st.dark),
                                        label = name,
                                        selected = st.palette == index,
                                        onClick = { vm.setPalette(index) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }

            // ---------- Apariencia ----------
            item { SectionHeader(str.appearance, Modifier.staggeredEnter(2)) }
            item {
                val source = remember { MutableInteractionSource() }
                val press = rememberPressScale(source, 0.96f)
                Surface(
                    onClick = { vm.setDark(!st.dark) },
                    interactionSource = source,
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEnter(3)
                        .graphicsLayer { scaleX = press; scaleY = press },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    ) {
                        val spin by animateFloatAsState(if (st.dark) 0f else -180f, Motion.wobbly(), label = "moon")
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .graphicsLayer { rotationZ = spin }
                                .clip(MaterialShapes.Cookie6Sided.toShape())
                                .background(colors.secondaryContainer),
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_dark_mode),
                                contentDescription = null,
                                tint = colors.onSecondaryContainer,
                                modifier = Modifier.graphicsLayer { rotationZ = -spin },
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(str.darkMode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (st.dark) str.on else str.off,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = st.dark,
                            onCheckedChange = { vm.setDark(it) },
                            thumbContent = {
                                Icon(
                                    painterResource(if (st.dark) R.drawable.ic_dark_mode else R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }

            // ---------- Desenfoque ----------
            item { SectionHeader(str.blur, Modifier.staggeredEnter(4)) }
            item {
                BlurPreview(
                    type = st.blur,
                    title = str.blurPreview,
                    modifier = Modifier.staggeredEnter(5).padding(bottom = 6.dp),
                )
            }
            val blurOptions = listOf(
                Triple(BlurType.KAWASE, str.kawaseName, str.kawaseDesc),
                Triple(BlurType.GAUSSIAN, str.gaussianName, str.gaussianDesc),
            )
            blurOptions.forEachIndexed { i, (type, name, desc) ->
                item(key = "blur-${type.code}") {
                    ChoiceRow(
                        badge = name.first().toString(),
                        title = name,
                        subtitle = desc,
                        selected = st.blur == type,
                        index = i,
                        count = blurOptions.size,
                        onClick = { vm.setBlur(type) },
                        modifier = Modifier.staggeredEnter(6 + i).padding(bottom = 3.dp),
                    )
                }
            }
            val blurNote = when {
                !BlurSupport.gaussian -> str.blurUnsupported
                !BlurSupport.kawase && st.blur == BlurType.KAWASE -> str.kawaseUnsupported
                else -> null
            }
            if (blurNote != null) {
                item {
                    Text(
                        blurNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp),
                    )
                }
            }
            item {
                Text(
                    str.blurDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp).staggeredEnter(8),
                )
            }

            // ---------- Idioma ----------
            item { SectionHeader(str.language, Modifier.staggeredEnter(9)) }
            AppLanguage.entries.forEachIndexed { i, lang ->
                item(key = lang.code) {
                    LanguageRow(
                        lang = lang,
                        selected = st.language == lang,
                        index = i,
                        count = AppLanguage.entries.size,
                        onClick = { vm.setLanguage(lang) },
                        modifier = Modifier.staggeredEnter(10 + i).padding(bottom = 3.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Swatch(
    fill: Brush,
    sample: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val press = rememberPressScale(source, 0.82f)
    val sel by animateFloatAsState(if (selected) 1f else 0f, Motion.bouncy(), label = "sel")
    val rotation by animateFloatAsState(if (selected) 120f else 0f, Motion.wobbly(), label = "rot")
    val cookie = MaterialShapes.Cookie9Sided.toShape()

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .graphicsLayer {
                    val s = press * (1f + 0.14f * sel)
                    scaleX = s
                    scaleY = s
                }
                .clickable(interactionSource = source, indication = null, onClick = onClick),
        ) {
            // Círculo cuando no está elegido, galleta giratoria cuando sí (cross-fade entre formas)
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = (1f - sel).coerceIn(0f, 1f) }
                    .clip(CircleShape)
                    .background(fill)
            )
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = sel.coerceIn(0f, 1f); rotationZ = rotation }
                    .clip(cookie)
                    .background(fill)
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn(Motion.wobbly(), initialScale = 0f) + fadeIn(),
                exit = scaleOut(targetScale = 0f) + fadeOut(),
            ) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = if (sample.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun LanguageRow(
    lang: AppLanguage,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val press = rememberPressScale(source, 0.95f)
    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = segmentShape(index, count),
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = press; scaleY = press },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(lang.flag, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(16.dp))
            Text(
                lang.nativeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(Motion.wobbly(), initialScale = 0f) + fadeIn(),
                exit = scaleOut(targetScale = 0f) + fadeOut(),
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null)
            }
        }
    }
}

/** Muestra cómo se ve el desenfoque elegido sobre figuras de colores del tema. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BlurPreview(type: BlurType, title: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val wobble by rememberInfiniteTransition(label = "preview").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "wobble",
    )
    // Al cambiar de tipo, el blur "late" con un pulso para que se note el cambio
    val pulse = remember { Animatable(14f) }
    LaunchedEffect(type) {
        pulse.snapTo(30f)
        pulse.animateTo(14f, Motion.wobbly())
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainer,
        modifier = modifier.fillMaxWidth().height(150.dp),
    ) {
        Box {
            Box(
                Modifier
                    .fillMaxSize()
                    .appBlur(pulse.value.dp, type),
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .offset(x = (40 + 24 * wobble).dp, y = 18.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(colors.primary)
                )
                Box(
                    Modifier
                        .size(80.dp)
                        .offset(x = (150 - 20 * wobble).dp, y = 50.dp)
                        .clip(CircleShape)
                        .background(colors.tertiary)
                )
                Box(
                    Modifier
                        .size(70.dp)
                        .offset(x = (240 + 16 * wobble).dp, y = 12.dp)
                        .clip(MaterialShapes.Sunny.toShape())
                        .background(colors.secondary)
                )
                Text(
                    "$ € ¥ £ R$",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = colors.onSurface,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                )
            }
            Surface(
                shape = CircleShape,
                color = colors.surface.copy(alpha = 0.75f),
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChoiceRow(
    badge: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val press = rememberPressScale(source, 0.95f)
    val spin by animateFloatAsState(if (selected) 90f else 0f, Motion.wobbly(), label = "badge")
    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = segmentShape(index, count),
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = press; scaleY = press },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { rotationZ = spin }
                    .clip(MaterialShapes.Cookie6Sided.toShape())
                    .background(if (selected) colors.primary else colors.secondaryContainer),
            ) {
                Text(
                    badge,
                    fontWeight = FontWeight.Black,
                    color = if (selected) colors.onPrimary else colors.onSecondaryContainer,
                    modifier = Modifier.graphicsLayer { rotationZ = -spin },
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(Motion.wobbly(), initialScale = 0f) + fadeIn(),
                exit = scaleOut(targetScale = 0f) + fadeOut(),
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null)
            }
        }
    }
}
