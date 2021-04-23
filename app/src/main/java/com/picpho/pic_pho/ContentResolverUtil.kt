package com.picpho.pic_pho

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileNotFoundException
import java.io.IOException

class ContentResolverUtil(context : Context) {
    private val contentResolver: ContentResolver = context.contentResolver
    fun isExist(uri: Uri) :Boolean{
        var pfd: ParcelFileDescriptor? = null
        try {
            pfd = contentResolver.openFileDescriptor(uri, "r")
            return pfd!=null
        } catch (e: FileNotFoundException){
            e.printStackTrace()
        }finally {
            try {
                pfd?.close()
            }catch (e : IOException){
                e.printStackTrace()
            }
        }
        return false
    }
}