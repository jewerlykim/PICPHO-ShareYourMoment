package com.picpho.pic_pho.WifiDirect

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import com.picpho.pic_pho.PhotoRoom.PhotoRoomActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.Socket
import java.util.*

class FileReceiveActionThread(var client: Socket, var context: Context) : Thread() {

    override fun run() {
        super.run()
        Log.d(TAG, "FileReceiverAction Start")
        var inputStream = client.getInputStream()
        var peersIP = client.inetAddress.hostName

        var protocolByteArray = ByteArray(1)
        inputStream.read(protocolByteArray)

        Log.d(
            TAG,
            "run: String(protocolByteArray, Charsets.UTF_8 ${
                String(
                    protocolByteArray,
                    Charsets.UTF_8
                )
            }"
        )

        when (String(protocolByteArray, Charsets.UTF_8)) {

            "0" -> {

            }

            "1" -> {
                if (!WifiDirectMainActivity.connectedPeerList.contains(peersIP)) {
                    WifiDirectMainActivity.connectedPeerList.add(peersIP)
                    WifiDirectMainActivity.connectedPeerMap.set(peersIP, 0)
                }
            }

            "2" -> {
                var photoDigitByteArray = ByteArray(1)
                inputStream.read(photoDigitByteArray)
                var readByteArraySize = 69
                when (String(photoDigitByteArray, Charsets.UTF_8)) {
                    "2" -> {
                        readByteArraySize = 70
                    }
                    "3" -> {
                        readByteArraySize = 71
                    }
                }

                var byteArray = ByteArray(readByteArraySize)
                var testSize: Int = inputStream.read(byteArray)
                Log.d(TAG, "run: testSize is === $testSize")
                var metaData = String(byteArray, Charsets.UTF_8)
                val jsonObject = JSONObject(metaData)
                var status = Integer.parseInt(jsonObject.get("status").toString())
                var photoOwnerMac = jsonObject.get("photoOwnerMac")
                var clientPhotoCount =
                    Integer.parseInt(jsonObject.get("clientPhotoCount").toString())

                Log.d(TAG, "stringjson receive: ${jsonObject}")

                val calendar: Calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val mon = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                var filename: String =
                    "/sdcard/DCIM/Picpho/" + "${year}+${mon}+${day}" + "_" + System.currentTimeMillis() + ".jpg"

                var file = File(filename)

                var photoinfo = PhotoInfo(
                    photoUri = Uri.parse(filename),
                    photoOwnerMac = photoOwnerMac.toString(),
                    photoOwnerIP = client.inetAddress.hostName,
                    absolutePath = filename
                )

                Log.d(TAG, "run: Photoinfo : ${photoinfo}")

                WifiDirectMainActivity.photoInfoList.add(photoinfo)
                WifiDirectMainActivity.filePathList.add(filename)

                val dirs = File(file!!.parent.toString())
                if (!dirs.exists()) dirs.mkdirs()

                if (file!!.createNewFile()) {
                    var byteSize = inputStream.copyTo(FileOutputStream(file))
                    if (!WifiDirectMainActivity.isGroupOwner) {
                        scanFile(context, file!!, "jpg")
                    }else
                        PhotoRoomActivity.scanFileForOwnerList.add(file!!)
                    Log.d(TAG, "FileReceiverAction Copyto Finished")
                }

                var groupMemberCount = WifiDirectMainActivity.groupMemberCount
                Log.d(TAG, "run: status ----- $status")


                if (status == 1) {
                    if (WifiDirectMainActivity.isGroupOwner) {
                        WifiDirectMainActivity.sentPhotoCount++
//                        WifiDirectMainActivity.textViewShowGroupCount!!.text =
//                            "Member : ${WifiDirectMainActivity.groupMemberCount}" + "Sent : ${WifiDirectMainActivity.sentPhotoCount}"

                        if (WifiDirectMainActivity.connectedPeerMap.get(peersIP) == 0) {
                            WifiDirectMainActivity.connectedPeerMap[peersIP] = clientPhotoCount
                        }
                        Log.d(TAG, "run: groupMemberCount != 0 ${groupMemberCount}&& WifiDirectMainActivity.sentPhotoCount ${WifiDirectMainActivity.sentPhotoCount}")
                        if (groupMemberCount != 0 && groupMemberCount == WifiDirectMainActivity.sentPhotoCount) {
                            CoroutineScope(Dispatchers.Main).launch {
                                WaitingForOwnerActivity.wifiChoosePhotoCardView!!.isEnabled = true
                                WaitingForOwnerActivity.wifiChoosePhotoCardView!!.setCardBackgroundColor(
                                    Color.parseColor("#76CBFF")
                                )
                            }
                        }
                    }

                } else if (status == 2) {
                    var intent = Intent(context, PhotoRoomActivity::class.java)
                    context.startActivity(intent)
                    sleep(1000)
                    client.close()
                }
            }

            "3" -> {
                var intent = Intent(context, PhotoRoomActivity::class.java)
                context.startActivity(intent)
                sleep(1000)
                client.close()
            }
        }
    }

    companion object {
        private const val TAG = "FileReceiveAction"

        fun scanFile(context: Context?, f: File, mimeType: String) {
            CoroutineScope(Dispatchers.Default).launch {
                MediaScannerConnection
                    .scanFile(context, arrayOf(f.absolutePath), arrayOf(mimeType), null)
            }
        }
    }
}


