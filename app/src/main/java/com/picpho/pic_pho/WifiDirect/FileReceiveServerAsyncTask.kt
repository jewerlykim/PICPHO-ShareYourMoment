package com.picpho.pic_pho.WifiDirect

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class FileReceiveServerAsyncTask(
    private val context: Context
) : AsyncTask<Void, Void, String?>() {

    companion object {
        var serverSocket: ServerSocket? = null
    }

    fun closeSocket(){
        if(serverSocket!=null) {
            serverSocket!!.close()
        }
    }

    override fun doInBackground(vararg params: Void): String? {

        Log.d("ServerSide", "doInBackground Start")
        try {
            serverSocket = ServerSocket(8989)
            serverSocket!!.reuseAddress
            var client : Socket

            var count = 0
            Log.d(TAG, "doInBackground: isClosed???? : ${serverSocket!!.isClosed}")
            while(!serverSocket!!.isClosed) {
                count++
                Log.d(TAG, "doInBackground: count : ${count}")
                
                if(this.isCancelled){
                    Log.d("ServerSide", "doInBackground Canceled")
                    WifiDirectBroadcastReceiver.ServerThread = null
                    break
                }

                try{
                    client = serverSocket!!.accept()
                    FileReceiveActionThread(client = client, context = context).run()

                }catch(e : IOException){
                    break
                }
            }

            Log.d("ServerSide", "doInBackground Finished")
            return null

        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("dointbackground", e.toString())
            return null
        }
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Log.d(TAG, "onPostExecute: FileReceiverServerAsyncTask !!!!!!!!")
    }

    override fun onCancelled() {
        Log.d(TAG, "onCancelled: FileReceiverServerAsyncTask Canceled!!!!!!!!")
        super.onCancelled()
    }
}