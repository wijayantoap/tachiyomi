package eu.kanade.tachiyomi.ui.setting

import android.os.Build
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.hasDisplayCutout

class SettingsReaderController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_reader

        preferenceCategory {
            titleRes = R.string.pref_category_general

            intListPreference {
                key = Keys.defaultViewer
                titleRes = R.string.pref_viewer_type
                entriesRes = arrayOf(
                    R.string.left_to_right_viewer, R.string.right_to_left_viewer,
                    R.string.vertical_viewer, R.string.webtoon_viewer, R.string.vertical_plus_viewer
                )
                entryValues = arrayOf("1", "2", "3", "4", "5")
                defaultValue = "1"
                summary = "%s"
            }
            intListPreference {
                key = Keys.rotation
                titleRes = R.string.pref_rotation_type
                entriesRes = arrayOf(
                    R.string.rotation_free, R.string.rotation_lock,
                    R.string.rotation_force_portrait, R.string.rotation_force_landscape
                )
                entryValues = arrayOf("1", "2", "3", "4")
                defaultValue = "1"
                summary = "%s"
            }
            intListPreference {
                key = Keys.readerTheme
                titleRes = R.string.pref_reader_theme
                entriesRes = arrayOf(R.string.black_background, R.string.gray_background, R.string.white_background)
                entryValues = arrayOf("1", "2", "0")
                defaultValue = "1"
                summary = "%s"
            }
            intListPreference {
                key = Keys.doubleTapAnimationSpeed
                titleRes = R.string.pref_double_tap_anim_speed
                entries = arrayOf(context.getString(R.string.double_tap_anim_speed_0), context.getString(R.string.double_tap_anim_speed_fast), context.getString(R.string.double_tap_anim_speed_normal))
                entryValues = arrayOf("1", "250", "500") // using a value of 0 breaks the image viewer, so min is 1
                defaultValue = "500"
                summary = "%s"
            }
            switchPreference {
                key = Keys.fullscreen
                titleRes = R.string.pref_fullscreen
                defaultValue = true
            }

            if (activity?.hasDisplayCutout() == true) {
                switchPreference {
                    key = Keys.cutoutShort
                    titleRes = R.string.pref_cutout_short
                    defaultValue = true
                }
            }

            switchPreference {
                key = Keys.keepScreenOn
                titleRes = R.string.pref_keep_screen_on
                defaultValue = true
            }
            switchPreference {
                key = Keys.showPageNumber
                titleRes = R.string.pref_show_page_number
                defaultValue = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                switchPreference {
                    key = Keys.trueColor
                    titleRes = R.string.pref_true_color
                    summaryRes = R.string.pref_true_color_summary
                    defaultValue = false
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_reading

            switchPreference {
                key = Keys.skipRead
                titleRes = R.string.pref_skip_read_chapters
                defaultValue = false
            }
            switchPreference {
                key = Keys.skipFiltered
                titleRes = R.string.pref_skip_filtered_chapters
                defaultValue = true
            }
            switchPreference {
                key = Keys.alwaysShowChapterTransition
                titleRes = R.string.pref_always_show_chapter_transition
                defaultValue = true
            }
        }

        preferenceCategory {
            titleRes = R.string.pager_viewer

            intListPreference {
                key = Keys.imageScaleType
                titleRes = R.string.pref_image_scale_type
                entriesRes = arrayOf(
                    R.string.scale_type_fit_screen, R.string.scale_type_stretch,
                    R.string.scale_type_fit_width, R.string.scale_type_fit_height,
                    R.string.scale_type_original_size, R.string.scale_type_smart_fit
                )
                entryValues = arrayOf("1", "2", "3", "4", "5", "6")
                defaultValue = "1"
                summary = "%s"
            }
            intListPreference {
                key = Keys.zoomStart
                titleRes = R.string.pref_zoom_start
                entriesRes = arrayOf(
                    R.string.zoom_start_automatic, R.string.zoom_start_left,
                    R.string.zoom_start_right, R.string.zoom_start_center
                )
                entryValues = arrayOf("1", "2", "3", "4")
                defaultValue = "1"
                summary = "%s"
            }
            switchPreference {
                key = Keys.enableTransitions
                titleRes = R.string.pref_page_transitions
                defaultValue = true
            }
            switchPreference {
                key = Keys.cropBorders
                titleRes = R.string.pref_crop_borders
                defaultValue = false
            }
        }

        preferenceCategory {
            titleRes = R.string.webtoon_viewer

            switchPreference {
                key = Keys.cropBordersWebtoon
                titleRes = R.string.pref_crop_borders
                defaultValue = false
            }

            intListPreference {
                key = Keys.webtoonSidePadding
                titleRes = R.string.pref_webtoon_side_padding
                entriesRes = arrayOf(
                    R.string.webtoon_side_padding_0,
                    R.string.webtoon_side_padding_10,
                    R.string.webtoon_side_padding_15,
                    R.string.webtoon_side_padding_20,
                    R.string.webtoon_side_padding_25
                )
                entryValues = arrayOf("0", "10", "15", "20", "25")
                defaultValue = "0"
                summary = "%s"
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_reader_navigation

            switchPreference {
                key = Keys.readWithTapping
                titleRes = R.string.pref_read_with_tapping
                defaultValue = true
            }
            switchPreference {
                key = Keys.readWithLongTap
                titleRes = R.string.pref_read_with_long_tap
                defaultValue = true
            }
            switchPreference {
                key = Keys.readWithVolumeKeys
                titleRes = R.string.pref_read_with_volume_keys
                defaultValue = false
            }
            switchPreference {
                key = Keys.readWithVolumeKeysInverted
                titleRes = R.string.pref_read_with_volume_keys_inverted
                defaultValue = false
            }.apply { dependency = Keys.readWithVolumeKeys }
        }
    }
}
