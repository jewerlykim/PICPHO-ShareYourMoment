package com.picpho.pic_pho.GroupVoiceCall

interface AGEventHandler {

    fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int)
    fun onUserOffline(uid: Int, reason: Int)
    fun onExtraCallback(type: Int, vararg data: Any?)

    companion object {
        const val EVENT_TYPE_ON_USER_AUDIO_MUTED = 7
        const val EVENT_TYPE_ON_SPEAKER_STATS = 8
        const val EVENT_TYPE_ON_AGORA_MEDIA_ERROR = 9
        const val EVENT_TYPE_ON_AUDIO_QUALITY = 10
        const val EVENT_TYPE_ON_APP_ERROR = 13
        const val EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED = 18
    }
}