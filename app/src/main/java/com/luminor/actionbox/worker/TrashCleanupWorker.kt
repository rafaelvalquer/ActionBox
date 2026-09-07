package com.luminor.actionbox.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.luminor.actionbox.ActionBoxApplication
import java.util.concurrent.TimeUnit

@androidx.hilt.work.HiltWorker
class TrashCleanupWorker @dagger.assisted.AssistedInject constructor(
    @dagger.assisted.Assisted appContext: Context,
    @dagger.assisted.Assisted params: WorkerParameters,
    private val repository: com.luminor.actionbox.data.repository.ActionRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        repository.purgeTrash(System.currentTimeMillis() - RETENTION_MILLIS)
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val UNIQUE_WORK = "actionbox-trash-cleanup"
        const val RETENTION_DAYS = 30L
        const val RETENTION_MILLIS = RETENTION_DAYS * 24L * 60L * 60L * 1000L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TrashCleanupWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
