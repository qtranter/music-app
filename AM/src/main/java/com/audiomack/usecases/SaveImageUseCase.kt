package com.audiomack.usecases

import android.content.Context
import android.net.Uri
import com.audiomack.utils.extensions.copyInputStreamToFile
import java.io.File

interface SaveImageUseCase {
    fun copyInputStreamToFile(uri: Uri, file: File): Long
}

class SaveImageUseCaseImpl(val context: Context?) : SaveImageUseCase {
    override fun copyInputStreamToFile(uri: Uri, file: File): Long {
        return context?.copyInputStreamToFile(uri, file) ?: 0L
    }
}
