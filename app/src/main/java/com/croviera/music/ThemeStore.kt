package com.croviera.music

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Portable custom-theme files.
 *
 * A theme captures every "Custom" look: colors, gradient, panel images, panel
 * shadows, fullscreen-player styling/stickers, nav-bar sizing/images and the
 * shape/opacity knobs. Referenced local images are embedded as base64 so a
 * theme file survives being copied to another phone.
 */
object ThemeStore {
    const val FORMAT = "croviera-theme"
    const val VERSION = 1
    private const val EMBED = "@embed:"
    private val SINGLE_IMAGES = listOf("customBgImg", "customSquareImg", "npBgImg", "customDisc")

    private fun hex(v: String?): String {
        val t = v?.trim().orEmpty()
        if (t.isEmpty() || t == "#") return ""
        return if (t.startsWith("#")) t else "#$t"
    }

    /** Snapshot the current custom-theme settings into a portable JSON object. */
    suspend fun capture(c: Context): JSONObject {
        val s = Prefs.snapshot(c)
        val o = JSONObject()
        o.put("format", FORMAT)
        o.put("version", VERSION)
        o.put("name", "custom theme")

        o.put("customBg", hex(s.custom.getOrNull(0)))
        o.put("customSurface", hex(s.custom.getOrNull(1)))
        o.put("customText", hex(s.custom.getOrNull(2)))
        o.put("customSubtext", hex(s.custom.getOrNull(3)))
        o.put("customAccent", hex(s.custom.getOrNull(4)))
        o.put("customBorder", hex(s.custom.getOrNull(5)))
        o.put("gradientStart", hex(s.gradient.start))
        o.put("gradientEnd", hex(s.gradient.end))
        o.put("gradientAngle", s.gradient.angle)
        o.put("customBgImg", s.bgImg)
        o.put("customSquareImg", s.squareImg)
        o.put("squareImgAlpha", s.squareImgAlpha)
        o.put("customDisc", s.customDisc)

        o.put("panelShadow", s.panelShadow)
        o.put("panelShadowColor", hex(s.panelShadowColor))
        o.put("panelShadowOffset", s.panelShadowOffset)
        o.put("panelShadowAlpha", s.panelShadowAlpha)

        o.put("npBgImg", s.npBgImg)
        o.put("npBgAlpha", s.npBgAlpha)
        o.put("npBgColor", hex(s.npBgColor))
        o.put("npTextColor", hex(s.npTextColor))
        o.put("npSubtextColor", hex(s.npSubtextColor))
        o.put("npAccentColor", hex(s.npAccentColor))
        o.put("npShowCover", s.npShowCover)
        o.put("npShowProgress", s.npShowProgress)
        o.put("npShowTitle", s.npShowTitle)
        o.put("npShowArtist", s.npShowArtist)
        o.put("npShowInfo", s.npShowInfo)
        o.put("npShowControls", s.npShowControls)
        o.put("npShowActions", s.npShowActions)
        o.put("npShowQueue", s.npShowQueue)
        o.put("npShowLyrics", s.npShowLyrics)
        o.put("npMaxWidth", s.npMaxWidth)
        o.put("npStickers", s.npStickers)
        o.put("npCoverRadius", s.npCoverRadius)
        o.put("npCoverSize", s.npCoverSize)
        o.put("npTitleSize", s.npTitleSize)
        o.put("npArtistSize", s.npArtistSize)
        o.put("npDim", s.npDim)
        o.put("npBgFit", s.npBgFit)
        o.put("npButtonRadius", s.npButtonRadius)
        o.put("npExpandedCover", s.npExpandedCover)
        o.put("npExpandedQueue", s.npExpandedQueue)
        o.put("npExpandedLyrics", s.npExpandedLyrics)

        o.put("navPosition", s.navPosition)
        o.put("navLabels", s.navLabels)
        o.put("navIcons", s.navIcons)
        o.put("navNerd", s.navNerd)
        o.put("navButtonImages", s.navButtonImages)
        o.put("navImageSize", s.navImageSize)
        o.put("navItemWidth", s.navItemWidth)
        o.put("navItemHeight", s.navItemHeight)
        o.put("navItemSpacing", s.navItemSpacing)
        o.put("navPadding", s.navPadding)
        o.put("navBarThickness", s.navBarThickness)
        o.put("navBgAlpha", s.navBgAlpha)
        o.put("navBgColor", hex(s.navBgColor))
        o.put("navImageFit", s.navImageFit)
        o.put("navTextSize", s.navTextSize)
        o.put("navNerdSize", s.navNerdSize)
        o.put("navSelectedAlpha", s.navSelectedAlpha)

        o.put("cornerSize", s.cornerSize)
        o.put("borderWidth", s.borderWidth)
        o.put("bgAlpha", s.bgAlpha)
        o.put("uiAlpha", s.uiAlpha)
        o.put("fontScale", s.fontScale.toDouble())
        o.put("lineHeightScale", s.lineHeightScale.toDouble())
        o.put("fontName", s.fontName)
        return o
    }

