package com.audiomack.data.user

import com.audiomack.model.AMResultItem
import java.util.ArrayList
import java.util.HashSet

interface UserDataInterface {
    val favoritedItemsCount: Int
    val myPlaylistsCount: Int
    fun isItemFavorited(item: AMResultItem): Boolean
    fun addItemToFavorites(item: AMResultItem)
    fun removeItemFromFavorites(item: AMResultItem)
    fun isArtistFollowed(artistId: String?): Boolean
    fun addItemToFavoritePlaylists(itemId: String)
    fun addItemToFavoriteMusic(itemId: String)
    fun addArtistToFollowing(artistId: String)
    fun addPlaylistToMyPlaylists(playlistId: String)
    fun removeArtistFromFollowing(artistId: String)
    fun addItemToReups(itemId: String)
    fun removeItemFromReups(itemId: String)
    fun isItemReuped(itemId: String): Boolean
    fun addItemToHighlights(item: AMResultItem)
    fun removeItemFromHighlights(item: AMResultItem)
    fun isItemHighlighted(item: AMResultItem): Boolean
    fun getHighlights(): List<AMResultItem>
    fun clearHighlights()
    fun setHighlights(items: MutableList<AMResultItem>)
}

object UserData : UserDataInterface {

    private val favoriteMusicIDs: MutableSet<String>
    private val favoritePlaylistIDs: MutableSet<String>
    private val followingArtistIDs: MutableSet<String>
    private val myPlaylistIDs: MutableSet<String>
    private val reupIDs: MutableSet<String>
    private var highlights: MutableList<AMResultItem>? = null

    override val favoritedItemsCount: Int
        get() = favoriteMusicIDs.size

    override val myPlaylistsCount: Int
        get() = myPlaylistIDs.size

    init {
        favoriteMusicIDs = HashSet()
        favoritePlaylistIDs = HashSet()
        followingArtistIDs = HashSet()
        myPlaylistIDs = HashSet()
        reupIDs = HashSet()
        highlights = ArrayList()
    }

    fun clear() {
        favoriteMusicIDs.clear()
        favoritePlaylistIDs.clear()
        followingArtistIDs.clear()
        myPlaylistIDs.clear()
        reupIDs.clear()
        highlights!!.clear()
    }

    override fun isItemFavorited(item: AMResultItem): Boolean {
        return if (item.isPlaylist) favoritePlaylistIDs.contains(item.itemId) else favoriteMusicIDs.contains(
            item.itemId
        )
    }

    override fun addItemToFavorites(item: AMResultItem) {
        if (item.isPlaylist) {
            favoritePlaylistIDs.add(item.itemId)
        } else {
            favoriteMusicIDs.add(item.itemId)
        }
    }

    override fun removeItemFromFavorites(item: AMResultItem) {
        if (item.isPlaylist) {
            favoritePlaylistIDs.remove(item.itemId)
        } else {
            favoriteMusicIDs.remove(item.itemId)
        }
    }

    override fun isArtistFollowed(artistId: String?): Boolean {
        return followingArtistIDs.contains(artistId)
    }

    override fun addItemToFavoritePlaylists(itemId: String) {
        favoritePlaylistIDs.add(itemId)
    }

    override fun addItemToFavoriteMusic(itemId: String) {
        favoriteMusicIDs.add(itemId)
    }

    override fun addArtistToFollowing(artistId: String) {
        followingArtistIDs.add(artistId)
    }

    override fun addPlaylistToMyPlaylists(playlistId: String) {
        myPlaylistIDs.add(playlistId)
    }

    override fun removeArtistFromFollowing(artistId: String) {
        followingArtistIDs.remove(artistId)
    }

    override fun addItemToReups(itemId: String) {
        reupIDs.add(itemId)
    }

    override fun removeItemFromReups(itemId: String) {
        reupIDs.remove(itemId)
    }

    override fun isItemReuped(itemId: String): Boolean {
        return reupIDs.contains(itemId)
    }

    override fun addItemToHighlights(item: AMResultItem) {
        if (highlights?.none { it.itemId == item.itemId } == true) {
            highlights?.add(item)
        }
    }

    override fun removeItemFromHighlights(item: AMResultItem) {
        highlights = highlights?.filter { it.itemId != item.itemId }?.toMutableList()
    }

    override fun isItemHighlighted(item: AMResultItem): Boolean {
        return highlights?.any { it.itemId == item.itemId } ?: false
    }

    override fun getHighlights(): List<AMResultItem> {
        return highlights?.toList() ?: ArrayList()
    }

    override fun clearHighlights() {
        highlights?.clear()
    }

    override fun setHighlights(items: MutableList<AMResultItem>) {
        highlights = items
    }
}
