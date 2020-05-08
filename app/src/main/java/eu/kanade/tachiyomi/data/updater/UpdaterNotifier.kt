package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager

/**
 * DownloadNotifier is used to show notifications when downloading and update.
 *
 * @param context context of application.
 */
internal class UpdaterNotifier(private val context: Context) {

    private val notificationBuilder = context.notificationBuilder(Notifications.CHANNEL_COMMON)

    /**
     * Call to show notification.
     *
     * @param id id of the notification channel.
     */
    private fun NotificationCompat.Builder.show(id: Int = Notifications.ID_UPDATER) {
        context.notificationManager.notify(id, build())
    }

    /**
     * Call when apk download starts.
     *
     * @param title tile of notification.
     */
    fun onDownloadStarted(title: String) {
        with(notificationBuilder) {
            setContentTitle(title)
            setContentText(context.getString(R.string.update_check_notification_download_in_progress))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)
        }
        notificationBuilder.show()
    }

    /**
     * Call when apk download progress changes.
     *
     * @param progress progress of download (xx%/100).
     */
    fun onProgressChange(progress: Int) {
        with(notificationBuilder) {
            setProgress(100, progress, false)
            setOnlyAlertOnce(true)
        }
        notificationBuilder.show()
    }

    /**
     * Call when apk download is finished.
     *
     * @param uri path location of apk.
     */
    fun onDownloadFinished(uri: Uri) {
        with(notificationBuilder) {
            setContentText(context.getString(R.string.update_check_notification_download_complete))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setOnlyAlertOnce(false)
            setProgress(0, 0, false)
            // Install action
            setContentIntent(NotificationHandler.installApkPendingActivity(context, uri))
            addAction(
                R.drawable.ic_system_update_alt_white_24dp,
                context.getString(R.string.action_install),
                NotificationHandler.installApkPendingActivity(context, uri)
            )
            // Cancel action
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.action_cancel),
                NotificationReceiver.dismissNotificationPendingBroadcast(context, Notifications.ID_UPDATER)
            )
        }
        notificationBuilder.show()
    }

    /**
     * Call when apk download throws a error
     *
     * @param url web location of apk to download.
     */
    fun onDownloadError(url: String) {
        with(notificationBuilder) {
            setContentText(context.getString(R.string.update_check_notification_download_error))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setOnlyAlertOnce(false)
            setProgress(0, 0, false)
            // Retry action
            addAction(
                R.drawable.ic_refresh_24dp,
                context.getString(R.string.action_retry),
                UpdaterService.downloadApkPendingService(context, url)
            )
            // Cancel action
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.action_cancel),
                NotificationReceiver.dismissNotificationPendingBroadcast(context, Notifications.ID_UPDATER)
            )
        }
        notificationBuilder.show(Notifications.ID_UPDATER)
    }
}
