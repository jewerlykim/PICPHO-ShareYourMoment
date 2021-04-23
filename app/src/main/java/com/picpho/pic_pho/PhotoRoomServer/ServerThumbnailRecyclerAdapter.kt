package com.picpho.pic_pho.PhotoRoomServer

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity.Companion.changeSelectedPhotoByClicked
import com.picpho.pic_pho.R

class ServerThumbnailRecyclerAdapter(private val photoModelList :  ArrayList<ServerThumbnailPhotoModel>): RecyclerView.Adapter<ServerThumbnailViewHolder>() {


    private val TAG = "ThumbnailRecyclerAdapter"




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerThumbnailViewHolder {
        return ServerThumbnailViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.serverphoto_recycler_item, parent, false))
    }



    override fun onBindViewHolder(holderServer: ServerThumbnailViewHolder, position: Int) {
        holderServer.bind(photoModelList[position])

        holderServer.itemView.setOnClickListener {
            changeSelectedPhotoByClicked(position)
        }


        Log.d(TAG, "onBindViewHolder: ${this.photoModelList[position]}")
    }

    override fun getItemCount(): Int {
        return photoModelList.size
    }

}