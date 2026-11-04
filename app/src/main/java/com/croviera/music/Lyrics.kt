package com.croviera.music

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object Lyrics {
    /** Returns list of (timeMs, text) lines parsed from a sidecar .lrc file. */
    fun forSong(c: Context, songPath: String): List<Pair<Long, String>> {
        if (songPath.isBlank()) return emptyList()
        val songFile = File(songPath)
        val base = songPath.substringBeforeLast('.')
        val name = songFile.nameWithoutExtension

        // 1. Explicitly assigned in the metadata editor. This is the path we
        //    store in meta_overrides.json, so it always wins over name matching.
        val assigned = Meta.get(c, songPath)?.optString("lrc", "")?.takeIf { it.isNotBlank() }
        if (assigned != null) {
            if (assigned.startsWith("content://")) {
                try {
                    c.contentResolver.openInputStream(Uri.parse(assigned))?.bufferedReader()?.use { reader ->
                        val parsed = parseLrc(reader.readText())
                        if (parsed.isNotEmpty()) return parsed
                    }
                } catch (_: Exception) {}
            } else {
                parseFile(File(assigned))?.let { return it }
            }
        }

        // 2. Lyrics copied into the app's own folder by the metadata editor.
        findInstalled(c, songPath)?.let { parseFile(it)?.let { lines -> return lines } }

        // 3. The classic sidecar names next to the audio file.
        val parent = songFile.parentFile
        val candidates = listOfNotNull(
            File("$base.lrc"),
            File("$base.LRC"),
            File("$songPath.lrc"),
            File("$base.txt"),
            parent?.let { File(it, "Lyrics/$name.lrc") },
            parent?.let { File(it, "lyrics/$name.lrc") },
        )
        for (f in candidates) parseFile(f)?.let { return it }

        // 4. Case-insensitive scan of the song's folder. This catches files like
        //    "Artist - Song.lrc" when the audio file is just "Song.mp3".
        try {
            songFile.parentFile?.listFiles()?.forEach { f ->
                if (f.isFile && f.extension.equals("lrc", true)) {
                    val sideName = f.nameWithoutExtension
                    if (sideName.equals(name, true) || sideName.contains(name, true) || name.contains(sideName, true)) {
                        parseFile(f)?.let { return it }
                    }
                }
            }
        } catch (_: Exception) {}

        // 5. Last resort: MediaStore. Some devices hide the DATA column or
        //    require the file to be indexed before it appears.
        if (name.isNotBlank()) try {
            c.contentResolver.query(
                MediaStore.Files.getContentUri("external"), arrayOf(MediaStore.Files.FileColumns.DATA),
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?",
                arrayOf("%lrc%", "%$name%"), null
            )?.use { cur ->
                val i = cur.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                while (cur.moveToNext()) {
                    val p = cur.getString(i) ?: continue
                    if (p.endsWith(".lrc", true) && File(p).nameWithoutExtension.lowercase().contains(name.lowercase())) {
                        parseFile(File(p))?.let { return it }
                    }
                }
            }
        } catch (_: Exception) {}
        return emptyList()
    }

    /** Number of timed lines in a stored lyrics file, for the metadata editor. */
    fun lineCount(path: String): Int = try {
        parseFile(File(path))?.size ?: 0
    } catch (_: Exception) { 0 }

    private fun sanitizedBase(songPath: String): String =
        File(songPath).nameWithoutExtension
            .ifBlank { "lyrics" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(64)

    private fun storedFileName(songPath: String): String {
        val hash = Integer.toHexString(songPath.hashCode())
        return "${sanitizedBase(songPath)}_$hash.lrc"
    }

    private fun findInstalled(c: Context, songPath: String): File? {
        val dir = File(c.filesDir, "lyrics")
        if (!dir.isDirectory) return null
        val exact = File(dir, storedFileName(songPath))
        if (exact.isFile) return exact
        // Files copied by older builds used just the sanitized song name.
        val legacy = File(dir, "${sanitizedBase(songPath)}.lrc")
        if (legacy.isFile) return legacy
        return null
    }

    private fun parseFile(f: File): List<Pair<Long, String>>? {
        if (!f.exists() || !f.isFile) return null
        return try {
            parseLrc(f.readText().removePrefix("\uFEFF")).takeIf { it.isNotEmpty() }
        } catch (_: Exception) { null }
    }

    /** Copy a user-picked .lrc into app storage and return the stored path. */
    fun installFromUri(c: Context, uri: Uri, songPath: String): String? {
        return try {
            val dir = File(c.filesDir, "lyrics").apply { mkdirs() }
            val target = File(dir, storedFileName(songPath))
            c.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (target.length() == 0L) { target.delete(); null } else target.absolutePath
        } catch (_: Exception) { null }
    }

    private val timeTag = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
    private val metaTag = Regex("^\\[[a-zA-Z]+:.*]$")
    fun parseLrc(text: String): List<Pair<Long, String>> {
        val out = mutableListOf<Pair<Long, String>>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val tags = timeTag.findAll(line).toList()
            if (tags.isEmpty()) continue
            val lyric = timeTag.replace(line, "").trim()
            if (lyric.isEmpty()) continue
            for (m in tags) {
                val min = m.groupValues[1].toLongOrNull() ?: continue
                val sec = m.groupValues[2].toLongOrNull() ?: continue
                val frac = m.groupValues[3]
                val fracMs = when (frac.length) {
                    0 -> 0L
                    1 -> (frac.toLongOrNull() ?: 0L) * 100
                    2 -> (frac.toLongOrNull() ?: 0L) * 10
                    else -> frac.toLongOrNull() ?: 0L
                }
                out.add((min * 60_000 + sec * 1000 + fracMs) to lyric)
            }
        }
        if (out.isNotEmpty()) return out.sortedBy { it.first }

        // No timestamps: keep it as plain, unsynced lyrics instead of
        // pretending the file was not found.
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !metaTag.matches(it) && timeTag.replace(it, "").isNotBlank() }
            .map { 0L to it }
    }
}
