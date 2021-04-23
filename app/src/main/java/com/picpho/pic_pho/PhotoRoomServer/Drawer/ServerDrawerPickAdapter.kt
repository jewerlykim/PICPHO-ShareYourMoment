package com.picpho.pic_pho.PhotoRoomServer.Drawer

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.R
import kotlinx.android.synthetic.main.item_recyclerview_pick_drawer.view.*

class ServerDrawerPickAdapter(var photoPickedList : ArrayList<Uri>) : RecyclerView.Adapter<ServerDrawerPickAdapter.ServerDrawerPickViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerDrawerPickViewHolder {
        return ServerDrawerPickViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_recyclerview_pick_drawer, parent, false))
    }

    override fun onBindViewHolder(holder: ServerDrawerPickViewHolder, position: Int) {
        holder.bind(photoPickedList[position])
    }

    override fun getItemCount(): Int {
        return photoPickedList.size
    }

    inner class ServerDrawerPickViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val itemImage = itemView.imageView_drawer_pick_item

        fun bind(serverPickModel: Uri){
            itemImage.setImageURI(serverPickModel)
        }
    }
}