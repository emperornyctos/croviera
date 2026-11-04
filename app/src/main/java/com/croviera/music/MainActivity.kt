package com.croviera.music

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.LocalImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import androidx.glance.appwidget.updateAll
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun PermissionScreen(onRequestPermissions: () -> Unit) {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Croviera needs storage permission to access your music files",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val permissionsArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    else
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.POST_NOTIFICATIONS)

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                // Restart the activity to re-check permissions and start the app
                recreate()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasPermissions = permissionsArray.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

        // Even if permissions are granted, verify they actually work by querying MediaStore
        val permissionsActuallyWork = if (hasPermissions) {
            try {
                contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media._ID),
                    null, null, null
                )?.use { it.count >= 0 } ?: false
            } catch (e: Exception) {
                false
            }
        } else false

        if (hasPermissions && permissionsActuallyWork) {
            startService(android.content.Intent(this, MusicService::class.java))
            setContent { CrovieraApp() }
            // Update widget on app start
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                try {
                    MusicWidget().updateAll(this@MainActivity)
                } catch (e: Exception) {}
            }
        } else {
            setContent {
                PermissionScreen(onRequestPermissions = {
                    val shouldShowRationale = permissionsArray.any { permission ->
                        shouldShowRequestPermissionRationale(permission)
                    }
                    if (shouldShowRationale) {
                        requestPermissionsLauncher.launch(permissionsArray)
                    } else {
                        // Permission was denied permanently - open settings
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = android.net.Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                })
            }
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                if (Prefs.updateCheck(this@MainActivity).first()) {
                    val conn = java.net.URL("https://api.github.com/repos/emperornyctos/croviera/releases/latest").openConnection() as java.net.HttpURLConnection
                    val body = conn.inputStream.bufferedReader().readText()
                    val tag = "\"tag_name\"\\s*:\\s*\"(v[^\"]+)\"".toRegex().find(body)?.groupValues?.get(1)
                    val current = packageManager.getPackageInfo(packageName, 0).versionName
                    if (tag != null && tag != "v$current") {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(this@MainActivity, "New Croviera update: $tag at github.com/emperornyctos/croviera/releases", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

fun albumArtUri(song: Song): Uri = if (!song.coverOverride.isNullOrEmpty()) Uri.parse(song.coverOverride)
else Uri.parse("content://media/external/audio/albumart/${song.albumId}")

/** Opacity applied to panels/dialogs so a background image shows through. 1f = fully opaque. */
val LocalUiAlpha = androidx.compose.runtime.staticCompositionLocalOf { 1f }

/** Global border thickness, adjustable from ~/settings. */
val LocalBorderWidth = androidx.compose.runtime.staticCompositionLocalOf { 1.dp }

val TAB_NAMES = listOf("home", "songs", "albums", "artists", "stats", "playlists", "eq", "settings")
val DEFAULT_NAV_LABELS = TAB_NAMES.associateWith { "~/$it" }
val DEFAULT_NAV_ICONS = mapOf(
    "home" to "\uF015",
    "songs" to "\uF001",
    "albums" to "\uF51F",
    "artists" to "\uF007",
    "stats" to "\uF201",
    "playlists" to "\uF03A",
    "eq" to "\uF1DE",
    "settings" to "\uF013",
)

fun parseHexColor(s: String?, fallback: Color): Color {
    val raw = s?.trim()?.removePrefix("#") ?: return fallback
    if (raw.length != 6 && raw.length != 8) return fallback
    return try {
        val value = raw.toLong(16)
        Color((if (raw.length == 6) 0xFF000000L or value else value).toInt())
    } catch (_: Exception) { fallback }
}

fun parseStringMap(json: String): Map<String, String> = try {
    val obj = org.json.JSONObject(json)
    obj.keys().asSequence().associateWith { obj.optString(it, "") }
} catch (_: Exception) { emptyMap() }

fun mapToJson(map: Map<String, String>): String {
    val obj = org.json.JSONObject()
    map.forEach { (k, v) -> if (v.isNotBlank()) obj.put(k, v) }
    return obj.toString()
}

@Composable
fun CrovieraSplash() {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text("croviera", color = Color(0xFF8A1F2D), fontSize = 18.sp, letterSpacing = 5.sp)
    }
}

fun tabTransition(kind: String): ContentTransform = when (kind) {
    "none" -> EnterTransition.None togetherWith ExitTransition.None
    "slide_left" -> slideInHorizontally(initialOffsetX = { it }) togetherWith slideOutHorizontally(targetOffsetX = { -it })
    "slide_right" -> slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it })
    "slide_up" -> slideInVertically(initialOffsetY = { it }) togetherWith slideOutVertically(targetOffsetY = { -it })
    "slide_down" -> slideInVertically(initialOffsetY = { -it }) togetherWith slideOutVertically(targetOffsetY = { it })
    "zoom" -> scaleIn(initialScale = 0.92f) togetherWith scaleOut(targetScale = 0.92f)
    "cmatrix" -> fadeIn(tween(140)) togetherWith fadeOut(tween(140))
    else -> fadeIn(tween(180)) togetherWith fadeOut(tween(140))
}

@Composable
fun NavBar(
    theme: ThemeColors,
    shape: RoundedCornerShape,
    position: String,
    labels: Map<String, String>,
    icons: Map<String, String>,
    nerd: Boolean,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vertical = position == "left" || position == "right"
    val nerdFont = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.symbols_nerd_mono))
    val item: @Composable (Int) -> Unit = { i ->
        val name = TAB_NAMES[i]
        val isSelected = selected == i
        val text = if (nerd) icons[name] ?: DEFAULT_NAV_ICONS.getValue(name)
        else labels[name] ?: DEFAULT_NAV_LABELS.getValue(name)
        Text(
            text = text,
            color = if (isSelected) theme.accent else theme.subtext,
            fontSize = if (nerd) 20.sp else 14.sp,
            fontFamily = if (nerd) nerdFont else null,
            maxLines = 1,
            modifier = Modifier
                .clip(shape)
                .background(if (isSelected) theme.accent.copy(alpha = 0.14f) else Color.Transparent, shape)
                .clickable { onSelect(i) }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
    if (vertical) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .background(theme.bg.copy(alpha = 0.94f), shape)
                .border(LocalBorderWidth.current, theme.border, shape)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { TAB_NAMES.indices.forEach { item(it) } }
    } else {
        Row(
            modifier = modifier
                .horizontalScroll(rememberScrollState())
                .background(theme.bg.copy(alpha = 0.94f), shape)
                .border(LocalBorderWidth.current, theme.border, shape)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { TAB_NAMES.indices.forEach { item(it) } }
    }
}

@Composable
fun MatrixRain(color: Color) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { withFrameNanos { frame++ } }
    }
    val f = frame
    val density = LocalDensity.current
    val glyphs = "01ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ"
    Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = 0.72f }) {
        val paint = android.graphics.Paint().apply {
            this.color = color.toArgb()
            isAntiAlias = true
            textSize = with(density) { 14.sp.toPx() }
        }
        val cell = with(density) { 16.dp.toPx() }
        val cols = (size.width / cell).toInt().coerceAtLeast(1)
        val rows = (size.height / cell).toInt().coerceAtLeast(1)
        for (i in 0 until cols) {
            val speed = 0.55f + (i % 7) * 0.14f
            val raw = ((f * speed + i * 7) % (rows + 12)).toInt()
            val downward = i % 2 == 0
            val head = if (downward) raw else rows - raw
            val drift = ((f / 6 + i * 5) % 9 - 4) * 0.7f
            for (j in 0 until 12) {
                val r = if (downward) head - j else head + j
                if (r < 0 || r >= rows) continue
                paint.alpha = (230 * (1f - j / 12f)).toInt().coerceIn(0, 255)
                val ch = glyphs[(i * 11 + r * 7 + f / 4) % glyphs.length]
                drawContext.canvas.nativeCanvas.drawText(ch.toString(), i * cell + drift, r * cell, paint)
            }
        }
    }
}

