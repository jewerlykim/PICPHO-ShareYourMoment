package com.picpho.pic_pho.PhotoAlbum

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picpho.pic_pho.App
import com.picpho.pic_pho.ImageHandler.ImageHandler
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoEnlargeActivity
import kotlinx.android.synthetic.main.item_photo_album.view.*

class PhotoAlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var albumImage = itemView.photo_album_item_image
    private var photomaxImage = itemView.imageView_photomax_album

    fun bindWithView(photoUri: Uri) {
        Glide
            .with(App.instance)
            .load(photoUri)
            .into(albumImage)

        photomaxImage.setOnClickListener {
            var imagepath = photoUri.path
            var orientation = ImageHandler.getOrientationOfImage(imagepath)


            var intent = Intent(App.instance, ServerPhotoEnlargeActivity::class.java)
            intent.putExtra("uri", photoUri.toString())
            intent.putExtra("orientation", orientation)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            App.instance.startActivity(intent)
        }
    }
}