package dev.charly.divisas.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.charly.divisas.R
import dev.charly.divisas.ui.i18n.LocalAppLocale
import dev.charly.divisas.ui.i18n.LocalStrings
import dev.charly.divisas.ui.i18n.Strings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private enum class Side { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConverterScreen(
    vm: ConverterViewModel,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val s = vm.state
    val str = LocalStrings.current
    val haptics = LocalHaptics.current
    // Al terminar una actualización pedida por el usuario: éxito o error se sienten distinto
    var awaitingRefresh by remember { mutableStateOf(false) }
    LaunchedEffect(s.loading) {
        if (!s.loading && awaitingRefresh) {
            awaitingRefresh = false
            if (s.offline || s.rates == null) haptics.error() else haptics.success()
        }
    }
    val locale = LocalAppLocale.current
    var picking by rememberSaveable { mutableStateOf<Side?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    // Desenfoca la pantalla mientras el menú o el selector de monedas están abiertos
    val backdropBlur by animateDpAsState(
        targetValue = if (menuOpen || picking != null) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "backdropBlur",
    )
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // La entrada escalonada solo se reproduce al abrir la pantalla
    var introDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1500); introDone = true }
    val intro = !introDone

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .appBlur(backdropBlur)
            .nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(str.appTitle, fontWeight = FontWeight.Bold) },
                actions = {
                    RefreshAction(
                        loading = s.loading,
                        label = str.refresh,
                        onClick = {
                            awaitingRefresh = true
                            haptics.click(0.6f)
                            vm.refresh()
                        },
                    )
                    OverflowMenu(
                        str = str,
                        open = menuOpen,
                        onOpenChange = { menuOpen = it },
                        onSettings = onOpenSettings,
                        onAbout = onOpenAbout,
                    )
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        val n = s.amountValue
        val others = FAVORITES.filter { it != s.from && it != s.to }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 4.dp,
                bottom = pad.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item {
                ConverterCards(
                    s = s,
                    str = str,
                    locale = locale,
                    onAmount = { new ->
                        haptics.key(deleting = new.length < s.amount.length)
                        vm.setAmount(new)
                    },
                    onPickFrom = { haptics.rise(0.5f); picking = Side.FROM },
                    onPickTo = { haptics.rise(0.5f); picking = Side.TO },
                    onSwap = vm::swap,
                    modifier = Modifier.staggeredEnter(0, intro),
                )
            }

            item {
                Column(Modifier.staggeredEnter(1, intro)) {
                    val one = s.convert(1.0, s.from, s.to)
                    AnimatedContent(
                        targetState = if (one != null) "1 ${s.from} = ${fmt(one, locale)} ${s.to}" else "",
                        transitionSpec = {
                            (fadeIn() + scaleIn(Motion.bouncy(), initialScale = 0.8f)) togetherWith
                                (fadeOut() + scaleOut(targetScale = 1.1f))
                        },
                        label = "rate",
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = statusText(s, str, locale),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (s.offline) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp),
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp).staggeredEnter(2, intro),
                ) {
                    itemsIndexed(FAVORITES.filter { it != s.from }, key = { _, c -> c }) { _, code ->
                        val selected = code == s.to
                        val scale by animateFloatAsState(if (selected) 1.1f else 1f, Motion.wobbly(), label = "chip")
                        FilterChip(
                            selected = selected,
                            onClick = { haptics.select(); vm.setTo(code) },
                            label = { Text("${flag(code)}  $code", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier
                                .animateItem(placementSpec = Motion.bouncy())
                                .graphicsLayer { scaleX = scale; scaleY = scale },
                        )
                    }
                }
            }

            item {
                AnimatedContent(
                    targetState = str.inOtherCurrencies(fmt(n, locale), s.from),
                    transitionSpec = {
                        (slideInVertically(Motion.bouncy()) { it } + fadeIn()) togetherWith
                            (slideOutVertically { -it } + fadeOut())
                    },
                    label = "otherTitle",
                    modifier = Modifier.staggeredEnter(3, intro),
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 10.dp),
                    )
                }
            }

            itemsIndexed(others, key = { _, c -> c }) { i, code ->
                CurrencyRow(
                    code = code,
                    name = currencyName(code, locale),
                    value = fmt(s.convert(n, s.from, code), locale),
                    shape = segmentShape(i, others.size),
                    onClick = { haptics.select(); vm.setTo(code) },
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = Motion.snappy(),
                            placementSpec = Motion.bouncy(),
                            fadeOutSpec = Motion.snappy(),
                        )
                        .staggeredEnter(4 + i, intro)
                        .padding(bottom = 3.dp),
                )
            }
        }
    }

    picking?.let { side ->
        CurrencyPicker(
            currencies = s.currencies,
            selected = if (side == Side.FROM) s.from else s.to,
            str = str,
            locale = locale,
            onPick = {
                haptics.select()
                if (side == Side.FROM) vm.setFrom(it) else vm.setTo(it)
            },
            onDismiss = { picking = null },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RefreshAction(loading: Boolean, label: String, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(0f) }
    AnimatedContent(
        targetState = loading,
        transitionSpec = {
            (scaleIn(Motion.wobbly(), initialScale = 0.3f) + fadeIn()) togetherWith
                (scaleOut(targetScale = 0.3f) + fadeOut())
        },
        label = "refresh",
    ) { isLoading ->
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.padding(horizontal = 4.dp).size(40.dp))
        } else {
            IconButton(onClick = {
                scope.launch { spin.animateTo(spin.targetValue + 360f, Motion.bouncy()) }
                onClick()
            }) {
                Icon(
                    painterResource(R.drawable.ic_refresh),
                    contentDescription = label,
                    modifier = Modifier.graphicsLayer { rotationZ = spin.value },
                )
            }
        }
    }
}

