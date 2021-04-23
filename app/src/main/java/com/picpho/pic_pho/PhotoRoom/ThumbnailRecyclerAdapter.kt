package com.picpho.pic_pho.PhotoRoom

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.R

class ThumbnailRecyclerAdapter(ThumbnailRecyclerViewInterface: ThumbnailRecyclerViewInterface): RecyclerView.Adapter<ThumbnailViewHolder>() {
    private var photoModelList = ArrayList<ThumbnailPhotoModel>()
    private val TAG = "ThumbnailRecyclerAdapter"
    //interface
    private var ThumbnailRecyclerViewInterface: ThumbnailRecyclerViewInterface? = null

    init{
        this.ThumbnailRecyclerViewInterface = ThumbnailRecyclerViewInterface
    }
    

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {

        return ThumbnailViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.photo_recycler_item, parent, false),
            this.ThumbnailRecyclerViewInterface!!)


    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bind(this.photoModelList[position])

        holder.itemView.setOnClickListener {
            PhotoRoomActivity.changeSelectedPhotoByClicked(position)
        }

        Log.d(TAG, "onBindViewHolder: ${this.photoModelList[position]}")
    }

    override fun getItemCount(): Int {
        return this.photoModelList.size
    }

    fun submitList(photoModelList: ArrayList<ThumbnailPhotoModel>){
        this.photoModelList = photoModelList
    }
}