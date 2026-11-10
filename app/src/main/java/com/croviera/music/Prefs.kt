package com.croviera.music

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

// Keep the original DataStore file name so existing settings survive the update.
private val Context.ds: DataStore<Preferences> by preferencesDataStore("physic")

object Prefs {
    private val THEME = stringPreferencesKey("theme")
    private val CUSTOM_BG = stringPreferencesKey("custom_bg")
    private val CUSTOM_ACCENT = stringPreferencesKey("custom_accent")
    private val CUSTOM_TEXT = stringPreferencesKey("custom_text")
    private val CUSTOM_SURFACE = stringPreferencesKey("custom_surface")
    private val CUSTOM_SUBTEXT = stringPreferencesKey("custom_subtext")
    private val CUSTOM_BORDER = stringPreferencesKey("custom_border")
    private val CUSTOM_GRADIENT_START = stringPreferencesKey("custom_gradient_start")
    private val CUSTOM_GRADIENT_END = stringPreferencesKey("custom_gradient_end")
    private val CUSTOM_GRADIENT_ANGLE = intPreferencesKey("custom_gradient_angle")
    private val ROUNDED = booleanPreferencesKey("rounded")
    private val FOLDERS = stringPreferencesKey("folders")
    private val SHUFFLE = booleanPreferencesKey("shuffle")
    private val REPEAT = intPreferencesKey("repeat") // 0 off, 1 all, 2 one

    private val WELCOME = stringPreferencesKey("welcome")
    private val PROFILE_NAME = stringPreferencesKey("profile_name")
    private val PROFILE_PIC = stringPreferencesKey("profile_pic")
    private val PROFILE_BANNER = stringPreferencesKey("profile_banner")
    private val PROFILE_BIO = stringPreferencesKey("profile_bio")
    private val PROFILE_PRONOUNS = stringPreferencesKey("profile_pronouns")
    private val FAV_SONG = stringPreferencesKey("fav_song")
    private val CUSTOM_DISC = stringPreferencesKey("custom_disc")
    private val CUSTOM_BG_IMG = stringPreferencesKey("custom_bg_img")
    private val CUSTOM_SQUARE_IMG = stringPreferencesKey("custom_square_img")
    private val SQUARE_IMG_ALPHA = intPreferencesKey("square_img_alpha")
    private val PANEL_SHADOW = booleanPreferencesKey("panel_shadow")
    private val PANEL_SHADOW_COLOR = stringPreferencesKey("panel_shadow_color")
    private val PANEL_SHADOW_OFFSET = intPreferencesKey("panel_shadow_offset")
    private val PANEL_SHADOW_ALPHA = intPreferencesKey("panel_shadow_alpha")
    private val PROFILE_LOCATION = stringPreferencesKey("profile_location")
    private val PROFILE_FAV_ALBUM = stringPreferencesKey("profile_fav_album")
    private val FONT_NAME = stringPreferencesKey("font_name")
    private val ONBOARDED = booleanPreferencesKey("onboarded")
    private val PAUSE_UNPLUG = booleanPreferencesKey("pause_unplug")
    private val UPDATE_CHECK = booleanPreferencesKey("update_check")
    private val PLAY_COUNT_PCT = intPreferencesKey("play_count_pct")
    private val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
    private val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
    private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    private val ROW_PAD = intPreferencesKey("row_pad")
    private val COVER_SIZE = intPreferencesKey("cover_size")
    private val NP_SIZE = intPreferencesKey("np_size")
    private val EQ_VERTICAL = booleanPreferencesKey("eq_vertical")
    private val SAVED_CUSTOM_THEMES = stringPreferencesKey("saved_custom_themes")
    private val BG_ALPHA = intPreferencesKey("bg_alpha")
    private val UI_ALPHA = intPreferencesKey("ui_alpha")
    private val FONT_SCALE = floatPreferencesKey("font_scale")
    private val LINE_HEIGHT_SCALE = floatPreferencesKey("line_height_scale")
    private val CORNER_SIZE = intPreferencesKey("corner_size")
    private val BORDER_WIDTH = intPreferencesKey("border_width")
    private val NAV_POSITION = stringPreferencesKey("nav_position")
    private val NAV_LABELS = stringPreferencesKey("nav_labels")
    private val NAV_ICONS = stringPreferencesKey("nav_icons")
    private val NAV_NERD = booleanPreferencesKey("nav_nerd")
    private val TRANSITION = stringPreferencesKey("transition")
    private val NP_LABEL = stringPreferencesKey("np_label")
    private val NP_EXPANDED_COVER = intPreferencesKey("np_expanded_cover")
    private val NP_EXPANDED_QUEUE = booleanPreferencesKey("np_expanded_queue")
    private val NP_EXPANDED_LYRICS = booleanPreferencesKey("np_expanded_lyrics")
    private val NP_BG_IMG = stringPreferencesKey("np_bg_img")
    private val NP_BG_ALPHA = intPreferencesKey("np_bg_alpha")
    private val NP_BG_COLOR = stringPreferencesKey("np_bg_color")
    private val NP_TEXT_COLOR = stringPreferencesKey("np_text_color")
    private val NP_SUBTEXT_COLOR = stringPreferencesKey("np_subtext_color")
    private val NP_ACCENT_COLOR = stringPreferencesKey("np_accent_color")
    private val NP_SHOW_COVER = booleanPreferencesKey("np_show_cover")
    private val NP_SHOW_PROGRESS = booleanPreferencesKey("np_show_progress")
    private val NP_SHOW_TITLE = booleanPreferencesKey("np_show_title")
    private val NP_SHOW_ARTIST = booleanPreferencesKey("np_show_artist")
    private val NP_SHOW_INFO = booleanPreferencesKey("np_show_info")
    private val NP_SHOW_CONTROLS = booleanPreferencesKey("np_show_controls")
    private val NP_SHOW_ACTIONS = booleanPreferencesKey("np_show_actions")
    private val NP_SHOW_QUEUE = booleanPreferencesKey("np_show_queue")
    private val NP_SHOW_LYRICS = booleanPreferencesKey("np_show_lyrics")
    private val NP_MAX_WIDTH = intPreferencesKey("np_max_width")
    private val NP_STICKERS = stringPreferencesKey("np_stickers")
    private val NAV_BUTTON_IMAGES = stringPreferencesKey("nav_button_images")
    private val NAV_IMAGE_SIZE = intPreferencesKey("nav_image_size")
    private val NAV_ITEM_WIDTH = intPreferencesKey("nav_item_width")
    private val NAV_ITEM_HEIGHT = intPreferencesKey("nav_item_height")
    private val NAV_ITEM_SPACING = intPreferencesKey("nav_item_spacing")
    private val NAV_PADDING = intPreferencesKey("nav_padding")
    private val NAV_BAR_THICKNESS = intPreferencesKey("nav_bar_thickness")
    private val NAV_BG_ALPHA = intPreferencesKey("nav_bg_alpha")
    private val NAV_BG_COLOR = stringPreferencesKey("nav_bg_color")
    private val NAV_IMAGE_FIT = stringPreferencesKey("nav_image_fit")
    private val NAV_TEXT_SIZE = intPreferencesKey("nav_text_size")
    private val NAV_NERD_SIZE = intPreferencesKey("nav_nerd_size")
    private val NAV_SELECTED_ALPHA = intPreferencesKey("nav_selected_alpha")
    private val NP_COVER_RADIUS = intPreferencesKey("np_cover_radius")
    private val NP_COVER_SIZE = intPreferencesKey("np_cover_size")
    private val NP_TITLE_SIZE = intPreferencesKey("np_title_size")
    private val NP_ARTIST_SIZE = intPreferencesKey("np_artist_size")
    private val NP_DIM = intPreferencesKey("np_dim")
    private val NP_BG_FIT = stringPreferencesKey("np_bg_fit")
    private val NP_BUTTON_RADIUS = intPreferencesKey("np_button_radius")
    // NOTE: 0f = "auto" (no explicit lineHeight applied) so the app keeps the
    // same look as 1.5.2.1 by default. Anything > 0 is treated as a multiplier.

