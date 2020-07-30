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
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

class MediaObserver(handler: Handler, val context: Context) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        MediaScannerWorker.setupTaskImmediately(context)
    }
}