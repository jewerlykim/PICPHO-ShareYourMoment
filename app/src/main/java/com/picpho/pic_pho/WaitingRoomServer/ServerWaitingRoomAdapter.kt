package com.picpho.pic_pho.WaitingRoomServer

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.R

class ServerWaitingRoomAdapter(private var modelWaitingList: ArrayList<ServerWaitingRoomActivity.GroupMember>) :
    RecyclerView.Adapter<ServerWaitingRoomHolder>() {
    private val TAG = "ServerWaitingRoomAdapter"


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerWaitingRoomHolder {
        return ServerWaitingRoomHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_waiting_room_server, parent, false)
        )
    }


    override fun getItemCount(): Int {
        return modelWaitingList.size
    }


    override fun onBindViewHolder(holder: ServerWaitingRoomHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: onBindViewHolder")
        if (modelWaitingList.size != 0)
            try {
                holder.bind(modelWaitingList[position])

            } catch (e: IndexOutOfBoundsException) {
            }
    }
}