    /** Saved-theme value for DataStore: local image paths, no base64 bloat. */
    suspend fun captureJson(c: Context, name: String): String = withContext(Dispatchers.IO) {
        val o = capture(c)
        o.put("name", name)
        o.toString()
    }

    /** Export the currently applied custom theme as a standalone file with images embedded. */
    suspend fun exportCurrent(c: Context, name: String): String = withContext(Dispatchers.IO) {
        val o = capture(c)
        o.put("name", name)
        embedImages(c, o)
        o.toString(2)
    }

    /** Export a stored (saved-theme) value, legacy pipe string or JSON. */
    suspend fun exportStored(c: Context, stored: String, name: String): String = withContext(Dispatchers.IO) {
        val t = stored.trim()
        if (t.startsWith("{")) {
            val o = try { JSONObject(t) } catch (_: Exception) { capture(c) }
            o.put("name", name)
            embedImages(c, o)
            return@withContext o.toString(2)
        }
        val o = capture(c)
        o.put("name", name)
        val list = t.split("|", limit = 11)
        if (list.size >= 6) {
            o.put("customBg", hex(list[0]))
            o.put("customSurface", hex(list[1]))
            o.put("customText", hex(list[2]))
            o.put("customSubtext", hex(list[3]))
            o.put("customAccent", hex(list[4]))
            o.put("customBorder", hex(list[5]))
            if (list.size >= 10) {
                o.put("gradientStart", if (list[6].isNotBlank()) hex(list[6]) else "")
                o.put("gradientEnd", if (list[7].isNotBlank()) hex(list[7]) else "")
                o.put("gradientAngle", list[8].toIntOrNull() ?: 135)
                o.put("customBgImg", list[9])
            } else {
                o.put("gradientStart", "")
                o.put("gradientEnd", "")
                o.put("customBgImg", "")
            }
            o.put("customSquareImg", list.getOrNull(10).orEmpty())
        }
        embedImages(c, o)
        o.toString(2)
    }

    /**
     * Use an included theme as the starting point for Custom. Only colors are
     * copied, so existing custom images/layout are kept; the user can then
     * tweak the included palette without losing their top-bar/fullscreen work.
     */
    suspend fun applyBase(c: Context, name: String) {
        val t = Themes.byName(name)
        fun h(color: androidx.compose.ui.graphics.Color): String =
            String.format("#%06X", color.toArgb() and 0xFFFFFF)
        Prefs.setCustom(c, h(t.bg), h(t.surface), h(t.text), h(t.subtext), h(t.accent), h(t.border))
        Prefs.setTheme(c, "Custom")
    }

    /** Apply a stored saved-theme value. Supports 1.0/1.1 pipe strings and new JSON. */
    suspend fun applyStored(c: Context, stored: String) {
        val t = stored.trim()
        if (t.startsWith("{")) {
            try {
                apply(c, JSONObject(t))
            } catch (_: Exception) {
            }
        } else if (t.isNotEmpty()) {
            applyLegacy(c, t)
        }
    }

