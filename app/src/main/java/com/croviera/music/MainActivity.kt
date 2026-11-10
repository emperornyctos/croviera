package com.croviera.music

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlin.math.max
import kotlin.math.roundToInt
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

/** How every settings/home panel is painted (square image + colored drop shadow). */
data class PanelStyle(
    val image: String = "",
    val imageAlpha: Float = 0.35f,
    val shadowEnabled: Boolean = false,
    val shadowColor: Color = Color(0x8C8A1F2D),
    val shadowOffset: Dp = 4.dp,
)

val LocalPanelStyle = androidx.compose.runtime.staticCompositionLocalOf { PanelStyle() }

/** A hard, offset, colorable drop shadow. Works on every API (no RenderNode tricks). */
fun Modifier.panelShadow(shape: RoundedCornerShape, style: PanelStyle): Modifier {
    if (!style.shadowEnabled || style.shadowColor.alpha <= 0f) return this
    return drawBehind {
        val offset = style.shadowOffset.toPx()
        if (offset <= 0f) return@drawBehind
        val r = shape.topStart.toPx(size, this)
        val radius = CornerRadius(r, r)
        // two layers: a softer one further down, then the crisp offset shape
        drawRoundRect(
            color = style.shadowColor.copy(alpha = style.shadowColor.alpha * 0.35f),
            topLeft = Offset(0f, offset * 1.9f),
            size = size,
            cornerRadius = radius
        )
        drawRoundRect(
            color = style.shadowColor,
            topLeft = Offset(0f, offset),
            size = size,
            cornerRadius = radius
        )
    }
}

@Composable
fun PanelSurface(
    theme: ThemeColors,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = LocalPanelStyle.current
    Box(
        modifier
            .panelShadow(shape, style)
            .clip(shape)
            .background(theme.surface.copy(alpha = LocalUiAlpha.current), shape)
            .border(LocalBorderWidth.current, theme.border, shape)
    ) {
        if (style.image.isNotEmpty()) {
            AsyncImage(
                model = style.image,
                contentDescription = null,
                modifier = Modifier.matchParentSize().alpha(LocalUiAlpha.current),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.matchParentSize().background(theme.surface.copy(alpha = style.imageAlpha)))
        }
        content()
    }
}

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

data class NavConfig(
    val position: String = "top",
    val labels: Map<String, String> = emptyMap(),
    val icons: Map<String, String> = emptyMap(),
    val nerd: Boolean = false,
    val images: Map<String, String> = emptyMap(),
    val imageSize: Int = 28,
    val itemWidth: Int = 0,
    val itemHeight: Int = 0,
    val spacing: Int = 2,
    val padding: Int = 4,
    val thickness: Int = 0,
    val bgAlpha: Int = 94,
    val bgColor: String = "",
    val imageFit: String = "crop",
    val textSize: Int = 14,
    val nerdSize: Int = 20,
    val selectedAlpha: Int = 14,
)

/**
 * The bar grows to fit configured item/image sizes, then stops at a sane size
 * so it still works on small phones and does not eat the whole screen.
 */
fun navExtent(config: NavConfig, vertical: Boolean): Dp {
    val pad = config.padding.dp
    val image = config.imageSize.dp
    val itemW = if (config.itemWidth > 0) config.itemWidth.dp else 0.dp
    val itemH = if (config.itemHeight > 0) config.itemHeight.dp else 0.dp
    val content = if (vertical) maxOf(itemW, image) + pad * 2 + 2.dp
    else maxOf(itemH, image) + pad * 2 + 2.dp
    val base = if (config.thickness > 0) config.thickness.dp else if (vertical) 92.dp else 40.dp
    val cap = if (vertical) 220.dp else 180.dp
    return maxOf(base, content).coerceAtMost(cap)
}

