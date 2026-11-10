package com.croviera.music

import android.content.Context
import org.json.JSONObject
import java.io.File

object Stats {
    private const val FILE = "stats.json"
    private const val SEP = "\u001F"
    private fun file(c: Context) = File(c.filesDir, FILE)

    private fun load(c: Context): JSONObject {
        return try { JSONObject(file(c).readText()) } catch (e: Exception) { JSONObject() }
    }

    private fun save(c: Context, j: JSONObject) { file(c).writeText(j.toString()) }

    /**
     * Stats are tied to the song's metadata, never its file path. Moving a
     * file between folders therefore keeps its play count and last-played
     * entry. Title + artist + album is stable across a move and does not leak
     * the old path into the UI.
     */
    fun songKey(title: String, artist: String, album: String): String =
        listOf(title.trim(), artist.trim(), album.trim()).joinToString(SEP)

    fun songKey(song: Song): String = songKey(song.title, song.artist, song.album)

    fun label(key: String): String {
        val parts = key.split(SEP)
        return if (parts.size >= 2 && parts[0].isNotBlank()) {
            if (parts[1].isNotBlank()) "${parts[0]} · ${parts[1]}" else parts[0]
        } else {
            key.substringAfterLast('/').substringBeforeLast('.')
        }
    }

    fun played(c: Context, song: Song) {
        val root = load(c)
        val key = songKey(song)
        val songs = root.optJSONObject("songs") ?: JSONObject().also { root.put("songs", it) }
        songs.put(key, songs.optInt(key, 0) + 1)
        val albums = root.optJSONObject("albums") ?: JSONObject().also { root.put("albums", it) }
        if (song.album.isNotBlank()) albums.put(song.album, albums.optInt(song.album, 0) + 1)
        val artists = root.optJSONObject("artists") ?: JSONObject().also { root.put("artists", it) }
        if (song.artist.isNotBlank()) artists.put(song.artist, artists.optInt(song.artist, 0) + 1)
        root.put("total", root.optInt("total", 0) + 1)
        root.put("last_key", key)
        // Kept for old builds/backups; new code reads last_key first.
        root.put("last_path", song.path)
        save(c, root)
    }

    fun lastPlayedKey(c: Context): String? {
        val root = load(c)
        return root.optString("last_key", "").takeIf { it.isNotBlank() }
            ?: root.optString("last_path", "").takeIf { it.isNotBlank() }
    }

    /**
     * Converts 1.0/1.1 path-keyed song stats into metadata keys. Unknown paths
     * are left alone so imported stats are not lost.
     */
    fun migrate(c: Context, songs: List<Song>) {
        if (songs.isEmpty()) return
        val root = load(c)
        val songsObj = root.optJSONObject("songs")
        val byPath = songs.associateBy { it.path }
        val byUri = songs.associateBy { it.uri.toString() }
        var changed = false
        if (songsObj != null) {
            val merged = JSONObject()
            songsObj.keys().forEach { k ->
                val count = songsObj.optInt(k, 0)
                val song = byPath[k] ?: byUri[k]
                val key = song?.let { songKey(it) } ?: k
                if (key != k) changed = true
                merged.put(key, merged.optInt(key, 0) + count)
            }
            if (changed) root.put("songs", merged)
        }
        val oldLast = root.optString("last_path", "")
        if (oldLast.isNotBlank()) {
            val song = byPath[oldLast] ?: byUri[oldLast]
            if (song != null) {
                val key = songKey(song)
                if (root.optString("last_key", "") != key) {
                    root.put("last_key", key)
                    changed = true
                }
            }
        }
        if (changed) save(c, root)
    }

    fun topArtists(c: Context, n: Int = 10): List<Pair<String, Int>> {
        val a = load(c).optJSONObject("artists") ?: return emptyList()
        return a.keys().asSequence().map { it to a.optInt(it) }.sortedByDescending { it.second }.take(n).toList()
    }

    fun topSongs(c: Context, n: Int = 10): List<Pair<String, Int>> {
        val s = load(c).optJSONObject("songs") ?: return emptyList()
        return s.keys().asSequence().map { it to s.optInt(it) }.sortedByDescending { it.second }.take(n).toList()
    }

    fun topAlbums(c: Context, n: Int = 10): List<Pair<String, Int>> {
        val a = load(c).optJSONObject("albums") ?: return emptyList()
        return a.keys().asSequence().map { it to a.optInt(it) }.sortedByDescending { it.second }.take(n).toList()
    }

    fun total(c: Context) = load(c).optInt("total", 0)

    /** Import Namida-style JSON: either { "songPath": count } or { "tracks": {path: {"plays": n}} } or our own export format. */
    fun importJson(c: Context, text: String, songs: List<Song> = emptyList()) {
        val src = try { JSONObject(text) } catch (e: Exception) { return }
        val root = load(c)
        val songStats = root.optJSONObject("songs") ?: JSONObject().also { root.put("songs", it) }
        val byPath = songs.associateBy { it.path }
        val byUri = songs.associateBy { it.uri.toString() }
        val walk: (JSONObject) -> Unit = { obj ->
            obj.keys().forEach { k ->
                val v = obj.opt(k)
                val count = when (v) {
                    is Number -> v.toInt()
                    is JSONObject -> v.optInt("plays", v.optInt("count", v.optInt("play_count", 0)))
                    else -> 0
                }
                val song = byPath[k] ?: byUri[k]
                val key = song?.let { songKey(it) } ?: k
                val looksLikeSong = song != null || k.contains(SEP) || k.endsWith(".mp3", true) ||
                    k.endsWith(".flac", true) || k.endsWith(".m4a", true) || k.endsWith(".ogg", true) ||
                    k.contains('/')
                if (count > 0 && looksLikeSong) {
                    songStats.put(key, songStats.optInt(key, 0) + count)
                    root.put("total", root.optInt("total", 0) + count)
                }
            }
        }
        if (src.has("tracks")) walk(src.getJSONObject("tracks"))
        else if (src.has("songs")) walk(src.getJSONObject("songs"))
        else walk(src)
        save(c, root)
    }

    fun exportJson(c: Context): String = load(c).toString(2)
}
