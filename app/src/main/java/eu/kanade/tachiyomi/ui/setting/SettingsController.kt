package eu.kanade.tachiyomi.ui.setting

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceController
import androidx.preference.PreferenceScreen
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import rx.Observable
import rx.Subscription
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class SettingsController : PreferenceController() {

    val preferences: PreferencesHelper = Injekt.get()
    val scope = CoroutineScope(Job() + Dispatchers.Main)

    var untilDestroySubscriptions = CompositeSubscription()
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        if (untilDestroySubscriptions.isUnsubscribed) {
            untilDestroySubscriptions = CompositeSubscription()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        untilDestroySubscriptions.unsubscribe()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(getThemedContext())
        preferenceScreen = screen
        setupPreferenceScreen(screen)
    }

    abstract fun setupPreferenceScreen(screen: PreferenceScreen): Any?

    private fun getThemedContext(): Context {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(activity, tv.resourceId)
    }

    open fun getTitle(): String? {
        return preferenceScreen?.title?.toString()
    }

    fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController<*> && parentController.getTitle() != null) {
                return
            }
            parentController = parentController.parentController
        }

        (activity as? AppCompatActivity)?.supportActionBar?.title = getTitle()
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        if (type.isEnter) {
            setTitle()
        }
        setHasOptionsMenu(type.isEnter)
        super.onChangeStarted(handler, type)
    }

    fun <T> Observable<T>.subscribeUntilDestroy(onNext: (T) -> Unit): Subscription {
        return subscribe(onNext).also { untilDestroySubscriptions.add(it) }
    }
}
