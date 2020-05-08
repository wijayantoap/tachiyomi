package eu.kanade.tachiyomi.ui.recent.history

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.source.SourceManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import uy.kohesive.injekt.injectLazy

/**
 * Adapter of HistoryHolder.
 * Connection between Fragment and Holder
 * Holder updates should be called from here.
 *
 * @param controller a HistoryController object
 * @constructor creates an instance of the adapter.
 */
class HistoryAdapter(controller: HistoryController) :
    FlexibleAdapter<HistoryItem>(null, controller, true) {

    val sourceManager by injectLazy<SourceManager>()

    val resumeClickListener: OnResumeClickListener = controller
    val removeClickListener: OnRemoveClickListener = controller
    val itemClickListener: OnItemClickListener = controller

    /**
     * DecimalFormat used to display correct chapter number
     */
    val decimalFormat = DecimalFormat(
        "#.###",
        DecimalFormatSymbols()
            .apply { decimalSeparator = '.' }
    )

    init {
        setDisplayHeadersAtStartUp(true)
        setStickyHeaders(true)
    }

    interface OnResumeClickListener {
        fun onResumeClick(position: Int)
    }

    interface OnRemoveClickListener {
        fun onRemoveClick(position: Int)
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}
