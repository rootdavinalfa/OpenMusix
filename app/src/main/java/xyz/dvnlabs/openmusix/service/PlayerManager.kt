/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import org.greenrobot.eventbus.EventBus

class PlayerManager(private val context: Context) {

    var isServiceBound = false
        private set


    fun bind() {
        Log.i("API-> BINDING:", "OK")
        Intent(context, PlayerService::class.java).also {
            context.bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        isServiceBound = true
        if (service != null) {
            EventBus.getDefault().post(service!!.getStatus())
        }
    }

    fun unbind() {
        Log.i("API-> UNBINDING:", "OK")
        context.unbindService(serviceConnection)
        isServiceBound = false
        service = null
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            val playerBinder = binder as PlayerService.PlayerBinder
            service = playerBinder.service
            isServiceBound = true
            if (service != null) {
                Log.i("API-> BINDER STATUS:", "OK")
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.i(context.packageName, "ServiceConnection::onServiceDisconnected() called")
            service = null
            isServiceBound = false
            if (service == null) {
                Log.i("API-> BINDER STATUS:", "DC")
            }
        }
    }

    companion object {
        @JvmStatic
        var service: PlayerService? = null
            private set
    }
}