package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.multiSelectListPreference
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import kotlinx.android.synthetic.main.pref_library_columns.view.landscape_columns
import kotlinx.android.synthetic.main.pref_library_columns.view.portrait_columns
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsLibraryController : SettingsController() {

    private val db: DatabaseHelper = Injekt.get()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_library

        preferenceCategory {
            titleRes = R.string.pref_category_display

            preference {
                titleRes = R.string.pref_library_columns
                onClick {
                    LibraryColumnsDialog().showDialog(router)
                }

                fun getColumnValue(value: Int): String {
                    return if (value == 0) {
                        context.getString(R.string.default_columns)
                    } else {
                        value.toString()
                    }
                }

                Observable.combineLatest(
                    preferences.portraitColumns().asObservable(),
                    preferences.landscapeColumns().asObservable()
                ) { portraitCols, landscapeCols -> Pair(portraitCols, landscapeCols) }
                    .subscribeUntilDestroy { (portraitCols, landscapeCols) ->
                        val portrait = getColumnValue(portraitCols)
                        val landscape = getColumnValue(landscapeCols)
                        summary = "${context.getString(R.string.portrait)}: $portrait, " +
                            "${context.getString(R.string.landscape)}: $landscape"
                    }
            }
        }

        val dbCategories = db.getCategories().executeAsBlocking()
        val categories = listOf(Category.createDefault()) + dbCategories

        preferenceCategory {
            titleRes = R.string.pref_category_library_update

            intListPreference {
                key = Keys.libraryUpdateInterval
                titleRes = R.string.pref_library_update_interval
                entriesRes = arrayOf(
                    R.string.update_never, R.string.update_1hour,
                    R.string.update_2hour, R.string.update_3hour, R.string.update_6hour,
                    R.string.update_12hour, R.string.update_24hour, R.string.update_48hour
                )
                entryValues = arrayOf("0", "1", "2", "3", "6", "12", "24", "48")
                defaultValue = "24"
                summary = "%s"

                onChange { newValue ->
                    val interval = (newValue as String).toInt()
                    LibraryUpdateJob.setupTask(context, interval)
                    true
                }
            }
            multiSelectListPreference {
                key = Keys.libraryUpdateRestriction
                titleRes = R.string.pref_library_update_restriction
                entriesRes = arrayOf(R.string.wifi, R.string.charging)
                entryValues = arrayOf("wifi", "ac")
                summaryRes = R.string.pref_library_update_restriction_summary
                defaultValue = setOf("wifi")

                preferences.libraryUpdateInterval().asImmediateFlow { isVisible = it > 0 }
                    .launchIn(scope)

                onChange {
                    // Post to event looper to allow the preference to be updated.
                    Handler().post { LibraryUpdateJob.setupTask(context) }
                    true
                }
            }
            switchPreference {
                key = Keys.updateOnlyNonCompleted
                titleRes = R.string.pref_update_only_non_completed
                defaultValue = false
            }
            multiSelectListPreference {
                key = Keys.libraryUpdateCategories
                titleRes = R.string.pref_library_update_categories
                entries = categories.map { it.name }.toTypedArray()
                entryValues = categories.map { it.id.toString() }.toTypedArray()
                preferences.libraryUpdateCategories().asFlow()
                    .onEach { mutableSet ->
                        val selectedCategories = mutableSet
                            .mapNotNull { id -> categories.find { it.id == id.toInt() } }
                            .sortedBy { it.order }

                        summary = if (selectedCategories.isEmpty()) {
                            context.getString(R.string.all)
                        } else {
                            selectedCategories.joinToString { it.name }
                        }
                    }
                    .launchIn(scope)
            }
            intListPreference {
                key = Keys.libraryUpdatePrioritization
                titleRes = R.string.pref_library_update_prioritization

                // The following array lines up with the list rankingScheme in:
                // ../../data/library/LibraryUpdateRanker.kt
                val priorities = arrayOf(
                    Pair("0", R.string.action_sort_alpha),
                    Pair("1", R.string.action_sort_last_checked)
                )
                val defaultPriority = priorities[0]

                entriesRes = priorities.map { it.second }.toTypedArray()
                entryValues = priorities.map { it.first }.toTypedArray()
                defaultValue = defaultPriority.first

                val selectedPriority = priorities.find { it.first.toInt() == preferences.libraryUpdatePrioritization().get() }
                summaryRes = selectedPriority?.second ?: defaultPriority.second
                onChange { newValue ->
                    summaryRes = priorities.find {
                        it.first == (newValue as String)
                    }?.second ?: defaultPriority.second
                    true
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_library_categories

            preference {
                titleRes = R.string.action_edit_categories

                val catCount = db.getCategories().executeAsBlocking().size
                summary = context.resources.getQuantityString(R.plurals.num_categories, catCount, catCount)

                onClick {
                    router.pushController(CategoryController().withFadeTransaction())
                }
            }

            intListPreference {
                key = Keys.defaultCategory
                titleRes = R.string.default_category

                entries = arrayOf(context.getString(R.string.default_category_summary)) +
                    categories.map { it.name }.toTypedArray()
                entryValues = arrayOf("-1") + categories.map { it.id.toString() }.toTypedArray()
                defaultValue = "-1"

                val selectedCategory = categories.find { it.id == preferences.defaultCategory() }
                summary = selectedCategory?.name
                    ?: context.getString(R.string.default_category_summary)
                onChange { newValue ->
                    summary = categories.find {
                        it.id == (newValue as String).toInt()
                    }?.name ?: context.getString(R.string.default_category_summary)
                    true
                }
            }
        }
    }

    class LibraryColumnsDialog : DialogController() {

        private val preferences: PreferencesHelper = Injekt.get()

        private var portrait = preferences.portraitColumns().getOrDefault()
        private var landscape = preferences.landscapeColumns().getOrDefault()

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val dialog = MaterialDialog(activity!!)
                .title(R.string.pref_library_columns)
                .customView(R.layout.pref_library_columns, horizontalPadding = true)
                .positiveButton(android.R.string.ok) {
                    preferences.portraitColumns().set(portrait)
                    preferences.landscapeColumns().set(landscape)
                }
                .negativeButton(android.R.string.cancel)

            onViewCreated(dialog.view)
            return dialog
        }

        fun onViewCreated(view: View) {
            with(view.portrait_columns) {
                displayedValues = arrayOf(context.getString(R.string.default_columns)) +
                    IntRange(1, 10).map(Int::toString)
                value = portrait

                setOnValueChangedListener { _, _, newValue ->
                    portrait = newValue
                }
            }
            with(view.landscape_columns) {
                displayedValues = arrayOf(context.getString(R.string.default_columns)) +
                    IntRange(1, 10).map(Int::toString)
                value = landscape

                setOnValueChangedListener { _, _, newValue ->
                    landscape = newValue
                }
            }
        }
    }
}
