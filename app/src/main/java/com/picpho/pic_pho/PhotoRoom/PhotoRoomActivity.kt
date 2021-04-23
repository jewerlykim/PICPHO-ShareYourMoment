package com.picpho.pic_pho.PhotoRoom

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.picpho.pic_pho.PhotoRoom.ViewPager.PhotoRoomPagerAdapter
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity
import com.picpho.pic_pho.R
import com.picpho.pic_pho.WifiDirect.FileReceiveActionThread
import com.picpho.pic_pho.WifiDirect.UI.HorizontalItemDecoration
import com.picpho.pic_pho.WifiDirect.UI.VerticalItemDecoration
import com.picpho.pic_pho.WifiDirect.UI.WifiDrawer.DrawerPhotoModel
import com.picpho.pic_pho.WifiDirect.UI.WifiDrawer.DrawerRecyclerAdapter
import com.picpho.pic_pho.WifiDirect.WaitingForOwnerActivity.Companion.waitingForOwnerActivity
import com.picpho.pic_pho.WifiDirect.WifiDirectBroadcastReceiver
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.filePathList
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.isGroupOwner
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.photoInfoList
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.storage
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.wifiDirectMainActivity
import com.picpho.pic_pho.databinding.ActivityPhotoroomBinding
import com.picpho.pic_pho.databinding.ItemPhotoroomPagerBinding
import kotlinx.coroutines.*
import java.io.File

class PhotoRoomActivity : AppCompatActivity(), ThumbnailRecyclerViewInterface {

    val TAG: String = "LOG"
    var photoModelList = ArrayList<ThumbnailPhotoModel>()
    var drawerPhotoList = PhotoRoomActivity.drawerPhotoList
    private var drawer: LinearLayout? = null
    private var drawerCover: LinearLayout? = null
    private var isPageOpen = false
    private var dialog: Dialog? = null

