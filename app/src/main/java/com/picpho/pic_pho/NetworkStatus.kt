package com.picpho.pic_pho

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build

class NetworkStatus {
    companion object {
        fun isConnected(context: Context): Boolean {
            var result = false
            val cm: ConnectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (cm != null) {
                    val capabilities: NetworkCapabilities? =
                        cm.getNetworkCapabilities(cm.activeNetwork)
                    if (capabilities != null) {
                        result = (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                || capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    }
                }
            } else {
                if (cm != null) {
                    val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
                    if (activeNetwork != null) {
                        // connected to the internet
                        result = (activeNetwork?.type === ConnectivityManager.TYPE_WIFI
                                || activeNetwork?.type === ConnectivityManager.TYPE_MOBILE)
                    }
                }
            }
            return result
        }
    }
}