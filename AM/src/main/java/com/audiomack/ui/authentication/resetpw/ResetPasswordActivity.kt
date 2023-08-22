package com.audiomack.ui.authentication.resetpw

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.audiomack.R

class ResetPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resetpassword)

        val token = requireNotNull(intent.getStringExtra(EXTRA_TOKEN))

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.container, ResetPasswordFragment.newInstance(token))
                .commit()
        }
    }

    companion object {
        private const val EXTRA_TOKEN = "extra_token"
        fun buildIntent(context: Context, token: String) =
            Intent(context, ResetPasswordActivity::class.java).apply {
                putExtra(EXTRA_TOKEN, token)
            }
    }
}
