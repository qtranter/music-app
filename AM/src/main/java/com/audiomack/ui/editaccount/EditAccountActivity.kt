package com.audiomack.ui.editaccount

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import java.lang.ref.WeakReference

class EditAccountActivity : BaseActivity() {

    private var fragmentRef: WeakReference<EditAccountFragment>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account)

        val fragment = EditAccountFragment.newInstance()
        fragmentRef = WeakReference(fragment)

        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, fragment)
            .commitAllowingStateLoss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fragmentRef?.get()?.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        @JvmStatic
        fun show(activity: Activity?) {
            activity?.let {
                val intent = Intent(it, EditAccountActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(intent)
            }
        }
    }
}
