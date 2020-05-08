package eu.kanade.tachiyomi.ui.migration

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.source_main_controller_card_header.title

/**
 * Item that contains the selection header.
 */
class SelectionHeader : AbstractHeaderItem<SelectionHeader.Holder>() {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.source_main_controller_card_header
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): Holder {
        return Holder(view, adapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: Holder,
        position: Int,
        payloads: List<Any?>?
    ) {
        // Intentionally empty
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : BaseFlexibleViewHolder(view, adapter) {
        init {
            title.text = view.context.getString(R.string.migration_selection_prompt)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is SelectionHeader
    }

    override fun hashCode(): Int {
        return 0
    }
}
