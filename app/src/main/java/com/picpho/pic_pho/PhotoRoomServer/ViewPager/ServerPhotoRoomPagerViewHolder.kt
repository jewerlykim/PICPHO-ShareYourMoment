package com.picpho.pic_pho.PhotoRoomServer.ViewPager

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import com.picpho.pic_pho.CellularSocket.SocketUtil.Companion.mSocket
import com.picpho.pic_pho.DoubleClickListener
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoEnlargeActivity
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity.Companion.photoPickedList
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity.Companion.photoPickedUriList
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity.Companion.serverDrawerPickAdapter
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity.Companion.serverThumbnailRecyclerAdapter
import com.picpho.pic_pho.PhotoRoomServer.ServerThumbnailPhotoModel
import com.picpho.pic_pho.R
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity.Companion.serverFilePathList
import kotlinx.android.synthetic.main.item_photoroom_pager.view.SelectedPhoto
import kotlinx.android.synthetic.main.item_serverphotoroom_pager.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServerPhotoRoomPagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val itemImage = itemView.SelectedPhoto
    private val favoriteImage = itemView.imageView_favorite_server
    private val bookmarkImage = itemView.imageView_bookmark_server
    private val maxImage = itemView.imageView_photomax_server
    private val icon_bar = itemView.linearLayout_image_icon_bar
    private val roomAddress: String = ServerPhotoRoomActivity.roomAddress!!
    private val likeCountText = itemView.textView_heart
    private val checkAnimation = itemView.downloadPhotoCheck


    fun bindWithView(serverThumbnailPhotoModel: ServerThumbnailPhotoModel, position : Int) {

        Log.d("TAG", "bindWithView: ${serverThumbnailPhotoModel.thumbnailPhoto}")
        Glide
            .with(App.instance)
            .load(serverThumbnailPhotoModel.thumbnailPhoto)
            .into(itemImage)

        likeCountText.text = serverThumbnailPhotoModel.likeCount.toString() + "개"

        itemImage.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick(v: View) {
                CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                    if (serverThumbnailPhotoModel.isPicked) {
                        bookmarkImage.setImageResource(R.drawable.download_2)
                        serverThumbnailPhotoModel.isPicked = false
                        photoPickedUriList.remove(serverThumbnailPhotoModel.thumbnailPhoto!!)
                        photoPickedList.remove(serverThumbnailPhotoModel)
                        serverFilePathList.add(serverThumbnailPhotoModel.absolutePath!!)
                    } else {
                        bookmarkImage.setImageResource(R.drawable.download_fill)
                        serverThumbnailPhotoModel.isPicked = true
                        photoPickedUriList.add(serverThumbnailPhotoModel.thumbnailPhoto!!)
                        photoPickedList.add(serverThumbnailPhotoModel)
                        serverFilePathList.remove(serverThumbnailPhotoModel.absolutePath!!)
                        showCheckAnimation()
                    }
                    delay(100)
                    serverThumbnailRecyclerAdapter.notifyItemChanged(position)
                    serverDrawerPickAdapter.notifyDataSetChanged()
                }
            }
        })


        if (serverThumbnailPhotoModel.userimg ==null){
            serverThumbnailPhotoModel.userimg = "https://user-images.githubusercontent.com/47134564/114669435-d7db3c80-9d3c-11eb-8f87-eeb9d58bab47.png"
        }
        if (serverThumbnailPhotoModel.userimg != null) {
            Glide
                .with(App.instance)
                .load(serverThumbnailPhotoModel.userimg)
                .error(R.drawable.ic_baseline_account_circle_blue_24)
                .into(itemView.ProfileInPhotoRoom)
        }

        if (serverThumbnailPhotoModel.username != null) {
            itemView.UsernameInPhotoRoom.text = serverThumbnailPhotoModel.username
        }

        CoroutineScope(Dispatchers.Main).launch {
            if (serverThumbnailPhotoModel.isLike) {
                favoriteImage.setImageResource(R.drawable.favorite_fill)
            } else {
                favoriteImage.setImageResource(R.drawable.favorite_2)
            }


            if (serverThumbnailPhotoModel.isPicked) {
                bookmarkImage.setImageResource(R.drawable.download_fill)
            } else {
                bookmarkImage.setImageResource(R.drawable.download_2)
            }
        }




        favoriteImage.setOnClickListener {
            if (serverThumbnailPhotoModel.isLike) {
                CoroutineScope(Dispatchers.Main).launch {
                    favoriteImage.setImageResource(R.drawable.favorite_2)
                }
                serverThumbnailPhotoModel.isLike = false
                CoroutineScope(Dispatchers.IO).launch {
                    mSocket!!.emit(
                        "clickLike",
                        roomAddress,
                        adapterPosition,
                        serverThumbnailPhotoModel.pictureowner,
                        0
                    )
                }

            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    favoriteImage.setImageResource(R.drawable.favorite_fill)
                }
                serverThumbnailPhotoModel.isLike = true
                CoroutineScope(Dispatchers.IO).launch {
                    mSocket!!.emit(
                        "clickLike",
                        roomAddress,
                        adapterPosition,
                        serverThumbnailPhotoModel.pictureowner,
                        1
                    )
                }
            }
        }

        bookmarkImage.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                if (serverThumbnailPhotoModel.isPicked) {
                    bookmarkImage.setImageResource(R.drawable.download_2)
                    serverThumbnailPhotoModel.isPicked = false
                    photoPickedUriList.remove(serverThumbnailPhotoModel.thumbnailPhoto!!)
                    photoPickedList.remove(serverThumbnailPhotoModel)
                    serverFilePathList.add(serverThumbnailPhotoModel.absolutePath!!)
                } else {
                    bookmarkImage.setImageResource(R.drawable.download_fill)
                    serverThumbnailPhotoModel.isPicked = true
                    photoPickedUriList.add(serverThumbnailPhotoModel.thumbnailPhoto!!)
                    photoPickedList.add(serverThumbnailPhotoModel)
                    serverFilePathList.remove(serverThumbnailPhotoModel.absolutePath!!)
                    showCheckAnimation()
                }
                delay(100)
                serverThumbnailRecyclerAdapter.notifyItemChanged(position)
                serverDrawerPickAdapter.notifyDataSetChanged()
            }
        }

        maxImage.setOnClickListener {
            var imagepath = serverThumbnailPhotoModel.thumbnailPhoto!!.getPath();
            var orientation = serverThumbnailPhotoModel.orientation


            var intent = Intent(App.instance, ServerPhotoEnlargeActivity::class.java)
            intent.putExtra("uri", serverThumbnailPhotoModel.thumbnailPhoto.toString())
            intent.putExtra("orientation", orientation)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            App.instance.startActivity(intent)
        }
    }

    fun showCheckAnimation() {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
            checkAnimation.visibility = View.VISIBLE
            checkAnimation.playAnimation()
            delay(1000)
            checkAnimation.visibility = View.GONE
            checkAnimation.pauseAnimation()
        }
    }
}