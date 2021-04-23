package com.picpho.pic_pho.WifiDirect

import android.net.Uri

class PhotoInfo(
    var photoUri: Uri? = null,
    var photoOwnerMac: String? = null,
    var photoOwnerIP: String? = null,
    var absolutePath : String? = null
)