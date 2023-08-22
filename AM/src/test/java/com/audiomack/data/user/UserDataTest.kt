package com.audiomack.data.user

import com.audiomack.model.AMResultItem
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test

class UserDataTest {

    private lateinit var sut: UserData

    @Before
    fun setup() {
        sut = UserData
        sut.clear()
    }

    @Test
    fun testFavoriteSongOrAlbum() {
        val music = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isPlaylist } doReturn false
        }
        assert(!sut.isItemFavorited(music))
        sut.addItemToFavorites(music)
        assert(sut.isItemFavorited(music))
        assert(sut.favoritedItemsCount == 1)
        sut.addItemToFavoriteMusic(music.itemId)
        assert(sut.isItemFavorited(music))
        assert(sut.favoritedItemsCount == 1)
        sut.addItemToFavorites(music)
        assert(sut.isItemFavorited(music))
        assert(sut.favoritedItemsCount == 1)
        sut.removeItemFromFavorites(music)
        assert(!sut.isItemFavorited(music))
        assert(sut.favoritedItemsCount == 0)
    }

    @Test
    fun testFavoritePlaylists() {
        val music = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isPlaylist } doReturn true
        }
        assert(!sut.isItemFavorited(music))
        sut.addItemToFavorites(music)
        assert(sut.isItemFavorited(music))
        sut.addItemToFavoritePlaylists(music.itemId)
        assert(sut.isItemFavorited(music))
        sut.addItemToFavorites(music)
        assert(sut.isItemFavorited(music))
        sut.removeItemFromFavorites(music)
        assert(!sut.isItemFavorited(music))
    }

    @Test
    fun testFollow() {
        val artistId = "456"
        assert(!sut.isArtistFollowed(artistId))
        sut.addArtistToFollowing(artistId)
        assert(sut.isArtistFollowed(artistId))
        sut.addArtistToFollowing(artistId)
        assert(sut.isArtistFollowed(artistId))
        sut.removeArtistFromFollowing(artistId)
        assert(!sut.isArtistFollowed(artistId))
    }

    @Test
    fun testReup() {
        val musicId = "789"
        assert(!sut.isItemReuped(musicId))
        sut.addItemToReups(musicId)
        assert(sut.isItemReuped(musicId))
        sut.addItemToReups(musicId)
        assert(sut.isItemReuped(musicId))
        sut.removeItemFromReups(musicId)
        assert(!sut.isItemReuped(musicId))
    }

    @Test
    fun testMyPlaylists() {
        val musicId = "789"
        sut.addPlaylistToMyPlaylists(musicId)
        assert(sut.myPlaylistsCount == 1)
        sut.addPlaylistToMyPlaylists(musicId)
        assert(sut.myPlaylistsCount == 1)
    }

    @Test
    fun testHighlights() {
        val id = "123"
        val music = mock<AMResultItem> {
            on { itemId } doReturn id
        }
        assert(!sut.isItemHighlighted(music))
        sut.addItemToHighlights(music)
        assert(sut.isItemHighlighted(music))
        assert(sut.getHighlights().first().itemId == id)
        sut.addItemToHighlights(music)
        assert(sut.isItemHighlighted(music))
        assert(sut.getHighlights().first().itemId == id)
        sut.removeItemFromHighlights(music)
        assert(!sut.isItemHighlighted(music))
        assert(sut.getHighlights().isEmpty())
        sut.setHighlights(mutableListOf(music))
        assert(sut.isItemHighlighted(music))
        assert(sut.getHighlights().first().itemId == id)
        sut.clearHighlights()
        assert(sut.getHighlights().isEmpty())
    }
}
