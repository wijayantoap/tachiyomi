package eu.kanade.tachiyomi

import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import eu.kanade.tachiyomi.extension.ExtensionUpdateJob
import eu.kanade.tachiyomi.ui.library.LibrarySort
import java.io.File

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @param preferences Preferences of the application.
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(preferences: PreferencesHelper): Boolean {
        val context = preferences.context
        val oldVersion = preferences.lastVersionCode().get()

        // Cancel app updater job for debug builds that don't include it
        if (BuildConfig.DEBUG && !BuildConfig.INCLUDE_UPDATER) {
            UpdaterJob.cancelTask(context)
        }

        if (oldVersion < BuildConfig.VERSION_CODE) {
            preferences.lastVersionCode().set(BuildConfig.VERSION_CODE)

            // Fresh install
            if (oldVersion == 0) {
                // Set up default background tasks
                if (BuildConfig.INCLUDE_UPDATER) {
                    UpdaterJob.setupTask(context)
                }
                ExtensionUpdateJob.setupTask(context)
                LibraryUpdateJob.setupTask(context)
                return false
            }

            if (oldVersion < 14) {
                // Restore jobs after upgrading to Evernote's job scheduler.
                if (BuildConfig.INCLUDE_UPDATER) {
                    UpdaterJob.setupTask(context)
                }
                LibraryUpdateJob.setupTask(context)
            }
            if (oldVersion < 15) {
                // Delete internal chapter cache dir.
                File(context.cacheDir, "chapter_disk_cache").deleteRecursively()
            }
            if (oldVersion < 19) {
                // Move covers to external files dir.
                val oldDir = File(context.externalCacheDir, "cover_disk_cache")
                if (oldDir.exists()) {
                    val destDir = context.getExternalFilesDir("covers")
                    if (destDir != null) {
                        oldDir.listFiles()?.forEach {
                            it.renameTo(File(destDir, it.name))
                        }
                    }
                }
            }
            if (oldVersion < 26) {
                // Delete external chapter cache dir.
                val extCache = context.externalCacheDir
                if (extCache != null) {
                    val chapterCache = File(extCache, "chapter_disk_cache")
                    if (chapterCache.exists()) {
                        chapterCache.deleteRecursively()
                    }
                }
            }
            if (oldVersion < 43) {
                // Restore jobs after migrating from Evernote's job scheduler to WorkManager.
                if (BuildConfig.INCLUDE_UPDATER) {
                    UpdaterJob.setupTask(context)
                }
                LibraryUpdateJob.setupTask(context)
                BackupCreatorJob.setupTask(context)

                // New extension update check job
                ExtensionUpdateJob.setupTask(context)
            }
            if (oldVersion < 44) {
                // Reset sorting preference if using removed sort by source
                if (preferences.librarySortingMode().get() == LibrarySort.SOURCE) {
                    preferences.librarySortingMode().set(LibrarySort.ALPHA)
                }
            }
            return true
        }
        return false
    }
}
