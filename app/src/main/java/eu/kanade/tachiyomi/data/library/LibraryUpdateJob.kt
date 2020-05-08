package eu.kanade.tachiyomi.data.library

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import java.util.concurrent.TimeUnit
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        LibraryUpdateService.start(context)
        return Result.success()
    }

    companion object {
        private const val TAG = "LibraryUpdate"

        fun setupTask(context: Context, prefInterval: Int? = null) {
            val preferences = Injekt.get<PreferencesHelper>()
            val interval = prefInterval ?: preferences.libraryUpdateInterval().get()
            if (interval > 0) {
                val restrictions = preferences.libraryUpdateRestriction()!!
                val acRestriction = "ac" in restrictions
                val wifiRestriction = if ("wifi" in restrictions) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                }

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(wifiRestriction)
                    .setRequiresCharging(acRestriction)
                    .build()

                val request = PeriodicWorkRequestBuilder<LibraryUpdateJob>(
                    interval.toLong(), TimeUnit.HOURS,
                    10, TimeUnit.MINUTES
                )
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request)
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
        }
    }
}
