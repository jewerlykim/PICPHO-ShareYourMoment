package com.picpho.pic_pho.PhotoRoomServer

import android.net.Uri

class ServerThumbnailPhotoModel(
    var thumbnailPhoto: Uri? = null,
    var taketime: String?,
    var pictureowner: String?,
    var index: Int,
    var count: Int,
    var username: String? = null,
    var userimg: String? = null,
    var isPicked: Boolean = false,
    var isLike: Boolean = false,
    var likeCount: Int = 0,
    var absolutePath : String? = null,
    var orientation: Int =0
)