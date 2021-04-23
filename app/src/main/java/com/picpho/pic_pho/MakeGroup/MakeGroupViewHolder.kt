package com.picpho.pic_pho.MakeGroup

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import com.picpho.pic_pho.R
import kotlinx.android.synthetic.main.item_make_group_recycler.view.*

class MakeGroupViewHolder(
    itemView: View,
    recyclerViewInterface: MakeGroupRecyclerViewInterface
) :
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener {

    private val TAG = "MakeGroupViewHolder"
    private val makeGroupView = itemView.make_group_profile
    private var makeGroupRecyclerViewInterface: MakeGroupRecyclerViewInterface? = null

    init {
        Log.d(TAG, "MakeGroupViewHolder - () called")

        //interface
        itemView.setOnClickListener(this)
        this.makeGroupRecyclerViewInterface = recyclerViewInterface
    }

    fun bind(makeGroupModel: MakeGroupModel) {
        Log.d(TAG, "MakeGroupViewHold er - bind() called")

        itemView.make_group_name.text = makeGroupModel.name

        if (makeGroupModel.isOnline == 1) {
            itemView.unselected_radio_button.setImageResource(R.drawable.ic_baseline_radio_button_online_24)
            itemView.textView_make_group_status_in_room.visibility = View.VISIBLE
        }


        Glide
            .with(App.instance)
            .load(makeGroupModel.profileImage)
            .error(R.drawable.ic_baseline_account_circle_blue_24)
            .into(itemView.make_group_profile)

    }


    override fun onClick(p0: View?) {
        Log.d(TAG, "MakeGroupViewHolder - onClick() called")

        //interface
        itemView.setOnClickListener(this)
        this.makeGroupRecyclerViewInterface?.onItemClicked()

    }


}