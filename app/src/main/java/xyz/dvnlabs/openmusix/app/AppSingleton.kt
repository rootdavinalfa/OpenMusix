package xyz.dvnlabs.openmusix.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import xyz.dvnlabs.openmusix.service.PlayerManager
import xyz.dvnlabs.openmusix.service.PlayerService
import xyz.dvnlabs.openmusix.util.MediaScannerWorker

class AppSingleton : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AppSingleton)
            modules(appModule)
        }
        startWorker()
    }

    fun startWorker() {
        MediaScannerWorker.setupTaskImmediately(this)
    }
}