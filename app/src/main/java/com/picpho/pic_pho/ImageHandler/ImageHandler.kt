package com.picpho.pic_pho.ImageHandler

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*

class ImageHandler(context: Context) {
    companion object {
        fun compressImage(originalImage: File, context: Context): File? {
            var resultImage: File? = null
            runBlocking {
                CoroutineScope(Dispatchers.Default).launch {
                    launch {
                        // Default compression
                        resultImage =
                            Compressor.compress(context = context, originalImage!!)
                            {
//                    resolution(1500, 1500)
                                quality(95)
                                format(Bitmap.CompressFormat.JPEG)
//                    size(1_097_152) // 2 MB
                            }
                    }
                }.join()
            }
            return resultImage
        }

        fun bitmapToString(bitmap: Bitmap): String? {
            val byteArrayOutputStream =
                ByteArrayOutputStream() //바이트 배열을 차례대로 읽어 들이기위한 ByteArrayOutputStream클래스 선언
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                90,
                byteArrayOutputStream
            ) //bitmap을 압축 (숫자 70은 70%로 압축한다는 뜻)
            val bytes: ByteArray = byteArrayOutputStream.toByteArray() //해당 bitmap을 byte배열로 바꿔준다.
            return Base64.encodeToString(bytes, Base64.DEFAULT) //String을 retrurn
        }

        fun convertString64ToImage(base64String: String): Bitmap {
            val decodedString = Base64.decode(base64String, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        }

        fun saveBitmapAsFile(
            bitmap: Bitmap,
            file: File,
            receivedTakeTime: String,
            receivedOwner: String
        ) {
            var os: OutputStream? = null
            try {
                file.createNewFile()
                os = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                var exif = ExifInterface(file)
                exif.setAttribute(ExifInterface.TAG_DATETIME, receivedTakeTime)
                exif.setAttribute(ExifInterface.TAG_ARTIST, receivedOwner)
                exif.saveAttributes()
                ServerPhotoRoomActivity.getExif(file)
                os.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        fun selectPhoto(): Intent {
            var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/*"
            return intent
        }

        fun getFullPathFromUri(context: Context, fileUri: Uri?): String? {
            var fullPath: String? = null
            val column = "_data"
            var cursor: Cursor = context.contentResolver.query(fileUri!!, null, null, null, null)!!
            if (cursor != null) {
                cursor.moveToFirst()
                var documentId: String = cursor.getString(0)
                if (documentId == null) {
                    for (i in 0 until cursor.columnCount) {
                        if (column.equals(cursor.getColumnName(i), ignoreCase = true)) {
                            fullPath = cursor.getString(i)
                            break
                        }
                    }
                } else {
                    documentId = documentId.substring(documentId.lastIndexOf(":") + 1)
                    cursor.close()
                    val projection = arrayOf(column)
                    try {
                        cursor = context.contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            projection,
                            MediaStore.Images.Media._ID + " = ? ",
                            arrayOf(documentId),
                            null
                        )!!
                        if (cursor != null) {
                            cursor.moveToFirst()
                            fullPath = cursor.getString(cursor.getColumnIndexOrThrow(column))
                        }
                    } finally {
                        if (cursor != null) cursor.close()
                    }
                }
            }
            return fullPath
        }

        fun getOrientationOfImage(filepath: String?): Int {
            var exif: ExifInterface? = null
            exif = try {
                ExifInterface(filepath!!)
            } catch (e: IOException) {
                e.printStackTrace()
                return -1
            }
            val orientation = exif!!.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
            if (orientation != -1) {
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> return 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> return 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> return 270
                }
            }
            return 0
        }

    }
}