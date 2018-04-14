package org.uamp.generator

import com.beust.klaxon.JsonObject
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

fun main(args: Array<String>) {
    val sourceDirectory = File(args.getOrNull(0) ?: System.getProperty("user.dir"))
    val manifestWriter = if (args.size == 2) File(args[1]).writer() else System.out.writer()

    manifestWriter.use {
        sourceDirectory
            .walk()
            .filter {
                it.isFile && it.extension.equals("mp3", ignoreCase = true)
            }
            .mapNotNull { file ->
                readAudioFileOrNull(file)
            }
            .mapNotNull { audioFile ->
                extractMetadataOrNull(audioFile)
            }
            .joinTo(manifestWriter, prefix = """{ "music": [""", postfix = "]}") {
                it.toJsonString(true)
            }
    }
}

fun readAudioFileOrNull(file: File): AudioFile? =
        try { AudioFileIO.read(file) } catch (e: Exception) { null }

fun extractMetadataOrNull(audioFile: AudioFile): JsonObject? {
    val js = JsonObject()

    with(audioFile.tag ?: return null) {
        js["title"] = getFirst(FieldKey.TITLE) ?: return null
        js["artist"] = getFirst(FieldKey.ARTIST) ?: getFirst(FieldKey.ALBUM_ARTIST) ?: return null
        js["album"] = getFirst(FieldKey.ALBUM) ?: return null
        js["genre"] = if (getFirst(FieldKey.GENRE).isNullOrBlank()) "unknown" else getFirst(FieldKey.GENRE)
        js["source"] = audioFile.file.absolutePath
        js["image"] = ""
        js["trackNumber"] = getFirst(FieldKey.TRACK).toIntOrNull() ?: 0
        js["totalTrackCount"] = getFirst(FieldKey.TRACK_TOTAL).toIntOrNull() ?: 0
        js["duration"] = audioFile.audioHeader.trackLength
        js["site"] = getFirst(FieldKey.URL_WIKIPEDIA_RELEASE_SITE)
            ?: getFirst(FieldKey.URL_WIKIPEDIA_ARTIST_SITE)
            ?: getFirst(FieldKey.URL_DISCOGS_ARTIST_SITE)
            ?: getFirst(FieldKey.URL_OFFICIAL_ARTIST_SITE)
            ?: ""

        return js
    }
}
