package com.picpho.pic_pho.GroupVoiceCall


import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.util.Log
import com.picpho.pic_pho.R
import io.agora.rtc.Constants
import io.agora.rtc.RtcEngine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WorkerThread(var mContext: Context) : Thread() {

    private var mWorkerHandler: WorkerThreadHandler? = null
    private var mReady = false
    var rtcEngine: RtcEngine? = null
    val engineConfig: EngineConfig
    val mEngineEventHandler: EngineEventHandler

    private class WorkerThreadHandler(var mWorkerThread: WorkerThread?) : Handler() {
        fun release() {
            mWorkerThread = null
        }

        override fun handleMessage(msg: Message) {
            if (mWorkerThread == null) {
                Log.d(TAG, "handleMessage: ")
                return
            }
            when (msg.what) {
                ACTION_WORKER_THREAD_QUIT -> mWorkerThread?.exit()
                ACTION_WORKER_JOIN_CHANNEL -> {
                    if (msg.obj != null) {
                        val data = msg.obj as Array<String>
                        mWorkerThread?.joinChannel(data[0], msg.arg1)
                    }
                }
                ACTION_WORKER_LEAVE_CHANNEL -> {
                    if (msg.obj != null) {
                        val channel = msg.obj as String
                        mWorkerThread?.leaveChannel(channel)
                    }
                }
            }
        }
    }

    fun waitForReady() {
        while (!mReady) {
            try {
                sleep(20)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            Log.d(TAG, "waitForReady: ")
        }
    }

    override fun run() {
        Log.d(TAG, "run: start to run")
        Looper.prepare()
        mWorkerHandler = WorkerThreadHandler(this)
        ensureRtcEngineReadyLock()
        mReady = true

        // enter thread looper
        Looper.loop()
    }

    fun joinChannel(channel: String, uid: Int) {
        if (currentThread() !== this) {
            Log.d(TAG, "joinChannel:  worker thread asynchronously $channel $uid")
            val envelop = Message()
            envelop.what = ACTION_WORKER_JOIN_CHANNEL
            envelop.obj = arrayOf(channel)
            envelop.arg1 = uid
            mWorkerHandler!!.sendMessage(envelop)
            return
        }
        ensureRtcEngineReadyLock()
        rtcEngine!!.joinChannel(null, channel, "OpenVCall", uid)
        engineConfig.mChannel = channel
        Log.d(TAG, "joinChannel:  $channel $uid")
    }

    fun leaveChannel(channel: String?) {
        if (currentThread() !== this) {
            Log.d(TAG, "leaveChannel: worker thread asynchronously $channel")
            val envelop = Message()
            envelop.what = ACTION_WORKER_LEAVE_CHANNEL
            envelop.obj = channel
            mWorkerHandler!!.sendMessage(envelop)
            return
        }
        if (rtcEngine != null) {
            rtcEngine!!.leaveChannel()
        }
        engineConfig.reset()
        Log.d(TAG, "leaveChannel: $channel")
    }

    private fun ensureRtcEngineReadyLock(): RtcEngine? {
        if (rtcEngine == null) {
            val appId = mContext.getString(R.string.appId)
            if (appId.isEmpty()) {
                throw RuntimeException("NEED TO use your App ID, get your own ID at https://dashboard.agora.io/")
            }
            try {
                Log.d(TAG, "ensureRtcEngineReadyLock: rtcEngine init")
                rtcEngine =
                    RtcEngine.create(mContext!!, appId, mEngineEventHandler.mRtcEngineEventHandler)
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException(
                    """NEED TO check rtc sdk init fatal error ${
                        Log.getStackTraceString(
                            e
                        )
                    } """.trimIndent()
                )
            }

            Log.d(TAG, "ensureRtcEngineReadyLock: rtcEngine set property")
            rtcEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine!!.enableAudioVolumeIndication(200, 3, false)
            rtcEngine!!.setLogFile(
                Environment.getExternalStorageDirectory()
                    .toString() + File.separator + mContext.packageName + "/log/agora-rtc.log"
            )
            rtcEngine!!.uploadLogFile()
        }
        return rtcEngine
    }

    fun eventHandler(): EngineEventHandler {
        return mEngineEventHandler
    }

    fun exit() {
        if (currentThread() !== this) {
            Log.d(TAG, "exit: exit app thread asynchronously")
            mWorkerHandler!!.sendEmptyMessage(ACTION_WORKER_THREAD_QUIT)
            return
        }
        mReady = false

        Log.d(TAG, "exit: > start")

        Looper.myLooper()!!.quit()
        mWorkerHandler!!.release()
    }

    init {
        engineConfig = EngineConfig()
        val currentTime = Calendar.getInstance().time
        engineConfig.mUid = SimpleDateFormat("HHmmss", Locale.KOREA).format(currentTime).toInt()
        Log.d(TAG, "mUid: ${engineConfig.mUid}")
        mEngineEventHandler = EngineEventHandler(mContext, engineConfig)
    }

    companion object {
        private const val ACTION_WORKER_THREAD_QUIT = 0X1010 // quit this thread
        private const val ACTION_WORKER_JOIN_CHANNEL = 0X2010
        private const val ACTION_WORKER_LEAVE_CHANNEL = 0X2011
        private const val TAG = "WorkerThread"
        fun getDeviceID(context: Context): String {
            // XXX according to the API docs, this value may change after factory reset
            // use Android id as device id
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }
}