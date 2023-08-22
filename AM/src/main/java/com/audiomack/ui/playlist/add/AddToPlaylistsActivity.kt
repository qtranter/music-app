package com.audiomack.ui.playlist.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.ui.playlist.edit.EditPlaylistActivity
import com.audiomack.ui.playlist.edit.EditPlaylistMode.CREATE
import com.audiomack.views.AMSnackbar

class AddToPlaylistsActivity : AppCompatActivity() {

    private lateinit var data: AddToPlaylistModel
    private val viewModel: AddToPlaylistsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addtoplaylists)

        data = intent.extras?.getParcelable("data") ?: throw IllegalStateException("Missing 'data' intent extra")

        initViewModel()
        initViewModelObservers()

        if (savedInstanceState == null) {
            viewModel.onCreate()
        }
    }

    private fun initViewModel() {
        viewModel.init(data)
    }

    private fun initViewModelObservers() {
        viewModel.closeEvent.observe(this, Observer {
            finish()
        })
        viewModel.showPlaylistsEvent.observe(this, Observer {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.container, AddToPlaylistsFragment())
                .commitAllowingStateLoss()
        })
        viewModel.newPlaylistEvent.observe(this, Observer {
            startActivity(EditPlaylistActivity.getLaunchIntent(this, CREATE, data))
            finish()
        })
        viewModel.playlistCannotBeEditedEvent.observe(this, Observer {
            AMSnackbar.Builder(this)
                .withTitle(getString(R.string.addtoplaylist_playlist_cannot_be_edited))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                .show()
        })
        viewModel.songCannotBeAddedEvent.observe(this, Observer {
            AMSnackbar.Builder(this)
                .withTitle(getString(R.string.addtoplaylist_song_cannot_be_added))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                .show()
        })
        viewModel.cannotRemoveLastTrackEvent.observe(this, Observer {
            AMSnackbar.Builder(this)
                .withTitle(getString(R.string.edit_playlist_tracks_reorder_error_last_track))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                .show()
        })
        viewModel.addedSongEvent.observe(this, Observer {
            AMSnackbar.Builder(this)
                .withTitle(getString(R.string.addtoplaylist_song_added))
                .withDrawable(R.drawable.ic_snackbar_playlist)
                .show()
        })
        viewModel.failedToAddSongEvent.observe(this, Observer {
            AMSnackbar.Builder(this)
                .withTitle(getString(R.string.addtoplaylist_song_added_failed))
                .withSubtitle(getString(R.string.please_try_again_later))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                .show()
        })
        viewModel.removedSongEvent.observe(this, Observer {
            AMSnackbar.Builder(this)
                .withTitle(getString(R.string.addtoplaylist_song_removed))
                .withDrawable(R.drawable.ic_snackbar_playlist)
                .show()
        })
        viewModel.failedToRemoveSongEvent.observe(this, Observer {
            AMSnackbar.Builder(this)
                .withTitle(getString(R.string.addtoplaylist_song_removed_failed))
                .withSubtitle(getString(R.string.please_try_again_later))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                .show()
        })
        viewModel.failedToFetchPlaylistEvent.observe(this, Observer {
            AMSnackbar.Builder(this)
                .withTitle(getString(R.string.select_playlist_error))
                .withSubtitle(getString(R.string.please_try_again_later))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                .show()
        })
    }

    companion object {
        @JvmStatic
        fun show(context: Context?, data: AddToPlaylistModel) {
            context?.let {
                it.startActivity(Intent(it, AddToPlaylistsActivity::class.java).apply {
                    putExtra("data", data)
                })
            }
        }
    }
}
