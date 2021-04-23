package com.picpho.pic_pho.WifiDirect

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.App
import com.picpho.pic_pho.LoginActivity
import com.picpho.pic_pho.R
import com.picpho.pic_pho.UnCatchTaskService
import com.picpho.pic_pho.WifiDirect.UI.PeerModel
import com.picpho.pic_pho.WifiDirect.UI.PeerRecyclerAdapter
import com.picpho.pic_pho.WifiDirect.UI.PeerViewHolder
import com.picpho.pic_pho.databinding.ActivityWifiSearchBinding
import com.snatik.storage.Storage
import kotlinx.android.synthetic.main.activity_wifi_search.*
import kotlinx.coroutines.*
import org.jetbrains.anko.toast
import java.lang.Thread.sleep
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList

class WifiDirectMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWifiSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startService(Intent(this, UnCatchTaskService::class.java))

        wifiDirectMainActivity = this
        storage = Storage(applicationContext)

        removeGroup()
        cancelConnect()

        LoginActivity.requestPermissionToUser(this)
        var builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager?.initialize(this, mainLooper, null)
        wifiDirectBroadcastReceiver = WifiDirectBroadcastReceiver(wifiP2pManager!!, channel!!, this)
        startService(Intent(this, UnCatchTaskService::class.java))

        photoOwnerMacAddress = getMacAddr()

        LoginActivity.requestPermissionToUser(this)

        peerRecyclerviewAdapter = PeerRecyclerAdapter()
        (peerRecyclerviewAdapter as PeerRecyclerAdapter).submitList(groupList)
        binding.recyclerviewPeerlist.apply {
            layoutManager =
                LinearLayoutManager(
                    this@WifiDirectMainActivity,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter = peerRecyclerviewAdapter
        }

        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        binding.wrapSearchTextview.setOnClickListener {
            searchWifiGroup()
        }
        binding.wrapWifiSearchButtonLayout.setOnClickListener {
            searchWifiGroup()
        }
    }

    private fun searchWifiGroup() {
        discoverPeers(wifiP2pManager!!)
        CoroutineScope(Dispatchers.Main).launch {
            binding.wrapSearchTextview.visibility = View.GONE
            binding.recyclerviewPeerlist.visibility = View.VISIBLE
            binding.wifiMakegroupCardView.isEnabled = false
            binding.wifiMakegroupCardView.visibility = View.GONE
        }
        animateWifiIcon()
    }


    fun animateWifiIcon() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.wifiSearchButton.setImageResource(R.drawable.wifi_one)
            sleep(1000)
            binding.wifiSearchButton.setImageResource(R.drawable.wifi_two)
            sleep(1000)
            binding.wifiSearchButton.setImageResource(R.drawable.wifi_three)
            sleep(1000)
            binding.researchTextview.visibility = View.VISIBLE
        }
    }


    fun createGroupClicked(view: View) {
        CoroutineScope(Dispatchers.IO).launch {
            wifiP2pManager!!.createGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    toast("방이 생성되었습니다.")
                    isGroupOwner = true
                    var intent: Intent = Intent(App.instance, WaitingForOwnerActivity::class.java)
                    startActivity(intent)
                }

                override fun onFailure(reason: Int) {
                    toast("방 만들기를 실패했습니다\n다시 시도해주세요!")
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("onResume", "onResume")
        CoroutineScope(Dispatchers.IO).launch {
            registerReceiver(wifiDirectBroadcastReceiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        runBlocking {
            CoroutineScope(Dispatchers.IO).launch {
                unregisterReceiver(wifiDirectBroadcastReceiver)
                if (WifiDirectBroadcastReceiver.ServerThread != null && !WifiDirectBroadcastReceiver.ServerThread!!.isCancelled) {
                    WifiDirectBroadcastReceiver.ServerThread!!.closeSocket()
                    WifiDirectBroadcastReceiver.ServerThread = null
                }
                deinitializeVariables()
                removeGroup()
            }.join()
        }
        super.onDestroy()
    }

    fun connectToPeer(peerDeviceMAC: String) {
        if (availablePeerList.isNotEmpty()) {
            val config = WifiP2pConfig().apply {
                deviceAddress = peerDeviceMAC
                groupOwnerIntent =
                    0
                wps.setup =
                    WpsInfo.PBC
            }

            CoroutineScope(Dispatchers.IO).launch {
                wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "onSuccess: ConnectToPeer")
                    }

                    override fun onFailure(reason: Int) {
                        Log.d(TAG, "onFailure: ConnectToPeer")
                    }
                })
            }
        } else {
            Log.d(TAG, "방이 사라졌습니다!")
        }
    }



    fun getMacAddr(): String {
        try {
            val all = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.getName().equals("wlan0", ignoreCase = true)) continue

                val macBytes = nif.getHardwareAddress() ?: return ""

                val res1 = StringBuilder()
                for (b in macBytes) {
                    res1.append(String.format("%02X:", b))
                }
                if (res1.length > 0) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (ex: Exception) {
        }

        return "02:00:00:00:00:00"
    }

    private fun discoverPeers(wifiP2pManager: WifiP2pManager) {
        CoroutineScope(Dispatchers.IO).launch {
            wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    toast("주변에 연결가능한 기기를 찾고 있습니다.\n잠시만 기다려주세요:)")
                }

                override fun onFailure(reason: Int) {
                    toast(
                        "주변에 연결가능한 기기를 찾지 못했습니다.\n" +
                                "다시 시도해 주세요!:)"
                    )
                }
            })
        }
    }

    companion object {
        var photoOwnerMacAddress: String? = null
        var groupMemberCount: Int = 0
        var sentPhotoCount: Int = 0
        var isGroupOwner = false
        var groupOwnerIP: String? = null // IP
        var photoInfoList = ArrayList<PhotoInfo>()
        var groupList = ArrayList<PeerModel>()
        val availablePeerList = ArrayList<WifiP2pDevice>()
        val connectedPeerList = ArrayList<String>()
        val connectedPeerMap = mutableMapOf<String, Int>()
        var wifiP2pManager: WifiP2pManager? = null
        var channel: WifiP2pManager.Channel? = null
        var peerRecyclerviewAdapter: RecyclerView.Adapter<PeerViewHolder>? = null
        var wifiDirectBroadcastReceiver: BroadcastReceiver? = null
        var wifiDirectMainActivity: Activity? = null


        var storage: Storage? = null
        var filePathList = ArrayList<String>()


        private lateinit var intentFilter: IntentFilter

        private const val TAG = "WifiDirectMainActivity"


        fun sendToServerPhotos(context: Context) {

            if (groupOwnerIP != null && !isGroupOwner && photoInfoList.isNotEmpty()) {

                val intent = Intent(context, SendStreamIntentService::class.java)

                var photoCountDigit = (photoInfoList.size).toString().length


                var i = 1
                for (photo in photoInfoList) {
                    if (!isGroupOwner) {
                        intent.putExtra("protocol", "2")
                        intent.putExtra("photoOwnerMac", photo.photoOwnerMac)
                        intent.putExtra("serverIP", groupOwnerIP)
                        intent.putExtra("serverPort", 8989)
                        intent.putExtra("uri", photo.photoUri.toString())
                        intent.putExtra("status", Integer(i / photoInfoList.size))
                        intent.putExtra("clientPhotoDigit", photoCountDigit)
                        intent.putExtra("clientPhotoCount", photoInfoList.size)
                        intent.setAction("com.picpho.picpho.CONNECT_TO_SERVER")
                        context.startService(intent)
                        i++
                    }
                }
            }
            sentPhotoCount++
        }

        fun deinitializeVariables(){
            photoInfoList.clear()
            groupList.clear()
            connectedPeerList.clear()
            connectedPeerMap.clear()
            isGroupOwner = false
            sentPhotoCount = 0
            groupMemberCount = 0
            photoOwnerMacAddress = null
            groupOwnerIP = null
        }

        fun removeGroup() {
            if (wifiP2pManager != null) {
                wifiP2pManager!!.requestGroupInfo(channel) { group ->
                    if (group != null) {
                        try {
                            wifiP2pManager!!.removeGroup(
                                channel,
                                object : WifiP2pManager.ActionListener {
                                    override fun onSuccess() {
                                        Log.d(TAG, "onSuccess: 그룹 삭제")
                                        isGroupOwner = false;
                                    }

                                    override fun onFailure(reason: Int) {
                                        Log.d(TAG, "onFailure: 그룹 삭제 실패")
                                    }
                                })
                        }catch (e:Exception){
                            Log.d(TAG, "removeGroup: catch?????")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        fun cancelConnect() {
            if (wifiP2pManager != null) {
                wifiP2pManager!!.requestGroupInfo(channel) { group ->
                    if (group != null) {
                        wifiP2pManager!!.cancelConnect(
                            channel,
                            object : WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    Log.d(TAG, "onSuccess: 그룹 삭제")
                                    isGroupOwner = false;
                                }

                                override fun onFailure(reason: Int) {
                                    Log.d(TAG, "onFailure: 그룹 삭제 실패")
                                }
                            })
                    }
                }
            }
        }
    }
}