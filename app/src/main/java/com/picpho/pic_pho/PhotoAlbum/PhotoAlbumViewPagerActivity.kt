package com.picpho.pic_pho.PhotoAlbum

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.picpho.pic_pho.App
import com.picpho.pic_pho.ContentResolverUtil
import com.picpho.pic_pho.Lobby.DBHelper
import com.picpho.pic_pho.databinding.ActivityPhotoAlbumViewPagerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

class PhotoAlbumViewPagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoAlbumViewPagerBinding
    private lateinit var photoAlbumPagerRecyclerAdapter: PhotoAlbumPagerRecyclerAdapter
    private lateinit var groupName: String
    private lateinit var absolutePathIndexArray: JSONArray
    var photoUriList: ArrayList<Uri>? = null
    var contentResolverUtil: ContentResolverUtil? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoAlbumViewPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        photoUriList = ArrayList()
        contentResolverUtil = ContentResolverUtil(this)

        groupName = intent.getStringExtra("groupName")
        absolutePathIndexArray = JSONArray(intent.getStringExtra("absolutePathList"))
        binding.textviewPhotoAlbumTitle.text = groupName
        photoAlbumPagerRecyclerAdapter =
            PhotoAlbumPagerRecyclerAdapter(photoUriList!!)

        binding.photoAlbumViewPager.apply {
            adapter = photoAlbumPagerRecyclerAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            binding.photoAlbumDotsIndicator.setViewPager2(this)
        }

        binding.textviewLeavePhotoAlbum.setOnClickListener {
            finishPhotoAlbum()
        }



        CoroutineScope(Dispatchers.Default).launch {
            var dbHelper = DBHelper(App.instance, "PICPHO.db", null, 2)
            var database = dbHelper.writableDatabase

            for (i in 0 until absolutePathIndexArray.length()) {
                var queryGroups = "SELECT * FROM Photos WHERE _id = ${absolutePathIndexArray[i]} "
                var cursor = database.rawQuery(queryGroups, null)
                if (cursor.moveToFirst()) {
                    var file = File(cursor.getString(1))
                    var uri = Uri.fromFile(file)
                    if (contentResolverUtil!!.isExist(uri)) {
                        photoUriList!!.add(uri)
                        Log.e("TAG", "onCreate: ViewPagerActivity!!!!!!!!!!!!!!!!: file exist")
                    } else {
                        Log.e("TAG", "onCreate: ViewPagerActivity!!!!!!!!!!!!!!!!: file not exist")
                    }
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                delay(100)
                photoAlbumPagerRecyclerAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onBackPressed() {
        finishPhotoAlbum()
    }

    fun finishPhotoAlbum() {
        finish()
    }
}