    private suspend fun applyLegacy(c: Context, pipe: String) {
        val list = pipe.split("|", limit = 11)
        if (list.size < 6) return
        Prefs.setCustom(
            c,
            hex(list[0]), hex(list[1]), hex(list[2]), hex(list[3]), hex(list[4]), hex(list[5])
        )
        if (list.size >= 10) {
            Prefs.setGradient(
                c,
                if (list[6].isNotBlank()) hex(list[6]) else "",
                if (list[7].isNotBlank()) hex(list[7]) else "",
                list[8].toIntOrNull() ?: 135
            )
            Prefs.setCustomBgImg(c, list[9])
        } else {
            Prefs.setGradient(c, "", "", 135)
            Prefs.setCustomBgImg(c, "")
        }
        Prefs.setCustomSquareImg(c, list.getOrNull(10).orEmpty())
        Prefs.setTheme(c, "Custom")
    }

    /** Apply a parsed theme spec to the app. */
    suspend fun apply(c: Context, raw: JSONObject) {
        val o = withContext(Dispatchers.IO) { materializeImages(c, raw) }
        Prefs.setCustom(
            c,
            hex(o.optString("customBg")),
            hex(o.optString("customSurface")),
            hex(o.optString("customText")),
            hex(o.optString("customSubtext")),
            hex(o.optString("customAccent")),
            hex(o.optString("customBorder"))
        )
        Prefs.setGradient(
            c,
            hex(o.optString("gradientStart")),
            hex(o.optString("gradientEnd")),
            o.optInt("gradientAngle", 135).coerceIn(0, 360)
        )
        Prefs.setCustomBgImg(c, o.optString("customBgImg", ""))
        Prefs.setCustomSquareImg(c, o.optString("customSquareImg", ""))
        Prefs.setSquareImgAlpha(c, o.optInt("squareImgAlpha", 35).coerceIn(0, 90))
        Prefs.setCustomDisc(c, o.optString("customDisc", ""))

        Prefs.setPanelShadow(c, o.optBoolean("panelShadow", false))
        Prefs.setPanelShadowColor(c, hex(o.optString("panelShadowColor", "#8A1F2D")))
        Prefs.setPanelShadowOffset(c, o.optInt("panelShadowOffset", 4).coerceIn(1, 16))
        Prefs.setPanelShadowAlpha(c, o.optInt("panelShadowAlpha", 55).coerceIn(0, 100))

        Prefs.setNpBgImg(c, o.optString("npBgImg", ""))
        Prefs.setNpBgAlpha(c, o.optInt("npBgAlpha", 45).coerceIn(0, 100))
        Prefs.setNpBgColor(c, hex(o.optString("npBgColor")))
        Prefs.setNpTextColor(c, hex(o.optString("npTextColor")))
        Prefs.setNpSubtextColor(c, hex(o.optString("npSubtextColor")))
        Prefs.setNpAccentColor(c, hex(o.optString("npAccentColor")))
        Prefs.setNpShowCover(c, o.optBoolean("npShowCover", true))
        Prefs.setNpShowProgress(c, o.optBoolean("npShowProgress", true))
        Prefs.setNpShowTitle(c, o.optBoolean("npShowTitle", true))
        Prefs.setNpShowArtist(c, o.optBoolean("npShowArtist", true))
        Prefs.setNpShowInfo(c, o.optBoolean("npShowInfo", true))
        Prefs.setNpShowControls(c, o.optBoolean("npShowControls", true))
        Prefs.setNpShowActions(c, o.optBoolean("npShowActions", true))
        Prefs.setNpShowQueue(c, o.optBoolean("npShowQueue", true))
        Prefs.setNpShowLyrics(c, o.optBoolean("npShowLyrics", true))
        Prefs.setNpMaxWidth(c, o.optInt("npMaxWidth", 420).coerceIn(220, 720))
        Prefs.setNpStickers(c, o.optString("npStickers", "[]"))
        Prefs.setNpCoverRadius(c, o.optInt("npCoverRadius", 0).coerceIn(0, 48))
        Prefs.setNpCoverSize(c, o.optInt("npCoverSize", 0).coerceIn(0, 480))
        Prefs.setNpTitleSize(c, o.optInt("npTitleSize", 24).coerceIn(14, 40))
        Prefs.setNpArtistSize(c, o.optInt("npArtistSize", 15).coerceIn(10, 28))
        Prefs.setNpDim(c, o.optInt("npDim", 0).coerceIn(0, 80))
        Prefs.setNpBgFit(c, o.optString("npBgFit", "crop"))
        Prefs.setNpButtonRadius(c, o.optInt("npButtonRadius", 18).coerceIn(0, 32))
        Prefs.setNpExpandedCover(c, o.optInt("npExpandedCover", 180).coerceIn(96, 360))
        Prefs.setNpExpandedQueue(c, o.optBoolean("npExpandedQueue", true))
        Prefs.setNpExpandedLyrics(c, o.optBoolean("npExpandedLyrics", true))

        Prefs.setNavPosition(c, o.optString("navPosition", "top"))
        Prefs.setNavLabels(c, o.optString("navLabels", "{}"))
        Prefs.setNavIcons(c, o.optString("navIcons", "{}"))
        Prefs.setNavNerd(c, o.optBoolean("navNerd", false))
        Prefs.setNavButtonImages(c, o.optString("navButtonImages", "{}"))
        Prefs.setNavImageSize(c, o.optInt("navImageSize", 28).coerceIn(12, 96))
        Prefs.setNavItemWidth(c, o.optInt("navItemWidth", 0).coerceIn(0, 240))
        Prefs.setNavItemHeight(c, o.optInt("navItemHeight", 0).coerceIn(0, 160))
        Prefs.setNavItemSpacing(c, o.optInt("navItemSpacing", 2).coerceIn(0, 24))
        Prefs.setNavPadding(c, o.optInt("navPadding", 4).coerceIn(0, 24))
        Prefs.setNavBarThickness(c, o.optInt("navBarThickness", 0).coerceIn(0, 240))
        Prefs.setNavBgAlpha(c, o.optInt("navBgAlpha", 94).coerceIn(0, 100))
        Prefs.setNavBgColor(c, hex(o.optString("navBgColor")))
        Prefs.setNavImageFit(c, o.optString("navImageFit", "crop"))
        Prefs.setNavTextSize(c, o.optInt("navTextSize", 14).coerceIn(8, 32))
        Prefs.setNavNerdSize(c, o.optInt("navNerdSize", 20).coerceIn(10, 48))
        Prefs.setNavSelectedAlpha(c, o.optInt("navSelectedAlpha", 14).coerceIn(0, 60))

        val corner = o.optInt("cornerSize", 0).coerceIn(0, 64)
        Prefs.setCornerSize(c, corner)
        Prefs.setRounded(c, corner > 0)
        Prefs.setBorderWidth(c, o.optInt("borderWidth", 1).coerceIn(0, 8))
        Prefs.setBgAlpha(c, o.optInt("bgAlpha", 45).coerceIn(0, 100))
        Prefs.setUiAlpha(c, o.optInt("uiAlpha", 100).coerceIn(15, 100))
        Prefs.setFontScale(c, o.optDouble("fontScale", 1.0).toFloat().coerceIn(0.7f, 2f))
        Prefs.setLineHeightScale(c, o.optDouble("lineHeightScale", 0.0).toFloat().coerceIn(0f, 2.5f))
        val font = o.optString("fontName", "")
        if (font.isNotBlank()) Prefs.setFontName(c, font)
        Prefs.setTheme(c, "Custom")
    }

