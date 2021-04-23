package com.picpho.pic_pho.WifiDirect.UI.WifiWaitingRoom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.R
import com.picpho.pic_pho.WifiDirect.UI.PeerModel

class WifiWaitingRecyclerAdapter : RecyclerView.Adapter<WifiWaitingViewHolder>(){

    companion object {
        var modelList = ArrayList<PeerModel>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiWaitingViewHolder {
        return WifiWaitingViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_wifi_waiting_room, parent, false
            )
        )

    }

    override fun onBindViewHolder(holder: WifiWaitingViewHolder, position: Int) {
        holder.bind(modelList[position], position)
    }

    override fun getItemCount(): Int {
        return modelList.size
    }

    fun submitList(modelList_arg: ArrayList<PeerModel>){
        modelList = modelList_arg
    }
}