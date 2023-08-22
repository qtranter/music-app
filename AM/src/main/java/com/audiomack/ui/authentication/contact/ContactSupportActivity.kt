package com.audiomack.ui.authentication.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.ui.settings.OptionsMenuFragment
import kotlinx.android.synthetic.main.activity_contact.buttonClose

class ContactSupportActivity : BaseActivity() {

    private val viewModel: ContactSupportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        buttonClose.setOnClickListener { viewModel.onCloseTapped() }

        viewModel.showOptionsEvent.observe(this, Observer { actions ->
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.container, OptionsMenuFragment.newInstance(actions), "options")
                    .addToBackStack("options")
                    .commitAllowingStateLoss()
        })

        viewModel.closeOptionsEvent.observe(this, Observer {
            popFragment()
        })

        viewModel.closeEvent.observe(this, Observer {
            finish()
        })

        supportFragmentManager
                .beginTransaction()
                .add(R.id.container, ContactSupportFragment.newInstance())
                .commit()
    }

    override fun popFragment(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }

    companion object {
        fun show(context: Context?) {
            val intent = Intent(context, ContactSupportActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(intent)
        }
    }
}