@Composable
private fun OverflowMenu(
    str: Strings,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val turn by animateFloatAsState(if (open) 90f else 0f, Motion.wobbly(), label = "menuIcon")
    Box {
        IconButton(onClick = { haptics.rise(0.5f); onOpenChange(true) }) {
            Icon(
                painterResource(R.drawable.ic_more_vert),
                contentDescription = str.moreOptions,
                modifier = Modifier.graphicsLayer { rotationZ = turn },
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { onOpenChange(false) },
            shape = RoundedCornerShape(20.dp),
        ) {
            DropdownMenuItem(
                text = { Text(str.settings) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_settings), contentDescription = null) },
                onClick = { haptics.click(0.7f); onOpenChange(false); onSettings() },
            )
            DropdownMenuItem(
                text = { Text(str.about) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_info), contentDescription = null) },
                onClick = { haptics.click(0.7f); onOpenChange(false); onAbout() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConverterCards(
    s: UiState,
    str: Strings,
    locale: Locale,
    onAmount: (String) -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val nudge = remember { Animatable(0f) }
    val swapSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(swapSource, 0.8f)

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // TIENES
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                color = colors.surfaceContainerHigh,
                modifier = Modifier.graphicsLayer { translationY = nudge.value * 18.dp.toPx() },
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Label(str.youHave)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyButton(s.from, upward = false, onClick = onPickFrom)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = s.amount,
                            onValueChange = onAmount,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.displaySmall.copy(
                                color = colors.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    CurrencyName(currencyName(s.from, locale))
                }
            }
            // RECIBES
            Surface(
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier.graphicsLayer { translationY = -nudge.value * 18.dp.toPx() },
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Label(str.youGet)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyButton(s.to, upward = true, onClick = onPickTo)
                        Spacer(Modifier.width(12.dp))
                        val result = s.convert(s.amountValue, s.from, s.to)
                        val shown = animatedNumber(result ?: 0.0) { remaining -> haptics.roll(remaining) }
                        Text(
                            text = if (result == null) "—" else fmt(shown, locale),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    CurrencyName(currencyName(s.to, locale))
                }
            }
        }

        // Botón de intercambio: galleta de MaterialShapes que gira, "explota" y empuja las tarjetas
        Surface(
            onClick = {
                haptics.swap()
                onSwap()
                scope.launch { rotation.animateTo(rotation.targetValue + 180f, Motion.wobbly()) }
                scope.launch { pop.snapTo(1.35f); pop.animateTo(1f, Motion.wobbly()) }
                scope.launch { nudge.snapTo(1f); nudge.animateTo(0f, Motion.wobbly()) }
            },
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer,
            border = BorderStroke(5.dp, colors.surface),
            interactionSource = swapSource,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    rotationZ = rotation.value
                    val sc = pop.value * pressScale
                    scaleX = sc
                    scaleY = sc
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_swap),
                    contentDescription = str.swap,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp).graphicsLayer { alpha = 0.75f },
    )
}

