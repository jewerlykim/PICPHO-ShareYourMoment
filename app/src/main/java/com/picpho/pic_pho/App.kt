package com.picpho.pic_pho

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.picpho.pic_pho.GroupVoiceCall.WorkerThread
import com.kakao.sdk.common.KakaoSdk


class App: Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        //{NATIVE_APP_KEY}
        KakaoSdk.init(this,"ece39b60a7c22e79f10be8045187312b",)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    companion object {
        lateinit var instance: App

        @get:Synchronized
        var workerThread: WorkerThread? = null

        fun initWorkerThread() {
            if (workerThread == null) {
                workerThread = WorkerThread(instance)
                workerThread!!.start()
                workerThread!!.waitForReady()
            }
        }

        @Synchronized
        fun deInitWorkerThread() {
            workerThread!!.exit()
            try {
                workerThread!!.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            workerThread = null
        }


    }
}