    fun theme(c: Context) = c.ds.data.map { it[THEME] ?: "Croviera" }
    fun rounded(c: Context) = c.ds.data.map { it[ROUNDED] ?: false }
    fun folders(c: Context) = c.ds.data.map { (it[FOLDERS] ?: "").split("\n").filter{ f -> f.isNotBlank() }.toSet() }
    fun shuffle(c: Context) = c.ds.data.map { it[SHUFFLE] ?: false }
    fun repeat(c: Context) = c.ds.data.map { it[REPEAT] ?: 0 }
    fun custom(c: Context) = c.ds.data.map {
        listOf(it[CUSTOM_BG], it[CUSTOM_SURFACE], it[CUSTOM_TEXT], it[CUSTOM_SUBTEXT], it[CUSTOM_ACCENT], it[CUSTOM_BORDER])
    }
    data class GradientPrefs(val start: String, val end: String, val angle: Int)
    fun gradient(c: Context) = c.ds.data.map {
        GradientPrefs(it[CUSTOM_GRADIENT_START] ?: "", it[CUSTOM_GRADIENT_END] ?: "", it[CUSTOM_GRADIENT_ANGLE] ?: 135)
    }
    fun cornerSize(c: Context) = c.ds.data.map { it[CORNER_SIZE] ?: 0 }
    fun borderWidth(c: Context) = c.ds.data.map { it[BORDER_WIDTH] ?: 1 }
    fun navPosition(c: Context) = c.ds.data.map { it[NAV_POSITION] ?: "top" }
    fun navLabels(c: Context) = c.ds.data.map { it[NAV_LABELS] ?: "{}" }
    fun navIcons(c: Context) = c.ds.data.map { it[NAV_ICONS] ?: "{}" }
    fun navNerd(c: Context) = c.ds.data.map { it[NAV_NERD] ?: false }
    fun transition(c: Context) = c.ds.data.map { it[TRANSITION] ?: "fade" }
    fun npLabel(c: Context) = c.ds.data.map { it[NP_LABEL] ?: "Now playing!" }
    fun npExpandedCover(c: Context) = c.ds.data.map { it[NP_EXPANDED_COVER] ?: 180 }
    fun npExpandedQueue(c: Context) = c.ds.data.map { it[NP_EXPANDED_QUEUE] ?: true }
    fun npExpandedLyrics(c: Context) = c.ds.data.map { it[NP_EXPANDED_LYRICS] ?: true }
    fun npBgImg(c: Context) = c.ds.data.map { it[NP_BG_IMG] ?: "" }
    suspend fun setNpBgImg(c: Context, v: String) = c.ds.edit { it[NP_BG_IMG] = v }
    fun npBgAlpha(c: Context) = c.ds.data.map { it[NP_BG_ALPHA] ?: 45 }
    suspend fun setNpBgAlpha(c: Context, v: Int) = c.ds.edit { it[NP_BG_ALPHA] = v }
    fun npBgColor(c: Context) = c.ds.data.map { it[NP_BG_COLOR] ?: "" }
    suspend fun setNpBgColor(c: Context, v: String) = c.ds.edit { it[NP_BG_COLOR] = v }
    fun npTextColor(c: Context) = c.ds.data.map { it[NP_TEXT_COLOR] ?: "" }
    suspend fun setNpTextColor(c: Context, v: String) = c.ds.edit { it[NP_TEXT_COLOR] = v }
    fun npSubtextColor(c: Context) = c.ds.data.map { it[NP_SUBTEXT_COLOR] ?: "" }
    suspend fun setNpSubtextColor(c: Context, v: String) = c.ds.edit { it[NP_SUBTEXT_COLOR] = v }
    fun npAccentColor(c: Context) = c.ds.data.map { it[NP_ACCENT_COLOR] ?: "" }
    suspend fun setNpAccentColor(c: Context, v: String) = c.ds.edit { it[NP_ACCENT_COLOR] = v }
    fun npShowCover(c: Context) = c.ds.data.map { it[NP_SHOW_COVER] ?: true }
    suspend fun setNpShowCover(c: Context, v: Boolean) = c.ds.edit { it[NP_SHOW_COVER] = v }
    fun npShowProgress(c: Context) = c.ds.data.map { it[NP_SHOW_PROGRESS] ?: true }
    suspend fun setNpShowProgress(c: Context, v: Boolean) = c.ds.edit { it[NP_SHOW_PROGRESS] = v }
    fun npShowTitle(c: Context) = c.ds.data.map { it[NP_SHOW_TITLE] ?: true }
    suspend fun setNpShowTitle(c: Context, v: Boolean) = c.ds.edit { it[NP_SHOW_TITLE] = v }
    fun npShowArtist(c: Context) = c.ds.data.map { it[NP_SHOW_ARTIST] ?: true }
    suspend fun setNpShowArtist(c: Context, v: Boolean) = c.ds.edit { it[NP_SHOW_ARTIST] = v }
    fun npShowInfo(c: Context) = c.ds.data.map { it[NP_SHOW_INFO] ?: true }
    suspend fun setNpShowInfo(c: Context, v: Boolean) = c.ds.edit { it[NP_SHOW_INFO] = v }
    fun npShowControls(c: Context) = c.ds.data.map { it[NP_SHOW_CONTROLS] ?: true }
    suspend fun setNpShowControls(c: Context, v: Boolean) = c.ds.edit { it[NP_SHOW_CONTROLS] = v }
    fun npShowActions(c: Context) = c.ds.data.map { it[NP_SHOW_ACTIONS] ?: true }
    suspend fun setNpShowActions(c: Context, v: Boolean) = c.ds.edit { it[NP_SHOW_ACTIONS] = v }
    fun npShowQueue(c: Context) = c.ds.data.map { it[NP_SHOW_QUEUE] ?: true }
    suspend fun setNpShowQueue(c: Context, v: Boolean) = c.ds.edit { it[NP_SHOW_QUEUE] = v }
    fun npShowLyrics(c: Context) = c.ds.data.map { it[NP_SHOW_LYRICS] ?: true }
    suspend fun setNpShowLyrics(c: Context, v: Boolean) = c.ds.edit { it[NP_SHOW_LYRICS] = v }
    fun npMaxWidth(c: Context) = c.ds.data.map { it[NP_MAX_WIDTH] ?: 420 }
    suspend fun setNpMaxWidth(c: Context, v: Int) = c.ds.edit { it[NP_MAX_WIDTH] = v }
    fun npStickers(c: Context) = c.ds.data.map { it[NP_STICKERS] ?: "[]" }
    suspend fun setNpStickers(c: Context, v: String) = c.ds.edit { it[NP_STICKERS] = v }
    fun navButtonImages(c: Context) = c.ds.data.map { it[NAV_BUTTON_IMAGES] ?: "{}" }
    suspend fun setNavButtonImages(c: Context, v: String) = c.ds.edit { it[NAV_BUTTON_IMAGES] = v }
    fun navImageSize(c: Context) = c.ds.data.map { it[NAV_IMAGE_SIZE] ?: 28 }
    suspend fun setNavImageSize(c: Context, v: Int) = c.ds.edit { it[NAV_IMAGE_SIZE] = v }
    fun navItemWidth(c: Context) = c.ds.data.map { it[NAV_ITEM_WIDTH] ?: 0 }
    suspend fun setNavItemWidth(c: Context, v: Int) = c.ds.edit { it[NAV_ITEM_WIDTH] = v }
    fun navItemHeight(c: Context) = c.ds.data.map { it[NAV_ITEM_HEIGHT] ?: 0 }
    suspend fun setNavItemHeight(c: Context, v: Int) = c.ds.edit { it[NAV_ITEM_HEIGHT] = v }
    fun navItemSpacing(c: Context) = c.ds.data.map { it[NAV_ITEM_SPACING] ?: 2 }
    suspend fun setNavItemSpacing(c: Context, v: Int) = c.ds.edit { it[NAV_ITEM_SPACING] = v }
    fun navPadding(c: Context) = c.ds.data.map { it[NAV_PADDING] ?: 4 }
    suspend fun setNavPadding(c: Context, v: Int) = c.ds.edit { it[NAV_PADDING] = v }
    fun navBarThickness(c: Context) = c.ds.data.map { it[NAV_BAR_THICKNESS] ?: 0 }
    suspend fun setNavBarThickness(c: Context, v: Int) = c.ds.edit { it[NAV_BAR_THICKNESS] = v }
    fun navBgAlpha(c: Context) = c.ds.data.map { it[NAV_BG_ALPHA] ?: 94 }
    suspend fun setNavBgAlpha(c: Context, v: Int) = c.ds.edit { it[NAV_BG_ALPHA] = v }
    fun navBgColor(c: Context) = c.ds.data.map { it[NAV_BG_COLOR] ?: "" }
    suspend fun setNavBgColor(c: Context, v: String) = c.ds.edit { it[NAV_BG_COLOR] = v }
    fun navImageFit(c: Context) = c.ds.data.map { it[NAV_IMAGE_FIT] ?: "crop" }
    suspend fun setNavImageFit(c: Context, v: String) = c.ds.edit { it[NAV_IMAGE_FIT] = v }
    fun navTextSize(c: Context) = c.ds.data.map { it[NAV_TEXT_SIZE] ?: 14 }
    suspend fun setNavTextSize(c: Context, v: Int) = c.ds.edit { it[NAV_TEXT_SIZE] = v }
    fun navNerdSize(c: Context) = c.ds.data.map { it[NAV_NERD_SIZE] ?: 20 }
    suspend fun setNavNerdSize(c: Context, v: Int) = c.ds.edit { it[NAV_NERD_SIZE] = v }
    fun navSelectedAlpha(c: Context) = c.ds.data.map { it[NAV_SELECTED_ALPHA] ?: 14 }
    suspend fun setNavSelectedAlpha(c: Context, v: Int) = c.ds.edit { it[NAV_SELECTED_ALPHA] = v }
    fun npCoverRadius(c: Context) = c.ds.data.map { it[NP_COVER_RADIUS] ?: 0 }
    suspend fun setNpCoverRadius(c: Context, v: Int) = c.ds.edit { it[NP_COVER_RADIUS] = v }
    fun npCoverSize(c: Context) = c.ds.data.map { it[NP_COVER_SIZE] ?: 0 }
    suspend fun setNpCoverSize(c: Context, v: Int) = c.ds.edit { it[NP_COVER_SIZE] = v }
    fun npTitleSize(c: Context) = c.ds.data.map { it[NP_TITLE_SIZE] ?: 24 }
    suspend fun setNpTitleSize(c: Context, v: Int) = c.ds.edit { it[NP_TITLE_SIZE] = v }
    fun npArtistSize(c: Context) = c.ds.data.map { it[NP_ARTIST_SIZE] ?: 15 }
    suspend fun setNpArtistSize(c: Context, v: Int) = c.ds.edit { it[NP_ARTIST_SIZE] = v }
    fun npDim(c: Context) = c.ds.data.map { it[NP_DIM] ?: 0 }
    suspend fun setNpDim(c: Context, v: Int) = c.ds.edit { it[NP_DIM] = v }
    fun npBgFit(c: Context) = c.ds.data.map { it[NP_BG_FIT] ?: "crop" }
    suspend fun setNpBgFit(c: Context, v: String) = c.ds.edit { it[NP_BG_FIT] = v }
    fun npButtonRadius(c: Context) = c.ds.data.map { it[NP_BUTTON_RADIUS] ?: 18 }
    suspend fun setNpButtonRadius(c: Context, v: Int) = c.ds.edit { it[NP_BUTTON_RADIUS] = v }
    fun welcome(c: Context) = c.ds.data.map { it[WELCOME] ?: "Welcome back." }
    fun profileName(c: Context) = c.ds.data.map { it[PROFILE_NAME] ?: "listener" }
    fun profilePic(c: Context) = c.ds.data.map { it[PROFILE_PIC] ?: "" }
    fun profileBanner(c: Context) = c.ds.data.map { it[PROFILE_BANNER] ?: "" }
    fun profileBio(c: Context) = c.ds.data.map { it[PROFILE_BIO] ?: "" }
    fun profilePronouns(c: Context) = c.ds.data.map { it[PROFILE_PRONOUNS] ?: "" }
    fun favSong(c: Context) = c.ds.data.map { it[FAV_SONG] ?: "" }
    fun customDisc(c: Context) = c.ds.data.map { it[CUSTOM_DISC] ?: "" }
    fun customBgImg(c: Context) = c.ds.data.map { it[CUSTOM_BG_IMG] ?: "" }
    fun customSquareImg(c: Context) = c.ds.data.map { it[CUSTOM_SQUARE_IMG] ?: "" }
    suspend fun setCustomSquareImg(c: Context, v: String) = c.ds.edit { it[CUSTOM_SQUARE_IMG] = v }
    fun squareImgAlpha(c: Context) = c.ds.data.map { it[SQUARE_IMG_ALPHA] ?: 35 }
    suspend fun setSquareImgAlpha(c: Context, v: Int) = c.ds.edit { it[SQUARE_IMG_ALPHA] = v }
    fun panelShadow(c: Context) = c.ds.data.map { it[PANEL_SHADOW] ?: false }
    suspend fun setPanelShadow(c: Context, v: Boolean) = c.ds.edit { it[PANEL_SHADOW] = v }
    fun panelShadowColor(c: Context) = c.ds.data.map { it[PANEL_SHADOW_COLOR] ?: "#8A1F2D" }
    suspend fun setPanelShadowColor(c: Context, v: String) = c.ds.edit { it[PANEL_SHADOW_COLOR] = v }
    fun panelShadowOffset(c: Context) = c.ds.data.map { it[PANEL_SHADOW_OFFSET] ?: 4 }
    suspend fun setPanelShadowOffset(c: Context, v: Int) = c.ds.edit { it[PANEL_SHADOW_OFFSET] = v }
    fun panelShadowAlpha(c: Context) = c.ds.data.map { it[PANEL_SHADOW_ALPHA] ?: 55 }
    suspend fun setPanelShadowAlpha(c: Context, v: Int) = c.ds.edit { it[PANEL_SHADOW_ALPHA] = v }
    fun profileLocation(c: Context) = c.ds.data.map { it[PROFILE_LOCATION] ?: "" }
    fun profileFavAlbum(c: Context) = c.ds.data.map { it[PROFILE_FAV_ALBUM] ?: "" }
    fun fontName(c: Context) = c.ds.data.map { it[FONT_NAME] ?: "DM Mono" }
    fun onboarded(c: Context) = c.ds.data.map { it[ONBOARDED] ?: false }
    suspend fun setOnboarded(c: Context, v: Boolean) = c.ds.edit { it[ONBOARDED] = v }
    fun pauseUnplug(c: Context) = c.ds.data.map { it[PAUSE_UNPLUG] ?: true }
    suspend fun setPauseUnplug(c: Context, v: Boolean) = c.ds.edit { it[PAUSE_UNPLUG] = v }
    fun updateCheck(c: Context) = c.ds.data.map { it[UPDATE_CHECK] ?: true }
    suspend fun setUpdateCheck(c: Context, v: Boolean) = c.ds.edit { it[UPDATE_CHECK] = v }
    fun playCountPct(c: Context) = c.ds.data.map { it[PLAY_COUNT_PCT] ?: 20 }
    suspend fun setPlayCountPct(c: Context, v: Int) = c.ds.edit { it[PLAY_COUNT_PCT] = v }
    fun skipSilence(c: Context) = c.ds.data.map { it[SKIP_SILENCE] ?: false }
    suspend fun setSkipSilence(c: Context, v: Boolean) = c.ds.edit { it[SKIP_SILENCE] = v }
    fun crossfadeMs(c: Context) = c.ds.data.map { it[CROSSFADE_MS] ?: 0 }
    suspend fun setCrossfadeMs(c: Context, v: Int) = c.ds.edit { it[CROSSFADE_MS] = v }
    fun keepScreenOn(c: Context) = c.ds.data.map { it[KEEP_SCREEN_ON] ?: true }
    suspend fun setKeepScreenOn(c: Context, v: Boolean) = c.ds.edit { it[KEEP_SCREEN_ON] = v }
    fun rowPad(c: Context) = c.ds.data.map { it[ROW_PAD] ?: 8 }
    suspend fun setRowPad(c: Context, v: Int) = c.ds.edit { it[ROW_PAD] = v }
    fun coverSize(c: Context) = c.ds.data.map { it[COVER_SIZE] ?: 40 }
    suspend fun setCoverSize(c: Context, v: Int) = c.ds.edit { it[COVER_SIZE] = v }
    fun npSize(c: Context) = c.ds.data.map { it[NP_SIZE] ?: 52 }
    suspend fun setNpSize(c: Context, v: Int) = c.ds.edit { it[NP_SIZE] = v }
    fun eqVertical(c: Context) = c.ds.data.map { it[EQ_VERTICAL] ?: false }
    suspend fun setEqVertical(c: Context, v: Boolean) = c.ds.edit { it[EQ_VERTICAL] = v }
    fun savedCustomThemes(c: Context) = c.ds.data.map { it[SAVED_CUSTOM_THEMES] ?: "{}" }
    suspend fun setSavedCustomThemes(c: Context, v: String) = c.ds.edit { it[SAVED_CUSTOM_THEMES] = v }
    fun bgAlpha(c: Context) = c.ds.data.map { it[BG_ALPHA] ?: 45 }
    suspend fun setBgAlpha(c: Context, v: Int) = c.ds.edit { it[BG_ALPHA] = v }
    fun uiAlpha(c: Context) = c.ds.data.map { it[UI_ALPHA] ?: 100 }
    private val LANGUAGE = stringPreferencesKey("language")
    fun language(c: Context) = c.ds.data.map { it[LANGUAGE] ?: "" }
    suspend fun setLanguage(c: Context, v: String) = c.ds.edit { it[LANGUAGE] = v }
    suspend fun setUiAlpha(c: Context, v: Int) = c.ds.edit { it[UI_ALPHA] = v }
    fun fontScale(c: Context) = c.ds.data.map { it[FONT_SCALE] ?: 1.0f }
    suspend fun setFontScale(c: Context, v: Float) = c.ds.edit { it[FONT_SCALE] = v }
    fun lineHeightScale(c: Context) = c.ds.data.map { it[LINE_HEIGHT_SCALE] ?: 0f }
    suspend fun setLineHeightScale(c: Context, v: Float) = c.ds.edit { it[LINE_HEIGHT_SCALE] = v }
    suspend fun setCornerSize(c: Context, v: Int) = c.ds.edit { it[CORNER_SIZE] = v }
    suspend fun setBorderWidth(c: Context, v: Int) = c.ds.edit { it[BORDER_WIDTH] = v }
    suspend fun setNavPosition(c: Context, v: String) = c.ds.edit { it[NAV_POSITION] = v }
    suspend fun setNavLabels(c: Context, v: String) = c.ds.edit { it[NAV_LABELS] = v }
    suspend fun setNavIcons(c: Context, v: String) = c.ds.edit { it[NAV_ICONS] = v }
    suspend fun setNavNerd(c: Context, v: Boolean) = c.ds.edit { it[NAV_NERD] = v }
    suspend fun setTransition(c: Context, v: String) = c.ds.edit { it[TRANSITION] = v }
    suspend fun setNpLabel(c: Context, v: String) = c.ds.edit { it[NP_LABEL] = v }
    suspend fun setNpExpandedCover(c: Context, v: Int) = c.ds.edit { it[NP_EXPANDED_COVER] = v }
    suspend fun setNpExpandedQueue(c: Context, v: Boolean) = c.ds.edit { it[NP_EXPANDED_QUEUE] = v }
    suspend fun setNpExpandedLyrics(c: Context, v: Boolean) = c.ds.edit { it[NP_EXPANDED_LYRICS] = v }
    suspend fun setGradient(c: Context, start: String, end: String, angle: Int) = c.ds.edit {
        it[CUSTOM_GRADIENT_START] = start
        it[CUSTOM_GRADIENT_END] = end
        it[CUSTOM_GRADIENT_ANGLE] = angle
    }

