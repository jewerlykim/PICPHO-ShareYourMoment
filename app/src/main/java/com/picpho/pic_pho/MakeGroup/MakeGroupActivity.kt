package com.picpho.pic_pho.MakeGroup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.nkzawa.emitter.Emitter
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.user.UserApiClient
import com.picpho.pic_pho.App
import com.picpho.pic_pho.CellularSocket.SocketUtil.Companion.createAndConnectSocket
import com.picpho.pic_pho.CellularSocket.SocketUtil.Companion.mSocket
import com.picpho.pic_pho.MakeGroup.selectedFriends.SelectedFriendsModel
import com.picpho.pic_pho.MakeGroup.selectedFriends.SelectedFriendsRecyclerAdapter
import com.picpho.pic_pho.MakeGroup.selectedFriends.SelectedFriendsRecyclerViewInterface
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity
import com.picpho.pic_pho.WifiDirect.UI.HorizontalItemDecoration
import com.picpho.pic_pho.databinding.ActivityMakeGroupBinding
import kotlinx.android.synthetic.main.activity_lobby.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_make_group.*
import kotlinx.android.synthetic.main.activity_photoroom.*
import kotlinx.android.synthetic.main.item_make_group_recycler.*
import kotlinx.android.synthetic.main.item_selected_friends.*
import kotlinx.coroutines.*
import org.jetbrains.anko.toast
import org.json.JSONArray


