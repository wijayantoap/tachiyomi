package eu.kanade.tachiyomi

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.LocaleHelper
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraHttpSender
import org.acra.sender.HttpSender
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.injectLazy
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@AcraCore(
    buildConfigClass = BuildConfig::class,
    excludeMatchingSharedPreferencesKeys = [".*username.*", ".*password.*", ".*token.*"]
)
@AcraHttpSender(
    uri = "https://tachiyomi.kanade.eu/crash_report",
    httpMethod = HttpSender.Method.PUT
)
open class App : Application(), LifecycleObserver {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        Injekt = InjektScope(DefaultRegistrar())
        Injekt.importModule(AppModule(this))

        setupAcra()
        setupNotificationChannels()

        LocaleHelper.updateConfiguration(this, resources.configuration)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.updateConfiguration(this, newConfig, true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @Suppress("unused")
    fun onAppBackgrounded() {
        val preferences: PreferencesHelper by injectLazy()
        if (preferences.lockAppAfter().get() >= 0) {
            SecureActivityDelegate.locked = true
        }
    }

    protected open fun setupAcra() {
        ACRA.init(this)
    }

    protected open fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }
}
