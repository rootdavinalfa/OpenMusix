/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

class MetaDataLoader {
    companion object {
        fun getImage(context: Context, url: String, defaultDrawable: Int): Bitmap {
            val retriever = MediaMetadataRetriever()
            var bitmap: Bitmap? = null
            try {
                retriever.setDataSource(context, Uri.parse(url))
                val image = retriever.embeddedPicture
                image?.let {
                    bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    retriever.release()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
            return bitmap ?: BitmapFactory.decodeResource(context.resources, defaultDrawable)
        }

        fun extractMeta(context: Context, url: String, metas: List<Int>): List<Any?> {
            val values: MutableList<Any?> = ArrayList()
            val retriever = MediaMetadataRetriever()
            Log.i(MetaDataLoader::class.java.simpleName, "Meta size: ${metas.size}")
            try {
                retriever.setDataSource(context, Uri.parse(url))
                for (meta in metas) {
                    when (meta) {
                        MediaMetadataRetriever.METADATA_KEY_ALBUM -> {
                            values.add(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                        }
                        MediaMetadataRetriever.METADATA_KEY_ARTIST -> {
                            values.add(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                        }
                        -99 -> {
                            values.add(retriever.embeddedPicture)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    retriever.release()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
            return values
        }
    }
}