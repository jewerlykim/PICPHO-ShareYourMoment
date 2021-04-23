package com.picpho.pic_pho.WifiDirect.UI.WifiDrawer

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.R
import java.util.*

class DrawerRecyclerAdapter(private var drawerPhotoList :  ArrayList<Uri>) :
    RecyclerView.Adapter<DrawerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawerViewHolder {
        return DrawerViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recyclerview_wifiphotoroom_pick_drawer,
                parent, false))
    }

    override fun onBindViewHolder(holder: DrawerViewHolder, position: Int) {
        holder.bindWithView(drawerPhotoList[position])
    }

    override fun getItemCount(): Int {
        return drawerPhotoList.size
    }


}