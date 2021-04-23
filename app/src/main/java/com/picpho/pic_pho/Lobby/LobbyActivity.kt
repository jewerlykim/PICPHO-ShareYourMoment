package com.picpho.pic_pho.Lobby

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.picpho.pic_pho.App
import com.picpho.pic_pho.MakeGroup.MakeGroupActivity
import com.picpho.pic_pho.PhotoAlbum.PhotoAlbumPageItemModel
import com.picpho.pic_pho.databinding.ActivityLobbyBinding
import kotlinx.android.synthetic.main.activity_lobby.*
import kotlinx.android.synthetic.main.group_recycler_item.*
import kotlinx.android.synthetic.main.group_recycler_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LobbyActivity : AppCompatActivity() {

    private val TAG = "LobbyActivity"
    val GroupName: String = "SW 정글"

    // Adapter list
    var modelList = ArrayList<GroupModel>()

    private lateinit var myRecyclerAdapter: GroupRecyclerAdapter
    private lateinit var binding: ActivityLobbyBinding
    lateinit var dbHelper: DBHelper
    lateinit var database: SQLiteDatabase

    companion object {
        var photoAlbumList = ArrayList<PhotoAlbumPageItemModel>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLobbyBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //어댑터 인스턴스 생성
        myRecyclerAdapter = GroupRecyclerAdapter()
        myRecyclerAdapter.submitList(this.modelList)

        //리사이클러뷰 설정
        binding.groupRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(this@LobbyActivity, LinearLayoutManager.VERTICAL, false)
            adapter = myRecyclerAdapter
        }

//        binding.floatingActionButtonLobby.setOnClickListener {
//            val intent = Intent(this, MakeGroupActivity::class.java)
//            startActivity(intent)
//        }
    }

    override fun onStart() {
        modelList.clear()
        // SQLite에서 DB를 조회할 것
        CoroutineScope(Dispatchers.Default).launch {
            dbHelper = DBHelper(App.instance, "PICPHO.db", null, 2)
            database = dbHelper.writableDatabase
            modelList.clear()

            var queryGroups = "SELECT * FROM Groups"
            var cursor = database.rawQuery(queryGroups, null)
            if (cursor.moveToFirst()) {
                for (i in 0 until cursor.count) {
                    val index = cursor.getString(0)
                    val groupName = cursor.getString(1)
                    val presentImage = cursor.getString(2)
                    val eventDate = cursor.getString(3)
                    val absolutePathList = cursor.getString(4)
                    val memberList = cursor.getString(5)
                    val isDeleted = cursor.getString(6)
                    Log.d(
                        "SQLITE",
                        "${index}, ${groupName}, ${presentImage}, ${eventDate}, ${absolutePathList}, ${memberList}"
                    )
                    if (!isDeleted.toBoolean()) {
//                        val groupModel = GroupModel(
//                            groupName = "$GroupName $i",
//                            presentImage = "https://img.tvreportcdn.de/cms-content/uploads/2020/09/01/75d6b835-c759-42ca-b753-f941121e9ba6.jpg",
//                            roomAddr = "test",
//                            memberUidList = ArrayList()
//                        )
                        val groupModel = GroupModel(
                            groupName = groupName,
                            presentImage = presentImage,
                            eventDate = eventDate,
                            absolutePathList = absolutePathList,
                            memberUidList = memberList
                        )
                        modelList.add(groupModel)
                    }
                    cursor.moveToNext()
                }
                Log.e(TAG, "onCreate: Looby, GroupItems Created, ${modelList}")
            }
            modelList.reverse()
        }
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            myRecyclerAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        finish()
    }

    fun clickSearchView(view: View) {
        if (binding.searchviewLobby.visibility == View.VISIBLE) {
            binding.searchviewLobby.visibility = View.GONE
        } else {
            binding.searchviewLobby.visibility = View.VISIBLE
        }
    }

    fun moveToMakeGroupActivity(view: View) {
        val intent = Intent(this, MakeGroupActivity::class.java)
        startActivity(intent)
    }
}