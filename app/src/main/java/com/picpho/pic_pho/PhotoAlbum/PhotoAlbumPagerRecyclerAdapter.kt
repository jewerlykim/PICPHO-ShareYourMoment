package com.picpho.pic_pho.PhotoAlbum

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.R
import kotlin.collections.ArrayList

class PhotoAlbumPagerRecyclerAdapter(private var photoAlbumList: ArrayList<Uri>) : RecyclerView.Adapter<PhotoAlbumViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoAlbumViewHolder {
        return PhotoAlbumViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_album, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoAlbumViewHolder, position: Int) {
        holder.bindWithView(photoAlbumList[position])
    }

    override fun getItemCount(): Int {
        return photoAlbumList.size
    }

}