    /** Import a theme file, save it into the theme list, and apply it. */
    suspend fun importTheme(c: Context, text: String): String? {
        val o = try { JSONObject(text) } catch (_: Exception) { return null }
        val looksLikeTheme = o.optString("format") == FORMAT || o.has("customBg") || o.has("customAccent")
        if (!looksLikeTheme) return null
        // Materialize embedded images once, then store local paths in DataStore
        // instead of keeping multi-megabyte base64 blobs in preferences.
        val resolved = withContext(Dispatchers.IO) { materializeImages(c, o) }
        val base = resolved.optString("name", "").trim().ifBlank { "imported theme" }
        val map = try {
            JSONObject(Prefs.savedCustomThemes(c).first())
        } catch (_: Exception) {
            JSONObject()
        }
        var name = base
        var n = 2
        while (map.has(name)) {
            name = "$base ($n)"
            n++
        }
        resolved.put("name", name)
        map.put(name, resolved.toString())
        Prefs.setSavedCustomThemes(c, map.toString())
        apply(c, resolved)
        return name
    }

    private fun embedImages(c: Context, o: JSONObject) {
        SINGLE_IMAGES.forEach { key ->
            val b64 = readFileB64(o.optString(key, ""))
            if (b64 != null) {
                o.put("img_$key", b64)
                o.put(key, EMBED + key)
            }
        }
        val nav = try { JSONObject(o.optString("navButtonImages", "{}")) } catch (_: Exception) { null }
        if (nav != null) {
            nav.keys().forEach { tab ->
                val b64 = readFileB64(nav.optString(tab, ""))
                if (b64 != null) {
                    val token = "nav_$tab"
                    o.put("img_$token", b64)
                    nav.put(tab, EMBED + token)
                }
            }
            o.put("navButtonImages", nav.toString())
        }
        val stickers = try { JSONArray(o.optString("npStickers", "[]")) } catch (_: Exception) { null }
        if (stickers != null) {
            for (i in 0 until stickers.length()) {
                val st = stickers.optJSONObject(i) ?: continue
                val b64 = readFileB64(st.optString("image", ""))
                if (b64 != null) {
                    val token = "sticker_$i"
                    o.put("img_$token", b64)
                    st.put("image", EMBED + token)
                }
            }
            o.put("npStickers", stickers.toString())
        }
    }

