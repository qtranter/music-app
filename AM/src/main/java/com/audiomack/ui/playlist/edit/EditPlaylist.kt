package com.audiomack.ui.playlist.edit

import com.audiomack.model.AMResultItem
import com.audiomack.ui.base.ViewStateProvider
import java.io.File
import java.io.InputStream

enum class EditPlaylistMode {
    CREATE, EDIT
}

class EditPlaylistException(
    val type: Type,
    val throwable: Throwable? = null
) {
    enum class Type {
        CREATE, EDIT, DELETE, TITLE, BANNER
    }
}

interface EditPlaylistItemProvider {
    val playlist: AMResultItem?
}

interface EditPlaylistImageProvider {
    val imageFile: File?
    val bannerFile: File?
    fun inputStreamToBase64(inputStream: InputStream): String
    fun fileToBase64(file: File): String
}

interface EditPlaylistViewStateProvider : ViewStateProvider {
    fun getTitle(): String
    fun getGenre(): String
    fun getDesc(): String
    fun isBannerVisible(): Boolean
}