@Composable
fun CrovieraApp() {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    // Read the first DataStore snapshot before drawing anything. This is what
    // stops the welcome/default-theme flash on cold start and on rotation.
    val snapshot by produceState<Prefs.Snapshot?>(null) { value = Prefs.snapshot(c) }
    val s = snapshot
    if (s == null) { CrovieraSplash(); return }

    val themeName by Prefs.theme(c).collectAsState(initial = s.theme)
    val rounded by Prefs.rounded(c).collectAsState(initial = s.rounded)
    val folders by Prefs.folders(c).collectAsState(initial = s.folders)
    val custom by Prefs.custom(c).collectAsState(initial = s.custom)
    val fontName by Prefs.fontName(c).collectAsState(initial = s.fontName)
    val bgImg by Prefs.customBgImg(c).collectAsState(initial = s.bgImg)
    val bgAlphaPct by Prefs.bgAlpha(c).collectAsState(initial = s.bgAlpha)
    val uiAlpha by Prefs.uiAlpha(c).collectAsState(initial = s.uiAlpha)
    val fontScale by Prefs.fontScale(c).collectAsState(initial = s.fontScale)
    val lineHeightScale by Prefs.lineHeightScale(c).collectAsState(initial = s.lineHeightScale)
    val cornerSize by Prefs.cornerSize(c).collectAsState(initial = s.cornerSize)
    val borderWidth by Prefs.borderWidth(c).collectAsState(initial = s.borderWidth)
    val gradient by Prefs.gradient(c).collectAsState(initial = s.gradient)
    val navPosition by Prefs.navPosition(c).collectAsState(initial = s.navPosition)
    val navLabelsJson by Prefs.navLabels(c).collectAsState(initial = s.navLabels)
    val navIconsJson by Prefs.navIcons(c).collectAsState(initial = s.navIcons)
    val navNerd by Prefs.navNerd(c).collectAsState(initial = s.navNerd)
    val transition by Prefs.transition(c).collectAsState(initial = s.transition)
    val npLabel by Prefs.npLabel(c).collectAsState(initial = s.npLabel)
    val npExpandedCover by Prefs.npExpandedCover(c).collectAsState(initial = s.npExpandedCover)
    val npExpandedQueue by Prefs.npExpandedQueue(c).collectAsState(initial = s.npExpandedQueue)
    val npExpandedLyrics by Prefs.npExpandedLyrics(c).collectAsState(initial = s.npExpandedLyrics)

    // A background image belongs to the custom theme only; built-in themes stay clean.
    val effectiveBgImg = if (themeName == "Custom") bgImg else ""
    val uiAlphaF = if (effectiveBgImg.isNotEmpty()) (uiAlpha / 100f).coerceIn(0.15f, 1f) else 1f
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, fontScale) {
        Density(baseDensity.density, baseDensity.fontScale * fontScale)
    }
    val lineHeightTextStyle = if (lineHeightScale > 0f) TextStyle(lineHeight = (14 * lineHeightScale).sp) else TextStyle()
    val borderDp = borderWidth.dp

    @Composable
    fun Wrap(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalUiAlpha provides uiAlphaF,
            LocalBorderWidth provides borderDp
        ) { content() }
    }

    val theme = if (themeName == "Custom") {
        ThemeColors(
            "Custom",
            parseHexColor(custom.getOrNull(0), Color.Black),
            parseHexColor(custom.getOrNull(1), Color(0xFF111111)),
            parseHexColor(custom.getOrNull(2), Color.White),
            parseHexColor(custom.getOrNull(3), Color.Gray),
            parseHexColor(custom.getOrNull(4), Color.White),
            parseHexColor(custom.getOrNull(5), Color.Gray.copy(alpha = 0.35f)),
        )
    } else Themes.byName(themeName)

    val shape = RoundedCornerShape(cornerSize.dp)
    val bgBrush: Brush = if (themeName == "Custom" && gradient.start.isNotBlank() && gradient.end.isNotBlank()) {
        val start = parseHexColor(gradient.start, theme.bg)
        val end = parseHexColor(gradient.end, theme.surface)
        val rad = Math.toRadians(gradient.angle.toDouble())
        Brush.linearGradient(
            colors = listOf(start, end),
            start = Offset.Zero,
            end = Offset((cos(rad) * 1200).toFloat(), (sin(rad) * 1200).toFloat())
        )
    } else SolidColor(theme.bg)

    val fontFamily = remember(fontName) {
        val bundled = mapOf(
            "DM Mono" to R.font.dm_mono_regular,
            "DM Mono Italic" to R.font.dm_mono_italic,
            "JetBrainsMono" to R.font.jetbrains_mono,
            "FiraCode" to R.font.fira_code,
            "CascadiaCode" to R.font.cascadia_code,
            "SpaceMono" to R.font.space_mono,
            "Iosevka" to R.font.iosevka,
            "MapleMono" to R.font.maple_mono,
            "MapleMono Italic" to R.font.maple_mono_italic,
        )
        when {
            bundled.containsKey(fontName) -> androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(bundled[fontName]!!)
            )
            else -> {
                val f = File(c.filesDir, "fonts/$fontName.ttf")
                if (f.exists()) androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(f)
                ) else androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.dm_mono_regular)
                )
            }
        }
    }

    val onboarded by Prefs.onboarded(c).collectAsState(initial = s.onboarded)

    if (!onboarded) {
        Surface(Modifier.fillMaxSize(), color = theme.bg) {
            Wrap { ProvideTextStyle(TextStyle(fontFamily = fontFamily).merge(lineHeightTextStyle)) { OnboardingScreen(theme) } }
        }
        return
    }

    val isLight = theme.bg.luminance() > 0.5f
    val surfA = theme.surface.copy(alpha = uiAlphaF)
    val m3Scheme = if (isLight) {
        androidx.compose.material3.lightColorScheme(
            primary = theme.accent, onPrimary = theme.bg, background = theme.bg, onBackground = theme.text,
            surface = surfA, onSurface = theme.text, surfaceVariant = surfA, onSurfaceVariant = theme.subtext,
            outline = theme.border, secondary = theme.accent, surfaceContainer = surfA, surfaceContainerHigh = surfA, surfaceContainerHighest = surfA,
            surfaceContainerLow = surfA, surfaceContainerLowest = surfA, surfaceBright = surfA, surfaceDim = surfA
        )
    } else {
        androidx.compose.material3.darkColorScheme(
            primary = theme.accent, onPrimary = theme.bg, background = theme.bg, onBackground = theme.text,
            surface = surfA, onSurface = theme.text, surfaceVariant = surfA, onSurfaceVariant = theme.subtext,
            outline = theme.border, secondary = theme.accent, surfaceContainer = surfA, surfaceContainerHigh = surfA, surfaceContainerHighest = surfA,
            surfaceContainerLow = surfA, surfaceContainerLowest = surfA, surfaceBright = surfA, surfaceDim = surfA
        )
    }

    androidx.compose.material3.MaterialTheme(colorScheme = m3Scheme, typography = androidx.compose.material3.Typography().run {
        copy(
            bodyLarge = bodyLarge.copy(fontFamily = fontFamily, color = theme.text),
            bodyMedium = bodyMedium.copy(fontFamily = fontFamily, color = theme.text),
            labelLarge = labelLarge.copy(fontFamily = fontFamily, color = theme.text),
            titleLarge = titleLarge.copy(fontFamily = fontFamily, color = theme.text),
            titleMedium = titleMedium.copy(fontFamily = fontFamily, color = theme.text),
            labelMedium = labelMedium.copy(fontFamily = fontFamily, color = theme.text)
        )
    }) {
    Wrap {
    Box(Modifier.fillMaxSize().background(bgBrush)) {
        if (effectiveBgImg.isNotEmpty()) AsyncImage(model = effectiveBgImg, contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(bgAlphaPct / 100f), contentScale = ContentScale.Crop)
        ProvideTextStyle(TextStyle(fontFamily = fontFamily).merge(lineHeightTextStyle)) {
            val imageLoader = remember {
                coil.ImageLoader.Builder(c)
                    .components {
                        add(coil.decode.GifDecoder.Factory())
                        add(coil.decode.SvgDecoder.Factory())
                    }
                    .build()
            }
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
            val welcome by Prefs.welcome(c).collectAsState(initial = s.welcome)
            val profileName by Prefs.profileName(c).collectAsState(initial = s.profileName)
            val profilePic by Prefs.profilePic(c).collectAsState(initial = s.profilePic)
            var tab by remember { mutableStateOf(0) }
            var npExpanded by remember { mutableStateOf(false) }
            val cardScroll = rememberScrollState()
            var matrixActive by remember { mutableStateOf(false) }
            LaunchedEffect(tab, transition) {
                if (transition == "cmatrix") {
                    matrixActive = true
                    delay(700)
                    matrixActive = false
                }
            }
            var songs by remember { mutableStateOf(listOf<Song>()) }
            val rescan = MusicIndex.rescanTick.intValue
            LaunchedEffect(folders, rescan) { songs = MusicIndex.songsInFolders(c, folders) }

            val title = Playback.title.collectAsState().value
            val artist = Playback.artist.collectAsState().value
            val playing = Playback.playing.collectAsState().value
            val shuffleOn by Prefs.shuffle(c).collectAsState(initial = false)
            val repeatOn by Prefs.repeat(c).collectAsState(initial = 0)
            var pos by remember { mutableStateOf(0f) }
            LaunchedEffect(playing) {
                while (playing) {
                    pos = Playback.player?.currentPosition?.toFloat() ?: 0f
                    kotlinx.coroutines.delay(400)
                }
            }
            val navLabels = remember(navLabelsJson) { parseStringMap(navLabelsJson) }
            val navIcons = remember(navIconsJson) { parseStringMap(navIconsJson) }

            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(
                        top = if (navPosition == "top") 48.dp else 0.dp,
                        bottom = if (navPosition == "bottom") 48.dp else 0.dp,
                        start = if (navPosition == "left") 100.dp else 0.dp,
                        end = if (navPosition == "right") 100.dp else 0.dp
                    ).padding(10.dp)
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = { tabTransition(transition) },
                            modifier = Modifier.fillMaxSize(),
                            label = "tab"
                        ) { currentTab ->
                            when (currentTab) {
                                0 -> HomeTab(songs, theme, shape, welcome, profileName, profilePic)
                                1 -> SongsTab(songs, theme, shape)
                                2 -> AlbumsTab(songs, theme, shape)
                                3 -> ArtistsTab(songs, theme, shape)
                                4 -> StatsTab(songs, theme)
                                5 -> PlaylistsTab(theme, shape)
                                6 -> EqualizerTab(theme)
                                7 -> SettingsTab(theme, themeName, rounded, folders) { newTheme, newRounded, newFolders ->
                                    scope.launch {
                                        if (newTheme != null) Prefs.setTheme(c, newTheme)
                                        if (newRounded != null) Prefs.setRounded(c, newRounded)
                                        if (newFolders != null) Prefs.setFolders(c, newFolders)
                                    }
                                }
                            }
                        }
                        if (matrixActive) MatrixRain(theme.accent)
                    }

                    if (title.isNotEmpty()) {
                        val cur = Playback.queue.value.getOrNull(Playback.player?.currentMediaItemIndex ?: 0)
                        Column(
                            Modifier.fillMaxWidth()
                                .heightIn(min = if (npExpanded) 0.dp else 140.dp)
                                .animateContentSize()
                                .border(LocalBorderWidth.current, theme.border, shape)
                                .background(theme.surface.copy(alpha = uiAlphaF), shape)
                                .then(if (npExpanded) Modifier.heightIn(max = 460.dp).verticalScroll(cardScroll) else Modifier)
                                .clickable { npExpanded = !npExpanded }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(npLabel, color = theme.accent, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (npExpanded) "[ collapse ]" else "[ expand ]", color = theme.subtext, fontSize = 10.sp)
                                    if (!npExpanded) {
                                        Spacer(Modifier.width(10.dp))
                                        val favSong by Prefs.favSong(c).collectAsState(initial = "")
                                        val curSongTitle = Playback.queue.value.getOrNull(Playback.player?.currentMediaItemIndex ?: -1)?.title ?: ""
                                        Text("★", color = if (favSong == curSongTitle && curSongTitle.isNotEmpty()) theme.accent else theme.subtext, fontSize = 28.sp,
                                            modifier = Modifier.clickable { if (curSongTitle.isNotEmpty()) scope.launch { Prefs.setFavSong(c, if (favSong == curSongTitle) "" else curSongTitle) } })
                                    }
                                }
                            }
                            if (npExpanded) {
                                // Expanded view is intentionally only the big cover,
                                // the expanded queue and the lyrics.
                                if (cur != null) {
                                    AsyncImage(
                                        model = albumArtUri(cur), contentDescription = null,
                                        modifier = Modifier.size(npExpandedCover.dp).clip(shape).border(LocalBorderWidth.current, theme.border, shape).align(Alignment.CenterHorizontally),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                val upNext = Playback.queue.value.drop((Playback.player?.currentMediaItemIndex ?: 0) + 1).take(6)
                                if (npExpandedQueue && upNext.isNotEmpty()) {
                                    Text("up next", color = theme.subtext, fontSize = 10.sp, letterSpacing = 1.sp)
                                    upNext.forEach { nextSong ->
                                        Text("· ${nextSong.title} — ${nextSong.artist}", color = theme.subtext, fontSize = 12.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (npExpandedLyrics) LyricsBlock(theme, pos, expanded = true)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (cur != null) {
                                        val npSize by Prefs.npSize(c).collectAsState(initial = 52)
                                        val customDisc by Prefs.customDisc(c).collectAsState(initial = "")
                                        if (customDisc.isNotEmpty()) {
                                            val discAnim = rememberInfiniteTransition()
                                            val discRot by discAnim.animateFloat(0f, 360f, infiniteRepeatable(tween(3500, easing = LinearEasing)))
                                            androidx.compose.foundation.Image(painter = coil.compose.rememberAsyncImagePainter(model = customDisc), contentDescription = null, modifier = Modifier.size((npSize / 2).dp).clip(RoundedCornerShape(4.dp)).graphicsLayer { rotationZ = discRot }, contentScale = ContentScale.Crop)
                                        } else {
                                            SpinningDisc(sizeDp = npSize / 2, accent = theme.accent)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        AsyncImage(
                                            model = albumArtUri(cur), contentDescription = null,
                                            modifier = Modifier.size(npSize.dp).clip(shape), contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(10.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.basicMarquee())
                                        Text(artist, color = theme.subtext, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.basicMarquee())
                                    }
                                }
                                val dur = (Playback.player?.duration?.toFloat() ?: 1f).coerceAtLeast(1f)
                                Slider(
                                    value = pos.coerceIn(0f, dur), valueRange = 0f..dur,
                                    onValueChange = { pos = it },
                                    onValueChangeFinished = { Playback.player?.seekTo(pos.toLong()) },
                                    colors = SliderDefaults.colors(
                                        thumbColor = theme.accent, activeTrackColor = theme.accent,
                                        inactiveTrackColor = theme.border
                                    )
                                )
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shuffle, "shuffle", tint = if (shuffleOn) theme.accent else theme.subtext,
                                        modifier = Modifier.clickable {
                                            scope.launch { Prefs.setShuffle(c, !shuffleOn); Playback.player?.shuffleModeEnabled = !shuffleOn }
                                        })
                                    Icon(Icons.Default.SkipPrevious, "prev", tint = theme.text, modifier = Modifier.clickable {
                                        Playback.player?.seekToPreviousMediaItem()
                                    })
                                    Icon(
                                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "playpause",
                                        tint = theme.accent, modifier = Modifier.size(40.dp).clickable {
                                            Playback.player?.let { if (it.isPlaying) it.pause() else it.play() }
                                        }
                                    )
                                    Icon(Icons.Default.SkipNext, "next", tint = theme.text, modifier = Modifier.clickable {
                                        Playback.player?.seekToNextMediaItem()
                                    })
                                    Icon(
                                        if (repeatOn == 2) Icons.Default.RepeatOne else Icons.Default.Repeat, "repeat",
                                        tint = if (repeatOn > 0) theme.accent else theme.subtext,
                                        modifier = Modifier.clickable {
                                            val next = (repeatOn + 1) % 3
                                            scope.launch {
                                                Prefs.setRepeat(c, next)
                                                Playback.player?.repeatMode = when (next) {
                                                    1 -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                                    2 -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                                    else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                                }
                                            }
                                        })
                                }
                                val upNext = Playback.queue.value.drop((Playback.player?.currentMediaItemIndex ?: 0) + 1).take(3)
                                if (upNext.isNotEmpty()) {
                                    Text("up next: ${upNext.joinToString(" · ") { it.title }}", color = theme.subtext, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                LyricsBlock(theme, pos, expanded = false)
                            }
                            Text(if (npExpanded) "[ tap anywhere to collapse ]" else "[ tap anywhere to expand ]",
                                color = theme.subtext.copy(alpha = 0.7f), fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                }
                val navMod = when (navPosition) {
                    "bottom" -> Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(4.dp)
                    "left" -> Modifier.align(Alignment.CenterStart).width(92.dp).fillMaxHeight().padding(4.dp)
                    "right" -> Modifier.align(Alignment.CenterEnd).width(92.dp).fillMaxHeight().padding(4.dp)
                    else -> Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(4.dp)
                }
                NavBar(theme, shape, navPosition, navLabels, navIcons, navNerd, tab, { tab = it }, navMod)
            }
        }
    }
}
}
}
}

@Composable
fun LyricsBlock(theme: ThemeColors, posMs: Float, expanded: Boolean = false) {
    val c = LocalContext.current
    val playerIndex = Playback.player?.currentMediaItemIndex ?: 0
    val curSong = Playback.queue.value.getOrNull(playerIndex)
    // Re-read the file when the current song changes or after metadata edits.
    val rescan = MusicIndex.rescanTick.intValue
    val doc = remember(curSong?.metaKey, rescan) {
        Lyrics.forSong(c, curSong?.metaKey ?: "")
    }
    var showLyrics by remember(curSong?.metaKey) { mutableStateOf(false) }
    val pos = posMs.toLong()
    val visible = expanded || showLyrics
    if (!expanded) {
        Text(if (showLyrics) "[hide lyrics]" else "[lyrics]", color = theme.accent, fontSize = 12.sp,
            modifier = Modifier.clickable { showLyrics = !showLyrics })
    }
    if (visible) {
        if (doc.isEmpty()) {
            Text("You actually have an .lrc file? Apply one in the metadata editor!", color = theme.subtext, fontSize = 12.sp)
        } else {
            val synced = doc.any { it.first > 0L }
            val idx = if (synced) doc.indexOfLast { it.first <= pos }.coerceAtLeast(0) else -1
            Column(
                if (expanded) Modifier.fillMaxWidth()
                else Modifier.height(180.dp).verticalScroll(rememberScrollState())
            ) {
                doc.forEachIndexed { i, (t, text) ->
                    Text(
                        text,
                        color = if (i == idx) theme.accent else theme.subtext,
                        fontSize = if (i == idx) 14.sp else 12.sp,
                        fontWeight = if (i == idx) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
    else "$m:${sec.toString().padStart(2, '0')}"
}

fun humanTime(ms: Long): String {
    val h = ms / 3600000
    val m = (ms % 3600000) / 60000
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun SpinningDisc(sizeDp: Int, accent: Color = Color(0xFF8A1F2D)) {
    val anim = rememberInfiniteTransition()
    val rot by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)))
    Canvas(Modifier.size(sizeDp.dp).padding(horizontal = 4.dp).graphicsLayer { rotationZ = rot }) {
        val rows = listOf(
            "..####..",
            ".######.",
            "########",
            "###..###",
            "###..###",
            "########",
            ".######.",
            "..####.."
        )
        val s = size.minDimension
        val px = s / 8f
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                if (ch == '#') drawRect(Color(0xFF141414), androidx.compose.ui.geometry.Offset(c * px, r * px), androidx.compose.ui.geometry.Size(px, px))
            }
        }
        // centre hole
        drawRect(Color.White, androidx.compose.ui.geometry.Offset(3 * px, 3 * px), androidx.compose.ui.geometry.Size(2 * px, 2 * px))
        // off-centre label mark: this is the bit that visibly goes in circles
        drawRect(accent, androidx.compose.ui.geometry.Offset(5 * px, 1 * px), androidx.compose.ui.geometry.Size(2 * px, px))
        // bright rim notch so the rotation reads even at tiny sizes
        drawRect(Color.White, androidx.compose.ui.geometry.Offset(6 * px, 2 * px), androidx.compose.ui.geometry.Size(px, px))
    }
}

@Composable
fun Panel(title: String, theme: ThemeColors, modifier: Modifier = Modifier, shape: RoundedCornerShape, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().border(LocalBorderWidth.current, theme.border, shape).background(theme.surface.copy(alpha = LocalUiAlpha.current), shape).padding(12.dp)) {
        Text(L.tr(LocalContext.current, title).uppercase(), color = theme.subtext, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun HomeTab(songs: List<Song>, theme: ThemeColors, shape: RoundedCornerShape, welcome: String, profileName: String, profilePic: String) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var editWelcome by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var editBio by remember { mutableStateOf(false) }
    var editPronouns by remember { mutableStateOf(false) }
    var editLocation by remember { mutableStateOf(false) }
    var editFavAlbum by remember { mutableStateOf(false) }
    var welcomeField by remember(welcome) { mutableStateOf(welcome) }
    var nameField by remember(profileName) { mutableStateOf(profileName) }
    val topSongs = remember(songs) { Stats.topSongs(c) }
    val topSongList = remember(topSongs, songs) {
        topSongs.mapNotNull { s -> songs.firstOrNull { it.path == s.first } }
    }
    val recent = remember(songs) { songs.sortedByDescending { it.dateAdded }.take(10) }
    val albums = remember(songs) { songs.groupBy { it.album } }
    val banner by Prefs.profileBanner(c).collectAsState(initial = "")
    val bio by Prefs.profileBio(c).collectAsState(initial = "")
    val pronouns by Prefs.profilePronouns(c).collectAsState(initial = "")
    val fav by Prefs.favSong(c).collectAsState(initial = "")
    val pickPfp = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            scope.launch { Prefs.setProfilePic(c, uri.toString()) }
        }
    }
    val pickBanner = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            scope.launch { Prefs.setProfileBanner(c, uri.toString()) }
        }
    }
    val location by Prefs.profileLocation(c).collectAsState(initial = "")
    val favAlbum by Prefs.profileFavAlbum(c).collectAsState(initial = "")
    var locationField by remember(location) { mutableStateOf(location) }
    var favAlbumField by remember(favAlbum) { mutableStateOf(favAlbum) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel("profile", theme, shape = shape) {
            if (banner.isNotEmpty()) {
                AsyncImage(model = banner, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(80.dp).clip(shape),
                    contentScale = ContentScale.Crop)
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profilePic.isNotEmpty()) {
                    AsyncImage(model = profilePic, contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(shape).border(LocalBorderWidth.current, theme.border, shape), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(44.dp).background(theme.bg, shape).border(LocalBorderWidth.current, theme.border, shape)) {
                        Text(profileName.firstOrNull()?.uppercase() ?: "?", color = theme.accent, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(profileName, color = theme.text, fontSize = 15.sp, modifier = Modifier.clickable { editName = true })
                    Text(L.tr(LocalContext.current, "tap to edit"), color = theme.subtext, fontSize = 10.sp)
                }
                Text(L.tr(LocalContext.current, "[set pfp]"), color = theme.accent, fontSize = 12.sp, modifier = Modifier.clickable { pickPfp.launch("image/*") })
            }
            if (pronouns.isNotEmpty()) Text("($pronouns)", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { editPronouns = true })
            if (location.isNotEmpty()) Text("@ $location", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { editLocation = true })
            if (favAlbum.isNotEmpty()) Text("★ fav album: $favAlbum", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { editFavAlbum = true })
            if (bio.isNotEmpty()) Text(bio, color = theme.text, fontSize = 13.sp, modifier = Modifier.clickable { editBio = true })
            else Text("tap to add a bio…", color = theme.subtext, fontSize = 13.sp, modifier = Modifier.clickable { editBio = true })
            val curIdx = Playback.player?.currentMediaItemIndex ?: -1
            if (fav.isNotEmpty()) Text("★ favorite: $fav", color = theme.subtext, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(L.tr(LocalContext.current, "[banner]"), color = theme.accent, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.clickable { pickBanner.launch("image/*") })
                Text(L.tr(LocalContext.current, "[pronouns]"), color = theme.accent, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.clickable { editPronouns = true })
                Text(L.tr(LocalContext.current, "[location]"), color = theme.accent, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.clickable { editLocation = true })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(L.tr(LocalContext.current, "[fav album]"), color = theme.accent, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.clickable { editFavAlbum = true })
                Text(L.tr(LocalContext.current, "[bio]"), color = theme.accent, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.clickable { editBio = true })
            }
        }

        Panel("welcome — tap to edit", theme, shape = shape) {
            Text(welcome, color = theme.text, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.clickable { editWelcome = true })
        }

        Panel("most played", theme, shape = shape) {
            if (topSongList.isEmpty()) Text(L.tr(LocalContext.current, "no plays yet"), color = theme.subtext, fontSize = 13.sp)
            Text("${songs.size} songs · ${albums.size} albums in library", color = theme.subtext, fontSize = 11.sp)
            topSongList.take(5).forEachIndexed { i, s ->
                Row(Modifier.fillMaxWidth().clickable { Playback.play(c, topSongList, i) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = albumArtUri(s), contentDescription = null, modifier = Modifier.size(34.dp).clip(shape))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(s.title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${s.artist} · ${s.album}", color = theme.subtext, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        val lastPath = remember { Stats.lastPlayedPath(c) }
        val lastSong = remember(songs, lastPath) { songs.firstOrNull { it.path == lastPath } }
        if (lastSong != null) Panel("last played", theme, shape = shape) {
            Text("${lastSong.title} — ${lastSong.artist}", color = theme.text, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clickable { Playback.play(c, songs, songs.indexOf(lastSong)) }.padding(vertical = 3.dp))
        }

        Panel("recently added", theme, shape = shape) {
            recent.take(5).forEachIndexed { i, s ->
                Text("${s.title} — ${s.artist}", color = theme.text, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable { Playback.play(c, recent, i) }.padding(vertical = 3.dp))
            }
        }
    }

    if (editWelcome) AlertDialog(
        onDismissRequest = { editWelcome = false },
        title = { Text(L.tr(LocalContext.current, "edit welcome")) },
        text = { OutlinedTextField(value = welcomeField, onValueChange = { welcomeField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setWelcome(c, welcomeField); editWelcome = false } }) { Text(L.tr(LocalContext.current, "ok")) } }
    )
    if (editName) AlertDialog(
        onDismissRequest = { editName = false },
        title = { Text(L.tr(LocalContext.current, "edit name")) },
        text = { OutlinedTextField(value = nameField, onValueChange = { nameField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfileName(c, nameField); editName = false } }) { Text(L.tr(LocalContext.current, "ok")) } }
    )
    var bioField by remember(bio) { mutableStateOf(bio) }
    var pronounsField by remember(pronouns) { mutableStateOf(pronouns) }
    if (editBio) AlertDialog(
        onDismissRequest = { editBio = false },
        title = { Text("edit bio") },
        text = { OutlinedTextField(value = bioField, onValueChange = { bioField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfileBio(c, bioField); editBio = false } }) { Text(L.tr(LocalContext.current, "ok")) } }
    )
    if (editPronouns) AlertDialog(
        onDismissRequest = { editPronouns = false },
        title = { Text("edit pronouns") },
        text = { OutlinedTextField(value = pronounsField, onValueChange = { pronounsField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfilePronouns(c, pronounsField); editPronouns = false } }) { Text(L.tr(LocalContext.current, "ok")) } }
    )
    if (editLocation) AlertDialog(
        onDismissRequest = { editLocation = false },
        title = { Text("edit location") },
        text = { OutlinedTextField(value = locationField, onValueChange = { locationField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfileLocation(c, locationField); editLocation = false } }) { Text(L.tr(LocalContext.current, "ok")) } }
    )
    if (editFavAlbum) AlertDialog(
        onDismissRequest = { editFavAlbum = false },
        title = { Text("edit favorite album") },
        text = { OutlinedTextField(value = favAlbumField, onValueChange = { favAlbumField = it }) },
        confirmButton = { TextButton(onClick = { scope.launch { Prefs.setProfileFavAlbum(c, favAlbumField); editFavAlbum = false } }) { Text(L.tr(LocalContext.current, "ok")) } }
    )
}

