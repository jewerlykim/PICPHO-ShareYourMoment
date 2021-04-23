package com.picpho.pic_pho.PhotoRoomServer.Drawer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import com.picpho.pic_pho.R
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity.GroupMember
import kotlinx.android.synthetic.main.item_recyclerview_member_drawer.view.*

class ServerDrawerMembersAdapter(var memberList: ArrayList<GroupMember>) :
    RecyclerView.Adapter<ServerDrawerMembersAdapter.ServerDrawerMemberViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ServerDrawerMemberViewHolder {
        return ServerDrawerMemberViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recyclerview_member_drawer, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ServerDrawerMemberViewHolder, position: Int) {
        holder.bind(memberList[position])
    }

    override fun getItemCount(): Int {
        return memberList.size
    }

    inner class ServerDrawerMemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemText = itemView.textView_DrawerMemberName
        private val itemImage = itemView.imageView_DrawerMemberImage

        fun bind(memberModel: GroupMember) {
            Glide
                .with(App.instance)
                .load(memberModel.ProfileUrl)
                .into(itemImage)
            itemText.text = memberModel.NickName
        }
    }
}