package com.picpho.pic_pho.Lobby

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picpho.pic_pho.App
import com.picpho.pic_pho.PhotoAlbum.PhotoAlbumViewPagerActivity
import com.picpho.pic_pho.R

class GroupRecyclerAdapter : RecyclerView.Adapter<GroupViewHolder>() {

    private val TAG = "GroupRecyclerAdapter"
    private var modelList = ArrayList<GroupModel>()

    //뷰홀더가 생성되었을 때
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.group_recycler_item,
                parent,
                false
            )
        )
    }

    //목록의 아이템 개
    override fun getItemCount(): Int {
        return this.modelList.size
    }

    //뷰와 뷰홀더가 묶였을 때
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        Log.d(TAG, "GroupRecyclerAdapter - onBindViewHolder() called / position: $position")
        holder.bind(this.modelList[position])

        //클릭 설정
        holder.itemView.setOnClickListener {
            // 나중에 포토앨범 액티비티로 연결할 부분
            val intent = Intent(App.instance, PhotoAlbumViewPagerActivity::class.java)
            intent.putExtra("groupName", modelList[position].groupName)
            intent.putExtra("absolutePathList", modelList[position].absolutePathList)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            App.instance.startActivity(intent)
        }
    }

    //외부에서 데이터 넘기기
    fun submitList(modelList: ArrayList<GroupModel>) {
        this.modelList = modelList
    }
}