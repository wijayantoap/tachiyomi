package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import java.util.Calendar
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SetTrackReadingDatesDialog<T> : DialogController
        where T : Controller, T : SetTrackReadingDatesDialog.Listener {

    private val item: TrackItem

    private val dateToUpdate: ReadingDate

    constructor(target: T, dateToUpdate: ReadingDate, item: TrackItem) : super(
        Bundle().apply {
            putSerializable(SetTrackReadingDatesDialog.KEY_ITEM_TRACK, item.track)
        }
    ) {
        targetController = target
        this.item = item
        this.dateToUpdate = dateToUpdate
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        val track = bundle.getSerializable(SetTrackReadingDatesDialog.KEY_ITEM_TRACK) as Track
        val service = Injekt.get<TrackManager>().getService(track.sync_id)!!
        item = TrackItem(track, service)
        dateToUpdate = ReadingDate.Start
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val listener = (targetController as? Listener)

        return MaterialDialog(activity!!)
            .title(
                when (dateToUpdate) {
                    ReadingDate.Start -> R.string.track_started_reading_date
                    ReadingDate.Finish -> R.string.track_finished_reading_date
                }
            )
            .datePicker(currentDate = getCurrentDate()) { _, date ->
                listener?.setReadingDate(item, dateToUpdate, date.timeInMillis)
            }
            .neutralButton(R.string.action_remove) {
                listener?.setReadingDate(item, dateToUpdate, 0L)
            }
    }

    private fun getCurrentDate(): Calendar {
        // Today if no date is set, otherwise the already set date
        return Calendar.getInstance().apply {
            item.track?.let {
                val date = when (dateToUpdate) {
                    ReadingDate.Start -> it.started_reading_date
                    ReadingDate.Finish -> it.finished_reading_date
                }
                if (date != 0L) {
                    timeInMillis = date
                }
            }
        }
    }

    interface Listener {
        fun setReadingDate(item: TrackItem, type: ReadingDate, date: Long)
    }

    enum class ReadingDate {
        Start,
        Finish
    }

    companion object {
        private const val KEY_ITEM_TRACK = "SetTrackReadingDatesDialog.item.track"
    }
}
