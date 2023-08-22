package com.audiomack.ui.playlist.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.model.Action
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.ui.settings.OptionsMenuFragment

class EditPlaylistActivity : BaseActivity() {

    private val viewModel: EditPlaylistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_editplaylist)

        initViewModelObservers()

        if (savedInstanceState == null) {
            val mode = intent.extras?.getSerializable(EXTRA_MODE) as? EditPlaylistMode
                ?: throw IllegalStateException("No mode specified in bundled extras")

            val data = intent.extras?.getParcelable(EXTRA_DATA) as? AddToPlaylistModel

            supportFragmentManager
                .beginTransaction()
                .add(R.id.container, EditPlaylistFragment.newInstance(mode, data))
                .commit()
        }
    }

    override fun popFragment(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }

    private fun initViewModelObservers() {
        val lifecycleOwner = this

        viewModel.apply {
            showOptionsEvent.observe(lifecycleOwner, showOptionsEventObserver)
            popBackStackEvent.observe(lifecycleOwner, popEventObserver)
            finishEvent.observe(lifecycleOwner, finishEventObserver)
            backEvent.observe(lifecycleOwner, backEventObserver)
        }
    }

    private val showOptionsEventObserver = Observer<List<Action>> { actions ->
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, OptionsMenuFragment.newInstance(actions))
            .addToBackStack(OptionsMenuFragment.TAG_BACK_STACK)
            .commit()
    }

    private val popEventObserver = Observer<Void> {
        popFragment()
    }

    private val finishEventObserver = Observer<Void> {
        finish()
    }

    private val backEventObserver = Observer<Void> {
        onBackPressed()
    }

    companion object {
        const val EXTRA_MODE = "com.audiomack.intent.extra.EXTRA_MODE"
        const val EXTRA_DATA = "com.audiomack.intent.extra.EXTRA_DATA"

        fun getLaunchIntent(context: Context, mode: EditPlaylistMode, data: AddToPlaylistModel ? = null): Intent =
            Intent(context, EditPlaylistActivity::class.java)
                .putExtra(EXTRA_MODE, mode)
                .putExtra(EXTRA_DATA, data)
    }
}
