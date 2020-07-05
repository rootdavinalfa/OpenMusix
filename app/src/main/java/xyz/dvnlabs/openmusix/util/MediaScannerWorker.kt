package xyz.dvnlabs.openmusix.util

import android.content.*
import android.net.Uri
import android.provider.MediaStore
import androidx.work.*
import xyz.dvnlabs.openmusix.data.MediaData
import xyz.dvnlabs.openmusix.data.MediaDataDB
import xyz.dvnlabs.openmusix.data.MediaGenre
import java.util.concurrent.TimeUnit

class MediaScannerWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private var mediaDB: MediaDataDB = MediaDataDB.getDatabase(applicationContext)
    private val projection = arrayOf(
        MediaStore.Audio.AudioColumns._ID,
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns.TITLE,
        MediaStore.Audio.AudioColumns.ALBUM_ID,
        MediaStore.Audio.AudioColumns.ARTIST_ID,
        MediaStore.Audio.AudioColumns.DATE_ADDED,
        MediaStore.Audio.AudioColumns.DATE_MODIFIED,
        MediaStore.Audio.AudioColumns.MIME_TYPE
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
                        contentURI = contentURI.toString()
                    )
                )

                val genresCursor = context.contentResolver.query(
                    MediaStore.Audio.Genres.getContentUriForAudioId("external", id.toInt()),
                    genreProjection, null, null, null
                )
                genresCursor.use { x ->
                    val genreIDColumn =
                        genresCursor?.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                    val genreNameColumn =
                        genresCursor?.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                    while (x!!.moveToNext()) {
                        val idGen = genresCursor?.getLong(genreIDColumn!!)
                        val name = genresCursor?.getString(genreNameColumn!!)
                        mediaDB.mediaGenreDAO().newMediaGenre(
                            MediaGenre(
                                fileID = id,
                                genreID = idGen!!,
                                genreName = name!!
                            )
                        )
                    }
                }
            }
        }
        return Result.success()
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