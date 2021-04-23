package com.picpho.pic_pho.PhotoRoomServer

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.Ack
import com.google.gson.Gson
import com.picpho.pic_pho.App
import com.picpho.pic_pho.CellularSocket.SocketUtil.Companion.createAndConnectSocket
import com.picpho.pic_pho.CellularSocket.SocketUtil.Companion.mSocket
import com.picpho.pic_pho.GroupVoiceCall.AGEventHandler
import com.picpho.pic_pho.GroupVoiceCall.EngineEventHandler
import com.picpho.pic_pho.GroupVoiceCall.WorkerThread
import com.picpho.pic_pho.Lobby.DBHelper
import com.picpho.pic_pho.LoginActivity
import com.picpho.pic_pho.MakeGroup.MakeGroupActivity.Companion.makeGroupActivity
import com.picpho.pic_pho.PhotoRoomServer.Drawer.ServerDrawerMemberModel
import com.picpho.pic_pho.PhotoRoomServer.Drawer.ServerDrawerMembersAdapter
import com.picpho.pic_pho.PhotoRoomServer.Drawer.ServerDrawerPickAdapter
import com.picpho.pic_pho.PhotoRoomServer.ViewPager.ServerPhotoRoomPagerAdapter
import com.picpho.pic_pho.R
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity.Companion.forScanFilePathList
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity.Companion.invitedFriendsList
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity.Companion.myKakaoId
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity.Companion.photoModelList
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity.Companion.serverWaitingRoomActivity
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity.Companion.userKakaoNickName
import com.picpho.pic_pho.databinding.ActivityServerphotoroomBinding
import com.picpho.pic_pho.databinding.ItemServerphotoroomPagerBinding
import kotlinx.android.synthetic.main.activity_photoroom.*
import kotlinx.android.synthetic.main.activity_serverphotoroom.*
import kotlinx.android.synthetic.main.dialog_accept_focus.*
import kotlinx.android.synthetic.main.item_photoroom_pager.*
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ServerPhotoRoomActivity : AppCompatActivity(), AGEventHandler {
    val TAG: String = "LOG"
    var returnedImage: Bitmap? = null

    lateinit var exifInterface: ExifInterface
    private var actualImage: File? = null
    private var compressedImage: File? = null
    private var photoFlag = false
    private var drawer: LinearLayout? = null
    private var drawerCover: LinearLayout? = null
    private var isPageOpen = false
    private var engineEventHandler: EngineEventHandler? = null
    private var currentPagerPosition = 0
    private val drawerMemberList = ArrayList<ServerDrawerMemberModel>()

    private var dialog: Dialog? = null

    // claim button clicked
    var isClickedClaim: Boolean = false

    private var photoOldPosition: Int = 0
    private var myName: String? = null
    private var moveToPosition: Int = 0


    @Volatile
    private var mAudioRouting = -1

    @Volatile
    private var mAudioMuted = false

    var numOfReceivedPhoto: Int = 0
    var numOfExpectedPhoto: Int = 0
    var numOfMyPhoto: Int = 0
    var workerThread: WorkerThread? = null

    var numOfMembers: Int = 0

    private var memberList: String? = null
    private var roomName: String? = null

    companion object {
        var photoPickedUriList = ArrayList<Uri>()
        var photoPickedList = ArrayList<ServerThumbnailPhotoModel>()
        lateinit var serverThumbnailRecyclerAdapter: ServerThumbnailRecyclerAdapter
        lateinit var serverPhotoRoomPagerAdapter: ServerPhotoRoomPagerAdapter
        lateinit var serverDrawerPickAdapter: ServerDrawerPickAdapter
        lateinit var serverDrawerMembersAdapter: ServerDrawerMembersAdapter
        lateinit var binding: ActivityServerphotoroomBinding
        lateinit var photoBinding: ItemServerphotoroomPagerBinding
        var roomAddress: String? = null
        var myLikeCounts: Int = 0

        var serverPhotoRoomActivity: Activity? = null

        fun changeSelectedPhotoByClicked(position: Int) {
            binding.ServerPhotoroomViewPager.currentItem = position
        }

        fun scanFile(context: Context?, f: File, mimeType: String) {
            MediaScannerConnection
                .scanFile(context, arrayOf(f.absolutePath), arrayOf(mimeType), null)
        }

        fun getExif(file: File) {
            var exif = ExifInterface(file)
            if (exif != null) {
                var myAttribute: String? = "[Exif information] \n\n"
                myAttribute += "TAG_DATETIME           ::: " + exif.getAttribute(ExifInterface.TAG_DATETIME)
                    .toString() + "\n"
                myAttribute += "TAG_ARTIST             ::: " + exif.getAttribute(ExifInterface.TAG_ARTIST)
                    .toString() + "\n"
            }
        }

        fun showDialog(context: Context, resource: Int, gravity: Int, color: Int): Dialog {
            var dialog = Dialog(context)
            dialog.setContentView(resource)
            dialog.window!!.setGravity(gravity)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(color))
            dialog.show()
            return dialog
        }
    }

    //    @RequiresApi(Build.VERSION_CODES.M)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityServerphotoroomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        photoBinding = ItemServerphotoroomPagerBinding.inflate(layoutInflater)

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


        serverPhotoRoomActivity = this

        if (dialog != null)
            dialog!!.dismiss()



        if (forScanFilePathList != null) {
            val sizeOfScanFileList: Int = forScanFilePathList.size
            for (i in 0 until sizeOfScanFileList) {
                CoroutineScope(Dispatchers.Default).launch {
                    MediaScannerConnection
                        .scanFile(
                            serverPhotoRoomActivity,
                            arrayOf(forScanFilePathList[i]),
                            arrayOf("jpg"),
                            null
                        )
                }
            }
        }



        binding.textviewLeavePhotoRoom.setOnClickListener {
            showDoYouWantLeaveThisRoom()
        }

        binding.imageViewPhotoRoomExit.setOnClickListener {
            showDoYouWantLeaveThisRoom()
        }

        binding.textViewExitphotoRoomExit.setOnClickListener {
            showDoYouWantLeaveThisRoom()
        }

        CoroutineScope(Dispatchers.Main).launch {
            binding.imageViewClaimFocus.setColorFilter(
                resources.getColor(R.color.picpho_salmon),
                PorterDuff.Mode.SRC_IN
            )
        }

        LoginActivity.requestPermissionToUser(this)


        roomAddress = intent.getStringExtra("roomAddress")


        if (mSocket == null)
            mSocket = createAndConnectSocket()

        photoFlag = false
        numOfExpectedPhoto = 0
        numOfMyPhoto = 0

        val channel: String = intent.getStringExtra("roomAddress")
        App.initWorkerThread()
        workerThread = App.workerThread
        engineEventHandler = workerThread!!.eventHandler()
        engineEventHandler!!.addEventHandler(this)
        workerThread!!.joinChannel(channel, workerThread!!.engineConfig.mUid)
        volumeControlStream = AudioManager.STREAM_VOICE_CALL

        serverPhotoRoomPagerAdapter = ServerPhotoRoomPagerAdapter(photoModelList)
        serverThumbnailRecyclerAdapter = ServerThumbnailRecyclerAdapter(photoModelList)
        serverDrawerPickAdapter = ServerDrawerPickAdapter(photoPickedUriList)
        serverDrawerMembersAdapter = ServerDrawerMembersAdapter(invitedFriendsList)

        binding.recyclerviewDrawerPicked.apply {
            layoutManager = LinearLayoutManager(
                this@ServerPhotoRoomActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = serverDrawerPickAdapter
        }
        binding.recyclerviewDrawerMember.apply {
            layoutManager = LinearLayoutManager(
                this@ServerPhotoRoomActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = serverDrawerMembersAdapter
        }

        binding.ServerPhotoroomViewPager.apply {
            adapter = serverPhotoRoomPagerAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }


        binding.ServerPhotoRoomRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@ServerPhotoRoomActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = serverThumbnailRecyclerAdapter
        }


        runBlocking {
            CoroutineScope(Dispatchers.IO).launch {
                mSocket!!.emit("RequestMemberList", roomAddress.toString())
                mSocket!!.on("done", imageReceiveDone)
                mSocket!!.on("privateRoomSuccess", privateRoomSuccess)
                mSocket!!.on("receiveFocus", focusReceive)
                mSocket!!.on("CancelFocus", focusCancel)
                mSocket!!.on("clickLike", likeReceive)
                mSocket!!.on("GetMemberListAndRoomName", getMemberListAndRoomName)
                delay(100)
            }.join()
        }

        drawer = binding.drawerPhotoRoom
        drawerCover = binding.drawerCover
        val leftAnimation = AnimationUtils.loadAnimation(this, R.anim.translate_left)
        val rightAnimation = AnimationUtils.loadAnimation(this, R.anim.translate_right)

        binding.imageViewPhotoRoomMoreAction.setOnClickListener {
            clickDrawer(leftAnimation, rightAnimation)
        }
        binding.drawerEmpty.setOnClickListener {
            clickDrawer(leftAnimation, rightAnimation)
        }

        binding.imageViewSound.setOnClickListener {
            onSwitchSpeakerClicked()
        }


        binding.imageViewClaimFocus.setOnClickListener {
            clickedClaim(this)
        }

        binding.linearLayoutVoice.setOnClickListener {
            onVoiceMuteClicked()
        }


        numOfMembers = intent.getIntExtra("numOfMembers", 0)
        CoroutineScope(Dispatchers.IO).launch {
            mSocket!!.emit("privateRoom", roomAddress.toString(), numOfMembers)
        }
    }

    fun clickedClaim(context: Context) {
        if (!isClickedClaim) {
            CoroutineScope(Dispatchers.Main).launch {
                showDoYouWantClaimDialog(context)
                isClickedClaim = !isClickedClaim
                delay(3000)
                dismissClaimFocus()
                isClickedClaim = !isClickedClaim
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showDoYouWantLeaveThisRoom() {
        runBlocking {
            CoroutineScope(Dispatchers.Main).launch {
                dialog =
                    showDialog(
                        context = serverPhotoRoomActivity!!,
                        resource = R.layout.dialog_leave_photoroom,
                        gravity = Gravity.CENTER,
                        color = Color.WHITE
                    )

                dialog!!.findViewById<TextView>(R.id.button_leave).setOnClickListener {
                    quitPhotoRoom()
                    dialog!!.dismiss()
                }
                dialog!!.findViewById<TextView>(R.id.button_continue).setOnClickListener {
                    dialog!!.dismiss()
                }
            }
        }
    }


    private fun showDoYouWantClaimDialog(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            dialog =
                showDialog(
                    context = context,
                    resource = R.layout.dialog_claim_focus,
                    gravity = Gravity.BOTTOM,
                    color = Color.WHITE
                )

            dialog!!.findViewById<TextView>(R.id.dismissClaimText).setOnClickListener {
                dialog!!.dismiss()
            }
            dialog!!.findViewById<TextView>(R.id.wantClaimText).setOnClickListener {
                clickClaimFocus()
                dialog!!.dismiss()
            }
        }
    }

    var getMemberListAndRoomName = Emitter.Listener {
        memberList = it[0].toString()
        roomName = it[1].toString()
        CoroutineScope(Dispatchers.Main).launch {
            binding.textViewPhotoRoomTitle.text = roomName


            Log.d(TAG, "memberList 1: $memberList")
            var jsonArray = JSONArray(memberList)
            Log.d(TAG, "memberList 2 : ${jsonArray}")
            val gson = Gson()
            for (i in 0 until jsonArray.length()) {
                val jsonstring = jsonArray.get(i).toString()
                var member = gson.fromJson(jsonstring, ServerDrawerMemberModel::class.java)
                drawerMemberList.add(member)
            }
            delay(100)
            serverDrawerMembersAdapter.notifyDataSetChanged()
        }
    }


    private fun dismissClaimFocus() {
        CoroutineScope(Dispatchers.IO).launch {
            mSocket!!.emit("CancelFocus", roomAddress)
        }
        CoroutineScope(Dispatchers.Main).launch {
            binding.imageViewClaimFocus.setColorFilter(
                resources.getColor(R.color.picpho_salmon),
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    var focusCancel = Emitter.Listener {
        val lock: String = it[0].toString()
        Log.d(TAG, "lock is : $lock")
        CoroutineScope(Dispatchers.Main).launch {
            binding.imageViewClaimFocus.setColorFilter(
                resources.getColor(R.color.picpho_salmon),
                PorterDuff.Mode.SRC_IN
            )
            binding.imageViewClaimFocus.isEnabled = true
        }
    }

    var likeReceive = Emitter.Listener {
        Log.d(TAG, "likeReceived: likeReceived")
        val photoIndex: Int = Integer.parseInt(it[0].toString())
        val photoOwner: String = it[1].toString()
        val photoLikeCount: Int = Integer.parseInt(it[2].toString())
        Log.d(
            TAG,
            "photoIndex, photoOwner, photoLikeCount: $photoIndex, $photoOwner, $photoLikeCount"
        )
        Log.d(TAG, "Integer.parseInt(it[3].toString()):is ${Integer.parseInt(it[3].toString())} ")
        when (Integer.parseInt(it[3].toString())) {
            1 -> likeClicked(photoIndex, photoOwner, photoLikeCount)
            0 -> unlikeClicked(photoIndex, photoOwner, photoLikeCount)
            else -> Log.d(TAG, "Flag error: not 0 or 1")
        }
    }

    private fun likeClicked(photoIndex: Int, photoOwner: String, photoLikeCount: Int) {
        Log.d(TAG, "likeClicked: photoIndex $photoIndex")
        Log.d(TAG, "likeClicked: myKakaoId $myKakaoId, $photoOwner")
        Log.d(TAG, "likeClicked: photoModelList size is ${photoModelList.size}")
        if (myKakaoId == photoOwner) {
            myLikeCounts++
            Log.d(TAG, "likeClicked: myLikeCounts++ == $myLikeCounts")
            CoroutineScope(Dispatchers.Main).launch {
                binding.iReceivedHeart.visibility = View.VISIBLE
                binding.iReceivedHeart.playAnimation()
                delay(2000)
                binding.iReceivedHeart.pauseAnimation()
                binding.iReceivedHeart.visibility = View.GONE
                binding.textViewHeartCount.text = myLikeCounts.toString()
            }
        }
        playLeftOrRightAnimation(photoIndex)

        Log.d(
            TAG,
            "likeClicked: photoIndex $photoIndex  photoModelList[photoIndex].likeCount is ${photoModelList[photoIndex].likeCount}"
        )

        // 사진에 대한 좋아요 수 늘리기
        photoModelList[photoIndex].likeCount = photoLikeCount
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            serverPhotoRoomPagerAdapter.notifyDataSetChanged()
            Log.d(TAG, "likeClicked: adapter notify data set changed")
        }
        Log.d(
            TAG,
            "likeClicked: like Click 끝남 photoModelList[photoIndex].likeCount is ${photoModelList[photoIndex].likeCount}"
        )
    }

    private fun playLeftOrRightAnimation(photoIndex: Int) {
        if (photoIndex > photoOldPosition) {
            Log.d(
                TAG,
                "playLeftOrRightAnimation: photoIndex is $photoIndex and photoOldPosition is $photoOldPosition"
            )
            CoroutineScope(Dispatchers.Main).launch {
                binding.rightHeart.visibility = View.VISIBLE
                binding.rightHeart.playAnimation()
                delay(2000)
                binding.rightHeart.pauseAnimation()
                binding.rightHeart.visibility = View.GONE
            }
        } else if (photoIndex < photoOldPosition) {
            Log.d(
                TAG,
                "playLeftOrRightAnimation: photoIndex is $photoIndex and photoOldPosition is $photoOldPosition"
            )
            CoroutineScope(Dispatchers.Main).launch {
                binding.leftHeart.visibility = View.VISIBLE
                binding.leftHeart.playAnimation()
                delay(2000)
                binding.leftHeart.pauseAnimation()
                binding.leftHeart.visibility = View.GONE
            }
        }
    }

    private fun unlikeClicked(photoIndex: Int, photoOwner: String, photoLikeCount: Int) {
        if (myKakaoId == photoOwner) {
            myLikeCounts--
            CoroutineScope(Dispatchers.Main).launch {
                binding.textViewHeartCount.text = myLikeCounts.toString()
            }
        }
        photoModelList[photoIndex].likeCount = photoLikeCount
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            serverPhotoRoomPagerAdapter.notifyDataSetChanged()
        }
    }


    var focusReceive = Emitter.Listener {
        Log.d(TAG, "focusreceive: focus received!!")
        val photoPosition: Int = Integer.parseInt(it[0].toString())
        val hostName: String = it[1].toString()
        val hostNickname: String = it[2].toString()

        if (hostName == myKakaoId) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.imageViewClaimFocus.setColorFilter(
                    resources.getColor(R.color.picpho_blue),
                    PorterDuff.Mode.SRC_IN
                )
            }
        } else {
            receiveClaimFocus(
                this,
                photoPosition = photoPosition,
                hostName = hostName,
                hostNickname = hostNickname
            )
            CoroutineScope(Dispatchers.Main).launch {
                binding.imageViewClaimFocus.setColorFilter(
                    resources.getColor(R.color.colorBackGroundGray),
                    PorterDuff.Mode.SRC_IN
                )
                binding.imageViewClaimFocus.isEnabled = false
            }
        }
    }

    private fun receiveClaimFocus(
        context: Context,
        photoPosition: Int,
        hostName: String,
        hostNickname: String
    ) {
        Log.d(TAG, "receiveClaimFocus: receive claim focus")
        CoroutineScope(Dispatchers.Main).launch {
            dialog =
                showDialog(
                    context = context,
                    resource = R.layout.dialog_accept_focus,
                    gravity = Gravity.BOTTOM,
                    color = Color.WHITE
                )
            dialog!!.findViewById<TextView>(R.id.focusUserName).text = hostNickname
            moveToPosition = photoPosition
            dialog!!.findViewById<TextView>(R.id.wantSkip).setOnClickListener {
                Log.d(TAG, "onCreate: focusDialogBinding.wantSkip.setOnClickListener")
                dialog!!.dismiss()
            }

            dialog!!.findViewById<TextView>(R.id.moveToHost).setOnClickListener {
                Log.d(
                    TAG,
                    "onCreate focusDialogBinding.moveToHost.setOnClickListener : moveToPostition is $moveToPosition"
                )
                binding.ServerPhotoroomViewPager.setCurrentItem(moveToPosition, true)
                dialog!!.dismiss()
            }

        }
    }


    private fun clickClaimFocus() {
        Log.d(
            TAG,
            "clickClaimFocus: emit claim focus userkakao id is $myKakaoId kakao nick name $userKakaoNickName"
        )
        CoroutineScope(Dispatchers.IO).launch {
            mSocket!!.emit(
                "claimFocus",
                roomAddress,
                photoOldPosition,
                myKakaoId!!,
                userKakaoNickName
            )
        }
    }


    private fun clickDrawer(leftAnimation: Animation, rightAnimation: Animation) {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
            if (isPageOpen) {
                Log.d("TAG", "true")
                drawerCover!!.visibility = View.GONE
                drawer!!.startAnimation(rightAnimation)
                drawer!!.visibility = View.GONE
                isPageOpen = false
            } else {
                Log.d("TAG", "false")
                drawerCover!!.visibility = View.VISIBLE

                drawerCover!!.setOnTouchListener { v, event ->
                    true
                }
                drawer!!.visibility = View.VISIBLE
                drawer!!.startAnimation(leftAnimation)
                isPageOpen = true
            }
        }
    }


    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ")

    }


    override fun onResume() {
        if (mSocket == null)
            mSocket = createAndConnectSocket()
        super.onResume()
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            serverThumbnailRecyclerAdapter.notifyDataSetChanged()
            serverPhotoRoomPagerAdapter.notifyDataSetChanged()
            binding.ServerPhotoroomViewPager.setCurrentItem(currentPagerPosition, true)
            binding.ServerPhotoRoomRecyclerView.smoothScrollToPosition(currentPagerPosition)
        }



        binding.ServerPhotoroomViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                currentPagerPosition = position

                var photoNewPosition: Int = 0
                val photoLastIndex: Int = serverThumbnailRecyclerAdapter.itemCount - 1
                if (photoOldPosition > position) {
                    photoNewPosition = position - 1
                } else if (photoOldPosition < position) {
                    photoNewPosition = position + 1
                }

                if (position == photoLastIndex) {
                    photoNewPosition = photoLastIndex
                }
                else if (position == 0) {
                    photoNewPosition = 0
                }
                CoroutineScope(Dispatchers.Main).launch {
                    binding.ServerPhotoRoomRecyclerView.smoothScrollToPosition(photoNewPosition)
                    photoOldPosition = position
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
        Log.d(TAG, "onStop: onStop on PhotoRoomActivity")

    }

    override fun onDestroy() {
        Log.d("onDestroy", "onDestroy")
        if (workerThread != null) {
            workerThread?.leaveChannel(workerThread!!.engineConfig.mChannel)
            workerThread = null
        }
        if (engineEventHandler != null) {
            engineEventHandler!!.removeEventHandler(this)
        }
        if (dialog != null)
            dialog!!.dismiss()

        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBackPressed() {
        if (binding.drawerPhotoRoom.visibility == View.VISIBLE) {
            binding.drawerCover.visibility == View.GONE
            binding.drawerPhotoRoom.visibility == View.GONE
        } else
            showDoYouWantLeaveThisRoom()
    }

    fun onSwitchSpeakerClicked() {
        workerThread!!.rtcEngine!!.setEnableSpeakerphone(mAudioRouting != 3)
        mAudioRouting = if (mAudioRouting == 3)
            -1
        else
            3
    }


    //todo : DB
    @RequiresApi(Build.VERSION_CODES.O)
    fun quitPhotoRoom() {
        CoroutineScope(Dispatchers.Default).launch {
            val deletePhotoSize: Int = ServerWaitingRoomActivity.serverFilePathList.size
            for (i in 0 until deletePhotoSize) {
                ServerWaitingRoomActivity.serverStorage!!.deleteFile(ServerWaitingRoomActivity.serverFilePathList[i])
                MediaScannerConnection
                    .scanFile(
                        applicationContext,
                        arrayOf(ServerWaitingRoomActivity.serverFilePathList[i]),
                        arrayOf("jpg"),
                        null
                    )
            }
        }

        if (photoPickedList.size > 0) {
            CoroutineScope(Dispatchers.Default).launch {
                var dbHelper = DBHelper(App.instance, "PICPHO.db", null, 2)
                var database = dbHelper.writableDatabase

                var query = "SELECT * FROM Photos WHERE _id ORDER BY rowid DESC LIMIT 1"
                var cursor = database.rawQuery(query, null)
                var photoTableLastIndex: String? = "0"
                if (cursor.moveToFirst()) {
                    photoTableLastIndex = cursor.getString(0)
                    Log.d("SQLITE", "${photoTableLastIndex}")
                }

                var indexList = ArrayList<Int>()

                var contentValuesImagePath = ContentValues()
                for (i in 0 until photoPickedList.size) {
                    contentValuesImagePath.put("photoAbsolutePath", photoPickedList[i].absolutePath)
                    database.insert("Photos", null, contentValuesImagePath)
                    indexList.add(photoTableLastIndex!!.toInt() + i + 1)
                }
                var presentImage: String
                if (photoPickedList.size == 0) {
                    presentImage = "null"
                } else {
                    presentImage = indexList[0].toString()
                }

                var memberList = ArrayList<String>()
                for (i in 0 until drawerMemberList.size) {
                    memberList.add(drawerMemberList[i].UserID)
                }
                var contentValuesGroup = ContentValues()
                contentValuesGroup.apply {
                    put("groupName", roomName) //
                    put("presentImage", presentImage) //
                    put(
                        "eventDate",
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    ) //
                    put("AbsolutePathList", indexList.toString()) //
                    put("memberList", memberList.toString()) //
                    put("isDeleted", false) //
                }
                database.insert("Groups", null, contentValuesGroup)

                var contentValuesFriend = ContentValues()
                for (i in 0 until drawerMemberList.size) {
                    var queryFriend =
                        "SELECT * FROM Friends WHERE userUid = ${drawerMemberList[i].UserID}"
                    var cursor = database.rawQuery(queryFriend, null)
                    if (cursor.moveToFirst()) {
                        // update exist friend
                        var userName = drawerMemberList[i].NickName
                        var userProfileImage = drawerMemberList[i].ProfileUrl
                        var userUid = drawerMemberList[i].UserID
                        var queryUpdateFriend =
                            "UPDATE Friends SET userName = '${userName}', userProfileImage = '${userProfileImage}' WHERE userUid = '${userUid}'"
                        database.rawQuery(queryUpdateFriend, null)
                        // insert new Friend
                    } else {
                        contentValuesFriend.apply {
                            put("userName", drawerMemberList[i].NickName)
                            put("userProfileImage", drawerMemberList[i].ProfileUrl)
                            put("userUid", drawerMemberList[i].UserID)
                        }
                        database.insert("Friends", null, contentValuesFriend)
                    }
                }
            }
        }

        mSocket!!.off("done")
        mSocket!!.off("privateRoomSuccess")
        mSocket!!.off("receiveFocus")
        mSocket!!.off("CancelFocus")
        mSocket!!.off("clickLike")
        mSocket!!.off("GetMemberListAndRoomName")



        mSocket!!.emit("leaveRoom", Ack {
            Log.e(TAG, "onBackPressed: leaveRoom ${it[0]}")
            if (it[0].equals("received")) {
                serverWaitingRoomActivity?.finish()
                makeGroupActivity?.finish()
                finish()
            }
        })
    }

    private fun onVoiceMuteClicked() {
        Log.d(TAG, "onVoiceMuteClicked: audio_status: $mAudioMuted")
        workerThread!!.rtcEngine!!.muteLocalAudioStream(!mAudioMuted.also { mAudioMuted = it })
        CoroutineScope(Dispatchers.Main).launch {
            if (mAudioMuted) {
                binding.imageViewVoice.setImageResource(R.drawable.ic_baseline_keyboard_voice_24)
                binding.linearLayoutVoice.setBackgroundResource(R.drawable.border_round_blue)
            } else {
                binding.imageViewVoice.setImageResource(R.drawable.ic_baseline_mic_off_24)
                binding.linearLayoutVoice.setBackgroundResource(0)
            }
            mAudioMuted = !mAudioMuted
        }
    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        Log.d(TAG, "onJoinChannelSuccess: ${channel}")
        runOnUiThread(Runnable {
            if (isFinishing) {
                return@Runnable
            }
            Log.d(TAG, "onJoinChannelSuccess: ${mAudioMuted}")
            workerThread!!.rtcEngine!!.muteLocalAudioStream(mAudioMuted)
        })
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        Log.d(TAG, "onUserOffline: " + (uid and 0xFFFFFFFFL.toInt()) + " " + reason)
    }

    override fun onExtraCallback(type: Int, vararg data: Any?) {
        runOnUiThread(Runnable {
            if (isFinishing) {
                return@Runnable
            }
            doHandleExtraCallback(type, *data as Array<out Any>)
        })
    }

    private fun doHandleExtraCallback(type: Int, vararg data: Any) {
        when (type) {
            AGEventHandler.EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED -> {
                notifyHeadsetPlugged(data[0] as Int)
            }
        }
    }

    fun notifyHeadsetPlugged(routing: Int) {
        mAudioRouting = routing
        val imageView = binding.imageViewSound
        CoroutineScope(Dispatchers.Main).launch {
            if (mAudioRouting == 3) { // Speakerphone
                imageView.setColorFilter(
                    resources.getColor(R.color.picpho_blue),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                imageView.clearColorFilter()
            }
        }
    }


    var privateRoomSuccess = Emitter.Listener {
    }


    var imageReceiveDone = Emitter.Listener {
        numOfExpectedPhoto = it[0].toString().toInt() - numOfMyPhoto
    }
}