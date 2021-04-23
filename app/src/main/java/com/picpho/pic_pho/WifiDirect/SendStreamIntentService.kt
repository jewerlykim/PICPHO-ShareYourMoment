package com.picpho.pic_pho.WifiDirect

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.JsonObject
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket


class SendStreamIntentService : IntentService("SendStreamIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        val context: Context = applicationContext
        val cr = context.contentResolver
        var socket = Socket()
        Log.d(TAG, "IntentService Start")

        when (intent!!.action) {

            ACTION_CONNECT_TO_SERVER -> {

                val protocol = intent.extras!!.getString(EXTRAS_PROTOCOL)
                val host = intent.extras!!.getString(EXTRAS_GROUP_OWNER_IP)
                val uri = intent.extras!!.getString(EXTRAS_URI)
                val photoOwnerMac = intent.extras!!.getString(EXTRAS_PHOTO_OWNER_MAC)
                val port = intent.extras!!.getInt(EXTRAS_GROUP_OWNER_PORT)
                val status = intent.extras!!.getInt(EXTRAS_SEND_STATUS)
                val clientPhotoCount = intent.extras!!.getInt(EXTRAS_CLIENT_PHOTO_COUNT)
                val clientPhotoDigit = intent.extras!!.getInt(EXTRAS_CLIENT_PHOTO_DIGIT)

                var jsonObject = JsonObject()
                jsonObject.addProperty("photoOwnerMac", photoOwnerMac)
                jsonObject.addProperty("status", status)
                jsonObject.addProperty("clientPhotoCount", clientPhotoCount)

                val socketAddress = InetSocketAddress(host.toString(), port)

                try {
                    socket.connect(socketAddress, SOCKET_TIMEOUT)
                    var outputStream = socket.getOutputStream()
                    Log.d(TAG, "Socket Connect Success")


                    outputStream.write(protocol!!.encodeToByteArray())

                    outputStream.write(clientPhotoDigit.toString().encodeToByteArray())

                    var stringjson: ByteArray = jsonObject.toString().encodeToByteArray()
                    outputStream.write(stringjson)

                    var input = cr.openInputStream(Uri.parse(uri.toString()))
                    input!!.copyTo(outputStream)

                    socket.close()

                } catch (e: IOException) {
                    Log.d(TAG, "Socket Connect failed")
                    socket.close()
                }
            }

            ACTION_FIRST_CONNECT -> {
                val protocol = intent.extras!!.getString(EXTRAS_PROTOCOL)
                val host = intent.extras!!.getString(EXTRAS_GROUP_OWNER_IP)
                val port = intent.extras!!.getInt(EXTRAS_GROUP_OWNER_PORT)

                val socketAddress = InetSocketAddress(host.toString(), port)

                try {
                    socket.connect(socketAddress, SOCKET_TIMEOUT)
                    var outputStream = socket.getOutputStream()
                    outputStream.write(protocol!!.encodeToByteArray())
                    socket.close()

                } catch (e: IOException) {
                    socket.close()
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        private const val TAG = "SendStreamIntentService"
        private val SOCKET_TIMEOUT = 5000
        val ACTION_CONNECT_TO_SERVER = "com.picpho.picpho.CONNECT_TO_SERVER"
        val ACTION_FIRST_CONNECT = "com.picpho.picpho.FIRST_CONNECT"
        val EXTRAS_GROUP_OWNER_IP = "serverIP"
        val EXTRAS_GROUP_OWNER_PORT = "serverPort"
        val EXTRAS_URI = "uri"
        var EXTRAS_SEND_STATUS = "status"
        val EXTRAS_PHOTO_OWNER_MAC = "photoOwnerMac"
        val EXTRAS_PROTOCOL = "protocol"
        val EXTRAS_CLIENT_PHOTO_COUNT = "clientPhotoCount"
        val EXTRAS_CLIENT_PHOTO_DIGIT = "clientPhotoDigit"
    }
}