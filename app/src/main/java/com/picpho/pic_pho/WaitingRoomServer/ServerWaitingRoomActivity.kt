package com.picpho.pic_pho.WaitingRoomServer

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.Ack
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kakao.sdk.user.UserApiClient
import com.picpho.pic_pho.App
import com.picpho.pic_pho.CellularSocket.SocketUtil.Companion.createAndConnectSocket
import com.picpho.pic_pho.CellularSocket.SocketUtil.Companion.mSocket
import com.picpho.pic_pho.ImageHandler.ImageHandler
import com.picpho.pic_pho.ImageHandler.ImageHandler.Companion.compressImage
import com.picpho.pic_pho.LoginActivity
import com.picpho.pic_pho.MakeGroup.MakeGroupActivity
import com.picpho.pic_pho.PhotoRoomServer.FileUtil
import com.picpho.pic_pho.PhotoRoomServer.FriendsPhotoCountModel
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity.Companion.photoPickedList
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity.Companion.photoPickedUriList
import com.picpho.pic_pho.PhotoRoomServer.ServerThumbnailPhotoModel
import com.picpho.pic_pho.R
import com.picpho.pic_pho.databinding.ActivityWaitingRoomServerBinding
import com.snatik.storage.Storage
import id.zelory.compressor.loadBitmap
import kotlinx.android.synthetic.main.activity_waiting_room_server.*
import kotlinx.android.synthetic.main.item_waiting_room_server.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.File
import java.util.concurrent.locks.Lock

class ServerWaitingRoomActivity : AppCompatActivity(), ServerWaitingRoomInterface {

    data class GroupMember(
        val UserID: String,
        val NickName: String,
        val ProfileUrl: String,
        val IsHost: Boolean,
        var status: Int
    )

    private lateinit var binding: ActivityWaitingRoomServerBinding
    private val TAG = "LOG"

    private lateinit var serverWaitingRoomAdapter: ServerWaitingRoomAdapter

    var infoOfUser: JsonObject? = null
    var numOfMyPhoto: Int = 0
    private var actualImage: File? = null
    lateinit var exif: ExifInterface
    private var compressedImage: File? = null
    var roomAddr: String? = null
    var roomName: String? = null
    private var dialog: Dialog? = null

    var returnedImage: Bitmap? = null

    var numOfReceivedPhoto: Int = 0

    var friendsCount: Int = 0
    var receivedFriendsCount: Int = 0
    var friendsPhotoCountList: MutableMap<String, FriendsPhotoCountModel> = mutableMapOf()


    var userTempImage: String? = null

