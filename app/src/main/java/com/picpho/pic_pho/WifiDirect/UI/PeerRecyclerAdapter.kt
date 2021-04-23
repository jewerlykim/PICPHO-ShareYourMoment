package com.picpho.pic_pho.WifiDirect.UI

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.R
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity

class PeerRecyclerAdapter : RecyclerView.Adapter<PeerViewHolder>() {

    private var activity = WifiDirectMainActivity()

    companion object {
        var modelList = ArrayList<PeerModel>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        return PeerViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_recyclerview_main, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        holder.bind(modelList[position])

        var device = modelList[position]
        var deviceName = device.deviceName
        var deviceAddr = device.deviceAddr
        var deviceIsOwner = device.isOwner

        holder.itemView.setOnClickListener {
            activity.connectToPeer(deviceAddr!!)
        }
    }

    override fun getItemCount(): Int {
        return modelList.size
    }

    fun submitList(modelList_arg: ArrayList<PeerModel>){
        modelList = modelList_arg
    }
}