package com.picpho.pic_pho.PhotoRoomServer

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import kotlinx.android.synthetic.main.photo_recycler_item.view.thumbnailPhoto
import kotlinx.android.synthetic.main.serverphoto_recycler_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ServerThumbnailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val TAG: String = "LOG"

    private val thumbnailPhotoView = itemView.thumbnailPhoto
    private val checkImageView = itemView.checkImageView


    fun bind(serverThumbnailPhotoModel: ServerThumbnailPhotoModel) {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
            Glide
                .with(App.instance)
                .load(serverThumbnailPhotoModel.thumbnailPhoto)
                .into(thumbnailPhotoView)

            if (serverThumbnailPhotoModel.isPicked) {
                checkImageView.visibility = View.VISIBLE
            }
            else {
                checkImageView.visibility = View.GONE
            }
        }
    }
}

