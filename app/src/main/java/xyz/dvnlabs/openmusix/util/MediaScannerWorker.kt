/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.dvnlabs.openmusix.data.*
import java.util.concurrent.TimeUnit

class MediaScannerWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private var mediaDB: MediaDB = MediaDB.getDatabase(applicationContext)
    private val projection = arrayOf(
        MediaStore.Audio.AudioColumns._ID,
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns.TITLE,
        MediaStore.Audio.AudioColumns.ALBUM_ID,
        MediaStore.Audio.AudioColumns.ARTIST_ID,
        MediaStore.Audio.AudioColumns.YEAR,
        MediaStore.Audio.AudioColumns.TRACK,
        MediaStore.Audio.AudioColumns.COMPOSER,
        MediaStore.Audio.AudioColumns.DATE_ADDED,
        MediaStore.Audio.AudioColumns.DATE_MODIFIED,
        MediaStore.Audio.AudioColumns.MIME_TYPE,
        MediaStore.Audio.AudioColumns.DATA
    )
    private val genreProjection = arrayOf(
        MediaStore.Audio.Genres._ID,
        MediaStore.Audio.Genres.NAME
    )

    private val selection =
        "${MediaStore.Audio.AudioColumns.IS_MUSIC} != 0 AND ${MediaStore.Audio.AudioColumns.MIME_TYPE} != 'audio/x-wav' "
    private val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
    private val query = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        sortOrder
    )

    override suspend fun doWork(): Result {
        query?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
            val displayNameColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST_ID)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED)
            val dateModifiedColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
            val typeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.YEAR)
            val trackColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TRACK)
            val composerColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.COMPOSER)
            while (it.moveToNext()) {
                val added = it.getLong(dateAddedColumn)
                val modified = it.getLong(dateModifiedColumn)
                val type = it.getString(typeColumn)
                val id = it.getLong(idColumn)
                val album = it.getLong(albumColumn)
                val displayName = it.getString(displayNameColumn)
                val contentURI: Uri =
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val artist = it.getLong(artistColumn)
                val title = it.getString(titleColumn)
                val path = it.getString(dataColumn)
                val year = it.getInt(yearColumn)
                val track = it.getInt(trackColumn)
                val composer = it.getString(composerColumn)
                mediaDB.mediaDataDAO().newMedia(
                    MediaData(
                        addedON = added,
                        modified = modified,
                        type = type,
                        fileID = id,
                        albumID = album,
                        artistID = artist,
                        displayName = displayName,
                        title = title,
                        contentURI = contentURI.toString(),
                        path = path,
                        year = year,
                        track = track,
                        composer = composer
                    )
                )
                addGenre(id)
                addAlbum(album)
                addArtist(artist)
            }
        }
        return Result.success()
    }

    private fun addGenre(id: Long) {
        val genresCursor = context.contentResolver.query(
            MediaStore.Audio.Genres.getContentUriForAudioId("external", id.toInt()),
            genreProjection, null, null, null
        )
        genresCursor?.use { x ->
            val genreIDColumn =
                genresCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val genreNameColumn =
                genresCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (x.moveToNext()) {
                val idGen = genresCursor.getLong(genreIDColumn)
                val name = genresCursor.getString(genreNameColumn)
                GlobalScope.launch {
                    mediaDB.mediaGenreDAO().newMediaGenre(
                        MediaGenre(
                            fileID = id,
                            genreID = idGen,
                            genreName = name
                        )
                    )
                }
            }
        }
    }

    private suspend fun addAlbum(albumID: Long) {
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Audio.AlbumColumns.ALBUM,
                MediaStore.Audio.AlbumColumns.ARTIST,
                MediaStore.Audio.AlbumColumns.FIRST_YEAR,
                MediaStore.Audio.AlbumColumns.LAST_YEAR,
                MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS
            )

            val selection =
                "${MediaStore.Audio.Albums._ID} == $albumID "
            val query = context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )
            query?.use {
                val albumNameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM)
                val artistNameColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST)
                val firstYearColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.FIRST_YEAR)
                val lastYearColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.LAST_YEAR)
                val numberSongColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS)



                while (it.moveToNext()) {
                    val albumName = it.getString(albumNameColumn)
                    val artistName = it.getString(artistNameColumn)
                    val firstYear = it.getInt(firstYearColumn)
                    val lastYear = it.getInt(lastYearColumn)
                    val numberSong = it.getInt(numberSongColumn)
                    mediaDB.mediaAlbumDAO().newAlbum(
                        MediaAlbum(
                            albumID = albumID,
                            albumName = albumName,
                            artistName = artistName,
                            firstYear = firstYear,
                            lastYear = lastYear,
                            songCount = numberSong
                        )
                    )
                }
            }
        }
    }

    private suspend fun addArtist(artistID: Long) {
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS,
                MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS
            )

            val selection =
                "${MediaStore.Audio.Artists._ID} == $artistID "
            val query = context.contentResolver.query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )
            query?.use {
                val artistNameColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.ARTIST)
                val numberAlbumColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS)
                val numberTrackColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS)



                while (it.moveToNext()) {
                    val artistName = it.getString(artistNameColumn)
                    val numberAlbum = it.getInt(numberAlbumColumn)
                    val numberTrack = it.getInt(numberTrackColumn)
                    mediaDB.mediaArtistDAO().newArtist(
                        MediaArtist(
                            artistID = artistID,
                            artistName = artistName,
                            albumCount = numberAlbum,
                            trackCount = numberTrack
                        )
                    )
                }
            }
        }
    }


    companion object {
        private const val TAG = "MediaScannerWorker"
        private const val DEFAULT_INTERVAL = 60

        fun setupTaskImmediately(context: Context) {
            val request = OneTimeWorkRequestBuilder<MediaScannerWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(request)

        }

        fun setupTaskPeriodic(context: Context, interval: Int = DEFAULT_INTERVAL) {
            if (interval > 0) {
                val request = PeriodicWorkRequestBuilder<MediaScannerWorker>(
                    interval.toLong(), TimeUnit.MINUTES,
                    10, TimeUnit.MINUTES
                )
                    .addTag(TAG)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, request)
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
        }
    }
}