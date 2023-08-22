package com.audiomack.data.user

import com.audiomack.data.storage.Storage
import com.audiomack.data.storage.StorageProvider
import java.io.File

class AccountImageProvider(
    storage: Storage = StorageProvider.getInstance()
) : AccountImages {

    private val dir = File(storage.cacheDir, "account").apply { mkdirs() }

    override val avatarFile: File
        get() = File(dir, "avatar.jpg")

    override val bannerFile: File
        get() = File(dir, "banner.jpg")
}