    data class Snapshot(
        val theme: String = "Croviera",
        val rounded: Boolean = false,
        val folders: Set<String> = emptySet(),
        val custom: List<String?> = List(6) { null },
        val fontName: String = "DM Mono",
        val bgImg: String = "",
        val bgAlpha: Int = 45,
        val uiAlpha: Int = 100,
        val fontScale: Float = 1f,
        val lineHeightScale: Float = 0f,
        val welcome: String = "Welcome back.",
        val profileName: String = "listener",
        val profilePic: String = "",
        val onboarded: Boolean = false,
        val keepScreenOn: Boolean = true,
        val cornerSize: Int = 0,
        val borderWidth: Int = 1,
        val navPosition: String = "top",
        val navLabels: String = "{}",
        val navIcons: String = "{}",
        val navNerd: Boolean = false,
        val transition: String = "fade",
        val npLabel: String = "Now playing!",
        val npExpandedCover: Int = 180,
        val npExpandedQueue: Boolean = true,
        val npExpandedLyrics: Boolean = true,
        val npBgImg: String = "",
        val npBgAlpha: Int = 45,
        val npBgColor: String = "",
        val npTextColor: String = "",
        val npSubtextColor: String = "",
        val npAccentColor: String = "",
        val npShowCover: Boolean = true,
        val npShowProgress: Boolean = true,
        val npShowTitle: Boolean = true,
        val npShowArtist: Boolean = true,
        val npShowInfo: Boolean = true,
        val npShowControls: Boolean = true,
        val npShowActions: Boolean = true,
        val npShowQueue: Boolean = true,
        val npShowLyrics: Boolean = true,
        val npMaxWidth: Int = 420,
        val npStickers: String = "[]",
        val navButtonImages: String = "{}",
        val navImageSize: Int = 28,
        val navItemWidth: Int = 0,
        val navItemHeight: Int = 0,
        val navItemSpacing: Int = 2,
        val navPadding: Int = 4,
        val navBarThickness: Int = 0,
        val navBgAlpha: Int = 94,
        val navBgColor: String = "",
        val navImageFit: String = "crop",
        val navTextSize: Int = 14,
        val navNerdSize: Int = 20,
        val navSelectedAlpha: Int = 14,
        val npCoverRadius: Int = 0,
        val npCoverSize: Int = 0,
        val npTitleSize: Int = 24,
        val npArtistSize: Int = 15,
        val npDim: Int = 0,
        val npBgFit: String = "crop",
        val npButtonRadius: Int = 18,
        val gradient: GradientPrefs = GradientPrefs("", "", 135),
        val squareImg: String = "",
        val squareImgAlpha: Int = 35,
        val customDisc: String = "",
        val panelShadow: Boolean = false,
        val panelShadowColor: String = "#8A1F2D",
        val panelShadowOffset: Int = 4,
        val panelShadowAlpha: Int = 55,
    )