@Composable
fun NavBar(
    theme: ThemeColors,
    shape: RoundedCornerShape,
    config: NavConfig,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vertical = config.position == "left" || config.position == "right"
    val nerdFont = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.symbols_nerd_mono))
    val extent = navExtent(config, vertical)
    val pad = config.padding.dp
    val imageSize = config.imageSize.dp.coerceAtMost((extent - pad * 2 - 8.dp).coerceAtLeast(12.dp))
    val bg = parseHexColor(config.bgColor, theme.bg).copy(alpha = (config.bgAlpha / 100f).coerceIn(0f, 1f))
    val item: @Composable (Int) -> Unit = { i ->
        val name = TAB_NAMES[i]
        val isSelected = selected == i
        val image = config.images[name].orEmpty()
        val base = Modifier
            .clip(shape)
            .background(if (isSelected) theme.accent.copy(alpha = (config.selectedAlpha / 100f).coerceIn(0f, 1f)) else Color.Transparent, shape)
            .clickable { onSelect(i) }
        val sized = if (vertical) {
            base
                .then(if (config.itemWidth > 0) Modifier.width(config.itemWidth.dp.coerceAtMost((extent - pad * 2).coerceAtLeast(24.dp))) else Modifier.fillMaxWidth())
                .then(if (config.itemHeight > 0) Modifier.height(config.itemHeight.dp.coerceAtMost((extent - pad * 2).coerceAtLeast(24.dp))) else Modifier.heightIn(min = 28.dp))
        } else {
            base
                .then(if (config.itemWidth > 0) Modifier.width(config.itemWidth.dp.coerceAtMost(220.dp)) else Modifier)
                .then(if (config.itemHeight > 0) Modifier.height(config.itemHeight.dp.coerceAtMost((extent - pad * 2).coerceAtLeast(24.dp))) else Modifier.heightIn(min = 28.dp))
        }
        Box(sized.padding(horizontal = 6.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            if (image.isNotEmpty()) {
                AsyncImage(
                    model = image,
                    contentDescription = name,
                    modifier = Modifier.size(imageSize).clip(shape),
                    contentScale = when (config.imageFit) {
                        "fit" -> ContentScale.Fit
                        "fill" -> ContentScale.FillBounds
                        else -> ContentScale.Crop
                    }
                )
            } else {
                val text = if (config.nerd) config.icons[name] ?: DEFAULT_NAV_ICONS.getValue(name)
                else config.labels[name] ?: DEFAULT_NAV_LABELS.getValue(name)
                Text(
                    text = text,
                    color = if (isSelected) theme.accent else theme.subtext,
                    fontSize = if (config.nerd) config.nerdSize.sp else config.textSize.sp,
                    fontFamily = if (config.nerd) nerdFont else null,
                    maxLines = 1
                )
            }
        }
    }
    if (vertical) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .width(extent)
                .background(bg, shape)
                .border(LocalBorderWidth.current, theme.border, shape)
                .padding(pad),
            verticalArrangement = Arrangement.spacedBy(config.spacing.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { TAB_NAMES.indices.forEach { item(it) } }
    } else {
        Row(
            modifier = modifier
                .horizontalScroll(rememberScrollState())
                .heightIn(min = extent)
                .background(bg, shape)
                .border(LocalBorderWidth.current, theme.border, shape)
                .padding(pad),
            horizontalArrangement = Arrangement.spacedBy(config.spacing.dp),
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
    val navImagesJson by Prefs.navButtonImages(c).collectAsState(initial = s.navButtonImages)
    val navImageSize by Prefs.navImageSize(c).collectAsState(initial = s.navImageSize)
    val navItemWidth by Prefs.navItemWidth(c).collectAsState(initial = s.navItemWidth)
    val navItemHeight by Prefs.navItemHeight(c).collectAsState(initial = s.navItemHeight)
    val navItemSpacing by Prefs.navItemSpacing(c).collectAsState(initial = s.navItemSpacing)
    val navPadding by Prefs.navPadding(c).collectAsState(initial = s.navPadding)
    val navBarThickness by Prefs.navBarThickness(c).collectAsState(initial = s.navBarThickness)
    val navBgAlpha by Prefs.navBgAlpha(c).collectAsState(initial = s.navBgAlpha)
    val navBgColor by Prefs.navBgColor(c).collectAsState(initial = s.navBgColor)
    val navImageFit by Prefs.navImageFit(c).collectAsState(initial = s.navImageFit)
    val navTextSize by Prefs.navTextSize(c).collectAsState(initial = s.navTextSize)
    val navNerdSize by Prefs.navNerdSize(c).collectAsState(initial = s.navNerdSize)
    val navSelectedAlpha by Prefs.navSelectedAlpha(c).collectAsState(initial = s.navSelectedAlpha)
    val npCoverRadius by Prefs.npCoverRadius(c).collectAsState(initial = s.npCoverRadius)
    val npCoverSize by Prefs.npCoverSize(c).collectAsState(initial = s.npCoverSize)
    val npTitleSize by Prefs.npTitleSize(c).collectAsState(initial = s.npTitleSize)
    val npArtistSize by Prefs.npArtistSize(c).collectAsState(initial = s.npArtistSize)
    val npDim by Prefs.npDim(c).collectAsState(initial = s.npDim)
    val npBgFit by Prefs.npBgFit(c).collectAsState(initial = s.npBgFit)
    val npButtonRadius by Prefs.npButtonRadius(c).collectAsState(initial = s.npButtonRadius)
    val keepScreenOn by Prefs.keepScreenOn(c).collectAsState(initial = s.keepScreenOn)
    val transition by Prefs.transition(c).collectAsState(initial = s.transition)
    val npLabel by Prefs.npLabel(c).collectAsState(initial = s.npLabel)
    val npExpandedQueue by Prefs.npExpandedQueue(c).collectAsState(initial = s.npExpandedQueue)
    val npExpandedLyrics by Prefs.npExpandedLyrics(c).collectAsState(initial = s.npExpandedLyrics)
    val squareImg by Prefs.customSquareImg(c).collectAsState(initial = s.squareImg)
    val squareImgAlpha by Prefs.squareImgAlpha(c).collectAsState(initial = s.squareImgAlpha)
    val panelShadowOn by Prefs.panelShadow(c).collectAsState(initial = s.panelShadow)
    val panelShadowColor by Prefs.panelShadowColor(c).collectAsState(initial = s.panelShadowColor)
    val panelShadowOffset by Prefs.panelShadowOffset(c).collectAsState(initial = s.panelShadowOffset)
    val panelShadowAlpha by Prefs.panelShadowAlpha(c).collectAsState(initial = s.panelShadowAlpha)

    // A background image belongs to the custom theme only; built-in themes stay clean.
    val effectiveBgImg = if (themeName == "Custom") bgImg else ""
    val uiAlphaF = if (effectiveBgImg.isNotEmpty()) (uiAlpha / 100f).coerceIn(0.15f, 1f) else 1f
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, fontScale) {
        Density(baseDensity.density, baseDensity.fontScale * fontScale)
    }
    val lineHeightTextStyle = if (lineHeightScale > 0f) TextStyle(lineHeight = (14 * lineHeightScale).sp) else TextStyle()
    val borderDp = borderWidth.dp

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

    val effectiveSquareImg = if (themeName == "Custom") squareImg else ""
    val panelStyle = PanelStyle(
        image = effectiveSquareImg,
        imageAlpha = (squareImgAlpha / 100f).coerceIn(0f, 1f),
        // Shadows are a Custom-theme feature. Built-in themes stay flat even
        // if a previously saved custom shadow is still in DataStore.
        shadowEnabled = themeName == "Custom" && panelShadowOn,
        shadowColor = parseHexColor(panelShadowColor, theme.accent).copy(alpha = (panelShadowAlpha / 100f).coerceIn(0f, 1f)),
        shadowOffset = panelShadowOffset.dp,
    )

    @Composable
    fun Wrap(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalUiAlpha provides uiAlphaF,
            LocalBorderWidth provides borderDp,
            LocalPanelStyle provides panelStyle
        ) { content() }
    }

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
    val view = LocalView.current
    LaunchedEffect(keepScreenOn) { view.keepScreenOn = keepScreenOn }

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
            var showFullscreen by remember { mutableStateOf(false) }
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
            LaunchedEffect(folders, rescan) {
                val loaded = withContext(Dispatchers.IO) { MusicIndex.songsInFolders(c, folders) }
                withContext(Dispatchers.IO) { Stats.migrate(c, loaded) }
                songs = loaded
            }

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
            val navImages = remember(navImagesJson) { parseStringMap(navImagesJson) }
            val navConfig = NavConfig(
                position = navPosition,
                labels = navLabels,
                icons = navIcons,
                nerd = navNerd,
                images = navImages,
                imageSize = navImageSize,
                itemWidth = navItemWidth,
                itemHeight = navItemHeight,
                spacing = navItemSpacing,
                padding = navPadding,
                thickness = navBarThickness,
                // Bar color/opacity and the selected-item wash are Custom-theme
                // styling. Built-in themes always use their own surface color.
                bgAlpha = if (themeName == "Custom") navBgAlpha else 94,
                bgColor = if (themeName == "Custom") navBgColor else "",
                imageFit = navImageFit,
                textSize = navTextSize,
                nerdSize = navNerdSize,
                selectedAlpha = if (themeName == "Custom") navSelectedAlpha else 14,
            )
            val navVertical = navPosition == "left" || navPosition == "right"
            val navSpace = navExtent(navConfig, navVertical) + 8.dp

            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(
                        top = if (navPosition == "top") navSpace else 0.dp,
                        bottom = if (navPosition == "bottom") navSpace else 0.dp,
                        start = if (navPosition == "left") navSpace else 0.dp,
                        end = if (navPosition == "right") navSpace else 0.dp
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
                        PanelSurface(
                            theme, shape,
                            Modifier.fillMaxWidth()
                                .heightIn(min = 140.dp)
                                .clickable { showFullscreen = true }
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(npLabel, color = theme.accent, fontSize = 12.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("[ expand ]", color = theme.subtext, fontSize = 10.sp)
                                        Spacer(Modifier.width(10.dp))
                                        val favSong by Prefs.favSong(c).collectAsState(initial = "")
                                        val curSongTitle = Playback.queue.value.getOrNull(Playback.player?.currentMediaItemIndex ?: -1)?.title ?: ""
                                        Text("★", color = if (favSong == curSongTitle && curSongTitle.isNotEmpty()) theme.accent else theme.subtext, fontSize = 28.sp,
                                            modifier = Modifier.clickable { if (curSongTitle.isNotEmpty()) scope.launch { Prefs.setFavSong(c, if (favSong == curSongTitle) "" else curSongTitle) } })
                                    }
                                }
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
                                Text("[ tap anywhere for fullscreen ]",
                                    color = theme.subtext.copy(alpha = 0.7f), fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                val navMod = when (navPosition) {
                    "bottom" -> Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(4.dp)
                    "left" -> Modifier.align(Alignment.CenterStart).fillMaxHeight().padding(4.dp)
                    "right" -> Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(4.dp)
                    else -> Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(4.dp)
                }
                NavBar(theme, shape, navConfig, tab, { tab = it }, navMod)

                if (showFullscreen) {
                    FullscreenPlayer(
                        theme = theme,
                        shape = shape,
                        bgBrush = bgBrush,
                        bgImg = effectiveBgImg,
                        bgImgAlpha = bgAlphaPct / 100f,
                        customTheme = themeName == "Custom",
                        defaultShowQueue = npExpandedQueue,
                        defaultShowLyrics = npExpandedLyrics,
                        onClose = { showFullscreen = false }
                    )
                }
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

data class NpSticker(
    val image: String,
    val x: Float,
    val y: Float,
    val size: Int,
    val alpha: Float,
    val rotation: Float,
)

fun parseStickers(json: String): List<NpSticker> = try {
    val arr = org.json.JSONArray(json)
    (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        val image = o.optString("image", "")
        if (image.isBlank()) null else NpSticker(
            image = image,
            x = o.optDouble("x", 0.5).toFloat().coerceIn(0f, 1f),
            y = o.optDouble("y", 0.5).toFloat().coerceIn(0f, 1f),
            size = o.optInt("size", 72).coerceIn(8, 512),
            alpha = o.optDouble("alpha", 0.8).toFloat().coerceIn(0f, 1f),
            rotation = o.optDouble("rotation", 0.0).toFloat().coerceIn(-360f, 360f),
        )
    }
} catch (_: Exception) {
    emptyList()
}

fun stickersToJson(list: List<NpSticker>): String {
    val arr = org.json.JSONArray()
    list.forEach { st ->
        arr.put(org.json.JSONObject().apply {
            put("image", st.image)
            put("x", st.x.toDouble())
            put("y", st.y.toDouble())
            put("size", st.size)
            put("alpha", st.alpha.toDouble())
            put("rotation", st.rotation.toDouble())
        })
    }
    return arr.toString()
}

@Composable
fun FullscreenPlayer(
    theme: ThemeColors,
    shape: RoundedCornerShape,
    bgBrush: Brush,
    bgImg: String,
    bgImgAlpha: Float,
    customTheme: Boolean,
    defaultShowQueue: Boolean,
    defaultShowLyrics: Boolean,
    onClose: () -> Unit,
) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler { onClose() }
    val queue by Playback.queue.collectAsState()
    val title by Playback.title.collectAsState()
    val artist by Playback.artist.collectAsState()
    val playing by Playback.playing.collectAsState()
    val shuffleOn by Prefs.shuffle(c).collectAsState(initial = false)
    val repeatOn by Prefs.repeat(c).collectAsState(initial = 0)
    val favSong by Prefs.favSong(c).collectAsState(initial = "")
    val npBgImg by Prefs.npBgImg(c).collectAsState(initial = "")
    val npBgAlpha by Prefs.npBgAlpha(c).collectAsState(initial = 45)
    val npBgColor by Prefs.npBgColor(c).collectAsState(initial = "")
    val npTextColor by Prefs.npTextColor(c).collectAsState(initial = "")
    val npSubtextColor by Prefs.npSubtextColor(c).collectAsState(initial = "")
    val npAccentColor by Prefs.npAccentColor(c).collectAsState(initial = "")
    val npShowCover by Prefs.npShowCover(c).collectAsState(initial = true)
    val npShowProgress by Prefs.npShowProgress(c).collectAsState(initial = true)
    val npShowTitle by Prefs.npShowTitle(c).collectAsState(initial = true)
    val npShowArtist by Prefs.npShowArtist(c).collectAsState(initial = true)
    val npShowInfo by Prefs.npShowInfo(c).collectAsState(initial = true)
    val npShowControls by Prefs.npShowControls(c).collectAsState(initial = true)
    val npShowActions by Prefs.npShowActions(c).collectAsState(initial = true)
    val npShowQueue by Prefs.npShowQueue(c).collectAsState(initial = true)
    val npShowLyrics by Prefs.npShowLyrics(c).collectAsState(initial = true)
    val npMaxWidth by Prefs.npMaxWidth(c).collectAsState(initial = 420)
    val npStickers by Prefs.npStickers(c).collectAsState(initial = "[]")
    val npCoverRadius by Prefs.npCoverRadius(c).collectAsState(initial = 0)
    val npCoverSize by Prefs.npCoverSize(c).collectAsState(initial = 0)
    val npTitleSize by Prefs.npTitleSize(c).collectAsState(initial = 24)
    val npArtistSize by Prefs.npArtistSize(c).collectAsState(initial = 15)
    val npDim by Prefs.npDim(c).collectAsState(initial = 0)
    val npBgFit by Prefs.npBgFit(c).collectAsState(initial = "crop")
    val npButtonRadius by Prefs.npButtonRadius(c).collectAsState(initial = 18)
    val index = Playback.player?.currentMediaItemIndex ?: 0
    val cur = queue.getOrNull(index)
    var pos by remember { mutableStateOf(0f) }
    var scrubbing by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(defaultShowQueue) }
    var showLyrics by remember { mutableStateOf(defaultShowLyrics) }
    LaunchedEffect(playing, cur?.metaKey, scrubbing) {
        if (!scrubbing) {
            while (true) {
                pos = Playback.player?.currentPosition?.toFloat() ?: 0f
                if (!playing) break
                delay(400)
            }
        }
    }
    val dur = (Playback.player?.duration?.toFloat() ?: cur?.durationMs?.toFloat() ?: 1f).coerceAtLeast(1f)
    val curTitle = cur?.title ?: title
    val ext = cur?.path?.substringAfterLast('.', "")?.uppercase().orEmpty()
    val info = listOfNotNull(
        ext.takeIf { it.isNotBlank() },
        cur?.year?.takeIf { it.isNotBlank() },
        formatTime(cur?.durationMs ?: dur.toLong())
    ).joinToString(" · ")
    val queueNext = queue.drop(index + 1)

    val npTheme = if (customTheme) theme.copy(
        text = parseHexColor(npTextColor, theme.text),
        subtext = parseHexColor(npSubtextColor, theme.subtext),
        accent = parseHexColor(npAccentColor, theme.accent),
    ) else theme
    val screenBg: Brush = if (customTheme && npBgColor.isNotBlank()) {
        SolidColor(parseHexColor(npBgColor, theme.bg))
    } else bgBrush
    val screenImg = if (customTheme && npBgImg.isNotBlank()) npBgImg else bgImg
    val screenImgAlpha = if (customTheme && npBgImg.isNotBlank()) {
        (npBgAlpha / 100f).coerceIn(0f, 1f)
    } else bgImgAlpha
    val maxW = (if (customTheme) npMaxWidth else 420).dp.coerceIn(220.dp, 720.dp)
    val stickers = remember(npStickers, customTheme) { if (customTheme) parseStickers(npStickers) else emptyList() }
    val showCover = !customTheme || npShowCover
    val showProgress = !customTheme || npShowProgress
    val showTitle = !customTheme || npShowTitle
    val showArtist = !customTheme || npShowArtist
    val showInfo = !customTheme || npShowInfo
    val showControls = !customTheme || npShowControls
    val showActions = !customTheme || npShowActions
    val showQueueSection = !customTheme || npShowQueue
    val showLyricsSection = !customTheme || npShowLyrics
    val coverMax = if (customTheme && npCoverSize > 0) npCoverSize.dp.coerceAtMost(maxW) else maxW
    val coverShape = if (customTheme && npCoverRadius > 0) RoundedCornerShape(npCoverRadius.dp) else shape
    val bgScale = when {
        !customTheme -> ContentScale.Crop
        npBgFit == "fit" -> ContentScale.Fit
        npBgFit == "fill" -> ContentScale.FillBounds
        else -> ContentScale.Crop
    }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(screenBg)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (screenImg.isNotEmpty()) {
            AsyncImage(
                model = screenImg,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(screenImgAlpha),
                contentScale = bgScale
            )
        }
        if (customTheme && npDim > 0) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (npDim / 100f).coerceIn(0f, 1f))))
        }
        stickers.forEach { st ->
            val sizeDp = st.size.dp
            AsyncImage(
                model = st.image,
                contentDescription = null,
                modifier = Modifier
                    .offset(x = maxWidth * st.x - sizeDp / 2, y = maxHeight * st.y - sizeDp / 2)
                    .size(sizeDp)
                    .graphicsLayer {
                        alpha = st.alpha
                        rotationZ = st.rotation
                    },
                contentScale = ContentScale.Fit
            )
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("now playing", color = npTheme.subtext, fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                Text("[ close ]", color = npTheme.accent, fontSize = 13.sp, modifier = Modifier.clickable { onClose() })
            }

            if (showCover) {
                PanelSurface(npTheme, coverShape, Modifier.widthIn(max = coverMax).fillMaxWidth()) {
                    AsyncImage(
                        model = cur?.let { albumArtUri(it) } ?: "",
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (showProgress) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(pos.toLong()), color = npTheme.subtext, fontSize = 11.sp)
                    Slider(
                        value = pos.coerceIn(0f, dur),
                        valueRange = 0f..dur,
                        onValueChange = { scrubbing = true; pos = it },
                        onValueChangeFinished = {
                            Playback.player?.seekTo(pos.toLong())
                            scrubbing = false
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = npTheme.accent,
                            activeTrackColor = npTheme.accent,
                            inactiveTrackColor = npTheme.border
                        )
                    )
                    Text(formatTime(dur.toLong()), color = npTheme.subtext, fontSize = 11.sp)
                }
            }

            if (showTitle) {
                Text(curTitle, color = npTheme.text, fontSize = (if (customTheme) npTitleSize else 24).sp, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            }
            if (showArtist) Text(artist, color = npTheme.subtext, fontSize = (if (customTheme) npArtistSize else 15).sp, textAlign = TextAlign.Center)
            if (showInfo) Text(info, color = npTheme.subtext.copy(alpha = 0.8f), fontSize = 11.sp, textAlign = TextAlign.Center)

            if (showControls) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(48.dp).clickable {
                        val next = (repeatOn + 1) % 3
                        scope.launch {
                            Prefs.setRepeat(c, next)
                            Playback.player?.repeatMode = when (next) {
                                1 -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                2 -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                            }
                        }
                    }, contentAlignment = Alignment.Center) {
                        Icon(
                            if (repeatOn == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            "repeat",
                            tint = if (repeatOn > 0) npTheme.accent else npTheme.subtext,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Box(Modifier.size(48.dp).clickable { Playback.player?.seekToPreviousMediaItem() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SkipPrevious, "prev", tint = npTheme.text, modifier = Modifier.size(30.dp))
                    }
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(if (customTheme) npButtonRadius.dp else 18.dp)).background(npTheme.accent).clickable {
                            Playback.player?.let { if (it.isPlaying) it.pause() else it.play() }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "playpause",
                            tint = npTheme.bg, modifier = Modifier.size(34.dp))
                    }
                    Box(Modifier.size(48.dp).clickable { Playback.player?.seekToNextMediaItem() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SkipNext, "next", tint = npTheme.text, modifier = Modifier.size(30.dp))
                    }
                    Box(Modifier.size(48.dp).clickable {
                        scope.launch { Prefs.setShuffle(c, !shuffleOn); Playback.player?.shuffleModeEnabled = !shuffleOn }
                    }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Shuffle, "shuffle", tint = if (shuffleOn) npTheme.accent else npTheme.subtext, modifier = Modifier.size(26.dp))
                    }
                }
            }

            if (showActions && (showLyricsSection || showQueueSection || curTitle.isNotEmpty())) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (showLyricsSection) {
                        Text(if (showLyrics) "[ lyrics ]" else " lyrics ", color = if (showLyrics) npTheme.accent else npTheme.subtext,
                            fontSize = 13.sp, modifier = Modifier.clickable { showLyrics = !showLyrics })
                    }
                    if (showQueueSection) {
                        Text(if (showQueue) "[ queue ]" else " queue ", color = if (showQueue) npTheme.accent else npTheme.subtext,
                            fontSize = 13.sp, modifier = Modifier.clickable { showQueue = !showQueue })
                    }
                    Text(
                        if (favSong == curTitle && curTitle.isNotEmpty()) "★ favorite" else "☆ favorite",
                        color = if (favSong == curTitle && curTitle.isNotEmpty()) npTheme.accent else npTheme.subtext,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            if (curTitle.isNotEmpty()) scope.launch {
                                Prefs.setFavSong(c, if (favSong == curTitle) "" else curTitle)
                            }
                        }
                    )
                }
            }

            if (showQueueSection && showQueue && queueNext.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().widthIn(max = maxW), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("up next", color = npTheme.subtext, fontSize = 10.sp, letterSpacing = 1.sp)
                    queueNext.take(20).forEachIndexed { i, s ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                Playback.player?.seekTo(index + 1 + i, 0)
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(model = albumArtUri(s), contentDescription = null,
                                modifier = Modifier.size(30.dp).clip(shape))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, color = npTheme.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(s.artist, color = npTheme.subtext, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(formatTime(s.durationMs), color = npTheme.subtext, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (showLyricsSection && showLyrics) {
                Box(Modifier.fillMaxWidth().widthIn(max = maxW)) {
                    LyricsBlock(npTheme, pos, expanded = true)
                }
            }
            Spacer(Modifier.height(18.dp))
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
    PanelSurface(theme, shape, modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(L.tr(LocalContext.current, title).uppercase(), color = theme.subtext, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
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
        val byKey = songs.associateBy { Stats.songKey(it) }
        topSongs.mapNotNull { s -> byKey[s.first] ?: songs.firstOrNull { it.path == s.first } }
    }
    val recent = remember(songs) { songs.sortedByDescending { it.dateAdded }.take(10) }
    val albums = remember(songs) { songs.groupBy { it.album } }
    val banner by Prefs.profileBanner(c).collectAsState(initial = "")
    val bio by Prefs.profileBio(c).collectAsState(initial = "")
    val pronouns by Prefs.profilePronouns(c).collectAsState(initial = "")
    val fav by Prefs.favSong(c).collectAsState(initial = "")
    val pickPfp = rememberCroppedImagePicker("pfp", 1f, theme) { path ->
        scope.launch { Prefs.setProfilePic(c, path) }
    }
    val pickBanner = rememberCroppedImagePicker("banner", 4f, theme) { path ->
        scope.launch { Prefs.setProfileBanner(c, path) }
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
                Text(L.tr(LocalContext.current, "[set pfp]"), color = theme.accent, fontSize = 12.sp, modifier = Modifier.clickable { pickPfp() })
            }
            if (pronouns.isNotEmpty()) Text("($pronouns)", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { editPronouns = true })
            if (location.isNotEmpty()) Text("@ $location", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { editLocation = true })
            if (favAlbum.isNotEmpty()) Text("★ fav album: $favAlbum", color = theme.subtext, fontSize = 12.sp, modifier = Modifier.clickable { editFavAlbum = true })
            if (bio.isNotEmpty()) Text(bio, color = theme.text, fontSize = 13.sp, modifier = Modifier.clickable { editBio = true })
            else Text("tap to add a bio…", color = theme.subtext, fontSize = 13.sp, modifier = Modifier.clickable { editBio = true })
            val curIdx = Playback.player?.currentMediaItemIndex ?: -1
            if (fav.isNotEmpty()) Text("★ favorite: $fav", color = theme.subtext, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(L.tr(LocalContext.current, "[banner]"), color = theme.accent, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.clickable { pickBanner() })
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

        val lastKey = remember { Stats.lastPlayedKey(c) }
        val lastSong = remember(songs, lastKey) {
            songs.firstOrNull { Stats.songKey(it) == lastKey } ?: songs.firstOrNull { it.path == lastKey }
        }
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
    val playsMap = remember(c, songs) { Stats.topSongs(c, Int.MAX_VALUE).toMap() }
    var sortMode by remember { mutableStateOf(0) }
    val shownSorted = remember(shown, sortMode, playsMap) {
        when (sortMode) {
            1 -> shown.sortedBy { it.year.toIntOrNull() ?: Int.MAX_VALUE }
            2 -> shown.sortedByDescending { playsMap[Stats.songKey(it)] ?: playsMap[it.path] ?: 0 }
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
            var songSort by remember(artistName) { mutableStateOf(0) } // album, year, a-z, length
            var albumSort by remember(artistName) { mutableStateOf(0) } // year, a-z, length
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
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("year", "a-z", "length").forEachIndexed { i, label ->
                            Text(if (albumSort == i) "[$label]" else " $label ", color = if (albumSort == i) theme.accent else theme.subtext,
                                fontSize = 12.sp, modifier = Modifier.clickable { albumSort = i }.padding(vertical = 2.dp))
                        }
                    }
                    val sortedAlbums = remember(byAlbum, albumSort) {
                        when (albumSort) {
                            1 -> byAlbum.entries.sortedBy { it.key.lowercase() }
                            2 -> byAlbum.entries.sortedBy { e -> e.value.sumOf { it.durationMs } }
                            else -> byAlbum.entries.sortedWith(compareBy(
                                { e -> e.value.mapNotNull { it.year.toIntOrNull() }.minOrNull() ?: Int.MAX_VALUE },
                                { e -> e.key.lowercase() }
                            ))
                        }
                    }
                    sortedAlbums.forEach { (album, albumSongs) ->
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
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("album", "year", "a-z", "length").forEachIndexed { i, label ->
                            Text(if (songSort == i) "[$label]" else " $label ", color = if (songSort == i) theme.accent else theme.subtext,
                                fontSize = 12.sp, modifier = Modifier.clickable { songSort = i }.padding(vertical = 2.dp))
                        }
                    }
                    val sortedSongs = remember(list, songSort) {
                        when (songSort) {
                            1 -> list.sortedWith(compareBy(
                                { it.year.toIntOrNull() ?: Int.MAX_VALUE },
                                { it.album.lowercase() },
                                { it.track.takeIf { t -> t > 0 } ?: Int.MAX_VALUE },
                                { it.title.lowercase() }
                            ))
                            2 -> list.sortedBy { it.title.lowercase() }
                            3 -> list.sortedBy { it.durationMs }
                            else -> list.sortedWith(compareBy(
                                { it.album.lowercase() },
                                { it.track.takeIf { t -> t > 0 } ?: Int.MAX_VALUE },
                                { it.title.lowercase() }
                            ))
                        }
                    }
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
    var sortMode by remember { mutableStateOf(0) } // 0 = track, 1 = title, 2 = year, 3 = duration
    val rowPad by Prefs.rowPad(c).collectAsState(initial = 8)
    val coverSize by Prefs.coverSize(c).collectAsState(initial = 40)
    val sorted = remember(songs, sortMode) {
        when (sortMode) {
            1 -> songs.sortedBy { it.title.lowercase() }
            2 -> songs.sortedBy { it.year.toIntOrNull() ?: Int.MAX_VALUE }
            3 -> songs.sortedBy { it.durationMs }
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
            Text("sort: ${listOf("track/album", "title", "year", "duration")[sortMode]}", color = theme.accent, fontSize = 12.sp,
                modifier = Modifier.clickable { sortMode = (sortMode + 1) % 4 }.border(LocalBorderWidth.current, theme.border).padding(6.dp))
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
    val byKey = remember(songs) { songs.associateBy { Stats.songKey(it) } }
    fun songLabel(key: String): String {
        val song = byKey[key]
        return if (song != null) "${song.title} · ${song.artist}" else Stats.label(key)
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
                    Stats.importJson(c, text, songs)
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
                Button(onClick = { Stats.importJson(c, importText, songs); showImport = false }) { Text(L.tr(LocalContext.current, "import")) }
            }
        }
    }
}

@Composable
fun StatCard(title: String, items: List<Pair<String, Int>>, theme: ThemeColors, onClick: () -> Unit) {
    PanelSurface(theme, RoundedCornerShape(0.dp), Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
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
    val customSquareImg by Prefs.customSquareImg(c).collectAsState(initial = "")
    val squareAlpha by Prefs.squareImgAlpha(c).collectAsState(initial = 35)
    val pickBg = rememberCroppedImagePicker("background", 9f / 16f, theme) { path ->
        scope.launch { Prefs.setCustomBgImg(c, path) }
    }
    val pickSquare = rememberCroppedImagePicker("square", 16f / 9f, theme) { path ->
        scope.launch { Prefs.setCustomSquareImg(c, path) }
    }
    var pendingThemeExport by remember { mutableStateOf<Pair<String, String>?>(null) }
    var themeFileMsg by remember { mutableStateOf("") }
    val themeExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val pending = pendingThemeExport
        if (uri != null && pending != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val text = if (pending.second == "__current__") {
                        ThemeStore.exportCurrent(c, pending.first)
                    } else {
                        ThemeStore.exportStored(c, pending.second, pending.first)
                    }
                    c.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                    withContext(Dispatchers.Main) { themeFileMsg = "saved ${pending.first}.json" }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) { themeFileMsg = "theme export failed" }
                }
            }
        }
        pendingThemeExport = null
    }
    val themeImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = c.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    val name = ThemeStore.importTheme(c, text)
                    themeFileMsg = if (name != null) "imported $name" else "not a croviera theme"
                } catch (_: Exception) {
                    themeFileMsg = "theme import failed"
                }
            }
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
                Text("start from an included theme (copies its colors into Custom)", color = theme.subtext, fontSize = 10.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Themes.all.forEach { base ->
                        Text(
                            "[${base.name}]",
                            color = theme.accent,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable { scope.launch { ThemeStore.applyBase(c, base.name) } }.padding(vertical = 4.dp)
                        )
                    }
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
                    TextButton(onClick = { pickBg() }) { Text("[ pick bg ]") }
                    if (customBgImg.isNotEmpty()) TextButton(onClick = { scope.launch { Prefs.setCustomBgImg(c, "") } }) { Text("[ clear ]") }
                    Text(if (customBgImg.isEmpty()) "none" else "set", color = theme.subtext, fontSize = 11.sp)
                }
                Text("square replacement image (all panels)", color = theme.text, fontSize = 13.sp)
                Text("one image, filled into every panel. great for a win95 window frame.", color = theme.subtext, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { pickSquare() }) { Text("[ pick square ]") }
                    if (customSquareImg.isNotEmpty()) TextButton(onClick = { scope.launch { Prefs.setCustomSquareImg(c, "") } }) { Text("[ clear ]") }
                    Text(if (customSquareImg.isEmpty()) "none" else "set", color = theme.subtext, fontSize = 11.sp)
                }
                if (customSquareImg.isNotEmpty()) {
                    Text("panel image dim: ${squareAlpha}%", color = theme.text, fontSize = 12.sp)
                    Slider(value = squareAlpha.toFloat(), onValueChange = { v ->
                        scope.launch { Prefs.setSquareImgAlpha(c, v.toInt().coerceIn(0, 90)) }
                    }, valueRange = 0f..90f,
                        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                }
                var newThemeName by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = newThemeName, onValueChange = { newThemeName = it }, label = { Text("save as…") }, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val nm = newThemeName.trim()
                        if (nm.isNotBlank()) {
                            scope.launch {
                                val j = try { org.json.JSONObject(Prefs.savedCustomThemes(c).first()) } catch (e: Exception) { org.json.JSONObject() }
                                j.put(nm, ThemeStore.captureJson(c, nm))
                                Prefs.setSavedCustomThemes(c, j.toString())
                            }
                            newThemeName = ""
                        }
                    }) { Text("[ save ]") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        pendingThemeExport = "custom theme" to "__current__"
                        themeExportLauncher.launch("croviera-theme.json")
                    }) { Text("[ export theme file ]") }
                    TextButton(onClick = {
                        themeImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text("[ import theme file ]") }
                }
                Text("theme files embed their images and every custom-theme knob, including shadows, fullscreen and top-bar settings.", color = theme.subtext, fontSize = 10.sp)
                if (themeFileMsg.isNotEmpty()) Text(themeFileMsg, color = theme.accent, fontSize = 11.sp)
                if (savedNames.isNotEmpty()) {
                    Text("saved themes:", color = theme.subtext, fontSize = 11.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        savedNames.forEach { name ->
                            var confirmDelete by remember { mutableStateOf(false) }
                            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    Modifier.padding(end = 4.dp, bottom = 4.dp).clickable {
                                        val j = try { org.json.JSONObject(existing) } catch (e: Exception) { org.json.JSONObject() }
                                        scope.launch { ThemeStore.applyStored(c, j.optString(name, "")) }
                                    },
                                    color = theme.surface,
                                    shape = RoundedCornerShape(0.dp)
                                ) { Text(name, color = theme.text, modifier = Modifier.padding(8.dp), fontSize = 11.sp) }
                                Text("[file]", color = theme.accent, fontSize = 10.sp, modifier = Modifier.clickable {
                                    val j = try { org.json.JSONObject(existing) } catch (e: Exception) { org.json.JSONObject() }
                                    pendingThemeExport = name to j.optString(name, "")
                                    val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).ifBlank { "theme" }
                                    themeExportLauncher.launch("$safe.json")
                                }.padding(end = 6.dp, bottom = 4.dp))
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
            if (themeName == "Custom") {
                val shadowOn by Prefs.panelShadow(c).collectAsState(initial = false)
                val shadowColor by Prefs.panelShadowColor(c).collectAsState(initial = "#8A1F2D")
                val shadowOffset by Prefs.panelShadowOffset(c).collectAsState(initial = 4)
                val shadowAlpha by Prefs.panelShadowAlpha(c).collectAsState(initial = 55)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("colored shadow below panels", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = shadowOn, onCheckedChange = { scope.launch { Prefs.setPanelShadow(c, it) } })
                }
                if (shadowOn) {
                    Text("shadow color", color = theme.subtext, fontSize = 11.sp)
                    ColorSliderPicker(current = shadowColor, onPick = { hex ->
                        scope.launch { Prefs.setPanelShadowColor(c, hex) }
                    })
                    Text("shadow offset: ${shadowOffset}dp", color = theme.text, fontSize = 12.sp)
                    Slider(value = shadowOffset.toFloat(), onValueChange = { v ->
                        scope.launch { Prefs.setPanelShadowOffset(c, v.toInt().coerceIn(1, 16)) }
                    }, valueRange = 1f..16f,
                        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                    Text("shadow opacity: ${shadowAlpha}%", color = theme.text, fontSize = 12.sp)
                    Slider(value = shadowAlpha.toFloat(), onValueChange = { v ->
                        scope.launch { Prefs.setPanelShadowAlpha(c, v.toInt().coerceIn(0, 100)) }
                    }, valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                }
            } else {
                Text("panel shadows are available in the Custom theme.", color = theme.subtext, fontSize = 11.sp)
            }
        }

        Panel("navigation / top bar", theme, shape = RoundedCornerShape(0.dp)) {
            val navPosition by Prefs.navPosition(c).collectAsState(initial = "top")
            val navNerd by Prefs.navNerd(c).collectAsState(initial = false)
            val navLabelsJson by Prefs.navLabels(c).collectAsState(initial = "{}")
            val navIconsJson by Prefs.navIcons(c).collectAsState(initial = "{}")
            val navImagesJson by Prefs.navButtonImages(c).collectAsState(initial = "{}")
            val navImageSize by Prefs.navImageSize(c).collectAsState(initial = 28)
            val navItemWidth by Prefs.navItemWidth(c).collectAsState(initial = 0)
            val navItemHeight by Prefs.navItemHeight(c).collectAsState(initial = 0)
            val navItemSpacing by Prefs.navItemSpacing(c).collectAsState(initial = 2)
            val navPadding by Prefs.navPadding(c).collectAsState(initial = 4)
            val navThickness by Prefs.navBarThickness(c).collectAsState(initial = 0)
            val navBgAlpha by Prefs.navBgAlpha(c).collectAsState(initial = 94)
            val navBgColor by Prefs.navBgColor(c).collectAsState(initial = "")
            val navImageFit by Prefs.navImageFit(c).collectAsState(initial = "crop")
            val navTextSize by Prefs.navTextSize(c).collectAsState(initial = 14)
            val navNerdSize by Prefs.navNerdSize(c).collectAsState(initial = 20)
            val navSelectedAlpha by Prefs.navSelectedAlpha(c).collectAsState(initial = 14)
            var labels by remember(navLabelsJson) { mutableStateOf(parseStringMap(navLabelsJson)) }
            var icons by remember(navIconsJson) { mutableStateOf(parseStringMap(navIconsJson)) }
            var images by remember(navImagesJson) { mutableStateOf(parseStringMap(navImagesJson)) }
            var navImageTarget by remember { mutableStateOf<String?>(null) }
            val pickNavImage = rememberCroppedImagePicker("nav", 1f, theme) { path ->
                val target = navImageTarget
                if (target != null) {
                    images = images.toMutableMap().apply { put(target, path) }
                    scope.launch { Prefs.setNavButtonImages(c, mapToJson(images)) }
                }
                navImageTarget = null
            }
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
            Text("button images (they replace the label/icon)", color = theme.subtext, fontSize = 11.sp)
            TAB_NAMES.forEach { name ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("~/$name", color = theme.subtext, fontSize = 10.sp, modifier = Modifier.width(64.dp))
                    val img = images[name].orEmpty()
                    if (img.isNotEmpty()) {
                        AsyncImage(model = img, contentDescription = null, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(0.dp)), contentScale = ContentScale.Crop)
                    }
                    TextButton(onClick = { navImageTarget = name; pickNavImage() }) { Text(if (img.isEmpty()) "[ pick ]" else "[ change ]", fontSize = 10.sp) }
                    if (img.isNotEmpty()) {
                        Text("[x]", color = theme.subtext, fontSize = 10.sp, modifier = Modifier.clickable {
                            images = images.toMutableMap().apply { remove(name) }
                            scope.launch { Prefs.setNavButtonImages(c, mapToJson(images)) }
                        })
                    }
                }
            }
            Text("button size", color = theme.subtext, fontSize = 11.sp)
            Text("image size: ${navImageSize}dp", color = theme.text, fontSize = 12.sp)
            Slider(value = navImageSize.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNavImageSize(c, v.toInt().coerceIn(12, 96)) }
            }, valueRange = 12f..96f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("item width: ${if (navItemWidth == 0) "auto" else "${navItemWidth}dp"}", color = theme.text, fontSize = 12.sp)
            Slider(value = navItemWidth.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNavItemWidth(c, v.toInt().coerceIn(0, 240)) }
            }, valueRange = 0f..240f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("item height: ${if (navItemHeight == 0) "auto" else "${navItemHeight}dp"}", color = theme.text, fontSize = 12.sp)
            Slider(value = navItemHeight.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNavItemHeight(c, v.toInt().coerceIn(0, 160)) }
            }, valueRange = 0f..160f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("bar thickness: ${if (navThickness == 0) "auto (fits items)" else "${navThickness}dp"}", color = theme.text, fontSize = 12.sp)
            Slider(value = navThickness.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNavBarThickness(c, v.toInt().coerceIn(0, 240)) }
            }, valueRange = 0f..240f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("item spacing: ${navItemSpacing}dp", color = theme.text, fontSize = 12.sp)
            Slider(value = navItemSpacing.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNavItemSpacing(c, v.toInt().coerceIn(0, 24)) }
            }, valueRange = 0f..24f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("bar padding: ${navPadding}dp", color = theme.text, fontSize = 12.sp)
            Slider(value = navPadding.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNavPadding(c, v.toInt().coerceIn(0, 24)) }
            }, valueRange = 0f..24f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            if (themeName == "Custom") {
                Text("bar opacity: ${navBgAlpha}%", color = theme.text, fontSize = 12.sp)
                Slider(value = navBgAlpha.toFloat(), onValueChange = { v ->
                    scope.launch { Prefs.setNavBgAlpha(c, v.toInt().coerceIn(0, 100)) }
                }, valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                Text("selected item wash: ${navSelectedAlpha}%", color = theme.text, fontSize = 12.sp)
                Slider(value = navSelectedAlpha.toFloat(), onValueChange = { v ->
                    scope.launch { Prefs.setNavSelectedAlpha(c, v.toInt().coerceIn(0, 60)) }
                }, valueRange = 0f..60f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            } else {
                Text("bar color, opacity and selected wash are Custom-theme styling.", color = theme.subtext, fontSize = 11.sp)
            }
            Text("image fit", color = theme.subtext, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("crop", "fit", "fill").forEach { fit ->
                    Text(if (navImageFit == fit) "[$fit]" else " $fit ", color = if (navImageFit == fit) theme.accent else theme.text,
                        fontSize = 13.sp, modifier = Modifier.clickable { scope.launch { Prefs.setNavImageFit(c, fit) } }.padding(vertical = 4.dp))
                }
            }
            Text("text size: ${navTextSize}sp", color = theme.text, fontSize = 12.sp)
            Slider(value = navTextSize.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNavTextSize(c, v.toInt().coerceIn(8, 32)) }
            }, valueRange = 8f..32f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("nerd glyph size: ${navNerdSize}sp", color = theme.text, fontSize = 12.sp)
            Slider(value = navNerdSize.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNavNerdSize(c, v.toInt().coerceIn(10, 48)) }
            }, valueRange = 10f..48f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            if (themeName == "Custom") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("bar color", color = theme.subtext, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    if (navBgColor.isNotBlank()) Text("[theme default]", color = theme.accent, fontSize = 10.sp, modifier = Modifier.clickable { scope.launch { Prefs.setNavBgColor(c, "") } })
                }
                ColorSliderPicker(current = navBgColor, onPick = { hex -> scope.launch { Prefs.setNavBgColor(c, hex) } })
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

        Panel("now playing / fullscreen", theme, shape = RoundedCornerShape(0.dp)) {
            val npLabel by Prefs.npLabel(c).collectAsState(initial = "Now playing!")
            val npQueue by Prefs.npExpandedQueue(c).collectAsState(initial = true)
            val npLyrics by Prefs.npExpandedLyrics(c).collectAsState(initial = true)
            var npLabelField by remember(npLabel) { mutableStateOf(npLabel) }
            OutlinedTextField(value = npLabelField, onValueChange = { v ->
                npLabelField = v
                scope.launch { Prefs.setNpLabel(c, v) }
            }, label = { Text("card title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("tap the card to open the fullscreen player", color = theme.subtext, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("show queue in fullscreen", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = npQueue, onCheckedChange = { scope.launch { Prefs.setNpExpandedQueue(c, it) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("show lyrics in fullscreen", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = npLyrics, onCheckedChange = { scope.launch { Prefs.setNpExpandedLyrics(c, it) } })
            }

            if (themeName == "Custom") {
            Text("custom fullscreen look (Custom theme)", color = theme.accent, fontSize = 12.sp)
            Text("colors, background, element toggles and stickers below are saved with the Custom theme and exported theme files.", color = theme.subtext, fontSize = 10.sp)

            val npBgImg by Prefs.npBgImg(c).collectAsState(initial = "")
            val npBgAlpha by Prefs.npBgAlpha(c).collectAsState(initial = 45)
            val npBgColor by Prefs.npBgColor(c).collectAsState(initial = "")
            val npTextColor by Prefs.npTextColor(c).collectAsState(initial = "")
            val npSubtextColor by Prefs.npSubtextColor(c).collectAsState(initial = "")
            val npAccentColor by Prefs.npAccentColor(c).collectAsState(initial = "")
            val npMaxWidth by Prefs.npMaxWidth(c).collectAsState(initial = 420)
            val npCoverRadius by Prefs.npCoverRadius(c).collectAsState(initial = 0)
            val npCoverSize by Prefs.npCoverSize(c).collectAsState(initial = 0)
            val npTitleSize by Prefs.npTitleSize(c).collectAsState(initial = 24)
            val npArtistSize by Prefs.npArtistSize(c).collectAsState(initial = 15)
            val npDim by Prefs.npDim(c).collectAsState(initial = 0)
            val npBgFit by Prefs.npBgFit(c).collectAsState(initial = "crop")
            val npButtonRadius by Prefs.npButtonRadius(c).collectAsState(initial = 18)
            val pickNpBg = rememberCroppedImagePicker("fullscreen_bg", 9f / 16f, theme) { path ->
                scope.launch { Prefs.setNpBgImg(c, path) }
            }
            Text("fullscreen background image", color = theme.text, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { pickNpBg() }) { Text(if (npBgImg.isEmpty()) "[ pick bg ]" else "[ change bg ]") }
                if (npBgImg.isNotEmpty()) TextButton(onClick = { scope.launch { Prefs.setNpBgImg(c, "") } }) { Text("[ clear ]") }
                Text(if (npBgImg.isEmpty()) "none" else "set", color = theme.subtext, fontSize = 11.sp)
            }
            if (npBgImg.isNotEmpty()) {
                Text("fullscreen bg opacity: ${npBgAlpha}%", color = theme.text, fontSize = 12.sp)
                Slider(value = npBgAlpha.toFloat(), onValueChange = { v ->
                    scope.launch { Prefs.setNpBgAlpha(c, v.toInt().coerceIn(0, 100)) }
                }, valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("fullscreen background color", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (npBgColor.isNotBlank()) Text("[theme default]", color = theme.accent, fontSize = 10.sp, modifier = Modifier.clickable { scope.launch { Prefs.setNpBgColor(c, "") } })
            }
            ColorSliderPicker(current = npBgColor, onPick = { hex -> scope.launch { Prefs.setNpBgColor(c, hex) } })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("fullscreen text color", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (npTextColor.isNotBlank()) Text("[theme default]", color = theme.accent, fontSize = 10.sp, modifier = Modifier.clickable { scope.launch { Prefs.setNpTextColor(c, "") } })
            }
            ColorSliderPicker(current = npTextColor, onPick = { hex -> scope.launch { Prefs.setNpTextColor(c, hex) } })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("fullscreen subtext color", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (npSubtextColor.isNotBlank()) Text("[theme default]", color = theme.accent, fontSize = 10.sp, modifier = Modifier.clickable { scope.launch { Prefs.setNpSubtextColor(c, "") } })
            }
            ColorSliderPicker(current = npSubtextColor, onPick = { hex -> scope.launch { Prefs.setNpSubtextColor(c, hex) } })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("fullscreen accent color", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (npAccentColor.isNotBlank()) Text("[theme default]", color = theme.accent, fontSize = 10.sp, modifier = Modifier.clickable { scope.launch { Prefs.setNpAccentColor(c, "") } })
            }
            ColorSliderPicker(current = npAccentColor, onPick = { hex -> scope.launch { Prefs.setNpAccentColor(c, hex) } })
            Text("content max width: ${npMaxWidth}dp", color = theme.text, fontSize = 12.sp)
            Slider(value = npMaxWidth.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNpMaxWidth(c, v.toInt().coerceIn(220, 720)) }
            }, valueRange = 220f..720f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))

            Text("cover corner radius: ${if (npCoverRadius == 0) "theme shape" else "${npCoverRadius}dp"}", color = theme.text, fontSize = 12.sp)
            Slider(value = npCoverRadius.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNpCoverRadius(c, v.toInt().coerceIn(0, 48)) }
            }, valueRange = 0f..48f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("cover max size: ${if (npCoverSize == 0) "full width" else "${npCoverSize}dp"}", color = theme.text, fontSize = 12.sp)
            Slider(value = npCoverSize.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNpCoverSize(c, v.toInt().coerceIn(0, 480)) }
            }, valueRange = 0f..480f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("title size: ${npTitleSize}sp", color = theme.text, fontSize = 12.sp)
            Slider(value = npTitleSize.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNpTitleSize(c, v.toInt().coerceIn(14, 40)) }
            }, valueRange = 14f..40f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("artist size: ${npArtistSize}sp", color = theme.text, fontSize = 12.sp)
            Slider(value = npArtistSize.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNpArtistSize(c, v.toInt().coerceIn(10, 28)) }
            }, valueRange = 10f..28f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("background dim: ${npDim}%", color = theme.text, fontSize = 12.sp)
            Slider(value = npDim.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNpDim(c, v.toInt().coerceIn(0, 80)) }
            }, valueRange = 0f..80f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
            Text("background image fit", color = theme.subtext, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("crop", "fit", "fill").forEach { fit ->
                    Text(if (npBgFit == fit) "[$fit]" else " $fit ", color = if (npBgFit == fit) theme.accent else theme.text,
                        fontSize = 13.sp, modifier = Modifier.clickable { scope.launch { Prefs.setNpBgFit(c, fit) } }.padding(vertical = 4.dp))
                }
            }
            Text("play button corner radius: ${npButtonRadius}dp", color = theme.text, fontSize = 12.sp)
            Slider(value = npButtonRadius.toFloat(), onValueChange = { v ->
                scope.launch { Prefs.setNpButtonRadius(c, v.toInt().coerceIn(0, 32)) }
            }, valueRange = 0f..32f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))

            Text("show / hide elements", color = theme.subtext, fontSize = 11.sp)
            val showCover by Prefs.npShowCover(c).collectAsState(initial = true)
            val showProgress by Prefs.npShowProgress(c).collectAsState(initial = true)
            val showTitle by Prefs.npShowTitle(c).collectAsState(initial = true)
            val showArtist by Prefs.npShowArtist(c).collectAsState(initial = true)
            val showInfo by Prefs.npShowInfo(c).collectAsState(initial = true)
            val showControls by Prefs.npShowControls(c).collectAsState(initial = true)
            val showActions by Prefs.npShowActions(c).collectAsState(initial = true)
            val showQueueSection by Prefs.npShowQueue(c).collectAsState(initial = true)
            val showLyricsSection by Prefs.npShowLyrics(c).collectAsState(initial = true)
            listOf(
                Triple("cover", showCover, Prefs::setNpShowCover),
                Triple("progress bar", showProgress, Prefs::setNpShowProgress),
                Triple("title", showTitle, Prefs::setNpShowTitle),
                Triple("artist", showArtist, Prefs::setNpShowArtist),
                Triple("format/year info", showInfo, Prefs::setNpShowInfo),
                Triple("playback controls", showControls, Prefs::setNpShowControls),
                Triple("lyrics/queue/favorite row", showActions, Prefs::setNpShowActions),
                Triple("queue section", showQueueSection, Prefs::setNpShowQueue),
                Triple("lyrics section", showLyricsSection, Prefs::setNpShowLyrics),
            ).forEach { (label, checked, setter) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = checked, onCheckedChange = { on -> scope.launch { setter(c, on) } })
                }
            }

            val stickersJson by Prefs.npStickers(c).collectAsState(initial = "[]")
            var stickers by remember { mutableStateOf(parseStickers(stickersJson)) }
            LaunchedEffect(stickersJson) { stickers = parseStickers(stickersJson) }
            fun saveStickers(next: List<NpSticker>) {
                stickers = next
                scope.launch { Prefs.setNpStickers(c, stickersToJson(next)) }
            }
            val pickSticker = rememberCroppedImagePicker("sticker", 1f, theme) { path ->
                saveStickers(stickers + NpSticker(path, 0.5f, 0.5f, 72, 0.85f, 0f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("stickers: ${stickers.size}", color = theme.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { pickSticker() }) { Text("[ add sticker ]") }
            }
            Text("stickers are placed over the fullscreen background. drag the sliders to position them.", color = theme.subtext, fontSize = 10.sp)
            stickers.forEachIndexed { idx, st ->
                Column(Modifier.fillMaxWidth().border(LocalBorderWidth.current, theme.border).padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = st.image, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(0.dp)), contentScale = ContentScale.Fit)
                        Spacer(Modifier.width(8.dp))
                        Text("sticker ${idx + 1}", color = theme.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("[x]", color = theme.subtext, fontSize = 11.sp, modifier = Modifier.clickable {
                            saveStickers(stickers.filterIndexed { i, _ -> i != idx })
                        })
                    }
                    Text("x: ${(st.x * 100).toInt()}%", color = theme.subtext, fontSize = 10.sp)
                    Slider(value = st.x, onValueChange = { v ->
                        saveStickers(stickers.toMutableList().also { it[idx] = st.copy(x = v) })
                    }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                    Text("y: ${(st.y * 100).toInt()}%", color = theme.subtext, fontSize = 10.sp)
                    Slider(value = st.y, onValueChange = { v ->
                        saveStickers(stickers.toMutableList().also { it[idx] = st.copy(y = v) })
                    }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                    Text("size: ${st.size}dp", color = theme.subtext, fontSize = 10.sp)
                    Slider(value = st.size.toFloat(), onValueChange = { v ->
                        saveStickers(stickers.toMutableList().also { it[idx] = st.copy(size = v.toInt().coerceIn(8, 512)) })
                    }, valueRange = 8f..512f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                    Text("opacity: ${(st.alpha * 100).toInt()}%", color = theme.subtext, fontSize = 10.sp)
                    Slider(value = st.alpha, onValueChange = { v ->
                        saveStickers(stickers.toMutableList().also { it[idx] = st.copy(alpha = v.coerceIn(0f, 1f)) })
                    }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                    Text("rotation: ${st.rotation.toInt()}°", color = theme.subtext, fontSize = 10.sp)
                    Slider(value = st.rotation, onValueChange = { v ->
                        saveStickers(stickers.toMutableList().also { it[idx] = st.copy(rotation = v.coerceIn(-180f, 180f)) })
                    }, valueRange = -180f..180f, colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent))
                }
            }
            } else {
                Text("fullscreen colors, images, element toggles and stickers are Custom-theme styling.", color = theme.subtext, fontSize = 11.sp)
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
        var backupMsg by remember { mutableStateOf("") }
        var backupText by remember { mutableStateOf("") }
        val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val json = Prefs.exportAll(c)
                        c.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                        withContext(Dispatchers.Main) { backupMsg = "backup exported" }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) { backupMsg = "backup export failed" }
                    }
                }
            }
        }
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val text = c.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                        Prefs.importAll(c, text)
                        Prefs.repairTypes(c)
                        MusicIndex.requestRescan()
                        backupMsg = "backup restored"
                    } catch (_: Exception) {
                        backupMsg = "backup import failed"
                    }
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
            if (backupMsg.isNotEmpty()) Text(backupMsg, color = theme.accent, fontSize = 11.sp)
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
            val pickDisc = rememberCroppedImagePicker("disc", 1f, theme) { path ->
                scope.launch { Prefs.setCustomDisc(c, path) }
            }
            Text("custom disc icon: ${if (customDisc.isEmpty()) "none (pixel-art default)" else "custom set"}", color = theme.text, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pickDisc() }) { Text("[ pick image ]") }
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

    val pickCover = rememberCroppedImagePicker("playlist", 1f, theme) { path ->
        val target = coverTarget
        if (target != null) {
            scope.launch(Dispatchers.IO) {
                val saved = Playlists.setCoverFromPath(c, target, path)
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
                                modifier = Modifier.clickable { coverTarget = open; pickCover() }.padding(vertical = 4.dp))
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

/**
 * Pick an image, then let the user pan/zoom it inside a fixed aspect frame before
 * saving a cropped copy into internal storage. Used for pfp, banner, disc,
 * background and the per-theme square replacement.
 */
@Composable
fun rememberCroppedImagePicker(
    prefix: String,
    aspect: Float,
    theme: ThemeColors,
    onSaved: (String) -> Unit
): () -> Unit {
    var pending by remember { mutableStateOf<Uri?>(null) }
    val latest by rememberUpdatedState(onSaved)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pending = uri
    }
    pending?.let { uri ->
        CropImageDialog(
            uri = uri,
            aspect = aspect,
            title = prefix,
            theme = theme,
            onCancel = { pending = null },
            onCropped = { path ->
                pending = null
                latest(path)
            }
        )
    }
    return { launcher.launch("image/*") }
}

@Composable
fun CropImageDialog(
    uri: Uri,
    aspect: Float,
    title: String,
    theme: ThemeColors,
    onCancel: () -> Unit,
    onCropped: (String) -> Unit,
) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var error by remember(uri) { mutableStateOf("") }
    var saving by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) { decodeBitmap(c, uri) }
        if (bitmap == null) error = "could not load image"
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = theme.bg) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("crop $title", color = theme.text, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Text("[ cancel ]", color = theme.subtext, fontSize = 13.sp, modifier = Modifier.clickable { onCancel() })
                }
                val bmp = bitmap
                if (bmp == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (error.isNotEmpty()) Text(error, color = theme.subtext, fontSize = 13.sp)
                        else CircularProgressIndicator(color = theme.accent)
                    }
                } else {
                    var scale by remember(bmp) { mutableStateOf(1f) }
                    var offset by remember(bmp) { mutableStateOf(Offset.Zero) }
                    var framePx by remember(bmp) { mutableStateOf(IntSize.Zero) }
                    val previewBitmap = remember(bmp) { bmp.asImageBitmap() }

                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val frameW = minOf(maxWidth, maxHeight * aspect)
                        val frameH = frameW / aspect
                        Box(
                            Modifier.size(frameW, frameH)
                                .clipToBounds()
                                .background(Color.Black)
                                .onSizeChanged { framePx = it }
                                .pointerInput(bmp, framePx) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val base = max(
                                            framePx.width / bmp.width.toFloat(),
                                            framePx.height / bmp.height.toFloat()
                                        )
                                        val nextScale = (scale * zoom).coerceIn(1f, 6f)
                                        val disp = base * nextScale
                                        val maxX = ((bmp.width * disp - framePx.width) / 2f).coerceAtLeast(0f)
                                        val maxY = ((bmp.height * disp - framePx.height) / 2f).coerceAtLeast(0f)
                                        scale = nextScale
                                        offset = Offset(
                                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                                        )
                                    }
                                }
                        ) {
                            // Draw the whole bitmap scaled around the frame centre. Scaling
                            // the already-cropped frame would trap the user inside the
                            // initial crop, so this keeps every pixel reachable.
                            Canvas(Modifier.fillMaxSize()) {
                                val base = max(
                                    size.width / bmp.width.toFloat(),
                                    size.height / bmp.height.toFloat()
                                )
                                val disp = base * scale
                                val drawW = bmp.width * disp
                                val drawH = bmp.height * disp
                                drawImage(
                                    image = previewBitmap,
                                    dstOffset = IntOffset(
                                        ((size.width - drawW) / 2f + offset.x).roundToInt(),
                                        ((size.height - drawH) / 2f + offset.y).roundToInt()
                                    ),
                                    dstSize = IntSize(
                                        drawW.roundToInt().coerceAtLeast(1),
                                        drawH.roundToInt().coerceAtLeast(1)
                                    )
                                )
                            }
                        }
                    }

                    Text("drag to pan · pinch to zoom", color = theme.subtext, fontSize = 11.sp)
                    if (error.isNotEmpty()) Text(error, color = Color(0xFFEF5350), fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                if (saving) return@Button
                                saving = true
                                scope.launch {
                                    val path = withContext(Dispatchers.IO) {
                                        cropAndSave(c, bmp, framePx, scale, offset, title)
                                    }
                                    saving = false
                                    if (path != null) onCropped(path)
                                    else error = "could not crop image"
                                }
                            },
                            enabled = !saving,
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                        ) {
                            Text(if (saving) "saving…" else "[ use image ]", color = theme.bg)
                        }
                    }
                }
            }
        }
    }
}

