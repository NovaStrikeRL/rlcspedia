package com.rlcspedia.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Revisa el feed con la app cerrada (mínimo de Android: cada 15 min) para que las
 * notificaciones lleguen al teléfono y al reloj. Los eventos los publica [com.rlcspedia.app.notify.Notifier].
 */
class FeedWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (LiveRepository.source != DataSource.LIQUIPEDIA) return Result.success()
        LiveRepository.refresh()
        delay(2_000) // deja que el Notifier publique antes de que el proceso se duerma
        return Result.success()
    }

    companion object {
        private const val NAME = "feed-refresh"
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<FeedWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }
        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
