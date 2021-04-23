package com.picpho.pic_pho.PhotoRoom

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import kotlinx.android.synthetic.main.photo_recycler_item.view.*

class ThumbnailViewHolder(
    itemView: View,
    recyclerViewInterface: ThumbnailRecyclerViewInterface
) :
    RecyclerView.ViewHolder(itemView) {

    val TAG: String = "LOG"
    private val thumbnailPhotoView = itemView.thumbnailPhoto
    private var thumbnailRecyclerViewInterface: ThumbnailRecyclerViewInterface? = null
    private val checkImageView = itemView.checkImageView

    init {
        Log.d(TAG, "ThumbnailViewHolder - () called")

        this.thumbnailRecyclerViewInterface = recyclerViewInterface
    }

    fun bind(thumbnailPhotoModel: ThumbnailPhotoModel) {


        Glide
            .with(App.instance)
            .load(thumbnailPhotoModel.thumbnailPhoto)
            .into(thumbnailPhotoView)

        if (thumbnailPhotoModel.isPicked)
            checkImageView.visibility = View.VISIBLE
        else
            checkImageView.visibility = View.GONE


    }
}