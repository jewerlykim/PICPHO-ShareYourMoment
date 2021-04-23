package com.picpho.pic_pho.PhotoRoomServer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import com.picpho.pic_pho.databinding.ActivityEnlargePhotoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ServerPhotoEnlargeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnlargePhotoBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnlargePhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)


        var intent = intent

        binding.imageViewQuitEnlargePhoto.setOnClickListener {
            finish()
        }

        var imagepath = intent.getStringExtra("uri")
        var orientation = intent.getIntExtra("orientation", -200)
        orientation = when (orientation){
            200 -> 0
            null -> 0
            1-> 0
            3-> 180
            8-> 270
            6-> 90
            else -> 0
        }

        CoroutineScope(Dispatchers.Main).launch {
            binding.imageViewEnlargePhoto.orientation = orientation
            binding.imageViewEnlargePhoto.setImage(ImageSource.uri(imagepath.toUri()))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}