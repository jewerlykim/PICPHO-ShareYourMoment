package com.picpho.pic_pho.GroupVoiceCall

import android.content.Context
import android.util.Log
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import java.util.concurrent.ConcurrentHashMap

class EngineEventHandler(val mContext : Context, val mConfig : EngineConfig) {
    private val TAG = "EngineEventHandler"
    private val mEventHandlerList = ConcurrentHashMap<AGEventHandler, Int>()

    fun addEventHandler(handler: AGEventHandler) {
        mEventHandlerList[handler] = 0
    }

    fun removeEventHandler(handler: AGEventHandler) {
        mEventHandlerList.remove(handler)
    }

    val mRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Log.d(TAG, "onUserJoined: ${uid}")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            Log.d(TAG, "onUserOffline: "  + (uid and 0xFFFFFFFFL.toInt()) + " " + reason)

            // FIXME this callBack may return times
            val it: Iterator<AGEventHandler> = mEventHandlerList.keys.iterator()
            while (it.hasNext()) {
                val handler = it.next()
                handler.onUserOffline(uid, reason)
            }
        }

        override fun onRtcStats(stats: RtcStats?) {
            super.onRtcStats(stats)
        }

        override fun onAudioVolumeIndication(
            speakers: Array<out AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            super.onAudioVolumeIndication(speakers, totalVolume)
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            super.onLeaveChannel(stats)
        }

        override fun onLastmileQuality(quality: Int) {
            super.onLastmileQuality(quality)
        }

        override fun onError(err: Int) {
            super.onError(err)
            Log.d(TAG, "onError: ${err}")
            val it: Iterator<AGEventHandler> = mEventHandlerList.keys.iterator()
            while (it.hasNext()) {
                val handler = it.next()
                handler.onExtraCallback(AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR, err, RtcEngine.getErrorDescription(err))
            }
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            Log.d(TAG, "onConnectionLost: ")
            val it: Iterator<AGEventHandler> = mEventHandlerList.keys.iterator()
            while (it.hasNext()) {
                val handler = it.next()
                handler.onExtraCallback(AGEventHandler.Companion.EVENT_TYPE_ON_APP_ERROR, ConstantApp.AppError.NO_NETWORK_CONNECTION)
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "onJoinChannelSuccess: " + channel + " " + (uid and 0xFFFFFFFFL.toInt()) + " " + elapsed)
            mConfig.mUid = uid
            val it: Iterator<AGEventHandler> = mEventHandlerList.keys.iterator()
            while (it.hasNext()) {
                val handler = it.next()
                handler.onJoinChannelSuccess(channel!!, uid, elapsed)
            }
        }

        override fun onWarning(warn: Int) {
            super.onWarning(warn)
            Log.d(TAG, "onWarning: $warn")
        }

        override fun onAudioRouteChanged(routing: Int) {
            super.onAudioRouteChanged(routing)
            Log.d(TAG, "onAudioRouteChanged: ${routing}")
            val it: Iterator<AGEventHandler> = mEventHandlerList.keys.iterator()
            while (it.hasNext()) {
                val handler = it.next()
                handler.onExtraCallback(AGEventHandler.Companion.EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED, routing)
            }
        }
    }
}