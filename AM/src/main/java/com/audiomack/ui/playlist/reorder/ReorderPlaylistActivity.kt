package com.audiomack.ui.playlist.reorder

import android.os.Bundle
import com.audiomack.R
import com.audiomack.activities.BaseActivity

class ReorderPlaylistActivity : BaseActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reorder_playlist)

        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, ReorderPlaylistFragment.newInstance())
            .commitAllowingStateLoss()
    }
}
