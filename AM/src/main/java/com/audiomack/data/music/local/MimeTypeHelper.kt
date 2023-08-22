package com.audiomack.data.music.local

import android.webkit.MimeTypeMap
import com.audiomack.utils.Url
import java.util.Locale

interface MimeTypeHelper {
    fun getFileExtensionFromUrl(url: Url?): String
    fun getMimeTypeFromExtension(ext: String): String?
    fun getMimeTypeFromUrl(url: Url?): String?
}

class MimeTypeHelperImpl(
    private val locale: Locale = Locale.getDefault()
) : MimeTypeHelper {
    override fun getFileExtensionFromUrl(url: Url?): String =
        MimeTypeMap.getFileExtensionFromUrl(url)

    override fun getMimeTypeFromExtension(ext: String) =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(locale))

    override fun getMimeTypeFromUrl(url: Url?): String? =
        getMimeTypeFromExtension(getFileExtensionFromUrl(url))
}
