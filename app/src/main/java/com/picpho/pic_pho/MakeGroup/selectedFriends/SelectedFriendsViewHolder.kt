package com.picpho.pic_pho.MakeGroup.selectedFriends

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import com.picpho.pic_pho.R
import kotlinx.android.synthetic.main.item_selected_friends.view.*

class SelectedFriendsViewHolder(
    itemView: View,
    recyclerViewInterface: SelectedFriendsRecyclerViewInterface
) :
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener {

    private val TAG = "SelectedFriendsViewHolder"
    private var selectedFriendsRecyclerViewInterface: SelectedFriendsRecyclerViewInterface? = null

    init {
        Log.d(TAG, "init() called")
        //interface
        itemView.setOnClickListener(this)
        this.selectedFriendsRecyclerViewInterface = recyclerViewInterface
    }

    fun bind(selectedFriendsModel: SelectedFriendsModel) {
        Log.d(TAG, "SelectedFriendsViewHolder - bind() called")
        Glide
            .with(App.instance)
            .load(selectedFriendsModel.profileImage)
            .error(R.drawable.ic_baseline_account_circle_blue_24)
            .into(itemView.imageview_selected_friend)
    }


    override fun onClick(p0: View?) {
        Log.d(TAG, "SelectedFriendsViewHolder - onClick() called")
        //interface
        itemView.setOnClickListener(this)
        this.selectedFriendsRecyclerViewInterface?.onItemClicked()
    }
}