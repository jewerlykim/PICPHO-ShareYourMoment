package com.picpho.pic_pho.WifiDirect.UI.WifiWaitingRoom

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import com.picpho.pic_pho.R
import com.picpho.pic_pho.WifiDirect.UI.PeerModel
import kotlinx.android.synthetic.main.item_wifi_waiting_room.view.*

class WifiWaitingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val waitingDeviceName = itemView.device_name_in_wifi_waiting_room
    private val waitingDeviceImage = itemView.profilephoto_in_wifi_waiting_room

    fun bind(groupModel: PeerModel, position : Int){
        when(position % 3){
            0 ->
                Glide
                    .with(App.instance)
                    .load(R.drawable.ic_baseline_send_blue_24)
                    .into(waitingDeviceImage)
            1 ->
                Glide
                    .with(App.instance)
                    .load(R.drawable.ic_baseline_send_picphosalmon_24)
                    .into(waitingDeviceImage)
            2 ->
                Glide
                    .with(App.instance)
                    .load(R.drawable.ic_baseline_send_yellow_24)
                    .into(waitingDeviceImage)
        }
        waitingDeviceName.text = groupModel.deviceName
    }
}