    /** Read the first DataStore value set so the UI never flashes defaults on launch/rotation. */
    suspend fun snapshot(c: Context): Snapshot {
        repairTypes(c)
        val p = c.ds.data.first()
        return Snapshot(
            theme = p[THEME] ?: "Croviera",
            rounded = p[ROUNDED] ?: false,
            folders = (p[FOLDERS] ?: "").split("\n").filter { it.isNotBlank() }.toSet(),
            custom = listOf(p[CUSTOM_BG], p[CUSTOM_SURFACE], p[CUSTOM_TEXT], p[CUSTOM_SUBTEXT], p[CUSTOM_ACCENT], p[CUSTOM_BORDER]),
            fontName = p[FONT_NAME] ?: "DM Mono",
            bgImg = p[CUSTOM_BG_IMG] ?: "",
            bgAlpha = p[BG_ALPHA] ?: 45,
            uiAlpha = p[UI_ALPHA] ?: 100,
            fontScale = p[FONT_SCALE] ?: 1f,
            lineHeightScale = p[LINE_HEIGHT_SCALE] ?: 0f,
            welcome = p[WELCOME] ?: "Welcome back.",
            profileName = p[PROFILE_NAME] ?: "listener",
            profilePic = p[PROFILE_PIC] ?: "",
            onboarded = p[ONBOARDED] ?: false,
            keepScreenOn = p[KEEP_SCREEN_ON] ?: true,
            cornerSize = p[CORNER_SIZE] ?: 0,
            borderWidth = p[BORDER_WIDTH] ?: 1,
            navPosition = p[NAV_POSITION] ?: "top",
            navLabels = p[NAV_LABELS] ?: "{}",
            navIcons = p[NAV_ICONS] ?: "{}",
            navNerd = p[NAV_NERD] ?: false,
            transition = p[TRANSITION] ?: "fade",
            npLabel = p[NP_LABEL] ?: "Now playing!",
            npExpandedCover = p[NP_EXPANDED_COVER] ?: 180,
            npExpandedQueue = p[NP_EXPANDED_QUEUE] ?: true,
            npExpandedLyrics = p[NP_EXPANDED_LYRICS] ?: true,
            npBgImg = p[NP_BG_IMG] ?: "",
            npBgAlpha = p[NP_BG_ALPHA] ?: 45,
            npBgColor = p[NP_BG_COLOR] ?: "",
            npTextColor = p[NP_TEXT_COLOR] ?: "",
            npSubtextColor = p[NP_SUBTEXT_COLOR] ?: "",
            npAccentColor = p[NP_ACCENT_COLOR] ?: "",
            npShowCover = p[NP_SHOW_COVER] ?: true,
            npShowProgress = p[NP_SHOW_PROGRESS] ?: true,
            npShowTitle = p[NP_SHOW_TITLE] ?: true,
            npShowArtist = p[NP_SHOW_ARTIST] ?: true,
            npShowInfo = p[NP_SHOW_INFO] ?: true,
            npShowControls = p[NP_SHOW_CONTROLS] ?: true,
            npShowActions = p[NP_SHOW_ACTIONS] ?: true,
            npShowQueue = p[NP_SHOW_QUEUE] ?: true,
            npShowLyrics = p[NP_SHOW_LYRICS] ?: true,
            npMaxWidth = p[NP_MAX_WIDTH] ?: 420,
            npStickers = p[NP_STICKERS] ?: "[]",
            navButtonImages = p[NAV_BUTTON_IMAGES] ?: "{}",
            navImageSize = p[NAV_IMAGE_SIZE] ?: 28,
            navItemWidth = p[NAV_ITEM_WIDTH] ?: 0,
            navItemHeight = p[NAV_ITEM_HEIGHT] ?: 0,
            navItemSpacing = p[NAV_ITEM_SPACING] ?: 2,
            navPadding = p[NAV_PADDING] ?: 4,
            navBarThickness = p[NAV_BAR_THICKNESS] ?: 0,
            navBgAlpha = p[NAV_BG_ALPHA] ?: 94,
            navBgColor = p[NAV_BG_COLOR] ?: "",
            navImageFit = p[NAV_IMAGE_FIT] ?: "crop",
            navTextSize = p[NAV_TEXT_SIZE] ?: 14,
            navNerdSize = p[NAV_NERD_SIZE] ?: 20,
            navSelectedAlpha = p[NAV_SELECTED_ALPHA] ?: 14,
            npCoverRadius = p[NP_COVER_RADIUS] ?: 0,
            npCoverSize = p[NP_COVER_SIZE] ?: 0,
            npTitleSize = p[NP_TITLE_SIZE] ?: 24,
            npArtistSize = p[NP_ARTIST_SIZE] ?: 15,
            npDim = p[NP_DIM] ?: 0,
            npBgFit = p[NP_BG_FIT] ?: "crop",
            npButtonRadius = p[NP_BUTTON_RADIUS] ?: 18,
            gradient = GradientPrefs(p[CUSTOM_GRADIENT_START] ?: "", p[CUSTOM_GRADIENT_END] ?: "", p[CUSTOM_GRADIENT_ANGLE] ?: 135),
            squareImg = p[CUSTOM_SQUARE_IMG] ?: "",
            squareImgAlpha = p[SQUARE_IMG_ALPHA] ?: 35,
            customDisc = p[CUSTOM_DISC] ?: "",
            panelShadow = p[PANEL_SHADOW] ?: false,
            panelShadowColor = p[PANEL_SHADOW_COLOR] ?: "#8A1F2D",
            panelShadowOffset = p[PANEL_SHADOW_OFFSET] ?: 4,
            panelShadowAlpha = p[PANEL_SHADOW_ALPHA] ?: 55,
        )
    }

