/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.app

import android.app.Application
import android.os.Handler
import android.provider.MediaStore
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import xyz.dvnlabs.openmusix.service.OpenMusixAPI
import xyz.dvnlabs.openmusix.util.MediaObserver
import xyz.dvnlabs.openmusix.util.MediaScannerWorker

class AppSingleton : Application() {
    private var mediaScannerHandler: Handler? = null
    private var mediaObserver: MediaObserver? = null
    private var openMusixAPI: OpenMusixAPI? = null
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AppSingleton)
            modules(appModule)
        }

        openMusixAPI = OpenMusixAPI(this)
        if (OpenMusixAPI.service == null) {
            openMusixAPI?.bind()
        }
        mediaScannerHandler = Handler()
        mediaObserver = MediaObserver(mediaScannerHandler!!, this)
        this.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver!!
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        this.contentResolver.unregisterContentObserver(mediaObserver!!)
        openMusixAPI?.unbind()
    }

    fun startWorker() {
        MediaScannerWorker.setupTaskImmediately(this)
    }
}