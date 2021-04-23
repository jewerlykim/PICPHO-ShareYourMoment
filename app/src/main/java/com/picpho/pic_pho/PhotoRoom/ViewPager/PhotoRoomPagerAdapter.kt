package com.picpho.pic_pho.PhotoRoom.ViewPager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.PhotoRoom.ThumbnailPhotoModel
import com.picpho.pic_pho.R

class PhotoRoomPagerAdapter(private var pageList : ArrayList<ThumbnailPhotoModel>): RecyclerView.Adapter<PhotoRoomPagerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoRoomPagerViewHolder {
        return PhotoRoomPagerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_photoroom_pager, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoRoomPagerViewHolder, position: Int) {
        holder.bindWithView(pageList[position], position)
    }

    override fun getItemCount(): Int {
        return pageList.size
    }
}