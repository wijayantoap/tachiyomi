package eu.kanade.tachiyomi.ui.manga

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.PagerControllerBinding
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersController
import eu.kanade.tachiyomi.ui.manga.info.MangaInfoController
import eu.kanade.tachiyomi.ui.manga.track.TrackController
import eu.kanade.tachiyomi.util.system.toast
import java.util.Date
import kotlinx.android.synthetic.main.main_activity.tabs
import rx.Subscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaController : RxController<PagerControllerBinding>, TabbedController {

    constructor(manga: Manga?, fromSource: Boolean = false) : super(
        Bundle().apply {
            putLong(MANGA_EXTRA, manga?.id ?: 0)
            putBoolean(FROM_SOURCE_EXTRA, fromSource)
        }
    ) {
        this.manga = manga
        if (manga != null) {
            source = Injekt.get<SourceManager>().getOrStub(manga.source)
        }
    }

    constructor(mangaId: Long) : this(
        Injekt.get<DatabaseHelper>().getManga(mangaId).executeAsBlocking()
    )

    @Suppress("unused")
    constructor(bundle: Bundle) : this(bundle.getLong(MANGA_EXTRA))

    var manga: Manga? = null
        private set

    var source: Source? = null
        private set

    private var adapter: MangaDetailAdapter? = null

    val fromSource = args.getBoolean(FROM_SOURCE_EXTRA, false)

    val lastUpdateRelay: BehaviorRelay<Date> = BehaviorRelay.create()

    val chapterCountRelay: BehaviorRelay<Float> = BehaviorRelay.create()

    val mangaFavoriteRelay: PublishRelay<Boolean> = PublishRelay.create()

    private val trackingIconRelay: BehaviorRelay<Boolean> = BehaviorRelay.create()

    private var trackingIconSubscription: Subscription? = null

    override fun getTitle(): String? {
        return manga?.title
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = PagerControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        if (manga == null || source == null) return

        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 301)

        adapter = MangaDetailAdapter()
        binding.pager.offscreenPageLimit = 3
        binding.pager.adapter = adapter

        if (!fromSource) {
            binding.pager.currentItem = CHAPTERS_CONTROLLER
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            activity?.tabs?.setupWithViewPager(binding.pager)
            trackingIconSubscription = trackingIconRelay.subscribe { setTrackingIconInternal(it) }
        }
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (manga == null || source == null) {
            activity?.toast(R.string.manga_not_in_db)
            router.popController(this)
        }
    }

    override fun configureTabs(tabs: TabLayout) {
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_FIXED
        }
    }

    override fun cleanupTabs(tabs: TabLayout) {
        trackingIconSubscription?.unsubscribe()
        setTrackingIconInternal(false)
    }

    fun setTrackingIcon(visible: Boolean) {
        trackingIconRelay.call(visible)
    }

    private fun setTrackingIconInternal(visible: Boolean) {
        val tab = activity?.tabs?.getTabAt(TRACK_CONTROLLER) ?: return
        val drawable = if (visible) {
            VectorDrawableCompat.create(resources!!, R.drawable.ic_done_white_18dp, null)
        } else {
            null
        }

        tab.icon = drawable
    }

    private inner class MangaDetailAdapter : RouterPagerAdapter(this@MangaController) {

        private val tabCount = if (Injekt.get<TrackManager>().hasLoggedServices()) 3 else 2

        private val tabTitles = listOf(
            R.string.manga_detail_tab,
            R.string.manga_chapters_tab,
            R.string.manga_tracking_tab
        )
            .map { resources!!.getString(it) }

        override fun getCount(): Int {
            return tabCount
        }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val controller = when (position) {
                    INFO_CONTROLLER -> MangaInfoController(fromSource)
                    CHAPTERS_CONTROLLER -> ChaptersController()
                    TRACK_CONTROLLER -> TrackController()
                    else -> error("Wrong position $position")
                }
                router.setRoot(RouterTransaction.with(controller))
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return tabTitles[position]
        }
    }

    companion object {
        const val FROM_SOURCE_EXTRA = "from_source"
        const val MANGA_EXTRA = "manga"

        const val INFO_CONTROLLER = 0
        const val CHAPTERS_CONTROLLER = 1
        const val TRACK_CONTROLLER = 2
    }
}