@Composable
private fun CurrencyName(name: String) {
    AnimatedContent(
        targetState = name,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "name",
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = 0.75f },
        )
    }
}

@Composable
private fun CurrencyButton(code: String, upward: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val press = rememberPressScale(source, 0.88f)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        interactionSource = source,
        modifier = Modifier.graphicsLayer { scaleX = press; scaleY = press },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        ) {
            // Al cambiar de moneda, la nueva entra deslizándose con rebote
            AnimatedContent(
                targetState = code,
                transitionSpec = {
                    val dir = if (upward) -1 else 1
                    (slideInVertically(Motion.bouncy()) { h -> dir * h } + fadeIn()) togetherWith
                        (slideOutVertically(Motion.snappy()) { h -> -dir * h } + fadeOut()) using
                        SizeTransform(clip = false)
                },
                label = "currency",
            ) { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flag(c), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(c, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Icon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null)
        }
    }
}

@Composable
private fun CurrencyRow(
    code: String,
    name: String,
    value: String?,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val press = rememberPressScale(source, 0.95f)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        interactionSource = source,
        modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = press; scaleY = press },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(flag(code), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Text(code, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) Color.Unspecified else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPicker(
    currencies: List<String>,
    selected: String,
    str: Strings,
    locale: Locale,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val q = normalize(query, locale)
    val filtered = currencies.filter {
        q.isEmpty() || it.lowercase().contains(q) || normalize(currencyName(it, locale), locale).contains(q)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(str.searchHint) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))
        val listState = rememberLazyListState()
        // Cada moneda que pasa al deslizar se siente como un "diente" de rueda
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .drop(1)
                .collect { haptics.tick(0.3f) }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        str.notFound,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
            itemsIndexed(filtered, key = { _, c -> c }) { i, code ->
                CurrencyRow(
                    code = code,
                    name = currencyName(code, locale),
                    value = null,
                    shape = segmentShape(i, filtered.size),
                    selected = code == selected,
                    onClick = {
                        onPick(code)
                        scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier
                        .animateItem(placementSpec = Motion.bouncy())
                        .staggeredEnter(minOf(i, 10))
                        .padding(bottom = 3.dp),
                )
            }
        }
    }
}

private fun statusText(s: UiState, str: Strings, locale: Locale): String {
    val time = s.updatedMillis?.let {
        DateTimeFormatter.ofPattern("d MMM, HH:mm", locale).format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
    } ?: ""
    return when {
        s.rates == null && s.loading -> str.loading
        s.rates == null -> str.loadError
        s.offline -> str.offline(time)
        else -> str.updated(time, s.source ?: "")
    }
}

private fun fmt(v: Double?, locale: Locale): String {
    if (v == null) return "—"
    val abs = kotlin.math.abs(v)
    val nf = NumberFormat.getNumberInstance(locale)
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = when {
        abs >= 1 || abs == 0.0 -> 2
        abs >= 0.01 -> 4
        else -> 6
    }
    return nf.format(v)
}

private fun currencyName(code: String, locale: Locale): String =
    runCatching { Currency.getInstance(code).getDisplayName(locale) }.getOrDefault(code)
        .replaceFirstChar { it.titlecase(locale) }

private fun normalize(s: String, locale: Locale): String =
    Normalizer.normalize(s.trim().lowercase(locale), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

private fun flag(code: String): String {
    val special = mapOf("EUR" to "🇪🇺", "XAF" to "🌍", "XOF" to "🌍", "XCD" to "🌎", "XPF" to "🌏", "ANG" to "🇨🇼", "XDR" to "🌐")
    special[code]?.let { return it }
    if (code.length < 2) return "🏳️"
    return code.take(2).uppercase().map { ch ->
        String(Character.toChars(0x1F1E6 + (ch - 'A')))
    }.joinToString("")
}