    /** Serialize every preference + data file into one JSON blob. */
    suspend fun exportAll(c: Context): String {
        val prefs = c.ds.data.first().asMap()
        val obj = org.json.JSONObject()
        obj.put("_version", 2)
        val p = org.json.JSONObject()
        // JSON cannot distinguish 1.0 from 1, so record the original DataStore
        // type of every preference. Without this, restoring a whole-number
        // Float under an Int key makes the next launch crash with a cast error.
        val types = org.json.JSONObject()
        for ((key, v) in prefs) {
            val k = key.name
            when (v) {
                is String -> { p.put(k, v); types.put(k, "string") }
                is Int -> { p.put(k, v); types.put(k, "int") }
                is Long -> { p.put(k, v); types.put(k, "long") }
                is Float -> { p.put(k, v.toDouble()); types.put(k, "float") }
                is Boolean -> { p.put(k, v); types.put(k, "boolean") }
                else -> {}
            }
        }
        obj.put("prefs", p)
        obj.put("_types", types)
        val stats = File(c.filesDir, "stats.json")
        if (stats.exists()) obj.put("stats", stats.readText())
        val pls = File(c.filesDir, "playlists.json")
        if (pls.exists()) obj.put("playlists", pls.readText())
        val plCovers = File(c.filesDir, "playlist_covers.json")
        if (plCovers.exists()) obj.put("playlist_covers", plCovers.readText())
        val coverDir = File(c.filesDir, "playlist_covers")
        if (coverDir.exists() && coverDir.listFiles()?.isNotEmpty() == true) {
            val co = org.json.JSONObject()
            coverDir.listFiles()?.forEach { f ->
                co.put(f.name, android.util.Base64.encodeToString(f.readBytes(), android.util.Base64.NO_WRAP))
            }
            obj.put("playlist_cover_files", co)
        }
        val meta = File(c.filesDir, "meta_overrides.json")
        if (meta.exists()) obj.put("meta", meta.readText())
        val fonts = File(c.filesDir, "fonts")
        if (fonts.exists() && fonts.listFiles()?.isNotEmpty() == true) {
            val fo = org.json.JSONObject()
            fonts.listFiles()?.forEach { f -> fo.put(f.name, f.readBytes().let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }) }
            obj.put("fonts", fo)
        }
        val lyrics = File(c.filesDir, "lyrics")
        if (lyrics.exists() && lyrics.listFiles()?.isNotEmpty() == true) {
            val lo = org.json.JSONObject()
            lyrics.listFiles()?.forEach { f -> lo.put(f.name, android.util.Base64.encodeToString(f.readBytes(), android.util.Base64.NO_WRAP)) }
            obj.put("lyrics", lo)
        }
        val images = File(c.filesDir, "images")
        if (images.exists() && images.listFiles()?.isNotEmpty() == true) {
            val io = org.json.JSONObject()
            images.listFiles()?.forEach { f -> io.put(f.name, android.util.Base64.encodeToString(f.readBytes(), android.util.Base64.NO_WRAP)) }
            obj.put("images", io)
        }
        return obj.toString(2)
    }

    /** Restore a blob produced by exportAll. */
    suspend fun importAll(c: Context, json: String) {
        val obj = try { org.json.JSONObject(json) } catch (e: Exception) { return }
        obj.optString("stats", "").takeIf { it.isNotBlank() }?.let { File(c.filesDir, "stats.json").writeText(it) }
        obj.optString("playlists", "").takeIf { it.isNotBlank() }?.let { File(c.filesDir, "playlists.json").writeText(it) }
        obj.optString("playlist_covers", "").takeIf { it.isNotBlank() }?.let { File(c.filesDir, "playlist_covers.json").writeText(it) }
        obj.optJSONObject("playlist_cover_files")?.let { co ->
            val dir = File(c.filesDir, "playlist_covers").apply { mkdirs() }
            co.keys().forEach { k ->
                try {
                    File(dir, k).writeBytes(android.util.Base64.decode(co.getString(k), android.util.Base64.NO_WRAP))
                } catch (_: Exception) {}
            }
        }
        obj.optString("meta", "").takeIf { it.isNotBlank() }?.let { File(c.filesDir, "meta_overrides.json").writeText(it) }
        obj.optJSONObject("fonts")?.let { fo ->
            val dir = File(c.filesDir, "fonts").apply { mkdirs() }
            fo.keys().forEach { k ->
                try {
                    File(dir, k).writeBytes(android.util.Base64.decode(fo.getString(k), android.util.Base64.NO_WRAP))
                } catch (_: Exception) {}
            }
        }
        obj.optJSONObject("lyrics")?.let { lo ->
            val dir = File(c.filesDir, "lyrics").apply { mkdirs() }
            lo.keys().forEach { k ->
                try {
                    File(dir, k).writeBytes(android.util.Base64.decode(lo.getString(k), android.util.Base64.NO_WRAP))
                } catch (_: Exception) {}
            }
        }
        obj.optJSONObject("images")?.let { io ->
            val dir = File(c.filesDir, "images").apply { mkdirs() }
            io.keys().forEach { k ->
                try {
                    File(dir, k).writeBytes(android.util.Base64.decode(io.getString(k), android.util.Base64.NO_WRAP))
                } catch (_: Exception) {}
            }
        }
        // Restore prefs last so image/font files already exist when the UI
        // recomposes and Coil tries to load the restored paths.
        obj.optJSONObject("prefs")?.let { p ->
            val types = obj.optJSONObject("_types")
            c.ds.edit { prefs ->
                p.keys().forEach { k ->
                    val v = p.opt(k)
                    if (v == null || v == org.json.JSONObject.NULL) return@forEach
                    val type = types?.optString(k, "").orEmpty().ifBlank { inferredType(k, v) }
                    when (type) {
                        "string" -> prefs[stringPreferencesKey(k)] = v.toString()
                        "boolean" -> prefs[booleanPreferencesKey(k)] = when (v) {
                            is Boolean -> v
                            is String -> v.equals("true", true)
                            is Number -> v.toInt() != 0
                            else -> false
                        }
                        "int" -> if (v is Number) prefs[intPreferencesKey(k)] = v.toInt()
                        "long" -> if (v is Number) prefs[longPreferencesKey(k)] = v.toLong()
                        "float" -> when (v) {
                            is Number -> prefs[floatPreferencesKey(k)] = v.toFloat()
                            is String -> v.toFloatOrNull()?.let { prefs[floatPreferencesKey(k)] = it }
                        }
                    }
                }
            }
        }
        // Old backups (and old backups restored by old builds) can still carry
        // an Int where the app expects a Float. Heal them before the UI reads.
        repairTypes(c)
    }

    private val FLOAT_KEYS = setOf("font_scale", "line_height_scale")

    private fun inferredType(key: String, v: Any?): String = when {
        key in FLOAT_KEYS -> "float"
        v is String -> "string"
        v is Boolean -> "boolean"
        v is Int -> "int"
        v is Long -> "long"
        v is Double || v is Float -> "float"
        else -> ""
    }

    /**
     * Repairs prefs written by 1.0/1.1 backup restore: JSON serialized
     * whole-number floats as ints, which makes typed DataStore reads throw and
     * closes the app on every launch until its data is cleared.
     */
    suspend fun repairTypes(c: Context) {
        val raw = c.ds.data.first().asMap()
        val fixes = raw.mapNotNull { (key, value) ->
            if (key.name in FLOAT_KEYS && value !is Float) {
                key.name to when (value) {
                    is Number -> value.toFloat()
                    is String -> value.toFloatOrNull() ?: 0f
                    else -> 0f
                }
            } else null
        }
        if (fixes.isEmpty()) return
        c.ds.edit { prefs ->
            fixes.forEach { (name, value) ->
                prefs.remove(stringPreferencesKey(name))
                prefs[floatPreferencesKey(name)] = value
            }
        }
    }

    suspend fun setWelcome(c: Context, v: String) = c.ds.edit { it[WELCOME] = v }
    suspend fun setProfileName(c: Context, v: String) = c.ds.edit { it[PROFILE_NAME] = v }
    suspend fun setProfilePic(c: Context, v: String) = c.ds.edit { it[PROFILE_PIC] = v }
    suspend fun setProfileBanner(c: Context, v: String) = c.ds.edit { it[PROFILE_BANNER] = v }
    suspend fun setProfileBio(c: Context, v: String) = c.ds.edit { it[PROFILE_BIO] = v }
    suspend fun setProfilePronouns(c: Context, v: String) = c.ds.edit { it[PROFILE_PRONOUNS] = v }
    suspend fun setFavSong(c: Context, v: String) = c.ds.edit { it[FAV_SONG] = v }
    suspend fun setCustomDisc(c: Context, v: String) = c.ds.edit { it[CUSTOM_DISC] = v }
    suspend fun setCustomBgImg(c: Context, v: String) = c.ds.edit { it[CUSTOM_BG_IMG] = v }
    suspend fun setProfileLocation(c: Context, v: String) = c.ds.edit { it[PROFILE_LOCATION] = v }
    suspend fun setProfileFavAlbum(c: Context, v: String) = c.ds.edit { it[PROFILE_FAV_ALBUM] = v }
    suspend fun setFontName(c: Context, v: String) = c.ds.edit { it[FONT_NAME] = v }

    suspend fun setTheme(c: Context, v: String) = c.ds.edit { it[THEME] = v }
    suspend fun setRounded(c: Context, v: Boolean) = c.ds.edit { it[ROUNDED] = v }
    suspend fun setFolders(c: Context, v: Set<String>) = c.ds.edit { it[FOLDERS] = v.joinToString("\n") }
    suspend fun setShuffle(c: Context, v: Boolean) = c.ds.edit { it[SHUFFLE] = v }
    suspend fun setRepeat(c: Context, v: Int) = c.ds.edit { it[REPEAT] = v }
    suspend fun setCustom(c: Context, bg: String?, surface: String?, text: String?, subtext: String?, accent: String?, border: String?) = c.ds.edit {
        if (bg != null) it[CUSTOM_BG] = bg
        if (surface != null) it[CUSTOM_SURFACE] = surface
        if (text != null) it[CUSTOM_TEXT] = text
        if (subtext != null) it[CUSTOM_SUBTEXT] = subtext
        if (accent != null) it[CUSTOM_ACCENT] = accent
        if (border != null) it[CUSTOM_BORDER] = border
    }

    fun artistImage(c: Context, artist: String) =
        c.ds.data.map { it[stringPreferencesKey("artist_img_$artist")] ?: "" }
    suspend fun setArtistImage(c: Context, artist: String, uri: String) =
        c.ds.edit { it[stringPreferencesKey("artist_img_$artist")] = uri }
}
