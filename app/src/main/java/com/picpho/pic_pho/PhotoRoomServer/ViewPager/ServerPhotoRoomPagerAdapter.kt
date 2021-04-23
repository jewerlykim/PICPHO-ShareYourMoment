package com.picpho.pic_pho.PhotoRoomServer.ViewPager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.PhotoRoomServer.ServerThumbnailPhotoModel
import com.picpho.pic_pho.R

class ServerPhotoRoomPagerAdapter(private var pageList : ArrayList<ServerThumbnailPhotoModel>):
    RecyclerView.Adapter<ServerPhotoRoomPagerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerPhotoRoomPagerViewHolder {
        return ServerPhotoRoomPagerViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_serverphotoroom_pager, parent, false))
    }

    override fun onBindViewHolder(holderServer: ServerPhotoRoomPagerViewHolder, position: Int) {
        holderServer.bindWithView(pageList[position], position)
    }

    override fun getItemCount(): Int {
        return pageList.size
    }
}