class MakeGroupActivity : AppCompatActivity(),
    MakeGroupRecyclerViewInterface, SelectedFriendsRecyclerViewInterface {

    val TAG: String = "log"
    lateinit var binding: ActivityMakeGroupBinding

    var makeGroupModelList = ArrayList<MakeGroupModel>()

    private lateinit var makeGroupRecyclerAdapter: MakeGroupRecyclerAdapter

    companion object {
        lateinit var invitedFriendsJsonArray: JsonArray

        var selectedFriends: RecyclerView? = null

        var selectedFriendsModelList = ArrayList<SelectedFriendsModel>()
        lateinit var selectedFriendsRecyclerAdapter: SelectedFriendsRecyclerAdapter

        var selectedFriendsList = ArrayList<MakeGroupModel>()
        var makeGroupActivity: Activity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeGroupActivity = this

        binding = ActivityMakeGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        selectedFriends = binding.selectedFriends

        if (mSocket == null) {
            Log.d(TAG, "onCreate: try connect socket")
            mSocket = createAndConnectSocket()
        }

        CoroutineScope(Dispatchers.IO).launch {
            mSocket!!.on("kakaoFriendsOnlineReturn", parseMakeGroupModelList)
            delay(200)
        }

        selectedFriendsModelList.clear()
        selectedFriendsList.clear()
        makeGroupModelList.clear()

        makeGroupRecyclerAdapter = MakeGroupRecyclerAdapter(this)
        makeGroupRecyclerAdapter.submitList(makeGroupModelList)

        binding.friendList.apply {
            layoutManager =
                LinearLayoutManager(
                    this@MakeGroupActivity,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter = makeGroupRecyclerAdapter
        }


        selectedFriendsRecyclerAdapter = SelectedFriendsRecyclerAdapter(this)
        selectedFriendsRecyclerAdapter.submitList(selectedFriendsModelList)
        update_selectedFriends_recyclerview()

    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (mSocket == null) {
            Log.d(TAG, "onResume: try socket connect in resume")
            mSocket = createAndConnectSocket()
        }

        makeGroupModelList.clear()

        TalkApiClient.instance.friends { friends, error ->
            if (error != null) {
                Log.e(TAG, "fail get friends list", error)
            } else if (friends != null) {
                Log.i(TAG, "success get friends list \n${friends.elements.joinToString("\n")}")

                var kakaoFriendsList = JSONArray()

                for (i in 0 until friends.elements.size) {
                    var kakaoFriend = JsonObject()
                    kakaoFriend.apply {
                        addProperty("name", friends.elements[i].profileNickname)
                        addProperty(
                            "profileImage",
                            friends.elements[i].profileThumbnailImage.toString()
                        )
                        addProperty("userId", friends.elements[i].id.toInt())
                    }
                    kakaoFriendsList.put(kakaoFriend)
                }
                Log.e(TAG, "onCreate: kakaoFriendsList 111  ${kakaoFriendsList}", )
                mSocket!!.emit("CheckFriendsOnline", kakaoFriendsList.toString())
            }
        }
    }

    var parseMakeGroupModelList = Emitter.Listener {
        Log.d(TAG, "parseMakeGroupModelList: parseMakeGroupModelList")
        var kakaoFriendsList = it[0].toString()
        var jsonArray = JSONArray(kakaoFriendsList)
        val gson = Gson()
        for (i in 0 until jsonArray.length()) {
            var groupModel = gson.fromJson(jsonArray[i].toString(), MakeGroupModel::class.java)
            makeGroupModelList.add(groupModel)
            Log.e(TAG, "parseMakeGroupModelList: ${groupModel}")
        }
        Log.d(TAG, "in parse make group model list : $makeGroupModelList")
        makeGroupAdapterNotifiDataSetChanged()
    }

    fun makeGroupAdapterNotifiDataSetChanged() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            makeGroupRecyclerAdapter.notifyDataSetChanged()
        }
    }


    fun update_selectedFriends_recyclerview() {
        binding.selectedFriends.apply {
            layoutManager =
                LinearLayoutManager(
                    this@MakeGroupActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
            adapter = selectedFriendsRecyclerAdapter
            addItemDecoration(HorizontalItemDecoration(20))
        }
    }


    fun activityfinish(view: View) {
        finish()
    }


    //function for finish action
    fun makeGroupAction(view: View) {
        //그룹방 이름 받아오기
        val roomName = binding.edittextRoomname.text.toString()
        Log.e(TAG, "group room name: ${roomName}")

        if (roomName.isEmpty()) {
            //방 이름이 공백일 때!
            Toast.makeText(
                App.instance,
                "방 이름을 입력해주세요.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        //TODO 본인 정보도 포함 시켜서 보내야함

        if (mSocket != null) {
            invitedFriendsJsonArray = JsonArray()
            for (i in 0 until makeGroupModelList.size) {
                if (makeGroupModelList[i].isSelected == true) {
                    var jsonobject: JsonObject? = JsonObject()
                    jsonobject!!.addProperty("userid", makeGroupModelList[i].userId.toString())
                    jsonobject!!.addProperty("name", makeGroupModelList[i].name.toString())
                    jsonobject!!.addProperty(
                        "profileImage",
                        makeGroupModelList[i].profileImage.toString()
                    )
                    jsonobject!!.addProperty("roomName", roomName)
                    invitedFriendsJsonArray.add(jsonobject)

                    selectedFriendsList.add(makeGroupModelList[i])
                }
            }

            if(selectedFriendsList.size < 1){
                toast("초대할 친구를 선택해주세요")
                return
            }

            UserApiClient.instance.me { user, error ->
                if (error != null) {
                    Log.e(TAG, "fail request user information", error)
                } else if (user != null) {
                    Log.i(TAG, "success request user information")
                    Log.d("jsonobject", "makeGroupAction: ${invitedFriendsJsonArray}")
                    mSocket!!.emit(
                        "selectedGroup",
                        invitedFriendsJsonArray,
                        "picpho" + user.id.toString(),
                        roomName,
                        user.kakaoAccount?.profile?.nickname.toString()
                    )

                    var roomAddr: String = "picpho" + user.id.toString()
                    val intent: Intent = Intent(this, ServerWaitingRoomActivity::class.java)
                    intent.putExtra("roomAddress", roomAddr)
                    intent.putExtra("invitedFriendsJsonArray", invitedFriendsJsonArray.toString())
                    intent.putExtra("isHost", true)
                    intent.putExtra("test", "makeroom")
                    intent.putExtra("roomName", roomName)

                    Log.e("log", intent.getStringExtra("roomAddress"))
                    Log.d("log", "makeGroupAction: ${selectedFriendsModelList.size}")
                    mSocket!!.off("kakaoFriendsOnlineReturn")

                    startActivity(intent)
                    finish()
                }
            }
        } else {
            Log.d(TAG, "makeGroupAction: socket null")
        }
    }

    override fun onBackPressed() {
        mSocket?.off("kakaoFriendsOnlineReturn")
        finish()
    }

    override fun onItemClicked() {
    }

    override fun onDestroy() {
        selectedFriendsModelList.clear()
        selectedFriendsList.clear()
        makeGroupModelList.clear()
        mSocket?.off("kakaoFriendsOnlineReturn")
        super.onDestroy()
        Log.e(TAG, "onDestroy: MakeGroupActivity destroy", )
    }
}

