package com.audiomack.ui.authentication.changepw

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.audiomack.R

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changepassword)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.container, ChangePasswordFragment.newInstance())
                .commit()
        }
    }
}