private fun decodeBitmap(c: Context, uri: Uri): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source = ImageDecoder.createSource(c.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val maxDim = 2048
                if (w > maxDim || h > maxDim) {
                    val scale = maxDim.toFloat() / max(w, h)
                    decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (_: Exception) {
            // Fall through to BitmapFactory for providers that hand back
            // streams ImageDecoder cannot seek (cloud pickers, odd mime types).
        }
    }
    return try {
        val bytes = c.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 2048) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (_: Exception) { null }
}

private fun cropAndSave(
    c: Context,
    bmp: Bitmap,
    frame: IntSize,
    scale: Float,
    offset: Offset,
    prefix: String,
): String? {
    if (frame.width <= 0 || frame.height <= 0) return null
    return try {
        val base = max(frame.width / bmp.width.toFloat(), frame.height / bmp.height.toFloat())
        val disp = base * scale
        val cropW = (frame.width / disp).roundToInt().coerceIn(1, bmp.width)
        val cropH = (frame.height / disp).roundToInt().coerceIn(1, bmp.height)
        val left = ((bmp.width / 2f) - (frame.width / 2f + offset.x) / disp).roundToInt().coerceIn(0, bmp.width - cropW)
        val top = ((bmp.height / 2f) - (frame.height / 2f + offset.y) / disp).roundToInt().coerceIn(0, bmp.height - cropH)
        val cropped = Bitmap.createBitmap(bmp, left, top, cropW, cropH)
        val dir = File(c.filesDir, "images").apply { mkdirs() }
        val safe = prefix.replace(Regex("[^A-Za-z0-9._-]"), "_").take(32).ifBlank { "image" }
        val file = File(dir, "${safe}_${System.currentTimeMillis()}.png")
        file.outputStream().use { out -> cropped.compress(Bitmap.CompressFormat.PNG, 100, out) }
        if (cropped !== bmp) cropped.recycle()
        Uri.fromFile(file).toString()
    } catch (_: Exception) { null }
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
