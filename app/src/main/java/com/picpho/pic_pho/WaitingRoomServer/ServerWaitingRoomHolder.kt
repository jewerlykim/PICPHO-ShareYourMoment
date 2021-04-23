package com.picpho.pic_pho.WaitingRoomServer

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import com.picpho.pic_pho.R
import kotlinx.android.synthetic.main.item_make_group_recycler.view.*
import kotlinx.android.synthetic.main.item_waiting_room_server.view.*

class ServerWaitingRoomHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val TAG = "ServerWaitingRoomHolder"

    fun bind(serverWaitingRoomModel: ServerWaitingRoomActivity.GroupMember) {

        when (serverWaitingRoomModel.status) {

            -1 -> {
                itemView.member_name_server.text = "친구를 기다리고 있어요."
                itemView.profilephoto_in_waiting_room_server.setImageResource(R.drawable.ic_baseline_account_circle_24)
                return
            }

            0 -> {
                itemView.wifi_progress_cloud_imageview.visibility = View.VISIBLE
                itemView.wifi_progress_cloud_ing_imageview.visibility = View.INVISIBLE
                itemView.wifi_progress_cloud_done_imageview.visibility = View.INVISIBLE
            }
            1 -> {
                itemView.wifi_progress_cloud_imageview.visibility = View.INVISIBLE
                itemView.wifi_progress_cloud_ing_imageview.visibility = View.VISIBLE
                itemView.wifi_progress_cloud_done_imageview.visibility = View.INVISIBLE
            }
            2 -> {
                itemView.wifi_progress_cloud_imageview.visibility = View.INVISIBLE
                itemView.wifi_progress_cloud_ing_imageview.visibility = View.INVISIBLE
                itemView.wifi_progress_cloud_done_imageview.visibility = View.VISIBLE
            }
        }

        itemView.member_name_server.text = serverWaitingRoomModel.NickName.toString()

        Log.d(TAG, "bind: ${itemView.member_name_server.text}")


        Glide
            .with(App.instance)
            .load(serverWaitingRoomModel.ProfileUrl)
            .error(R.drawable.ic_baseline_account_circle_blue_24)
            .into(itemView.profilephoto_in_waiting_room_server)


    }
}