@Composable
fun SongsTab(songs: List<Song>, theme: ThemeColors, shape: RoundedCornerShape) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var addSong by remember { mutableStateOf<Song?>(null) }
    var editSong by remember { mutableStateOf<Song?>(null) }
    var query by remember { mutableStateOf("") }
    val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
    val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
    val shown = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) || it.album.contains(query, ignoreCase = true)
        }
    }
    val playsMap = remember(c) { Stats.topSongs(c, Int.MAX_VALUE).toMap() }
    var sortMode by remember { mutableStateOf(0) }
    val shownSorted = remember(shown, sortMode, playsMap) {
        when (sortMode) {
            1 -> shown.sortedBy { it.year.toIntOrNull() ?: Int.MAX_VALUE }
            2 -> shown.sortedByDescending { playsMap[it.path] ?: 0 }
            else -> shown
        }
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(L.tr(LocalContext.current, "search")) },
            modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("sort: ${listOf("title", "year", "plays")[sortMode]}", color = theme.accent, fontSize = 12.sp,
                modifier = Modifier.clickable { sortMode = (sortMode + 1) % 3 }.border(LocalBorderWidth.current, theme.border).padding(6.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(shownSorted) { s ->
                val idx = songs.indexOf(s)
                Row(Modifier.fillMaxWidth().padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).clickable { Playback.play(c, songs, idx) }, verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = albumArtUri(s), contentDescription = null, modifier = Modifier.size(coverSize.dp).clip(shape))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${s.artist} · ${s.album}", color = theme.subtext, fontSize = 11.sp, maxLines = 1)
                        }
                        Text(formatTime(s.durationMs), color = theme.subtext, fontSize = 11.sp)
                    }
                    Text(L.tr(LocalContext.current, " +"), color = theme.accent, fontSize = 16.sp,
                        modifier = Modifier.clickable { addSong = s }.padding(start = 10.dp, top = 6.dp, bottom = 6.dp))
                    Text(L.tr(LocalContext.current, " ✎"), color = theme.accent, fontSize = 16.sp,
                        modifier = Modifier.clickable { editSong = s }.padding(start = 10.dp, top = 6.dp, bottom = 6.dp))
                }
            }
        }
    }
    addSong?.let { song ->
        AlertDialog(
            onDismissRequest = { addSong = null },
            title = { Text(L.tr(LocalContext.current, "add to playlist")) },
            text = {
                Column {
                    Playlists.names(c).forEach { name ->
                        Text(name, modifier = Modifier.clickable {
                            Playlists.add(c, name, song.path); addSong = null
                        }.padding(vertical = 6.dp), color = theme.text)
                    }
                    var newPlaylistName by remember { mutableStateOf("") }
                    OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it }, label = { Text(L.tr(LocalContext.current, "new playlist")) })
                    Button(onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            Playlists.create(c, newPlaylistName); Playlists.add(c, newPlaylistName, song.path); addSong = null
                        }
                    }) { Text(L.tr(LocalContext.current, "create and add")) }
                }
            },
            confirmButton = {}
        )
    }
    editSong?.let { song ->
        var tTitle by remember(song) { mutableStateOf(song.title) }
        var tArtist by remember(song) { mutableStateOf(song.artist) }
        var tAlbum by remember(song) { mutableStateOf(song.album) }
        var tYear by remember(song) { mutableStateOf(song.year) }
        var tTrack by remember(song) { mutableStateOf(song.track.toString()) }
        var tCover by remember(song) { mutableStateOf(song.coverOverride ?: "") }
        var tLrc by remember(song) { mutableStateOf(Meta.get(c, song.metaKey)?.optString("lrc", "") ?: "") }
        var lrcError by remember(song) { mutableStateOf("") }
        val lrcLines = remember(tLrc) { if (tLrc.isBlank()) 0 else Lyrics.lineCount(tLrc) }
        val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                tCover = uri.toString()
            }
        }
        val pickLrc = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    val saved = Lyrics.installFromUri(c, uri, song.metaKey)
                    if (saved != null) {
                        // Save immediately so the lyrics work even if the dialog
                        // is closed with the back button.
                        Meta.set(c, song.metaKey, null, null, null, null, null, null, saved)
                    }
                    withContext(Dispatchers.Main) {
                        if (saved != null) {
                            tLrc = saved
                            lrcError = ""
                            MusicIndex.requestRescan()
                        } else {
                            lrcError = "could not read that file"
                        }
                    }
                }
            }
        }
        AlertDialog(
            onDismissRequest = { editSong = null },
            title = { Text("metadata editor") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tCover.isNotEmpty()) AsyncImage(model = tCover, contentDescription = null, modifier = Modifier.size(96.dp).clip(shape))
                    Button(onClick = { pickCover.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) {
                        Text("pick cover", color = theme.bg)
                    }
                    OutlinedTextField(value = tTitle, onValueChange = { tTitle = it }, label = { Text("title") })
                    OutlinedTextField(value = tArtist, onValueChange = { tArtist = it }, label = { Text("artist") })
                    OutlinedTextField(value = tAlbum, onValueChange = { tAlbum = it }, label = { Text("album") })
                    OutlinedTextField(value = tYear, onValueChange = { tYear = it }, label = { Text("year") })
                    OutlinedTextField(value = tTrack, onValueChange = { tTrack = it }, label = { Text("track #") })
                    Text(
                        "lyrics file: " + if (tLrc.isEmpty()) "none"
                        else "${File(tLrc).name} (${lrcLines} lines)",
                        color = if (lrcError.isEmpty()) theme.subtext else Color(0xFFEF5350),
                        fontSize = 11.sp
                    )
                    if (lrcError.isNotEmpty()) Text(lrcError, color = Color(0xFFEF5350), fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { pickLrc.launch("*/*") }) {
                            Text("[ pick .lrc ]")
                        }
                        if (tLrc.isNotEmpty()) TextButton(onClick = {
                            tLrc = ""
                            lrcError = ""
                            scope.launch(Dispatchers.IO) {
                                Meta.set(c, song.metaKey, null, null, null, null, null, null, "")
                            }
                            MusicIndex.requestRescan()
                        }) { Text("[ clear ]") }
                    }
                    Text("delete", color = Color(0xFFEF5350), modifier = Modifier.clickable {
                        var deleted = false
                        try {
                            deleted = c.contentResolver.delete(song.uri, null, null) > 0
                        } catch (_: Exception) {}
                        if (!deleted) {
                            try { deleted = java.io.File(song.path).delete() } catch (_: Exception) {}
                        }
                        if (!deleted) {
                            try {
                                android.provider.MediaStore.createDeleteRequest(c.contentResolver, listOf(song.uri)).send(c, 0, null)
                            } catch (_: Exception) {}
                        }
                        MusicIndex.requestRescan()
                        editSong = null
                    })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Meta.set(c, song.metaKey, tTitle, tArtist, tAlbum, tYear, tTrack, tCover, tLrc)
                    MusicIndex.requestRescan()
                    editSong = null
                }) { Text(L.tr(LocalContext.current, "ok")) }
            }
        )
    }
}

