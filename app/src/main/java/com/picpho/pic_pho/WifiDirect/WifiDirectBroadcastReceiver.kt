package com.picpho.pic_pho.WifiDirect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.picpho.pic_pho.WifiDirect.UI.PeerModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val mainActivity: WifiDirectMainActivity
) : BroadcastReceiver() {

    companion object {
        var ServerThread: FileReceiveServerAsyncTask? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        when (intent.action) {


            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.


                when (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)) {
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                        // Wifi P2P is enabled
                    }
                    else -> {
                        // Wifi P2P is not enabled
                        Toast.makeText(
                            context,
                            "Wifi를 켜주세요!\n 와이파이존이 아니어도 괜찮아요!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {

                // The peer list has changed! We should probably do something about that.

                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                WifiDirectMainActivity.wifiP2pManager!!.requestGroupInfo(
                    WifiDirectMainActivity.channel
                ) { group ->
                    if (group == null) {
                        manager.requestPeers(channel) { peers ->
                            WifiDirectMainActivity.availablePeerList.clear()
                            WifiDirectMainActivity.availablePeerList.addAll(peers!!.deviceList)
                            WifiDirectMainActivity.groupList.clear()
                            for (peer in peers!!.deviceList) {
                                Log.d(
                                    "peer.primaryDeviceType",
                                    "onReceive: peer.primaryDeviceType ${peer.primaryDeviceType}"
                                )
                                var primaryDeviceTypeLastWord =
                                    peer.primaryDeviceType.equals("10-0050F204-5")
                                if (peer.isGroupOwner && !WifiDirectMainActivity.isGroupOwner && primaryDeviceTypeLastWord) {
                                    WifiDirectMainActivity.groupList.add(
                                        PeerModel(
                                            peer.deviceName + "의 방",
                                            peer.deviceAddress,
                                            peer.isGroupOwner.toString()
                                        )
                                    )
                                }
                            }
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(100)
                                WifiDirectMainActivity.peerRecyclerviewAdapter!!.notifyDataSetChanged()
                            }
                        }
                    } else {
                        WifiDirectMainActivity.groupList.clear()
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100)
                            WifiDirectMainActivity.peerRecyclerviewAdapter!!.notifyDataSetChanged()
                        }
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Connection state changed! We should probably do something about that.

                manager.requestConnectionInfo(channel) { info ->

                    if (info!!.groupFormed && info.isGroupOwner) {
                        WifiDirectMainActivity.isGroupOwner = true
                        WifiDirectMainActivity.wifiP2pManager!!.requestGroupInfo(
                            WifiDirectMainActivity.channel
                        ) { group ->
                            if (group != null) {
                                WifiDirectMainActivity.groupMemberCount = group!!.clientList.size
                                WifiDirectMainActivity.groupList.clear()

                                for (peer in group.clientList) {
                                    var peerModel = PeerModel(
                                        peer.deviceName,
                                        peer.deviceAddress,
                                        peer.isGroupOwner.toString()
                                    )
                                    WifiDirectMainActivity.groupList.add(peerModel)
                                }

                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(100)
                                    WaitingForOwnerActivity.wifiWaitingRecyclerAdapter!!.notifyDataSetChanged()
                                    WifiDirectMainActivity.peerRecyclerviewAdapter!!.notifyDataSetChanged()
                                }
                            }
                        }

                        if (ServerThread == null) {
                            ServerThread =
                                FileReceiveServerAsyncTask(context).execute() as FileReceiveServerAsyncTask?
                        }

                    } else if (info!!.groupFormed) {
                        WifiDirectMainActivity.isGroupOwner = false
                        WifiDirectMainActivity.groupOwnerIP = info.groupOwnerAddress.hostAddress

                        WifiDirectMainActivity.wifiP2pManager!!.requestGroupInfo(
                            WifiDirectMainActivity.channel
                        ) { group ->
                            if (group != null) {
                                if (ServerThread == null) {
                                    if (WifiDirectMainActivity.groupOwnerIP != null && !WifiDirectMainActivity.isGroupOwner) {
                                        val intent =
                                            Intent(context, SendStreamIntentService::class.java)
                                        intent.putExtra("protocol", "1")
                                        intent.putExtra(
                                            "serverIP",
                                            WifiDirectMainActivity.groupOwnerIP
                                        )
                                        intent.putExtra("serverPort", 8989)
                                        intent.setAction("com.picpho.picpho.FIRST_CONNECT")
                                        context.startService(intent)
                                    }
                                    ServerThread =
                                        FileReceiveServerAsyncTask(context).execute() as FileReceiveServerAsyncTask?
                                }
                                var intent = Intent(context, WaitingForOwnerActivity::class.java)
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }
}
