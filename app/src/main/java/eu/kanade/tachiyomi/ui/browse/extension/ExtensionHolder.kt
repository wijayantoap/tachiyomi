package eu.kanade.tachiyomi.ui.browse.extension

import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.holder.SlicedHolder
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.getResourceColor
import io.github.mthli.slice.Slice
import kotlinx.android.synthetic.main.extension_card_item.card
import kotlinx.android.synthetic.main.extension_card_item.ext_button
import kotlinx.android.synthetic.main.extension_card_item.ext_title
import kotlinx.android.synthetic.main.extension_card_item.image
import kotlinx.android.synthetic.main.extension_card_item.lang
import kotlinx.android.synthetic.main.extension_card_item.version

class ExtensionHolder(view: View, override val adapter: ExtensionAdapter) :
    BaseFlexibleViewHolder(view, adapter),
    SlicedHolder {

    override val slice = Slice(card).apply {
        setColor(adapter.cardBackground)
    }

    override val viewToSlice: View
        get() = card

    init {
        ext_button.setOnClickListener {
            adapter.buttonClickListener.onButtonClick(bindingAdapterPosition)
        }
    }

    fun bind(item: ExtensionItem) {
        val extension = item.extension
        setCardEdges(item)

        // Set source name
        ext_title.text = extension.name
        version.text = extension.versionName
        lang.text = if (extension !is Extension.Untrusted) {
            LocaleHelper.getSourceDisplayName(extension.lang, itemView.context)
        } else {
            itemView.context.getString(R.string.ext_untrusted).toUpperCase()
        }

        GlideApp.with(itemView.context).clear(image)
        if (extension is Extension.Available) {
            GlideApp.with(itemView.context)
                .load(extension.iconUrl)
                .into(image)
        } else {
            extension.getApplicationIcon(itemView.context)?.let { image.setImageDrawable(it) }
        }
        bindButton(item)
    }

    @Suppress("ResourceType")
    fun bindButton(item: ExtensionItem) = with(ext_button) {
        isEnabled = true
        isClickable = true

        setTextColor(context.getResourceColor(R.attr.colorAccent))

        val extension = item.extension

        val installStep = item.installStep
        if (installStep != null) {
            setText(
                when (installStep) {
                    InstallStep.Pending -> R.string.ext_pending
                    InstallStep.Downloading -> R.string.ext_downloading
                    InstallStep.Installing -> R.string.ext_installing
                    InstallStep.Installed -> R.string.ext_installed
                    InstallStep.Error -> R.string.action_retry
                }
            )
            if (installStep != InstallStep.Error) {
                isEnabled = false
                isClickable = false
            }
        } else if (extension is Extension.Installed) {
            when {
                extension.hasUpdate -> {
                    setText(R.string.ext_update)
                }
                extension.isObsolete -> {
                    setTextColor(context.getResourceColor(R.attr.colorError))
                    setText(R.string.ext_obsolete)
                }
                else -> {
                    setText(R.string.ext_details)
                }
            }
        } else if (extension is Extension.Untrusted) {
            setText(R.string.ext_trust)
        } else {
            setText(R.string.ext_install)
        }
    }
}
