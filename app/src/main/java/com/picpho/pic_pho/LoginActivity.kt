package com.picpho.pic_pho

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.iid.FirebaseInstanceId
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.AuthErrorCause.*
import com.kakao.sdk.user.UserApiClient
import com.picpho.pic_pho.CellularSocket.SocketUtil.Companion.createAndConnectSocket
import com.picpho.pic_pho.CellularSocket.SocketUtil.Companion.mSocket
import com.picpho.pic_pho.PhotoRoomServer.ServerPhotoRoomActivity
import com.picpho.pic_pho.WaitingRoomServer.ServerWaitingRoomActivity
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity
import com.picpho.pic_pho.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {
    private val TAG = "LoginActivity"

    lateinit var intentfromfirebase: Intent // = getIntent()


    val callBack: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Log.d(TAG, "token ${token} error ${error}")
            when {
                error.toString() == AccessDenied.toString() -> {
                }
                error.toString() == InvalidClient.toString() -> {
                }
                error.toString() == InvalidGrant.toString() -> {
                }
                error.toString() == InvalidRequest.toString() -> {
                }
                error.toString() == InvalidScope.toString() -> {
                }
                error.toString() == Misconfigured.toString() -> {
                }
                error.toString() == ServerError.toString() -> {
                }
                error.toString() == Unauthorized.toString() -> {
                }
                else -> { // Unknown
                }
            }
        } else if (token != null) {

            registerMemberToServer()

            mSocket?.disconnect()
            mSocket = null

            val intent = Intent(this, SelectP2pOrServerActivity::class.java)
            startActivity(intent)

        }
    }

    companion object {
        var roomAddress: String? = null


        fun requestPermissionToUser(activity: Activity) {
            var writePermission =
                ContextCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            var readPermission =
                ContextCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            var locationPermission =
                ContextCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            var recordAudioPermission =
                ContextCompat.checkSelfPermission(App.instance, Manifest.permission.RECORD_AUDIO)

            if (writePermission == PackageManager.PERMISSION_DENIED
                || readPermission == PackageManager.PERMISSION_DENIED
                || locationPermission == PackageManager.PERMISSION_DENIED
                || recordAudioPermission == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.RECORD_AUDIO
                    ),
                    1
                )
            }
        }
        var loginActivity: Activity? = null
    }

    private lateinit var binding: ActivityLoginBinding
    private var dialogLogin: Dialog? = null


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loginActivity = this

        if (dialogLogin != null)
            dialogLogin!!.dismiss()

        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
            } else if (tokenInfo != null) {

                registerMemberToServer()

                if (roomAddress.isNullOrEmpty()) {

                    val intent = Intent(this, SelectP2pOrServerActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                }
            }
        }

        roomAddress = null
        intentfromfirebase = getIntent()
        if (intentfromfirebase != null) {
            roomAddress = intentfromfirebase.extras?.getString("roomAddress")

            if (roomAddress != null) {
                UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
                    if (error != null) {

                    } else if (tokenInfo != null) {
                        var waitingroomIntent: Intent =
                            Intent(this, ServerWaitingRoomActivity::class.java)
                        waitingroomIntent.putExtra("roomAddress", roomAddress)
                        waitingroomIntent.putExtra("test", "2")
                        startActivity(waitingroomIntent)
                    }
                }

            }
        }

        requestPermissionToUser(this)

        binding.kakaoLoginButton.setOnClickListener {
            Log.d(TAG, "onCreate: kakao_login_button")
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                UserApiClient.instance.loginWithKakaoTalk(this, callback = callBack)
            } else {
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callBack)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val networkStatus: Boolean = NetworkStatus.isConnected(this)
        if (!networkStatus) {
            runBlocking {
                CoroutineScope(Dispatchers.Main).launch {
                    dialogLogin =
                        ServerPhotoRoomActivity.showDialog(
                            context = loginActivity!!,
                            resource = R.layout.dialog_invite,
                            gravity = Gravity.CENTER,
                            color = Color.WHITE
                        )

                    dialogLogin!!.findViewById<TextView>(R.id.cancelText).setOnClickListener {
                        dialogLogin!!.dismiss()
                    }
                    dialogLogin!!.findViewById<TextView>(R.id.switchToP2pText).setOnClickListener {
                        switchToWifiRoom()
                        dialogLogin!!.dismiss()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (dialogLogin != null)
            dialogLogin!!.dismiss()
        super.onDestroy()
    }

    fun switchToWifiRoom() {
        val intent = Intent(this, WifiDirectMainActivity::class.java)
        startActivity(intent)
    }


    fun registerMemberToServer() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
            } else if (user != null) {
                mSocket = createAndConnectSocket()

                //send to server id, nickname, profile url
                val firebasetoken = FirebaseInstanceId.getInstance().token

                runBlocking {
                    CoroutineScope(Dispatchers.IO).launch {
                        var profile: String? = null
                        if (user.kakaoAccount?.profile?.profileImageUrl.toString() == "null") {
                            profile = "https://user-images.githubusercontent.com/47134564/114669435-d7db3c80-9d3c-11eb-8f87-eeb9d58bab47.png"

                        } else {
                            profile = user.kakaoAccount?.profile?.profileImageUrl.toString()
                        }
                        mSocket!!.emit(
                            "RegisterMemberToDB",
                            user.id,
                            user.kakaoAccount?.profile?.nickname,
                            profile,
                            firebasetoken
                        )
                    }.join()
                }

            }
        }

    }




    @RequiresApi(Build.VERSION_CODES.P)
    fun getHash() {
        try {
            val info =
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signatures = info.signingInfo.apkContentsSigners
            for (signature in signatures) {
                val messageDigest: MessageDigest = MessageDigest.getInstance("SHA")
                messageDigest.update(signature.toByteArray())
                val key = String(Base64.encode(messageDigest.digest(), 0))
                Log.d("Hash Key: ", "!@!@!$key!@!@!")
            }
        } catch (e: Exception) {
            Log.e("not fount", e.toString())
        }
    }

}