    companion object {
        lateinit var thumbnailRecyclerAdapter: ThumbnailRecyclerAdapter
        lateinit var photoRoomPagerAdapter: PhotoRoomPagerAdapter
        lateinit var drawerRecyclerAdapter: DrawerRecyclerAdapter
        lateinit var binding: ActivityPhotoroomBinding
        lateinit var photobinding: ItemPhotoroomPagerBinding
        var drawerPhotoList = ArrayList<DrawerPhotoModel>()
        var drawerPhotoUriList = ArrayList<Uri>()
        fun changeSelectedPhotoByClicked(position: Int) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.photoroomViewPager.currentItem = position
            }
        }

        var scanFileForOwnerList = ArrayList<File>()
        var photoRoomActivity: Activity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityPhotoroomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        photobinding = ItemPhotoroomPagerBinding.inflate(layoutInflater)

        if (!photoInfoList.isEmpty()) {
            var uri: Uri
            for (photo in photoInfoList) {
                Log.d(TAG, "onResume: in photoroom URI bind to Item : ${photo.photoUri}")

                if (photo.photoOwnerIP.equals("picpho")) {
                    uri = photo.photoUri!!
                } else {
                    uri = Uri.fromFile(File(photo.photoUri.toString()))
                    photo.photoUri = uri // photo list update with uri
                }

                var photoModel =
                    ThumbnailPhotoModel(thumbnailPhoto = uri, path = photo.absolutePath, photoOwnerIp = photo.photoOwnerIP, photoOwnerMac = photo.photoOwnerMac)
                this.photoModelList.add(photoModel)
            }

            var sortedModelList = photoModelList.sortedWith(compareBy({it.photoOwnerMac}))
            photoModelList.apply {
                runBlocking {
                    clear()
                }
                runBlocking {
                    addAll(sortedModelList)
                }
            }
        }


        thumbnailRecyclerAdapter = ThumbnailRecyclerAdapter(this)
        thumbnailRecyclerAdapter.submitList(this.photoModelList)
        photoRoomPagerAdapter = PhotoRoomPagerAdapter(this.photoModelList)

        binding.photoroomViewPager.apply {
            adapter = photoRoomPagerAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }
        binding.PhotoRoomRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@PhotoRoomActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = thumbnailRecyclerAdapter
            addItemDecoration(HorizontalItemDecoration(10))
        }
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            thumbnailRecyclerAdapter.notifyDataSetChanged()
        }
        drawerRecyclerAdapter = DrawerRecyclerAdapter(drawerPhotoUriList)


        binding.photoroomDrawalPhotosRecyclerview.apply {
            layoutManager = GridLayoutManager(
                this@PhotoRoomActivity,
                3,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = drawerRecyclerAdapter
            CoroutineScope(Dispatchers.Main).launch {
                addItemDecoration(HorizontalItemDecoration(10))
                addItemDecoration(VerticalItemDecoration(20))
            }
        }


        var photoOldPosition: Int = 0



        binding.photoroomViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                var photoNewPosition: Int = 0
                val photoLastIndex: Int = thumbnailRecyclerAdapter.itemCount - 1
                if (photoOldPosition > position) {
                    photoNewPosition = position - 1
                } else if (photoOldPosition < position) {
                    photoNewPosition = position + 1
                }
                CoroutineScope(Dispatchers.Main).launch {
                    if (position == photoLastIndex)
                        photoNewPosition = photoLastIndex

                    else if (position == 0)
                        photoNewPosition = 0

                    binding.PhotoRoomRecyclerView.smoothScrollToPosition(photoNewPosition)
                    photoOldPosition = position
                }
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    photoRoomPagerAdapter.notifyDataSetChanged()
                }
            }
        })

        photoRoomActivity = this


        drawer = binding.drawerPhotoRoom
        drawerCover = binding.drawerCover
        val leftAnimation = AnimationUtils.loadAnimation(this, R.anim.translate_left)
        val rightAnimation = AnimationUtils.loadAnimation(this, R.anim.translate_right)

        binding.imageViewPhotoRoomMoreAction.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                clickDrawer(leftAnimation, rightAnimation)
            }
        }

        binding.drawerEmpty.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                clickDrawer(leftAnimation, rightAnimation)
            }
        }

        binding.imageViewPhotoRoomExit.setOnClickListener {
            showDoYouWantLeaveThisRoom()
        }

        binding.textViewExitphotoRoomExit.setOnClickListener {
            showDoYouWantLeaveThisRoom()
        }

        binding.textviewLeavePhotoRoom.setOnClickListener {
            showDoYouWantLeaveThisRoom()
        }






        if (isGroupOwner) {
            val fileCount: Int = scanFileForOwnerList.size
            Log.d(
                TAG,
                "onCreate: WifiDirectMainActivity.filePathList.size ${scanFileForOwnerList.size}"
            )
            for (i in 0 until fileCount)
                FileReceiveActionThread.scanFile(this, scanFileForOwnerList[i], "jpg")
        }
    }

    override fun onStart() {
        super.onStart()
    }

    fun clickDrawer(leftAnimation: Animation, rightAnimation: Animation) {
        CoroutineScope(Dispatchers.Main).launch {
            if (isPageOpen) {
                Log.d("TAG", "true")
                drawerCover!!.visibility = View.GONE
                drawer!!.startAnimation(rightAnimation)
                drawer!!.visibility = View.GONE
                isPageOpen = false
            } else {
                Log.d("TAG", "false")
                drawerCover!!.visibility = View.VISIBLE
                drawer!!.visibility = View.VISIBLE
                drawer!!.startAnimation(leftAnimation)
                isPageOpen = true
            }
        }
    }

    override fun onResume() {
        super.onResume()

    }

    fun beforeFinish() {
        CoroutineScope(Dispatchers.IO).launch {
            WifiDirectMainActivity.removeGroup()
            if (WifiDirectBroadcastReceiver.ServerThread != null && !WifiDirectBroadcastReceiver.ServerThread!!.isCancelled) {
                WifiDirectBroadcastReceiver.ServerThread!!.closeSocket()
                WifiDirectBroadcastReceiver.ServerThread = null
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerPhotoRoom.visibility == View.VISIBLE) {
            binding.drawerCover.visibility = View.GONE
            binding.drawerPhotoRoom.visibility = View.GONE
        } else
            showDoYouWantLeaveThisRoom()
    }

    override fun onStop() {
        beforeFinish()
        super.onStop()
        Log.d(TAG, "onStop: onStop on PhotoRoomActivity")

    }

    override fun onDestroy() {
        beforeFinish()
        super.onDestroy()
        Log.d("onDestroy", "onDestroy")

    }

    fun showDoYouWantLeaveThisRoom() {
        runBlocking {
            CoroutineScope(Dispatchers.Main).launch {
                dialog =
                    ServerPhotoRoomActivity.showDialog(
                        context = photoRoomActivity!!,
                        resource = R.layout.dialog_leave_photoroom,
                        gravity = Gravity.CENTER,
                        color = Color.WHITE
                    )

                dialog!!.findViewById<TextView>(R.id.button_leave).setOnClickListener {
                    finishAction()
                    dialog!!.dismiss()
                }
                dialog!!.findViewById<TextView>(R.id.button_continue).setOnClickListener {
                    dialog!!.dismiss()
                }
            }
        }
    }

    //function for finish action
    fun finishAction() {
        beforeFinish()
        wifiDirectMainActivity?.finish()
        waitingForOwnerActivity?.finish()
        runBlocking {
            CoroutineScope(Dispatchers.Default).launch {
                val deletePhotoSize: Int = filePathList.size
                for (i in 0 until deletePhotoSize) {
                    storage!!.deleteFile(filePathList[i])
                    scanFileByPath(filePathList[i], "jpg")
                }
            }.join()
        }
        finish()
    }

    fun scanFileByPath(absolutePath: String, mimeType: String) {
        runBlocking {
            CoroutineScope(Dispatchers.Default).launch {
                MediaScannerConnection
                    .scanFile(
                        applicationContext,
                        arrayOf(absolutePath),
                        arrayOf(mimeType),
                        null
                    )
            }.join()
        }
    }
}