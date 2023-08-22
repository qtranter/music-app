package com.audiomack.download

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.audiomack.download.RestoreDownloadsResult.Failure
import com.audiomack.download.RestoreDownloadsResult.Success
import io.reactivex.Single

class RestoreDownloadsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : RxWorker(appContext, workerParams) {

    private val restoreDownloadsUseCase: RestoreDownloadsUseCase = RestoreDownloadsUseCaseImpl()

    override fun createWork(): Single<Result> = restoreDownloadsUseCase.restore(backgroundScheduler)
        .map(this::mapResult)

    private fun mapResult(result: RestoreDownloadsResult) = when (result) {
        is Success -> Result.success()
        is Failure -> Result.failure()
    }

    companion object {
        const val TAG_RESTORE_ALL = "com.audiomack.download.tag.RESTORE_ALL"
    }
}