    companion object {
        var photoModelList = ArrayList<ServerThumbnailPhotoModel>()
        var myKakaoId: String? = null
        var userKakaoNickName: String? = null

        var serverStorage: Storage? = null
        var serverFilePathList = ArrayList<String>()
        var serverWaitingRoomActivity: Activity? = null

        var invitedFriendsList = ArrayList<GroupMember>()
        var mutex = Mutex()
        var forScanFilePathList = ArrayList<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaitingRoomServerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        serverWaitingRoomActivity = this
        runBlocking {
            try {
                photoModelList.clear()
                photoPickedUriList?.clear()
                photoPickedList?.clear()
                invitedFriendsList?.clear()
            } catch (e: IndexOutOfBoundsException) {
            }
        }

        dialog?.dismiss()

        if (mSocket == null)
            mSocket = createAndConnectSocket()


        LoginActivity.requestPermissionToUser(this)

        runBlocking {
            CoroutineScope(Dispatchers.IO).launch {
                mSocket!!.on("DoSelectPhoto", DoSelectPhoto)
                mSocket!!.on("SendPictureFromServer", imageReturn)
                mSocket!!.on("enterMember", showEnteredMember)
                mSocket!!.on("startPhotoRoom", startPhotoRoom)
                mSocket!!.on("selectStatus", skipFriend)
                delay(100)
            }.join()
        }

        serverWaitingRoomAdapter = ServerWaitingRoomAdapter(invitedFriendsList)
        binding.waitingMemberRecyclerview.apply {
            layoutManager = LinearLayoutManager(
                this@ServerWaitingRoomActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = serverWaitingRoomAdapter
        }

        UserApiClient.instance.me { user, error ->
            if (error != null) {
            } else if (user != null) {
                userKakaoNickName = user.kakaoAccount?.profile?.nickname.toString()
                userTempImage = user.kakaoAccount?.profile?.profileImageUrl.toString()
            }
        }

        serverStorage = Storage(applicationContext)

        Log.d(TAG, "gogogogogogogo: ")
        roomAddr = intent.getStringExtra("roomAddress")
        roomName = intent.getStringExtra("roomName")
        var ishost = intent.getBooleanExtra("isHost", false)
        var jsonobjectlist = intent.getSerializableExtra("invitedFriendsJsonArray")

        if (ishost) {
            binding.buttonReinvite.visibility = View.VISIBLE
            binding.buttonForceStart.visibility = View.VISIBLE
        }


        Log.d(TAG, "invitedFriendsJsonArray: ${jsonobjectlist}, roomAddress is $roomAddr")



        Log.d(TAG, "onCreate: ${roomAddr}")
        Log.d(TAG, "ishost: ${ishost}")
        if (roomAddr != null) {
            if (mSocket != null) {
                UserApiClient.instance.me { user, error ->
                    if (error != null) {
                        infoOfUser = null
                    } else if (user != null) {
                        Log.d(TAG, "enterToWaitingroomWithInvitation")
                        infoOfUser = JsonObject()
                        infoOfUser!!.addProperty("UserID", user.id.toString())
                        myKakaoId = user.id.toString()
                        infoOfUser!!.addProperty(
                            "NickName",
                            user.kakaoAccount?.profile?.nickname.toString()
                        )

                        var profile: String? = null
                        if (user.kakaoAccount?.profile?.profileImageUrl.toString() == "null") {
                            profile = "https://user-images.githubusercontent.com/47134564/114666782-9f862f00-9d39-11eb-879e-ae427b2819a7.png"

                        } else {
                            profile = user.kakaoAccount?.profile?.profileImageUrl.toString()
                        }


                        infoOfUser!!.addProperty(
                            "ProfileImg",
                            profile
                        )
                        Log.d(TAG, "getUserInfo11: ${infoOfUser}")
                        if (ishost) {
                            infoOfUser!!.addProperty("IsHost", "True")
                            infoOfUser!!.addProperty("roomName", roomName)
                            Log.d(
                                TAG,
                                "onCreate: before emit host room Addr $roomAddr, infoOfUser $infoOfUser, size is ${MakeGroupActivity.invitedFriendsJsonArray.size()}"
                            )
                            mSocket!!.emit(
                                "enterRoom",
                                roomAddr,
                                infoOfUser,
                                MakeGroupActivity.invitedFriendsJsonArray.size(),
                                Ack {
                                    // for host, the Ack is useless
                                })
                        } else {
                            infoOfUser!!.addProperty("IsHost", "False")
                            Log.d(
                                TAG,
                                "onCreate: before emit client room Addr $roomAddr, infoOfUser is $infoOfUser"
                            )
                            mSocket!!.emit("enterRoom", roomAddr, infoOfUser, 0, Ack {
                                if (it[0] == 1) {
                                    // 없는 방에 초대된 유저가 접속한 경우
                                    CoroutineScope(Dispatchers.Main).launch {
                                        binding.textViewRoomNotExist.visibility = View.VISIBLE
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }


        binding.serverChoosePhotoCardview.setOnClickListener {
            Log.d(
                TAG,
                "onCreate: ACTION_GET_CONTENTACTION_GET_CONTENTACTION_GET_CONTENTACTION_GET_CONTENTACTION_GET_CONTENTACTION_GET_CONTENT"
            )
            var intent = ImageHandler.selectPhoto()
//            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/*"
            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            var userid: String? = null

            UserApiClient.instance.me { user, error ->
                if (error != null) {
                    userid = null
                } else if (user != null) {
                    userid = user.id.toString()
                }

            }
            Log.d(TAG, "onCreate: ${userid}")
            mSocket!!.emit("selectStatus", 1, roomAddr, infoOfUser)
            //serverWaitingRoomAdapter.modelList[0].status = 1

            startActivityForResult(intent, 100)
        }
    }

    var skipFriend = Emitter.Listener {
        var skipFriendKakaoId: String = it[0].toString()
        friendsPhotoCountList[skipFriendKakaoId]!!.isDone = true
        receivedFriendsCount++
        if (friendsCount == receivedFriendsCount) {
            //note sort
            runBlocking {
                CoroutineScope(Dispatchers.Default).launch {
                    var sortedModelList =
                        photoModelList.sortedWith(
                            compareBy(
                                { it.taketime },
                                { it.pictureowner })
                        )
                    photoModelList.clear()
                    photoModelList.addAll(sortedModelList)
                    delay(100)

                }.join()
            }
            mSocket!!.emit("receivedAll", myKakaoId, roomAddr)
        }
    }


    override fun onResume() {
        if (mSocket == null)
            mSocket = createAndConnectSocket()
        super.onResume()
    }

    override fun onItemClicked() {
    }

    override fun onDestroy() {
        CoroutineScope(Dispatchers.IO).launch {
            mSocket!!.off("enterMember")
            mSocket!!.off("SendPictureFromServer")
            mSocket!!.off("startPhotoRoom")
            mSocket!!.off("selectStatus")
            delay(100)
        }
        dialog?.dismiss()
        super.onDestroy()
    }

    override fun onBackPressed() {
        runBlocking {
            CoroutineScope(Dispatchers.Main).launch {
                dialog =
                    ServerPhotoRoomActivity.showDialog(
                        context = this@ServerWaitingRoomActivity,
                        resource = R.layout.dialog_leave_waitingroom,
                        gravity = Gravity.CENTER,
                        color = Color.WHITE
                    )
                dialog!!.findViewById<TextView>(R.id.button_leave).setOnClickListener {
                    dialog?.dismiss()
                    mSocket!!.emit("leaveRoom", Ack {
                        Log.e(TAG, "onBackPressed: leaveRoom ${it[0]}")
                        if (it[0].equals("received")) {
                            finish()
                        }
                    })
                }

                dialog!!.findViewById<TextView>(R.id.button_continue).setOnClickListener {
                    dialog?.dismiss()
                }
            }
        }
    }

    var startPhotoRoom = Emitter.Listener {
        CoroutineScope(Dispatchers.IO).launch {
            mSocket!!.off("enterMember")
            mSocket!!.off("SendPictureFromServer")
            mSocket!!.off("startPhotoRoom")
            mSocket!!.off("selectStatus")
            delay(100)
        }
        Log.d(
            TAG,
            "startPhotoRoom:startPhotoRoomstartPhotoRoomstartPhotoRoomstartPhotoRoomstartPhotoRoom "
        )
        val intent = Intent(App.instance, ServerPhotoRoomActivity::class.java)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.putExtra("roomAddress", roomAddr)
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    var showEnteredMember = Emitter.Listener {
        CoroutineScope(Dispatchers.Default).launch {
            mutex.withLock {
                try {
                    var jsonArray: JSONArray = JSONArray(it[0].toString())
                    var gson = Gson()
                    friendsCount = jsonArray.length() - 1

                    var tmp_invitedFriendList = ArrayList<GroupMember>()

                    for (i in 0 until jsonArray.length()) {
                        var jsonstring = jsonArray.get(i).toString()
                        var member = gson.fromJson(jsonstring, GroupMember::class.java)

                        if (member.UserID != myKakaoId && !friendsPhotoCountList.containsKey(member.UserID)) {
                            friendsPhotoCountList[member.UserID] = FriendsPhotoCountModel()
                        }
//                        invitedFriendsList.add(member)
                        tmp_invitedFriendList.add(member)
                    }

                    var dummyHolders = it[1].toString().toInt() - jsonArray.length()
                    for (j in 0 until dummyHolders) {
                        var member = GroupMember("dummy", "dummy", "dummy", false, -1)
//                        invitedFriendsList.add(member)
                        tmp_invitedFriendList.add(member)
                    }
                    invitedFriendsList.clear()
                    invitedFriendsList.addAll(tmp_invitedFriendList)
                    serverWaitingRoomNotifyDatasetChanged()

                } catch (e: IndexOutOfBoundsException) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun serverWaitingRoomNotifyDatasetChanged() {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
            if (serverWaitingRoomAdapter != null) {
                try {
                    delay(100)
                    serverWaitingRoomAdapter?.notifyDataSetChanged()

                } catch (e: IndexOutOfBoundsException) {
                    e.printStackTrace()
                }
            }
        }.join()
    }

    var DoSelectPhoto = Emitter.Listener {
        Log.d(TAG, "DoSelectPhoto: DoSelectPhoto")
        CoroutineScope(Dispatchers.Main).launch {
            binding.serverChoosePhotoCardview.visibility = View.VISIBLE
            binding.hostButtons.visibility = View.INVISIBLE
        }
        mSocket!!.off("DoSelectPhoto")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            100 -> {
                var userName: String = userKakaoNickName!!
                var userImage: String = userTempImage!!
                var pictureowner: String = myKakaoId!!

                UserApiClient.instance.me { user, error ->
                    if (error != null) {
                    } else if (user != null) {
                        userName = user.kakaoAccount?.profile?.nickname.toString()
                        userImage = user.kakaoAccount?.profile?.profileImageUrl.toString()
                        Log.d(
                            TAG, "user.kakaoAccount?.profile?.nickname.toString()" +
                                    "${user.kakaoAccount?.profile?.nickname.toString()}" +
                                    "${user.kakaoAccount?.profile?.profileImageUrl.toString()}" +
                                    "onActivityResult: pictureowner $pictureowner"
                        )
                    }
                }
                if (data != null) {
                    if (data.clipData != null) {
                        CoroutineScope(Dispatchers.Default).launch(Dispatchers.Default) {
                            val count = data.clipData!!.itemCount
                            numOfMyPhoto = count;
                            for (i in 0 until count) {
                                var imageUri = data.clipData!!.getItemAt(i).uri
                                var taketime: String? = null

                                var absolutePath: String =
                                    ImageHandler.getFullPathFromUri(App.instance, imageUri)!!


                                val takeFlags: Int = intent.flags and
                                        (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                contentResolver.takePersistableUriPermission(imageUri, takeFlags)

                                serverFilePathList.add(
                                    absolutePath
                                )
                                actualImage = FileUtil.from(App.instance, imageUri)

                                var orientation: Int = 0
                                //메타정보추출하기
                                exif = ExifInterface(actualImage!!)
                                if (exif != null) {
                                    taketime =
                                        exif.getAttribute(ExifInterface.TAG_DATETIME).toString()
                                    orientation =
                                        exif!!.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
                                }
                                var timeIndexPlusNumber : String = (i%10).toString()
                                if (taketime == "null") {
                                    taketime = System.currentTimeMillis().toString() + timeIndexPlusNumber
                                    Log.d(
                                        TAG,
                                        "onActivityResult: System.currentTimeMillis().toString() ${
                                            System.currentTimeMillis().toString()
                                        }"
                                    )
                                }
                                runBlocking {
                                    compressedImage =
                                        compressImage(actualImage!!, this@ServerWaitingRoomActivity)
                                }



                                CoroutineScope(Dispatchers.IO).launch {
                                    var image_string =
                                        ImageHandler.bitmapToString(loadBitmap(compressedImage!!))
                                    mSocket!!.emit(
                                        "SendPictureFromClient",
                                        image_string,
                                        roomAddr,
                                        taketime,
                                        pictureowner,
                                        i,
                                        count,
                                        userName,
                                        userImage
                                    )

                                    var photoinfo = ServerThumbnailPhotoModel(
                                        imageUri,
                                        taketime,
                                        pictureowner,
                                        i,
                                        count,
                                        userName,
                                        userImage,
                                        absolutePath = absolutePath,
                                        orientation = orientation
                                    )

                                    photoModelList.add(photoinfo)
                                }
                            }
                            mSocket!!.emit("selectStatus", 2, roomAddr, infoOfUser)
                        }
                    } else {
                        val imageUri = data.data
                        var taketime: String? = null
                        Log.d(TAG, "onActivityResult: imageUri ${imageUri.toString()}")
                        var absolutePath: String =
                            ImageHandler.getFullPathFromUri(this, imageUri).toString()
                        serverFilePathList.add(
                            absolutePath
                        )

                        val takeFlags: Int = intent.flags and
                                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        contentResolver.takePersistableUriPermission(imageUri!!, takeFlags)

                        actualImage = FileUtil.from(App.instance, imageUri)

                        exif = ExifInterface(actualImage!!)
                        if (exif != null) {
                            taketime = exif.getAttribute(ExifInterface.TAG_DATETIME).toString()
                        }
                        if (taketime == null)
                            taketime = System.currentTimeMillis().toString()


                        runBlocking {
                            compressedImage =
                                compressImage(actualImage!!, this@ServerWaitingRoomActivity)
                        }


                        CoroutineScope(Dispatchers.IO).launch {
                            var image_string =
                                ImageHandler.bitmapToString(loadBitmap(compressedImage!!))
                            mSocket!!.emit(
                                "SendPictureFromClient",
                                image_string,
                                roomAddr,
                                taketime,
                                pictureowner,
                                0,
                                1,
                                userName,
                                userImage
                            )

                            var photoinfo = ServerThumbnailPhotoModel(
                                imageUri,
                                taketime,
                                pictureowner,
                                0,
                                1,
                                username = userName,
                                userimg = userImage,
                                absolutePath = absolutePath
                            )
                            Log.d(TAG, "onActivityResult: username $userName, userimg $userImage")

                            photoModelList.add(photoinfo)
                        }
                        mSocket!!.emit("selectStatus", 2, roomAddr, infoOfUser)
                    }

                } else
                    mSocket!!.emit("selectStatus", 3, roomAddr, infoOfUser)
                CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                    binding.loadingAnimation.visibility = View.VISIBLE
                    binding.loadingAnimation.playAnimation()
                    binding.serverChoosePhotoCardview.visibility = View.GONE
                }
            }
        }
    }


    val mutex = Mutex()


    var imageReturn = Emitter.Listener { it ->

        var data = it[0].toString()
        var receivedtaketime = it[1].toString()
        var receivedowner = it[2].toString()
        var currentorder = it[3].toString()
        var totalcount = it[4].toString().toInt()
        var sum_totalCount = it[5].toString()
        var is_everyone = it[6].toString().toInt()
        var username = it[7].toString()
        var userimg = it[8].toString()
        Log.d(TAG, "username: $username,, userimg $userimg")

        Log.d(TAG, "receivedowner: $receivedowner and myKakaoId is $myKakaoId")
        Log.d(TAG, "receivedtaketime: $receivedtaketime and receivedtaketime is $receivedtaketime")

//        friendsPhotoCountList[receivedowner]!!.goalPhotoCounts = totalcount
//        (friendsPhotoCountList[receivedowner]!!.receivedPhotoCounts)++

        returnedImage = ImageHandler.convertString64ToImage(data)
        runBlocking {
            CoroutineScope(Dispatchers.Default).launch {
                Log.d(
                    TAG,
                    "friendsPhotoCountList[receivedowner]!!.received전전전PhotoCounts: ${friendsPhotoCountList[receivedowner]!!.receivedPhotoCounts}"
                )
                mutex.withLock {
                    friendsPhotoCountList[receivedowner]!!.goalPhotoCounts = totalcount
                    (friendsPhotoCountList[receivedowner]!!.receivedPhotoCounts)++
                }
                Log.d(
                    TAG,
                    "friendsPhotoCountList[receivedowner]!!.receivedP후후후hotoCounts: ${friendsPhotoCountList[receivedowner]!!.receivedPhotoCounts}"
                )
                Log.d(
                    TAG,
                    "friendsPhotoCountList: ${friendsPhotoCountList}, size is ${friendsPhotoCountList.size}"
                )


                var filepath: String = "/sdcard" + "/DCIM/Picpho/"
                var filename: String =
                    "Picpho_" + System.currentTimeMillis().toString() + ".jpg"
//                "Picpho_" + receivedowner + "_" + System.currentTimeMillis().toString() + ".jpg"
                var fullFilePath: String = filepath + filename
                serverFilePathList.add(fullFilePath)
                forScanFilePathList.add(fullFilePath)

                var file = File(fullFilePath)
                val dirs = File(file.parent.toString())
                if (!dirs.exists()) dirs.mkdirs()
                ImageHandler.saveBitmapAsFile(
                    returnedImage!!,
                    file,
                    receivedtaketime,
                    receivedowner
                )
//            var photoinfo = ServerThumbnailPhotoModel(Uri.fromFile(file!!))
                var photoinfo = ServerThumbnailPhotoModel(
                    Uri.fromFile(file),
                    receivedtaketime,
                    receivedowner,
                    currentorder.toInt(),
                    totalcount.toInt(),
                    absolutePath = fullFilePath,
                    username = username,
                    userimg = userimg
                )
                photoModelList.add(photoinfo)
                numOfReceivedPhoto++

                Log.d(
                    TAG, "friendsPhotoCountList[receivedowner]!!.goalPhotoCounts: " +
                            "${friendsPhotoCountList[receivedowner]!!.goalPhotoCounts}" +
                            "friendsPhotoCountList[receivedowner]!!.receivedPhotoCounts" +
                            "${friendsPhotoCountList[receivedowner]!!.receivedPhotoCounts}"
                )

                if (friendsPhotoCountList[receivedowner]!!.goalPhotoCounts
                    == friendsPhotoCountList[receivedowner]!!.receivedPhotoCounts
                ) {
                    friendsPhotoCountList[receivedowner]!!.isDone = true
                    receivedFriendsCount++
                }


                if (friendsCount == receivedFriendsCount) {
                    for (tmp in photoModelList) {

                    }
                    //note sort
                    var sortedModelList =
                        photoModelList.sortedWith(
                            compareBy(
                                { it.pictureowner },
                                { it.taketime }
                            )
                        )
                    runBlocking {
                        photoModelList.clear()
                    }
                    runBlocking {
                        photoModelList.addAll(sortedModelList)
                    }
                    for (tmp in photoModelList) {

                    }

                    mSocket!!.emit("receivedAll", myKakaoId, roomAddr)
                }
                Log.d(
                    TAG,
                    "go to photoroom: ${numOfReceivedPhoto} || ${sum_totalCount.toInt()} || ${numOfMyPhoto}"
                )
            }
        }
    }

    fun reInviteFriends(view: View) {
        Log.d(TAG, "ServerWaitingRoomActivity - reInviteFriends() called")


        var jsonObjectList = intent.getSerializableExtra("invitedFriendsJsonArray")

        roomName = intent.getStringExtra("roomName").toString()

        mSocket!!.emit(
            "selectedGroup",
            MakeGroupActivity.invitedFriendsJsonArray, roomAddr, roomName, userKakaoNickName
        )
    }

    fun forcedStart(view: View) {
        mSocket!!.emit("forcedStart", roomAddr)
    }

}
