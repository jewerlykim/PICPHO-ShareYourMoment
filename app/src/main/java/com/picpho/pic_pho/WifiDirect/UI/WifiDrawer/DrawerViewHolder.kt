package com.picpho.pic_pho.WifiDirect.UI.WifiDrawer

import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import kotlinx.android.synthetic.main.item_recyclerview_wifiphotoroom_pick_drawer.view.*

class DrawerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val itemImage = itemView.wifi_imageView_drawer_pick_item

    fun bindWithView(uri: Uri){
        Glide
            .with(App.instance)
            .load(uri)
            .into(itemImage)
    }
}