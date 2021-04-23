package com.picpho.pic_pho.Lobby

import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import com.picpho.pic_pho.R
import kotlinx.android.synthetic.main.group_recycler_item.view.*
import org.json.JSONArray
import java.io.File


class GroupViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
    val TAG: String = "로그"

    private val usernameTextView = itemView.textView_group_name
    private val groupMemberImageView0 = itemView.imageview_group_member0
    private val groupMemberImageView1 = itemView.imageview_group_member1
    private val groupMemberImageView2 = itemView.imageview_group_member2
    private val extraFriendCount = itemView.textView_extra_friend_count
    private val groupAlbumImageView = itemView.imageView_group_album
    private val groupAlbumDateTextView = itemView.textView_group_date


    //기본 생성자
    init {
        Log.d(TAG, "GroupViewHolder - () called")
    }

    //데이터와 뷰를 묶는다.
    fun bind(groupModel: GroupModel){
        Log.d(TAG, "GroupViewHolder - bind() called")

        usernameTextView.text = groupModel.groupName
        groupAlbumDateTextView.text = groupModel.eventDate

        var dbHelper = DBHelper(App.instance, "PICPHO.db", null, 2)
        var database = dbHelper.writableDatabase
        var presentImagePath:Uri? = null
        var queryPresentImage = "SELECT * FROM Photos WHERE _id = ${groupModel.presentImage}"
        var cursor = database.rawQuery(queryPresentImage, null)

        if(cursor.moveToFirst()){
            var file = File(cursor.getString(1))
            presentImagePath = Uri.fromFile(file)
        }

        if(groupModel.presentImage != "null") {
            groupAlbumImageView.setImageURI(presentImagePath)
        }else{
            groupAlbumImageView.setImageResource(R.drawable.example_group)
        }

        val arrayMemberUidList = JSONArray(groupModel.memberUidList)
        val memberCount = arrayMemberUidList.length()
        val memberUriList = ArrayList<String>()

        if(memberCount > 1) {
            for (i in 0..1) {
                var queryFriends =
                    "SELECT userProfileImage FROM Friends WHERE userUid = ${arrayMemberUidList[i]}"
                var cursor = database.rawQuery(queryFriends, null)
                if (cursor != null) {
                    cursor.moveToFirst()
                    memberUriList.add(cursor.getString(0))
                }
                Log.e(TAG, "GroupBiewHolder에서 이미지 테스트: ${cursor.getString(0)}")
            }

            if (memberCount > 2) {
                var queryFriends =
                    "SELECT userProfileImage FROM Friends WHERE userUid = ${arrayMemberUidList[2]}"
                var cursor = database.rawQuery(queryFriends, null)
                cursor.moveToFirst()
                memberUriList.add(cursor.getString(0))
                Log.e(TAG, "GroupBiewHolder에서 이미지 테스트: ${cursor.getString(0)}")
            }
        }else{
            Log.d(TAG, "bind: members ${memberCount}")
        }

        // 방에는 최소 2명의 유저가 있어야 하므로, 인덱스 0, 1은 확정
        Glide.with(App.instance).load(memberUriList[0].toUri()).into(groupMemberImageView0)
        Glide.with(App.instance).load(memberUriList[1].toUri()).into(groupMemberImageView1)

        if(memberCount >= 3){
            Glide.with(App.instance).load(memberUriList[2].toUri()).into(groupMemberImageView2)
            if(memberCount > 3){
                extraFriendCount.visibility = View.VISIBLE
                extraFriendCount.text = "+${memberCount - 3}"
            }
        }
    }
}