@Composable
fun AlbumsTab(songs: List<Song>, theme: ThemeColors, shape: RoundedCornerShape) {
    val c = LocalContext.current
    val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
    val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
    val albums = remember(songs) { songs.groupBy { it.album } }
    val detailScroll = rememberScrollState()
    var selected by remember { mutableStateOf<Pair<String, List<Song>>?>(null) }
    val sel = selected
    if (sel != null) BackHandler { selected = null }
    if (sel != null) {
        DetailView(theme, title = sel.first, subtitle = "${sel.second.first().artist} · ${sel.second.size} tracks",
            coverModel = albumArtUri(sel.second.first()), songs = sel.second, onBack = { selected = null }, scrollState = detailScroll)
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            albums.entries.sortedBy { it.key }.forEach { (name, list) ->
                item {
                    Row(Modifier.fillMaxWidth().clickable { selected = name to list }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = albumArtUri(list.first()), contentDescription = null, modifier = Modifier.size(coverSize.dp + 16.dp).clip(shape))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            val year = list.mapNotNull { it.year.toIntOrNull() }.minOrNull()
                            Text(name, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${list.first().artist} · ${year ?: "?"} · ${list.size} tracks · ${humanTime(list.sumOf { it.durationMs })}", color = theme.subtext, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsTab(songs: List<Song>, theme: ThemeColors, shape: RoundedCornerShape) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    val artists = remember(songs) { songs.groupBy { it.artist } }
    val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
    val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
    val detailScroll = rememberScrollState()
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    if (selectedAlbum != null) BackHandler { selectedAlbum = null }
    else if (selectedArtist != null) BackHandler { selectedArtist = null }

    when {
        selectedAlbum != null && selectedArtist != null -> {
            val list = songs.filter { it.artist == selectedArtist && it.album == selectedAlbum }
            DetailView(theme, title = selectedAlbum!!, subtitle = selectedArtist!!,
                coverModel = list.firstOrNull()?.let { albumArtUri(it) }, songs = list,
                onBack = { selectedAlbum = null }, shape = shape, scrollState = detailScroll)
        }
        selectedArtist != null -> {
            val artistName = selectedArtist!!
            val list = songs.filter { it.artist == artistName }
            val byAlbum = list.groupBy { it.album }
            val customImg by Prefs.artistImage(c, artistName).collectAsState(initial = "")
            var showAlbums by remember(artistName) { mutableStateOf(false) }
            val pickImg = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                    scope.launch { Prefs.setArtistImage(c, artistName, uri.toString()) }
                }
            }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(L.tr(LocalContext.current, "← back"), color = theme.accent, modifier = Modifier.clickable { selectedArtist = null }.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val imgModel: Any = if (customImg.isEmpty()) albumArtUri(list.first()) else customImg
                    AsyncImage(model = imgModel,
                        contentDescription = null, modifier = Modifier.size(72.dp).clip(shape).border(LocalBorderWidth.current, theme.border, shape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(artistName, color = theme.text, fontSize = 18.sp)
                        Text("${list.size} songs · ${humanTime(list.sumOf { it.durationMs })} · [change img]", color = theme.subtext, fontSize = 12.sp,
                            modifier = Modifier.clickable { pickImg.launch("image/*") })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (!showAlbums) "[songs]" else " songs ", color = if (!showAlbums) theme.accent else theme.subtext,
                        fontSize = 13.sp, modifier = Modifier.clickable { showAlbums = false }.padding(vertical = 4.dp))
                    Text(if (showAlbums) "[albums]" else " albums ", color = if (showAlbums) theme.accent else theme.subtext,
                        fontSize = 13.sp, modifier = Modifier.clickable { showAlbums = true }.padding(vertical = 4.dp))
                }
                Spacer(Modifier.height(4.dp))
                if (showAlbums) {
                    byAlbum.entries.sortedBy { it.key }.forEach { (album, albumSongs) ->
                        Row(Modifier.fillMaxWidth().clickable { selectedAlbum = album }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = albumArtUri(albumSongs.first()), contentDescription = null, modifier = Modifier.size(coverSize.dp + 8.dp).clip(shape))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                val year = albumSongs.mapNotNull { it.year.toIntOrNull() }.minOrNull()
                                Text(album, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${year ?: "?"} · ${albumSongs.size} tracks · ${humanTime(albumSongs.sumOf { it.durationMs })}", color = theme.subtext, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    val sortedSongs = remember(list) { list.sortedBy { s -> s.track.let { if (it == 0) Int.MAX_VALUE else it } } }
                    sortedSongs.forEachIndexed { i, s ->
                        Row(Modifier.fillMaxWidth().clickable { Playback.play(c, sortedSongs, i) }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = albumArtUri(s), contentDescription = null, modifier = Modifier.size(coverSize.dp).clip(shape))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${s.album} · ${s.year}", color = theme.subtext, fontSize = 11.sp, maxLines = 1)
                            }
                            Text(formatTime(s.durationMs), color = theme.subtext, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        else -> {
            LazyColumn(Modifier.fillMaxSize()) {
                artists.entries.sortedBy { it.key }.forEach { (name, list) ->
                    item {
                        Row(Modifier.fillMaxWidth().clickable { selectedArtist = name }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                            val customImg by Prefs.artistImage(c, name).collectAsState(initial = "")
                            AsyncImage(model = customImg.ifEmpty { albumArtUri(list.first()).toString() }, contentDescription = null,
                                Modifier.size(coverSize.dp + 8.dp).clip(shape).border(LocalBorderWidth.current, theme.border, shape))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(name, color = theme.text)
                                Text("${list.size} songs · ${humanTime(list.sumOf { it.durationMs })}", color = theme.subtext, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailView(theme: ThemeColors, title: String, subtitle: String, coverModel: Any?, songs: List<Song>, onBack: () -> Unit, shape: RoundedCornerShape = RoundedCornerShape(0.dp), scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()) {
    val c = LocalContext.current
    var sortMode by remember { mutableStateOf(0) } // 0 = album order (track), 1 = title A-Z, 2 = year
    val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
    val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
    val sorted = remember(songs, sortMode) {
        when (sortMode) {
            1 -> songs.sortedBy { it.title.lowercase() }
            2 -> songs.sortedBy { it.year.toIntOrNull() ?: Int.MAX_VALUE }
            else -> songs.sortedBy { it.track.let { t -> if (t == 0) Int.MAX_VALUE else t } }
        }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        Text(L.tr(LocalContext.current, "← back"), color = theme.accent, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (coverModel != null) {
                AsyncImage(model = coverModel, contentDescription = null, modifier = Modifier.size(72.dp).clip(shape))
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(title, color = theme.text, fontSize = 18.sp)
                Text(subtitle, color = theme.subtext, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("sort: ${listOf("track/album", "title", "year")[sortMode]}", color = theme.accent, fontSize = 12.sp,
                modifier = Modifier.clickable { sortMode = (sortMode + 1) % 3 }.border(LocalBorderWidth.current, theme.border).padding(6.dp))
        }
        Spacer(Modifier.height(6.dp))
        sorted.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().clickable { Playback.play(c, sorted, i) }.padding(vertical = rowPad.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = albumArtUri(s), contentDescription = null, modifier = Modifier.size(coverSize.dp).clip(shape))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.artist, color = theme.subtext, fontSize = 11.sp, maxLines = 1)
                }
                Text(formatTime(s.durationMs), color = theme.subtext, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun StatsTab(songs: List<Song>, theme: ThemeColors) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var top by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var importText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var topArtists by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    val byPath = remember(songs) { songs.associateBy { it.path } }
    fun songLabel(path: String): String {
        val song = byPath[path]
        return if (song != null) "${song.title} · ${song.artist}"
        else path.substringAfterLast('/').substringBeforeLast('.')
    }
    fun refreshStats() {
        top = Stats.topSongs(c)
        albums = Stats.topAlbums(c)
        total = Stats.total(c)
        topArtists = Stats.topArtists(c)
    }
    LaunchedEffect(Unit) { refreshStats() }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = c.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    Stats.importJson(c, text)
                    refreshStats()
                    showImport = false
                } catch (_: Exception) {}
            }
        }
    }
    var fullView by remember { mutableStateOf<String?>(null) }

    if (fullView != null) BackHandler { fullView = null }

    when (fullView) {
        "songs" -> FullListView("most played songs", top.map { songLabel(it.first) to it.second }, theme) { fullView = null }
        "albums" -> FullListView("top albums", albums, theme) { fullView = null }
        "artists" -> FullListView("top artists", topArtists, theme) { fullView = null }
        else -> Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Panel("total plays: $total", theme, shape = RoundedCornerShape(0.dp)) {
                if (top.isNotEmpty()) Text("top: ${songLabel(top.first().first)}", color = theme.text, fontSize = 13.sp)
                else Text("nothing yet — play something!", color = theme.subtext, fontSize = 12.sp)
            }
            StatCard("most played songs", top.take(5).map { songLabel(it.first) to it.second }, theme) { fullView = "songs" }
            StatCard("top albums", albums.take(5), theme) { fullView = "albums" }
            StatCard("top artists", topArtists.take(5), theme) { fullView = "artists" }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(L.tr(LocalContext.current, "[import stats json]"), color = theme.accent, modifier = Modifier.clickable { showImport = !showImport })
                Text("[import stats file]", color = theme.accent, modifier = Modifier.clickable { importFile.launch(arrayOf("application/json", "text/plain", "*/*")) })
            }
            if (showImport) {
                OutlinedTextField(value = importText, onValueChange = { importText = it }, label = { Text(L.tr(LocalContext.current, "paste json")) },
                    modifier = Modifier.fillMaxWidth().height(160.dp))
                Button(onClick = { Stats.importJson(c, importText); showImport = false }) { Text(L.tr(LocalContext.current, "import")) }
            }
        }
    }
}

@Composable
fun StatCard(title: String, items: List<Pair<String, Int>>, theme: ThemeColors, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().border(LocalBorderWidth.current, theme.border).background(theme.surface).clickable { onClick() }.padding(12.dp)) {
        Text(L.tr(LocalContext.current, title).uppercase(), color = theme.accent, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        if (items.isEmpty()) Text("no data", color = theme.subtext, fontSize = 12.sp)
        items.forEachIndexed { i, (name, count) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text("${i + 1}. $name", color = theme.text, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count", color = theme.subtext, fontSize = 12.sp)
            }
        }
        Text("tap for full list →", color = theme.subtext, fontSize = 10.sp)
    }
}

@Composable
fun FullListView(title: String, items: List<Pair<String, Int>>, theme: ThemeColors, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(L.tr(LocalContext.current, "← back"), color = theme.accent, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
        Text(L.tr(LocalContext.current, title).uppercase(), color = theme.text, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        items.forEachIndexed { i, (name, count) ->
            Row(Modifier.fillMaxWidth().border(LocalBorderWidth.current, theme.border).padding(10.dp)) {
                Text("${i + 1}. $name", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count plays", color = theme.subtext, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun SettingsTab(
    theme: ThemeColors, themeName: String, rounded: Boolean, folders: Set<String>,
    onUpdate: (String?, Boolean?, Set<String>?) -> Unit
) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var allFolders by remember { mutableStateOf(setOf<String>()) }
    val customState by Prefs.custom(c).collectAsState(initial = listOf(null, null, null, null, null, null))
    var customBg by remember(customState) { mutableStateOf(customState.getOrNull(0) ?: "#") }
    var customAccent by remember(customState) { mutableStateOf(customState.getOrNull(4) ?: "#") }
    var customText by remember(customState) { mutableStateOf(customState.getOrNull(2) ?: "#") }
    var customSurface by remember(customState) { mutableStateOf(customState.getOrNull(1) ?: "#") }
    var customSubtext by remember(customState) { mutableStateOf(customState.getOrNull(3) ?: "#") }
    var customBorder by remember(customState) { mutableStateOf(customState.getOrNull(5) ?: "#") }
    val customBgImg by Prefs.customBgImg(c).collectAsState(initial = "")
    val pickBg = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            scope.launch { Prefs.setCustomBgImg(c, uri.toString()) }
        }
    }
    val gradientState by Prefs.gradient(c).collectAsState(initial = Prefs.GradientPrefs("", "", 135))
    var gradStart by remember(gradientState) { mutableStateOf(gradientState.start.ifBlank { customBg }) }
    var gradEnd by remember(gradientState) { mutableStateOf(gradientState.end.ifBlank { customAccent }) }
    var gradAngle by remember(gradientState) { mutableStateOf(gradientState.angle) }
    val gradientOn = gradientState.start.isNotBlank() && gradientState.end.isNotBlank()
    var fontUrl by remember { mutableStateOf("") }
    var fontMsg by remember { mutableStateOf("") }
    var fontTick by remember { mutableStateOf(0) }
    val fontName by Prefs.fontName(c).collectAsState(initial = "DM Mono")
    val downloaded = remember(fontTick) { File(c.filesDir, "fonts").apply { mkdirs() }.listFiles()?.map { it.nameWithoutExtension }?.toSet() ?: emptySet() }
    val pickFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    var display = uri.lastPathSegment ?: "font_${System.currentTimeMillis()}.ttf"
                    try {
                        c.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cur ->
                            if (cur.moveToFirst()) {
                                val idx = cur.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (idx >= 0) display = cur.getString(idx) ?: display
                            }
                        }
                    } catch (_: Exception) {}
                    val base = display.substringAfterLast('/').substringAfterLast('\\')
                        .removeSuffix(".ttf").removeSuffix(".TTF").removeSuffix(".otf").removeSuffix(".OTF")
                        .ifBlank { "font_${System.currentTimeMillis()}" }
                    val target = File(c.filesDir, "fonts/$base.ttf").also { it.parentFile?.mkdirs() }
                    c.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { out -> input.copyTo(out) } }
                    withContext(Dispatchers.Main) {
                        if (target.length() > 0L) {
                            Prefs.setFontName(c, base)
                            fontMsg = "installed $base"
                            fontTick++
                        } else {
                            fontMsg = "font import failed"
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) { fontMsg = "font import failed" }
                }
            }
        }
    }
    LaunchedEffect(Unit) { allFolders = MusicIndex.foldersOfLibrary(c) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel("theme", theme, shape = RoundedCornerShape(0.dp)) {
            LazyRow {
                items(Themes.all.size + 1) { i ->
                    val name = if (i < Themes.all.size) Themes.all[i].name else "Custom"
                    Surface(
                        Modifier.padding(end = 8.dp, bottom = 4.dp).clickable { onUpdate(name, null, null) },
                        color = if (name == themeName) theme.accent else theme.bg,
                        shape = RoundedCornerShape(if (rounded) 8.dp else 0.dp)
                    ) { Text(name, color = if (name == themeName) theme.bg else theme.text, modifier = Modifier.padding(8.dp), fontSize = 11.sp) }
                }
            }
            if (themeName == "Custom") {
                val existing by Prefs.savedCustomThemes(c).collectAsState(initial = "{}")
                val savedNames = remember(existing) {
                    try {
                        val obj = org.json.JSONObject(existing)
                        obj.keys().asSequence().toList()
                    } catch (e: Exception) { emptyList() }
                }
                Row(Modifier.fillMaxWidth()) {
                    Text("color picks", color = theme.subtext, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        scope.launch {
                            Prefs.setCustom(c, customBg, customSurface, customText, customSubtext, customAccent, customBorder)
                        }
                    }) { Text(L.tr(LocalContext.current, "apply")) }
                }
                listOf("bg" to customBg, "surface" to customSurface, "text" to customText, "subtext" to customSubtext, "accent" to customAccent, "border" to customBorder).forEachIndexed { i, pair ->
                    val label = pair.first
                    val state = pair.second
                    Column(Modifier.fillMaxWidth().border(LocalBorderWidth.current, theme.border).padding(8.dp)) {
                        Text(label, color = theme.text, fontSize = 12.sp)
                        ColorSliderPicker(
                            current = state,
                            onPick = { hex ->
                                val bg = if (label == "bg") hex else customBg
                                val surface = if (label == "surface") hex else customSurface
                                val text = if (label == "text") hex else customText
                                val subtext = if (label == "subtext") hex else customSubtext
                                val accent = if (label == "accent") hex else customAccent
                                val border = if (label == "border") hex else customBorder
                                when (label) {
                                    "bg" -> customBg = hex
                                    "surface" -> customSurface = hex
                                    "text" -> customText = hex
                                    "subtext" -> customSubtext = hex
                                    "accent" -> customAccent = hex
                                    else -> customBorder = hex
                                }
                                scope.launch { Prefs.setCustom(c, bg, surface, text, subtext, accent, border) }
                            }
                        )
                        OutlinedTextField(value = state, onValueChange = { v ->
                            val bg = if (label == "bg") v else customBg
                            val surface = if (label == "surface") v else customSurface
                            val text = if (label == "text") v else customText
                            val subtext = if (label == "subtext") v else customSubtext
                            val accent = if (label == "accent") v else customAccent
                            val border = if (label == "border") v else customBorder
                            when (label) {
                                "bg" -> customBg = v
                                "surface" -> customSurface = v
                                "text" -> customText = v
                                "subtext" -> customSubtext = v
                                "accent" -> customAccent = v
                                else -> customBorder = v
                            }
                            scope.launch { Prefs.setCustom(c, bg, surface, text, subtext, accent, border) }
                        }, label = { Text(L.tr(LocalContext.current, "$label #hex")) }, modifier = Modifier.fillMaxWidth())
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("gradient background", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = gradientOn, onCheckedChange = { on ->
                        if (on) {
                            gradStart = customBg
                            gradEnd = customAccent
                            scope.launch { Prefs.setGradient(c, gradStart, gradEnd, gradAngle) }
                        } else {
                            scope.launch { Prefs.setGradient(c, "", "", gradAngle) }
                        }
                    })
                }
                if (gradientOn) {
                    Text("gradient start", color = theme.subtext, fontSize = 11.sp)
                    ColorSliderPicker(current = gradStart, onPick = { hex ->
                        gradStart = hex
                        scope.launch { Prefs.setGradient(c, hex, gradEnd, gradAngle) }
                    })
                    Text("gradient end", color = theme.subtext, fontSize = 11.sp)
                    ColorSliderPicker(current = gradEnd, onPick = { hex ->
                        gradEnd = hex
                        scope.launch { Prefs.setGradient(c, gradStart, hex, gradAngle) }
                    })
                    Text("gradient angle: ${gradAngle}°", color = theme.text, fontSize = 12.sp)
                    Slider(value = gradAngle.toFloat(), onValueChange = { v ->
                        gradAngle = v.toInt().coerceIn(0, 360)
                        scope.launch { Prefs.setGradient(c, gradStart, gradEnd, gradAngle) }
                    }, valueRange = 0f..360f,
                        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                }
                Text("custom theme background image", color = theme.text, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { pickBg.launch("image/*") }) { Text("[ pick bg ]") }
                    if (customBgImg.isNotEmpty()) TextButton(onClick = { scope.launch { Prefs.setCustomBgImg(c, "") } }) { Text("[ clear ]") }
                    Text(if (customBgImg.isEmpty()) "none" else "set", color = theme.subtext, fontSize = 11.sp)
                }
                var newThemeName by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = newThemeName, onValueChange = { newThemeName = it }, label = { Text("save as…") }, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        if (newThemeName.isNotBlank()) {
                            val j = try { org.json.JSONObject(existing) } catch (e: Exception) { org.json.JSONObject() }
                            val savedGradStart = if (gradientOn) gradStart.trimStart('#') else ""
                            val savedGradEnd = if (gradientOn) gradEnd.trimStart('#') else ""
                            j.put(newThemeName, "${customBg.trimStart('#')}|${customSurface.trimStart('#')}|${customText.trimStart('#')}|${customSubtext.trimStart('#')}|${customAccent.trimStart('#')}|${customBorder.trimStart('#')}|$savedGradStart|$savedGradEnd|$gradAngle|${customBgImg}")
                            scope.launch { Prefs.setSavedCustomThemes(c, j.toString()) }
                            newThemeName = ""
                        }
                    }) { Text("[ save ]") }
                }
                if (savedNames.isNotEmpty()) {
                    Text("saved themes:", color = theme.subtext, fontSize = 11.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        savedNames.forEach { name ->
                            var confirmDelete by remember { mutableStateOf(false) }
                            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    Modifier.padding(end = 4.dp, bottom = 4.dp).clickable {
                                        val j = try { org.json.JSONObject(existing) } catch (e: Exception) { org.json.JSONObject() }
                                        val list = j.optString(name, "").split("|", limit = 10)
                                        if (list.size >= 6) {
                                            scope.launch {
                                                Prefs.setCustom(c, "#" + list[0], "#" + list[1], "#" + list[2], "#" + list[3], "#" + list[4], "#" + list[5])
                                                if (list.size >= 10) {
                                                    Prefs.setGradient(
                                                        c,
                                                        if (list[6].isNotBlank()) "#" + list[6] else "",
                                                        if (list[7].isNotBlank()) "#" + list[7] else "",
                                                        list[8].toIntOrNull() ?: 135
                                                    )
                                                    Prefs.setCustomBgImg(c, list[9])
                                                } else {
                                                    Prefs.setGradient(c, "", "", 135)
                                                    Prefs.setCustomBgImg(c, "")
                                                }
                                            }
                                        }
                                    },
                                    color = theme.surface,
                                    shape = RoundedCornerShape(0.dp)
                                ) { Text(name, color = theme.text, modifier = Modifier.padding(8.dp), fontSize = 11.sp) }
                                Text("✕", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { confirmDelete = true }.padding(end = 8.dp, bottom = 4.dp))
                            }
                            if (confirmDelete) {
                                AlertDialog(
                                    onDismissRequest = { confirmDelete = false },
                                    title = { Text("delete theme?") },
                                    text = { Text("Delete saved theme \"$name\"?") },
                                    confirmButton = { TextButton(onClick = {
                                        val j = try { org.json.JSONObject(existing) } catch (e: Exception) { org.json.JSONObject() }
                                        j.remove(name)
                                        scope.launch { Prefs.setSavedCustomThemes(c, j.toString()) }
                                        confirmDelete = false
                                    }) { Text("delete") } },
                                    dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("cancel") } }
                                )
                            }
                        }
                    }
                }
                Button(onClick = { scope.launch { Prefs.setCustom(c, customBg, customSurface, customText, customSubtext, customAccent, customBorder) } }) { Text(L.tr(LocalContext.current, "apply")) }
            }
        }

        Panel("font", theme, shape = RoundedCornerShape(0.dp)) {
            val bundledFonts = listOf("DM Mono", "DM Mono Italic", "JetBrainsMono", "FiraCode", "CascadiaCode", "SpaceMono", "Iosevka", "MapleMono", "MapleMono Italic")
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                bundledFonts.forEach { f ->
                    Text(f, color = if (fontName == f) theme.accent else theme.text,
                        modifier = Modifier.clickable { scope.launch { Prefs.setFontName(c, f) } }.padding(end = 14.dp), fontSize = 13.sp)
                }
                downloaded.filter { it != "dm_mono_regular" && it != "dm_mono_italic" && it != "space_mono" && it != "cascadia_code" && it != "fira_code" && it != "jetbrains_mono" && it != "iosevka" && it != "maple_mono" && it != "maple_mono_italic" }.forEach {
                    Text(it, color = if (fontName == it) theme.accent else theme.text,
                        modifier = Modifier.clickable { scope.launch { Prefs.setFontName(c, it) } }.padding(end = 14.dp), fontSize = 13.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = fontUrl, onValueChange = { fontUrl = it }, label = { Text(L.tr(LocalContext.current, ".ttf url")) }, modifier = Modifier.weight(1f))
                TextButton(onClick = { pickFont.launch(arrayOf("font/*", "application/octet-stream", "*/*")) }) { Text("[ pick .ttf ]") }
                Button(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val conn = URL(fontUrl).openConnection() as HttpURLConnection
                                conn.inputStream.use { input ->
                                    val name = fontUrl.substringAfterLast('/').substringBefore('?')
                                    val file = File(c.filesDir, "fonts/$name").also { it.parentFile?.mkdirs() }
                                    FileOutputStream(file).use { out -> input.copyTo(out) }
                                }
                            }
                            val name = fontUrl.substringAfterLast('/').substringBefore('?').removeSuffix(".ttf")
                            withContext(Dispatchers.Main) {
                                Prefs.setFontName(c, name)
                                fontMsg = "installed $name"
                            }
                        } catch (e: Exception) { fontMsg = "download failed" }
                    }
                }) { Text(L.tr(LocalContext.current, "dl")) }
            }
            if (fontMsg.isNotEmpty()) Text(fontMsg, color = theme.subtext, fontSize = 11.sp)
        }

        Panel("shape", theme, shape = RoundedCornerShape(0.dp)) {
            val cornerSize by Prefs.cornerSize(c).collectAsState(initial = 0)
            val borderWidth by Prefs.borderWidth(c).collectAsState(initial = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (cornerSize > 0) "rounded corners" else "square corners", color = theme.text, modifier = Modifier.weight(1f))
                Switch(checked = cornerSize > 0, onCheckedChange = { on ->
                    onUpdate(null, on, null)
                    scope.launch { Prefs.setCornerSize(c, if (on) 10 else 0) }
                })
            }
            Text("corner radius: ${cornerSize}dp", color = theme.text, fontSize = 13.sp)
            Slider(value = cornerSize.toFloat(), onValueChange = { v -> scope.launch { Prefs.setCornerSize(c, v.toInt().coerceIn(0, 32)) } },
                valueRange = 0f..32f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("border width: ${borderWidth}dp", color = theme.text, fontSize = 13.sp)
            Slider(value = borderWidth.toFloat(), onValueChange = { v -> scope.launch { Prefs.setBorderWidth(c, v.toInt().coerceIn(0, 4)) } },
                valueRange = 0f..4f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
        }

        Panel("navigation", theme, shape = RoundedCornerShape(0.dp)) {
            val navPosition by Prefs.navPosition(c).collectAsState(initial = "top")
            val navNerd by Prefs.navNerd(c).collectAsState(initial = false)
            val navLabelsJson by Prefs.navLabels(c).collectAsState(initial = "{}")
            val navIconsJson by Prefs.navIcons(c).collectAsState(initial = "{}")
            var labels by remember(navLabelsJson) { mutableStateOf(parseStringMap(navLabelsJson)) }
            var icons by remember(navIconsJson) { mutableStateOf(parseStringMap(navIconsJson)) }
            Text("position", color = theme.subtext, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("top", "bottom", "left", "right").forEach { pos ->
                    Text(if (navPosition == pos) "[$pos]" else " $pos ", color = if (navPosition == pos) theme.accent else theme.text,
                        fontSize = 13.sp, modifier = Modifier.clickable { scope.launch { Prefs.setNavPosition(c, pos) } }.padding(vertical = 4.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("nerd font icons instead of text", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = navNerd, onCheckedChange = { scope.launch { Prefs.setNavNerd(c, it) } })
            }
            Text("rename buttons / paste a nerd-font glyph", color = theme.subtext, fontSize = 11.sp)
            TAB_NAMES.forEach { name ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("~/$name", color = theme.subtext, fontSize = 10.sp, modifier = Modifier.width(64.dp))
                    OutlinedTextField(value = labels[name] ?: "", onValueChange = { v ->
                        labels = labels.toMutableMap().apply { if (v.isBlank()) remove(name) else put(name, v) }
                        scope.launch { Prefs.setNavLabels(c, mapToJson(labels)) }
                    }, label = { Text("label") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = icons[name] ?: "", onValueChange = { v ->
                        icons = icons.toMutableMap().apply { if (v.isBlank()) remove(name) else put(name, v) }
                        scope.launch { Prefs.setNavIcons(c, mapToJson(icons)) }
                    }, label = { Text("icon") }, singleLine = true, modifier = Modifier.width(70.dp))
                }
            }
        }

        Panel("transitions", theme, shape = RoundedCornerShape(0.dp)) {
            val currentTransition by Prefs.transition(c).collectAsState(initial = "fade")
            Text("tab transition", color = theme.subtext, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("fade", "cmatrix", "slide_left", "slide_right", "slide_up", "slide_down", "zoom", "none").forEach { kind ->
                    Text(if (currentTransition == kind) "[$kind]" else " $kind ", color = if (currentTransition == kind) theme.accent else theme.text,
                        fontSize = 13.sp, modifier = Modifier.clickable { scope.launch { Prefs.setTransition(c, kind) } }.padding(vertical = 4.dp))
                }
            }
        }

        Panel("now playing card", theme, shape = RoundedCornerShape(0.dp)) {
            val npLabel by Prefs.npLabel(c).collectAsState(initial = "Now playing!")
            val npCover by Prefs.npExpandedCover(c).collectAsState(initial = 180)
            val npQueue by Prefs.npExpandedQueue(c).collectAsState(initial = true)
            val npLyrics by Prefs.npExpandedLyrics(c).collectAsState(initial = true)
            var npLabelField by remember(npLabel) { mutableStateOf(npLabel) }
            OutlinedTextField(value = npLabelField, onValueChange = { v ->
                npLabelField = v
                scope.launch { Prefs.setNpLabel(c, v) }
            }, label = { Text("card title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("expanded cover: ${npCover}dp", color = theme.text, fontSize = 13.sp)
            Slider(value = npCover.toFloat(), onValueChange = { v -> scope.launch { Prefs.setNpExpandedCover(c, v.toInt().coerceIn(80, 280)) } },
                valueRange = 80f..280f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("show queue when expanded", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = npQueue, onCheckedChange = { scope.launch { Prefs.setNpExpandedQueue(c, it) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("show lyrics when expanded", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = npLyrics, onCheckedChange = { scope.launch { Prefs.setNpExpandedLyrics(c, it) } })
            }
        }

        val pauseUnplug by Prefs.pauseUnplug(c).collectAsState(initial = true)
        val updateCheck by Prefs.updateCheck(c).collectAsState(initial = false)
        val playPct by Prefs.playCountPct(c).collectAsState(initial = 20)
        Panel("playback", theme, shape = RoundedCornerShape(0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("pause when headphones unplugged", color = theme.text, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Switch(checked = pauseUnplug, onCheckedChange = { checked ->
                    scope.launch { Prefs.setPauseUnplug(c, checked); Playback.player?.setHandleAudioBecomingNoisy(checked) }
                })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("check for updates on launch", color = theme.text, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Switch(checked = updateCheck, onCheckedChange = { scope.launch { Prefs.setUpdateCheck(c, it) } })
            }
            Column {
                Text("count a play after ${playPct}% of the song", color = theme.text, fontSize = 13.sp)
                Slider(value = playPct.toFloat(),
                    onValueChange = { v -> scope.launch { Prefs.setPlayCountPct(c, v.toInt().coerceIn(1, 99)) } },
                    valueRange = 1f..99f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("skip silence in tracks", color = theme.text, modifier = Modifier.weight(1f), fontSize = 13.sp)
                val skipSilence by Prefs.skipSilence(c).collectAsState(initial = false)
                Switch(checked = skipSilence, onCheckedChange = { scope.launch { Prefs.setSkipSilence(c, it); Playback.player?.setSkipSilenceEnabled(it) } })
            }
            Column {
                val crossfadeMs by Prefs.crossfadeMs(c).collectAsState(initial = 0)
                Text("crossfade (fade-in): ${crossfadeMs}ms", color = theme.text, fontSize = 13.sp)
                Slider(value = crossfadeMs.toFloat(),
                    onValueChange = { v -> scope.launch { Prefs.setCrossfadeMs(c, v.toInt().coerceIn(0, 8000)) } },
                    valueRange = 0f..8000f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("keep screen on", color = theme.text, modifier = Modifier.weight(1f), fontSize = 13.sp)
                val keepScreenOn by Prefs.keepScreenOn(c).collectAsState(initial = true)
                Switch(checked = keepScreenOn, onCheckedChange = { scope.launch { Prefs.setKeepScreenOn(c, it) } })
            }
        }

        Panel("index folders", theme, shape = RoundedCornerShape(0.dp)) {
            allFolders.sorted().forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = folders.contains(f), onCheckedChange = { checked ->
                        val new = folders.toMutableSet()
                        if (checked) new.add(f) else new.remove(f)
                        onUpdate(null, null, new)
                    })
                    Text(f, color = theme.subtext, fontSize = 12.sp)
                }
            }
        }

        Panel("about", theme, shape = RoundedCornerShape(0.dp)) {
            Text("croviera v${LocalContext.current.packageManager.getPackageInfo(LocalContext.current.packageName, 0).versionName}", color = theme.text, fontSize = 13.sp)
            Text(L.tr(LocalContext.current, "github.com/emperornyctos/croviera"), color = theme.accent, fontSize = 12.sp)
            Text(L.tr(LocalContext.current, "vibecoded with love"), color = theme.subtext, fontSize = 11.sp)
        }

        val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
        val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
        val npSize by Prefs.npSize(c).collectAsState(initial = 52)
        val eqVertical by Prefs.eqVertical(c).collectAsState(initial = false)
        val bgAlphaPct by Prefs.bgAlpha(c).collectAsState(initial = 45)
        val uiAlpha by Prefs.uiAlpha(c).collectAsState(initial = 100)
        val fontScale by Prefs.fontScale(c).collectAsState(initial = 1f)
        val lineHeightScale by Prefs.lineHeightScale(c).collectAsState(initial = 0f)
        val backupMsg by remember { mutableStateOf("") }
        var backupText by remember { mutableStateOf("") }
        val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val json = Prefs.exportAll(c)
                        c.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    } catch (_: Exception) {}
                }
            }
        }
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val text = c.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                        Prefs.importAll(c, text)
                        MusicIndex.requestRescan()
                    } catch (_: Exception) {}
                }
            }
        }
        Panel("backup / restore", theme, shape = RoundedCornerShape(0.dp)) {
            Text("export profile, stats, playlists, metadata edits and settings into one file", color = theme.subtext, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { exportLauncher.launch("croviera-backup.json") }) { Text("[ export all ]") }
                TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("[ import ]") }
                TextButton(onClick = { scope.launch { backupText = Prefs.exportAll(c) } }) { Text("[ show json ]") }
            }
            if (backupText.isNotEmpty()) {
                Text(backupText, color = theme.subtext, fontSize = 10.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = { backupText = "" }) { Text("[ close ]") }
            }
            Text("background image opacity: $bgAlphaPct%", color = theme.text, fontSize = 13.sp)
            Slider(value = bgAlphaPct.toFloat(), onValueChange = { v -> scope.launch { Prefs.setBgAlpha(c, v.toInt().coerceIn(0, 100)) } }, valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("ui panel opacity (with bg): $uiAlpha%", color = theme.text, fontSize = 13.sp)
            Slider(value = uiAlpha.toFloat(), onValueChange = { v -> scope.launch { Prefs.setUiAlpha(c, v.toInt().coerceIn(15, 100)) } }, valueRange = 15f..100f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("font size: ${(fontScale * 100).toInt()}%", color = theme.text, fontSize = 13.sp)
            Slider(value = fontScale, onValueChange = { v -> scope.launch { Prefs.setFontScale(c, (v * 20).toInt() / 20f) } }, valueRange = 0.7f..2.0f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            val lhShown = if (lineHeightScale <= 0f) 1f else lineHeightScale
            Text("line height: ${if (lineHeightScale <= 0f) "default" else "${(lineHeightScale * 100).toInt()}%"}", color = theme.text, fontSize = 13.sp)
            Slider(value = lhShown, onValueChange = { v ->
                // snapping to the left end returns to "auto" (0f)
                scope.launch { Prefs.setLineHeightScale(c, if (v <= 0.85f) 0f else (v * 20).toInt() / 20f) }
            }, valueRange = 0.8f..2.5f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("song row padding: ${rowPad}dp", color = theme.text, fontSize = 13.sp)
            Slider(value = rowPad.toFloat(), onValueChange = { v -> scope.launch { Prefs.setRowPad(c, v.toInt().coerceIn(0, 24)) } }, valueRange = 0f..24f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("cover size in lists: ${coverSize}dp", color = theme.text, fontSize = 13.sp)
            Slider(value = coverSize.toFloat(), onValueChange = { v -> scope.launch { Prefs.setCoverSize(c, v.toInt().coerceIn(24, 96)) } }, valueRange = 24f..96f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("now playing cover: ${npSize}dp", color = theme.text, fontSize = 13.sp)
            Slider(value = npSize.toFloat(), onValueChange = { v -> scope.launch { Prefs.setNpSize(c, v.toInt().coerceIn(32, 96)) } }, valueRange = 32f..96f,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))

            val customDisc by Prefs.customDisc(c).collectAsState(initial = "")
            val pickDisc = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    try { c.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                    scope.launch { Prefs.setCustomDisc(c, uri.toString()) }
                }
            }
            Text("custom disc icon: ${if (customDisc.isEmpty()) "none (pixel-art default)" else "custom set"}", color = theme.text, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pickDisc.launch("image/*") }) { Text("[ pick svg/gif/png ]") }
                if (customDisc.isNotEmpty()) TextButton(onClick = { scope.launch { Prefs.setCustomDisc(c, "") } }) { Text("[ clear ]") }
            }

        }

        val lang by Prefs.language(c).collectAsState(initial = "")
        Panel("language", theme, shape = RoundedCornerShape(0.dp)) {
            val languages = listOf(
                "" to "System Default",
                "en" to "English",
                "es" to "Español",
                "fr" to "Français",
                "de" to "Deutsch",
                "ja" to "日本語",
                "zh" to "中文",
                "it" to "Italiano",
                "pt" to "Português",
                "ru" to "Русский",
                "ko" to "한국어",
                "ar" to "العربية"
            )
            Column {
                languages.forEach { (code, name) ->
                    Row(Modifier.fillMaxWidth().clickable { scope.launch { Prefs.setLanguage(c, code) } }.padding(vertical = 8.dp)) {
                        Text(name, color = if (lang == code) theme.accent else theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        if (lang == code) Text("✓", color = theme.accent, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsTab(theme: ThemeColors, shape: RoundedCornerShape) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var names by remember { mutableStateOf(Playlists.names(c)) }
    var openPlaylist by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf<String?>(null) }
    var importText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var coversTick by remember { mutableStateOf(0) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var coverTarget by remember { mutableStateOf<String?>(null) }

    val refresh = { names = Playlists.names(c); coversTick++ }
    val covers = remember(names, coversTick) { names.associateWith { Playlists.cover(c, it) } }

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = coverTarget
        if (uri != null && target != null) {
            scope.launch(Dispatchers.IO) {
                val saved = Playlists.setCoverFromUri(c, target, uri)
                withContext(Dispatchers.Main) { if (saved != null) coversTick++ }
            }
        }
        coverTarget = null
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = c.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    Playlists.importJson(c, text)
                    refresh()
                    showImport = false
                } catch (_: Exception) {}
            }
        }
    }

    if (openPlaylist != null) BackHandler { openPlaylist = null }

    when (val open = openPlaylist) {
        null -> Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Panel(L.tr(c, "your playlists"), theme, shape = RoundedCornerShape(0.dp)) {
                if (names.isEmpty()) Text(L.tr(c, "no playlists yet — make one!"), color = theme.subtext, fontSize = 13.sp)
                names.forEach { name ->
                    val cover = covers[name].orEmpty()
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).clickable { openPlaylist = name }, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(shape).background(theme.surface).border(LocalBorderWidth.current, theme.border, shape), contentAlignment = Alignment.Center) {
                                if (cover.isNotEmpty()) AsyncImage(model = File(cover), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Text("♪", color = theme.subtext, fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(name, color = theme.text, modifier = Modifier.weight(1f))
                        }
                        Text("[rename]", color = theme.subtext, fontSize = 12.sp,
                            modifier = Modifier.clickable { renameTarget = name }.padding(horizontal = 6.dp, vertical = 4.dp))
                        Text(L.tr(c, "[delete]"), color = theme.subtext, fontSize = 12.sp,
                            modifier = Modifier.clickable { Playlists.delete(c, name); refresh() }.padding(horizontal = 6.dp, vertical = 4.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Button(onClick = { showNew = !showNew }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) { Text(L.tr(c, "[ new playlist ]"), color = theme.bg) }
                if (showNew) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(L.tr(c, "name")) })
                    Button(onClick = {
                        val n = newName.trim()
                        if (n.isNotEmpty() && !names.contains(n)) {
                            Playlists.create(c, n); refresh(); newName = ""; showNew = false
                        }
                    }) { Text(L.tr(c, "create")) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(L.tr(c, "[export]"), color = theme.accent, modifier = Modifier.clickable { exportText = Playlists.exportJson(c) })
                Text(L.tr(c, "[import]"), color = theme.accent, modifier = Modifier.clickable { showImport = !showImport })
                Text("[import file]", color = theme.accent, modifier = Modifier.clickable { importFile.launch(arrayOf("application/json", "text/plain", "*/*")) })
            }
            exportText?.let { text ->
                Panel("export (copy this into a file to share)", theme, shape = RoundedCornerShape(0.dp)) {
                    Text(text, color = theme.subtext, fontSize = 10.sp, maxLines = 20, overflow = TextOverflow.Ellipsis)
                }
            }
            if (showImport) {
                OutlinedTextField(value = importText, onValueChange = { importText = it }, label = { Text(L.tr(c, "paste playlist json")) }, modifier = Modifier.fillMaxWidth().height(160.dp))
                Button(onClick = { Playlists.importJson(c, importText); refresh(); showImport = false }) { Text(L.tr(c, "import")) }
            }
        }
        else -> {
            val paths = Playlists.paths(c, open)
            var removeTick by remember(open) { mutableStateOf(0) }
            val fullIndex = remember(removeTick) {
                MusicIndex.songsInFolders(c, emptySet()).associateBy { it.path }
            }
            val entries = remember(paths, fullIndex) {
                paths.mapIndexedNotNull { index, path -> fullIndex[path]?.let { index to it } }
            }
            val songs = remember(entries) { entries.map { it.second } }
            val cover = remember(open, coversTick) { Playlists.cover(c, open) }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(L.tr(c, "← back"), color = theme.accent, modifier = Modifier.clickable { openPlaylist = null; refresh() }.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp).clip(shape).background(theme.surface).border(LocalBorderWidth.current, theme.border, shape), contentAlignment = Alignment.Center) {
                        if (cover.isNotEmpty()) AsyncImage(model = File(cover), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text("♪", color = theme.subtext, fontSize = 26.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(open, color = theme.text, fontSize = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("[rename]", color = theme.accent, fontSize = 12.sp,
                                modifier = Modifier.clickable { renameTarget = open }.padding(vertical = 4.dp))
                            Text("[change cover]", color = theme.accent, fontSize = 12.sp,
                                modifier = Modifier.clickable { coverTarget = open; pickCover.launch(arrayOf("image/*")) }.padding(vertical = 4.dp))
                            if (cover.isNotEmpty()) Text("[remove cover]", color = theme.subtext, fontSize = 12.sp,
                                modifier = Modifier.clickable { Playlists.removeCover(c, open); coversTick++ }.padding(vertical = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (songs.isEmpty()) Text(L.tr(c, "empty — add songs from the songs tab (+ icon)"), color = theme.subtext, fontSize = 12.sp)
                entries.forEachIndexed { row, (pathIndex, s) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).clickable { Playback.play(c, songs, row) }, verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = albumArtUri(s), contentDescription = null, modifier = Modifier.size(40.dp).clip(shape))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, color = theme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${s.artist} · ${s.album}", color = theme.subtext, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                        Text("▲", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable {
                            val target = entries.getOrNull(row - 1)?.first
                            if (target != null) { Playlists.move(c, open, pathIndex, target); removeTick++ }
                        }.padding(horizontal = 8.dp, vertical = 6.dp))
                        Text("▼", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable {
                            val target = entries.getOrNull(row + 1)?.first
                            if (target != null) { Playlists.move(c, open, pathIndex, target); removeTick++ }
                        }.padding(horizontal = 8.dp, vertical = 6.dp))
                        Text("[x]", color = theme.subtext, modifier = Modifier.clickable { Playlists.remove(c, open, pathIndex); removeTick++ }.padding(start = 6.dp, top = 6.dp, bottom = 6.dp))
                    }
                }
            }
        }
    }

    renameTarget?.let { old ->
        var renameText by remember(old) { mutableStateOf(old) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("rename playlist") },
            text = {
                Column {
                    OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text(L.tr(c, "name")) })
                    val trimmed = renameText.trim()
                    if (trimmed.isNotEmpty() && trimmed != old && names.contains(trimmed)) {
                        Text("name already exists", color = theme.subtext, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val new = renameText.trim()
                    if (new.isNotEmpty() && (new == old || !names.contains(new))) {
                        if (Playlists.rename(c, old, new)) {
                            if (openPlaylist == old) openPlaylist = new
                            refresh()
                        }
                        renameTarget = null
                    }
                }) { Text(L.tr(c, "ok")) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("cancel") } }
        )
    }
}

@Composable
fun ColorSliderPicker(current: String, onPick: (String) -> Unit) {
    val initialHsv = remember(current) {
        try {
            val parsed = android.graphics.Color.parseColor(if (current.startsWith("#")) current else "#$current")
            val out = FloatArray(3)
            android.graphics.Color.colorToHSV(parsed, out)
            out
        } catch (e: Exception) { FloatArray(3) }
    }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var sat by remember { mutableStateOf(initialHsv[1]) }
    var value by remember { mutableStateOf(initialHsv[2]) }
    LaunchedEffect(current) {
        // keep the slider synced when hex is typed manually
        hue = initialHsv[0]; sat = initialHsv[1]; value = initialHsv[2]
    }
    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    val hex = String.format("#%02X%02X%02X", (rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(Color(rgb)).border(LocalBorderWidth.current, Color.Gray))
            Spacer(Modifier.width(8.dp))
            Text(hex, color = Color.Gray, fontSize = 12.sp)
        }
        Text("hue", fontSize = 10.sp, color = Color.Gray)
        Slider(value = hue, onValueChange = { hue = it; onPick(stringColor(hue, sat, value)) }, valueRange = 0f..360f)
        Text("saturation", fontSize = 10.sp, color = Color.Gray)
        Slider(value = sat, onValueChange = { sat = it; onPick(stringColor(hue, sat, value)) }, valueRange = 0f..1f)
        Text("value", fontSize = 10.sp, color = Color.Gray)
        Slider(value = value, onValueChange = { value = it; onPick(stringColor(hue, sat, value)) }, valueRange = 0f..1f)
    }
}

private fun stringColor(hue: Float, sat: Float, value: Float): String {
    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    return String.format("#%02X%02X%02X", (rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
}

@Composable
fun OnboardingScreen(theme: ThemeColors) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var welcome by remember { mutableStateOf("Welcome back.") }
    var name by remember { mutableStateOf("listener") }
    var foldersOn by remember { mutableStateOf(setOf<String>()) }
    var allFolders by remember { mutableStateOf(setOf<String>()) }
    var themeName by remember { mutableStateOf("Croviera") }
    var rounded by remember { mutableStateOf(false) }
    var fontName by remember { mutableStateOf("DM Mono") }
    LaunchedEffect(Unit) { allFolders = MusicIndex.foldersOfLibrary(c) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("welcome to croviera", color = theme.text, fontSize = 24.sp, modifier = Modifier.fillMaxWidth())
        Text("you can change all of this later in ~/settings", color = theme.subtext, fontSize = 12.sp)

        Panel("profile", theme, shape = RoundedCornerShape(0.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("your name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = welcome, onValueChange = { welcome = it }, label = { Text("welcome message") }, modifier = Modifier.fillMaxWidth())
        }
        Panel("theme", theme, shape = RoundedCornerShape(0.dp)) {
            LazyRow {
                items(Themes.all.size) { i ->
                    val t = Themes.all[i]
                    Surface(Modifier.padding(end = 8.dp).clickable { themeName = t.name }, color = if (themeName == t.name) theme.accent else theme.bg) {
                        Text(t.name, color = if (themeName == t.name) theme.bg else theme.text, modifier = Modifier.padding(8.dp), fontSize = 11.sp)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("rounded corners", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = rounded, onCheckedChange = { rounded = it })
            }
        }
        Panel("font", theme, shape = RoundedCornerShape(0.dp)) {
            listOf("DM Mono", "JetBrainsMono", "FiraCode", "CascadiaCode", "SpaceMono").forEach {
                Text(it, color = if (fontName == it) theme.accent else theme.subtext,
                    modifier = Modifier.clickable { fontName = it }.padding(vertical = 3.dp))
            }
        }
        Panel("index your music folders", theme, shape = RoundedCornerShape(0.dp)) {
            allFolders.sorted().forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = foldersOn.contains(f), onCheckedChange = { checked ->
                        foldersOn = if (checked) foldersOn + f else foldersOn - f
                    })
                    Text(f, color = theme.subtext, fontSize = 12.sp)
                }
            }
        }
        Button(onClick = {
            scope.launch {
                Prefs.setProfileName(c, name)
                Prefs.setWelcome(c, welcome)
                Prefs.setTheme(c, themeName)
                Prefs.setRounded(c, rounded)
                Prefs.setCornerSize(c, if (rounded) 10 else 0)
                Prefs.setFontName(c, fontName)
                Prefs.setFolders(c, foldersOn)
                Prefs.setOnboarded(c, true)
            }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) { Text("[ finish ]", color = theme.bg) }
    }
}