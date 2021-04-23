package com.picpho.pic_pho

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.picpho.pic_pho.WifiDirect.WifiDirectBroadcastReceiver
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity

class UnCatchTaskService() : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.e("Error", "onTaskRemoved - " + rootIntent)
        WifiDirectMainActivity.removeGroup()
        if (WifiDirectBroadcastReceiver.ServerThread != null && !WifiDirectBroadcastReceiver.ServerThread!!.isCancelled) {
            WifiDirectBroadcastReceiver.ServerThread!!.closeSocket()
            WifiDirectBroadcastReceiver.ServerThread = null
        }
        stopSelf()
    }
}