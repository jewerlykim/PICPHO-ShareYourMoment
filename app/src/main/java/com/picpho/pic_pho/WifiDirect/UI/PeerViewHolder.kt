package com.picpho.pic_pho.WifiDirect.UI

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_recyclerview_main.view.*

class PeerViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){

    private val deviceName = itemView.tv_deviceName

    fun bind(groupmodel : PeerModel){
        deviceName.text = groupmodel.deviceName
    }
}