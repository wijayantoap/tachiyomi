package eu.kanade.tachiyomi.ui.base.holder

import android.view.View
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.flexibleadapter.items.ISectionable
import eu.kanade.tachiyomi.util.system.dpToPx
import io.github.mthli.slice.Slice

interface SlicedHolder {

    val slice: Slice

    val adapter: FlexibleAdapter<IFlexible<*>>

    val viewToSlice: View

    fun setCardEdges(item: ISectionable<*, *>) {
        // Position of this item in its header. Defaults to 0 when header is null.
        var position = 0

        // Number of items in the header of this item. Defaults to 1 when header is null.
        var count = 1

        if (item.header != null) {
            val sectionItems = adapter.getSectionItems(item.header)
            position = sectionItems.indexOf(item)
            count = sectionItems.size
        }

        when {
            // Only one item in the card
            count == 1 -> applySlice(2f, topRect = false, bottomRect = false, topShadow = true, bottomShadow = true)
            // First item of the card
            position == 0 -> applySlice(2f, topRect = false, bottomRect = true, topShadow = true, bottomShadow = false)
            // Last item of the card
            position == count - 1 -> applySlice(2f, topRect = true, bottomRect = false, topShadow = false, bottomShadow = true)
            // Middle item
            else -> applySlice(0f, topRect = false, bottomRect = false, topShadow = false, bottomShadow = false)
        }
    }

    private fun applySlice(
        radius: Float,
        topRect: Boolean,
        bottomRect: Boolean,
        topShadow: Boolean,
        bottomShadow: Boolean
    ) {
        val margin = margin

        slice.setRadius(radius)
        slice.showLeftTopRect(topRect)
        slice.showRightTopRect(topRect)
        slice.showLeftBottomRect(bottomRect)
        slice.showRightBottomRect(bottomRect)
        setMargins(margin, if (topShadow) margin else 0, margin, if (bottomShadow) margin else 0)
    }

    private fun setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        if (viewToSlice.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = viewToSlice.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
        }
    }

    val margin
        get() = 8.dpToPx
}
