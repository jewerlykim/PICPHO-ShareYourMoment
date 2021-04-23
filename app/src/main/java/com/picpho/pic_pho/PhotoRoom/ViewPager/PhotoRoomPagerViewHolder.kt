package com.picpho.pic_pho.PhotoRoom.ViewPager

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import com.picpho.pic_pho.DoubleClickListener
import com.picpho.pic_pho.PhotoRoom.PhotoRoomActivity.Companion.drawerPhotoUriList
import com.picpho.pic_pho.PhotoRoom.PhotoRoomActivity.Companion.drawerRecyclerAdapter
import com.picpho.pic_pho.PhotoRoom.PhotoRoomActivity.Companion.thumbnailRecyclerAdapter
import com.picpho.pic_pho.PhotoRoom.ThumbnailPhotoModel
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoEnlargeActivity
import com.picpho.pic_pho.R
import com.picpho.pic_pho.WifiDirect.UI.WifiDrawer.DrawerPhotoModel
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity.Companion.filePathList
import kotlinx.android.synthetic.main.item_photoroom_pager.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhotoRoomPagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val itemImage = itemView.SelectedPhoto
    private val bookMarkImage = itemView.imageView_bookmark_wifi
    private var drawerPhotoModel: DrawerPhotoModel? = null
    private val maxImage = itemView.imageView_photomax_wifi
    private val checkAnimation = itemView.downloadPhotoCheck

    fun bindWithView(thumbnailPhotoModel: ThumbnailPhotoModel, position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            Glide
                .with(App.instance)
                .load(thumbnailPhotoModel.thumbnailPhoto)
                .into(itemImage)

            if (thumbnailPhotoModel.isPicked) {
                bookMarkImage.setImageResource(R.drawable.download_fill)
            } else {
                bookMarkImage.setImageResource(R.drawable.download_2)
            }
        }

        itemImage.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick(v: View) {
                CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                    if (thumbnailPhotoModel.isPicked) {
                        bookMarkImage.setImageResource(R.drawable.download_2)
                        thumbnailPhotoModel.isPicked = false
                        drawerPhotoUriList.remove(thumbnailPhotoModel.thumbnailPhoto!!)

                        filePathList.add(thumbnailPhotoModel.path!!)
                    } else {

                        bookMarkImage.setImageResource(R.drawable.download_fill)
                        thumbnailPhotoModel.isPicked = true
                        drawerPhotoUriList.add(thumbnailPhotoModel.thumbnailPhoto!!)

                        filePathList.remove(thumbnailPhotoModel.path!!)
                        Log.d("photo delete", "bindWithView: ${thumbnailPhotoModel.path!!}")
                        showCheckAnimation()
                    }
                    delay(100)
                    thumbnailRecyclerAdapter.notifyItemChanged(position)
                    drawerRecyclerAdapter.notifyDataSetChanged()
                }
            }
        })



        bookMarkImage.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                if (thumbnailPhotoModel.isPicked) {
                    bookMarkImage.setImageResource(R.drawable.download_2)
                    thumbnailPhotoModel.isPicked = false
                    drawerPhotoUriList.remove(thumbnailPhotoModel.thumbnailPhoto!!)
                    // photo delete
                    filePathList.add(thumbnailPhotoModel.path!!)
                } else {

                    bookMarkImage.setImageResource(R.drawable.download_fill)
                    thumbnailPhotoModel.isPicked = true
                    drawerPhotoUriList.add(thumbnailPhotoModel.thumbnailPhoto!!)
                    // photo delete
                    filePathList.remove(thumbnailPhotoModel.path!!)
                    Log.d("photo delete", "bindWithView: ${thumbnailPhotoModel.path!!}")
                    showCheckAnimation()
                }
                delay(100)
                thumbnailRecyclerAdapter.notifyItemChanged(position)
                drawerRecyclerAdapter.notifyDataSetChanged()
            }
        }

        maxImage.setOnClickListener {
            var intent = Intent(App.instance, ServerPhotoEnlargeActivity::class.java)
            intent.putExtra("uri", thumbnailPhotoModel.thumbnailPhoto.toString())
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