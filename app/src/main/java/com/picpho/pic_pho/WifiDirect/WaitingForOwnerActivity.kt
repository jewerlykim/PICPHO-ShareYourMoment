package com.picpho.pic_pho.WifiDirect

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.App
import com.picpho.pic_pho.ImageHandler.ImageHandler
import com.picpho.pic_pho.ImageHandler.ImageHandler.Companion.compressImage
import com.picpho.pic_pho.PhotoRoom.PhotoRoomActivity
import com.picpho.pic_pho.PhotoRoomServer.FileUtil
import com.picpho.pic_pho.WifiDirect.UI.WifiWaitingRoom.WifiWaitingRecyclerAdapter
import com.picpho.pic_pho.WifiDirect.UI.WifiWaitingRoom.WifiWaitingViewHolder
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.filePathList
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.groupOwnerIP
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.isGroupOwner
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.photoInfoList
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.photoOwnerMacAddress
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.sendToServerPhotos
import com.picpho.pic_pho.databinding.ActivityWaitingForOwnerBinding
import kotlinx.android.synthetic.main.activity_waiting_for_owner.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class WaitingForOwnerActivity : AppCompatActivity() {

    var photoFlag = false
    private val TAG = "WaitingForOwnerActivity"
    private lateinit var binding: ActivityWaitingForOwnerBinding

    companion object {
        var wifiWaitingRecyclerAdapter: RecyclerView.Adapter<WifiWaitingViewHolder>? = null
        var wifiChoosePhotoCardView: CardView? = null
        var waitingForOwnerActivity: Activity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaitingForOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        photoFlag = false
        waitingForOwnerActivity = this
        wifiChoosePhotoCardView = binding.wifiChoosePhotoCardview

        wifiWaitingRecyclerAdapter = WifiWaitingRecyclerAdapter()
        (wifiWaitingRecyclerAdapter as WifiWaitingRecyclerAdapter).submitList(WifiDirectMainActivity.groupList)
        binding.waitingDeviceRecyclerview.apply {
            layoutManager =
                LinearLayoutManager(
                    this@WaitingForOwnerActivity,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter = wifiWaitingRecyclerAdapter
        }

        wifiChoosePhotoCardView!!.setOnClickListener {
            if (!photoFlag) {
                var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                intent.type = "image/*"
                var REQUESTCODE: Int = 0
                REQUESTCODE = if (isGroupOwner)
                    200
                else
                    100
                startActivityForResult(intent, REQUESTCODE)
                binding.wifiChoosePhotoCardview.visibility = View.GONE
                photoFlag = true
            }
        }

        if (!isGroupOwner)
            showPeerListInWaitingRoom()
        else {
            CoroutineScope(Dispatchers.Main).launch {
                binding.waitingTextForOwner.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.Main).launch {
            if (isGroupOwner) {
                wifiChoosePhotoCardView!!.isEnabled = false
                wifiChoosePhotoCardView!!.setCardBackgroundColor(Color.LTGRAY)
            }
            delay(100)
            wifiWaitingRecyclerAdapter!!.notifyDataSetChanged()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            100 -> {
                if (data != null) {
                    if (data.clipData != null) {
                        var count = data.clipData!!.itemCount
                        for (i in 0 until count) {
                            var imageUri = data.clipData!!.getItemAt(i).uri
                            var absolutePath: String =
                                ImageHandler.getFullPathFromUri(this, imageUri)!!
                            var actualImage: File = FileUtil.from(this, imageUri)
                            var compressedImage: File? = compressImage(actualImage, this)
                            if (compressedImage != null)
                                imageUri = Uri.fromFile(compressedImage)

                            var photoinfo = PhotoInfo(
                                imageUri,
                                photoOwnerMacAddress!!,
                                "picpho",
                                absolutePath = absolutePath
                            )
                            photoInfoList.add(photoinfo)

                            filePathList.add(
                                absolutePath
                            )
                        }
                        sendToServerPhotos(this)
                    } else {
                        var imageUri = data.data
                        var absolutePath: String = ImageHandler.getFullPathFromUri(this, imageUri)!!
                        var actualImage: File = FileUtil.from(this, imageUri!!)
                        var compressedImage: File? = compressImage(actualImage, this)
                        if (compressedImage != null)
                            imageUri = Uri.fromFile(compressedImage)

                        var photoinfo = PhotoInfo(
                            imageUri,
                            photoOwnerMacAddress!!,
                            "picpho",
                            absolutePath = absolutePath
                        )
                        photoInfoList.add(photoinfo)

                        filePathList.add(
                            absolutePath
                        )
                        sendToServerPhotos(this)
                    }
                }
                var waitingOtherFriendsText: String = "다른 친구들이 사진을 모두 선택할 때까지\n잠시만 기다려주세요!"
                CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                    binding.loadingAnimation.visibility = View.VISIBLE
                    binding.loadingAnimation.playAnimation()
                    binding.wifiWaitingTextviewForClient.text = waitingOtherFriendsText
                }
            }


            200 -> {
                if (data != null) {
                    if (data.clipData != null) {
                        var count = data.clipData!!.itemCount

                        for (i in 0 until count) {
                            var imageUri = data.clipData!!.getItemAt(i).uri
                            var absolutePath: String =
                                ImageHandler.getFullPathFromUri(this, imageUri)!!
                            var actualImage: File = FileUtil.from(this, imageUri)
                            var compressedImage: File? = compressImage(actualImage, this)
                            if (compressedImage != null)
                                imageUri = Uri.fromFile(compressedImage)
                            var photoinfo = PhotoInfo(
                                imageUri,
                                photoOwnerMacAddress!!,
                                "picpho",
                                absolutePath = absolutePath
                            )
                            photoInfoList.add(photoinfo)
                            // 사진 삭제를 위한 추가
                            filePathList.add(
                                absolutePath
                            )
                        }
                    } else {
                        var imageUri = data.data
                        var absolutePath: String = ImageHandler.getFullPathFromUri(this, imageUri)!!
                        var actualImage: File = FileUtil.from(this, imageUri!!)
                        var compressedImage: File? = compressImage(actualImage, this)
                        if (compressedImage != null)
                            imageUri = Uri.fromFile(compressedImage)
                        var photoinfo = PhotoInfo(
                            imageUri,
                            photoOwnerMacAddress!!,
                            "picpho",
                            absolutePath = absolutePath
                        )
                        photoInfoList.add(photoinfo)
                        filePathList.add(
                            absolutePath
                        )
                    }
                }

                CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                    binding.loadingAnimation.visibility = View.VISIBLE
                    binding.loadingAnimation.playAnimation()
                }

                for (peer in WifiDirectMainActivity.connectedPeerList) {
                    CoroutineScope(Dispatchers.Default).launch {
                        var counter = 0
                        var clientPhotoCount: Int =
                            WifiDirectMainActivity.connectedPeerMap.get(peer)!!
                        var receivePhotoCount = photoInfoList.size - clientPhotoCount
                        var STATUS = 3
                        var uri: Uri?
                        if (isGroupOwner) {
                            if (receivePhotoCount != 0) {

                                for (photo in photoInfoList) {
                                    if (!peer.equals(photo.photoOwnerIP)) {

                                        val intent = Intent(
                                            App.instance,
                                            SendStreamIntentService::class.java
                                        )
                                        if (photo.photoOwnerIP.equals("picpho")) {
                                            uri = photo.photoUri
                                        } else {
                                            uri = Uri.fromFile(File(photo.photoUri.toString()))
                                        }
                                        counter++
                                        STATUS -= (counter / receivePhotoCount)
                                        intent.putExtra("protocol", "2")
                                        intent.putExtra("photoOwnerMac", photo.photoOwnerMac)
                                        intent.putExtra("serverIP", peer)
                                        intent.putExtra("uri", uri.toString())
                                        intent.putExtra("serverPort", 8989)
                                        intent.putExtra("status", STATUS)
                                        intent.action = "com.picpho.picpho.CONNECT_TO_SERVER"
                                        startService(intent)
                                    }
                                }
                            } else {

                                val intent =
                                    Intent(App.instance, SendStreamIntentService::class.java)
                                intent.putExtra("protocol", "3")
                                intent.putExtra("serverIP", peer)
                                intent.putExtra("serverPort", 8989)
                                intent.action = "com.picpho.picpho.FIRST_CONNECT"
                                startService(intent)
                            }
                        }
                    }
                }

                var intentToPhotoRoom = Intent(this, PhotoRoomActivity::class.java)
                startActivity(intentToPhotoRoom)
            }
        }
    }

    override fun onBackPressed() {
        CoroutineScope(Dispatchers.IO).launch {
            WifiDirectMainActivity.removeGroup()
            if (WifiDirectBroadcastReceiver.ServerThread != null && !WifiDirectBroadcastReceiver.ServerThread!!.isCancelled) {
                WifiDirectBroadcastReceiver.ServerThread!!.cancel(true)
                WifiDirectBroadcastReceiver.ServerThread = null
            }
        }
        super.onBackPressed()
    }

    fun sendToServerSkipPhoto() {
        if (groupOwnerIP != null && !isGroupOwner && photoInfoList.isEmpty()) {
            val intent = Intent(this, SendStreamIntentService::class.java)
            if (!isGroupOwner) {
                intent.putExtra("protocol", "3")
                intent.putExtra("serverIP", groupOwnerIP)
                intent.putExtra("serverPort", 8989)
                intent.action = "com.picpho.picpho.FIRST_CONNECT"
                startService(intent)
            }
        }
    }

    fun showPeerListInWaitingRoom() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val roomMessageInWifiWaitingRoom: String = "의 방입니다! \n 공유할 사진을 선택해주세요!"
        WifiDirectMainActivity.wifiP2pManager?.requestGroupInfo(
            WifiDirectMainActivity.channel
        ) { group ->
            if (group != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    binding.waitingDeviceRecyclerview.visibility = View.GONE
                    binding.wifiWaitingTextviewForClient.text =
                        group!!.owner.deviceName + roomMessageInWifiWaitingRoom
                    binding.wifiWaitingTextviewForClient.visibility = View.VISIBLE
                }
            }
        }
    }
}