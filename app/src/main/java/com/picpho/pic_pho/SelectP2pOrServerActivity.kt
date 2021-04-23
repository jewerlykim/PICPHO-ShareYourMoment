package com.picpho.pic_pho

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.picpho.pic_pho.Lobby.LobbyActivity
import com.picpho.pic_pho.WifiDirect.WifiDirectMainActivity
import com.picpho.pic_pho.databinding.ActivitySelectP2pOrServerBinding
import com.kakao.sdk.user.UserApiClient


class SelectP2pOrServerActivity : AppCompatActivity() {
    private var TAG = "SelectP2pOrServerActivi"
    private lateinit var binding: ActivitySelectP2pOrServerBinding
    private var clicked: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectP2pOrServerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        LoginActivity.requestPermissionToUser(this)


        // true = invisible , false = visible
        var nearbyQuestionMarkVisibility = true
        binding.nearbyQuestionMark.setOnClickListener {
            if (nearbyQuestionMarkVisibility) {
                binding.nearbyHelpImage.visibility = View.VISIBLE
                nearbyQuestionMarkVisibility = false
            } else {
                binding.nearbyHelpImage.visibility = View.INVISIBLE
                nearbyQuestionMarkVisibility = true
            }
        }
        var farQuestionMarkVisibility = true
        binding.farQuestionMark.setOnClickListener {
            if (farQuestionMarkVisibility) {
                binding.farHelpImage.visibility = View.VISIBLE
                farQuestionMarkVisibility = false
            } else {
                binding.farHelpImage.visibility = View.INVISIBLE
                farQuestionMarkVisibility = true

            }
        }


        binding.kakaoLogoutButton.setOnClickListener {
            UserApiClient.instance.logout { error ->
                if (error != null) {
                    Toast.makeText(this, "로그아웃에 실패했습니다. $error", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                }
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }
    }


    fun selectP2pMode(view: View) {
        val intent = Intent(this, WifiDirectMainActivity::class.java)
        startActivity(intent)
    }

    fun selectServerMode(view: View) {
        val intent = Intent(this, LobbyActivity::class.java)
        startActivity(intent)
    }


}