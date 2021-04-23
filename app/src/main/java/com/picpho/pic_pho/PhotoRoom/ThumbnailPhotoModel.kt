package com.picpho.pic_pho.PhotoRoom

import android.net.Uri

class ThumbnailPhotoModel(
    var thumbnailPhoto: Uri?  = null,
    var isPicked : Boolean = false,
    var path : String? = null,
    var photoOwnerIp : String? = null,
    var photoOwnerMac : String? =null
)