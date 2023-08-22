package com.audiomack.common

import android.content.Context
import androidx.work.WorkManager
import com.audiomack.MainApplication

interface WorkManagerProvider {
    val workManager: WorkManager
}

class WorkManagerProviderImpl(context: Context) : WorkManagerProvider {

    override val workManager: WorkManager = WorkManager.getInstance(context)

    companion object {
        @Volatile
        private var instance: WorkManagerProvider? = null

        fun init(context: Context): WorkManagerProvider = instance ?: synchronized(this) {
            instance ?: WorkManagerProviderImpl(context).also { instance = it }
        }

        fun getInstance(): WorkManagerProvider =
            instance ?: MainApplication.context?.let { init(it) }
            ?: throw IllegalStateException("WorkManagerProviderImpl was not initialized")
    }
}
