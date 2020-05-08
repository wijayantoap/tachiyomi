package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.multiSelectListPreference
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.getFilePicker
import java.io.File
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsDownloadController : SettingsController() {

    private val db: DatabaseHelper by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_downloads

        preference {
            key = Keys.downloadsDirectory
            titleRes = R.string.pref_download_directory
            onClick {
                val ctrl = DownloadDirectoriesDialog()
                ctrl.targetController = this@SettingsDownloadController
                ctrl.showDialog(router)
            }

            preferences.downloadsDirectory().asFlow()
                .onEach { path ->
                    val dir = UniFile.fromUri(context, Uri.parse(path))
                    summary = dir.filePath ?: path
                }
                .launchIn(scope)
        }
        switchPreference {
            key = Keys.downloadOnlyOverWifi
            titleRes = R.string.pref_download_only_over_wifi
            defaultValue = true
        }
        preferenceCategory {
            titleRes = R.string.pref_remove_after_read

            switchPreference {
                key = Keys.removeAfterMarkedAsRead
                titleRes = R.string.pref_remove_after_marked_as_read
                defaultValue = false
            }
            intListPreference {
                key = Keys.removeAfterReadSlots
                titleRes = R.string.pref_remove_after_read
                entriesRes = arrayOf(
                    R.string.disabled, R.string.last_read_chapter,
                    R.string.second_to_last, R.string.third_to_last, R.string.fourth_to_last,
                    R.string.fifth_to_last
                )
                entryValues = arrayOf("-1", "0", "1", "2", "3", "4")
                defaultValue = "-1"
                summary = "%s"
            }
        }

        val dbCategories = db.getCategories().executeAsBlocking()
        val categories = listOf(Category.createDefault()) + dbCategories

        preferenceCategory {
            titleRes = R.string.pref_download_new

            switchPreference {
                key = Keys.downloadNew
                titleRes = R.string.pref_download_new
                defaultValue = false
            }
            multiSelectListPreference {
                key = Keys.downloadNewCategories
                titleRes = R.string.pref_download_new_categories
                entries = categories.map { it.name }.toTypedArray()
                entryValues = categories.map { it.id.toString() }.toTypedArray()

                preferences.downloadNew().asImmediateFlow { isVisible = it }
                    .launchIn(scope)

                preferences.downloadNewCategories().asFlow()
                    .onEach { mutableSet ->
                        val selectedCategories = mutableSet
                            .mapNotNull { id -> categories.find { it.id == id.toInt() } }
                            .sortedBy { it.order }

                        summary = if (selectedCategories.isEmpty()) {
                            resources?.getString(R.string.all)
                        } else {
                            selectedCategories.joinToString { it.name }
                        }
                    }
                    .launchIn(scope)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            DOWNLOAD_DIR -> if (data != null && resultCode == Activity.RESULT_OK) {
                val context = applicationContext ?: return
                val uri = data.data
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                if (uri != null) {
                    @Suppress("NewApi")
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                }

                val file = UniFile.fromUri(context, uri)
                preferences.downloadsDirectory().set(file.uri.toString())
            }
        }
    }

    fun predefinedDirectorySelected(selectedDir: String) {
        val path = Uri.fromFile(File(selectedDir))
        preferences.downloadsDirectory().set(path.toString())
    }

    fun customDirectorySelected(currentDir: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        try {
            startActivityForResult(intent, DOWNLOAD_DIR)
        } catch (e: ActivityNotFoundException) {
            startActivityForResult(preferences.context.getFilePicker(currentDir), DOWNLOAD_DIR)
        }
    }

    class DownloadDirectoriesDialog : DialogController() {

        private val preferences: PreferencesHelper = Injekt.get()

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!
            val currentDir = preferences.downloadsDirectory().get()
            val externalDirs = (getExternalDirs() + File(activity.getString(R.string.custom_dir))).map(File::toString)
            val selectedIndex = externalDirs.indexOfFirst { it in currentDir }

            return MaterialDialog(activity)
                .listItemsSingleChoice(
                    items = externalDirs,
                    initialSelection = selectedIndex
                ) { _, position, text ->
                    val target = targetController as? SettingsDownloadController
                    if (position == externalDirs.lastIndex) {
                        target?.customDirectorySelected(currentDir)
                    } else {
                        target?.predefinedDirectorySelected(text.toString())
                    }
                }
        }

        private fun getExternalDirs(): List<File> {
            val defaultDir = Environment.getExternalStorageDirectory().absolutePath +
                File.separator + resources?.getString(R.string.app_name) +
                File.separator + "downloads"

            return mutableListOf(File(defaultDir)) +
                ContextCompat.getExternalFilesDirs(activity!!, "").filterNotNull()
        }
    }

    private companion object {
        const val DOWNLOAD_DIR = 104
    }
}
