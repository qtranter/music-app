package com.audiomack.ui.player.maxi.morefromartist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.data.player.PlayerDataSource

class PlayerMoreFromArtistViewModelFactory(
    private val playerDataSource: PlayerDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlayerMoreFromArtistViewModel(playerDataSource) as T
    }
}