    private fun readFileB64(path: String): String? {
        if (path.isBlank() || !path.startsWith("file://")) return null
        return try {
            val f = File(Uri.parse(path).path ?: return null)
            if (!f.exists() || f.length() == 0L) null
            else Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun materializeImages(c: Context, src: JSONObject): JSONObject {
        val o = JSONObject(src.toString())
        val dir = File(c.filesDir, "images").apply { mkdirs() }
        SINGLE_IMAGES.forEach { key ->
            val v = o.optString(key, "")
            if (v.startsWith(EMBED)) {
                val token = v.removePrefix(EMBED)
                o.put(key, writeImage(c, dir, token, o.optString("img_$token", "")))
            }
        }
        val nav = try { JSONObject(o.optString("navButtonImages", "{}")) } catch (_: Exception) { null }
        if (nav != null) {
            nav.keys().forEach { tab ->
                val v = nav.optString(tab, "")
                if (v.startsWith(EMBED)) {
                    val token = v.removePrefix(EMBED)
                    nav.put(tab, writeImage(c, dir, token, o.optString("img_$token", "")))
                }
            }
            o.put("navButtonImages", nav.toString())
        }
        val stickers = try { JSONArray(o.optString("npStickers", "[]")) } catch (_: Exception) { null }
        if (stickers != null) {
            for (i in 0 until stickers.length()) {
                val st = stickers.optJSONObject(i) ?: continue
                val v = st.optString("image", "")
                if (v.startsWith(EMBED)) {
                    val token = v.removePrefix(EMBED)
                    st.put("image", writeImage(c, dir, token, o.optString("img_$token", "")))
                }
            }
            o.put("npStickers", stickers.toString())
        }
        return o
    }

    private fun writeImage(c: Context, dir: File, token: String, b64: String): String {
        if (b64.isBlank()) return ""
        return try {
            val safe = token.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).ifBlank { "image" }
            val f = File(dir, "theme_${System.currentTimeMillis()}_${safe}.png")
            f.writeBytes(Base64.decode(b64, Base64.NO_WRAP))
            Uri.fromFile(f).toString()
        } catch (_: Exception) {
            ""
        }
    }
}
