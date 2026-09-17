package com.example.simpledocumentscanner.ui

import android.content.Context

data class AppSettings(
    val themeColor: ScannerThemeColor = ScannerThemeColor.OCEAN,
    val customThemeColor: Int? = null,
    val displayMode: DisplayMode = DisplayMode.SYSTEM,
)

enum class DisplayMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val savedTheme = preferences.getString(KEY_THEME_COLOR, null)
        val themeColor = savedTheme
            ?.let { value -> ScannerThemeColor.entries.find { it.name == value } }
            ?: ScannerThemeColor.OCEAN
        val displayMode = preferences.getString(KEY_DISPLAY_MODE, null)
            ?.let { value -> DisplayMode.entries.find { it.name == value } }
            ?: if (preferences.contains(KEY_DARK_MODE)) {
                if (preferences.getBoolean(KEY_DARK_MODE, false)) DisplayMode.DARK else DisplayMode.LIGHT
            } else {
                DisplayMode.SYSTEM
            }
        return AppSettings(
            themeColor = themeColor,
            customThemeColor = if (preferences.contains(KEY_CUSTOM_THEME_COLOR)) {
                preferences.getInt(KEY_CUSTOM_THEME_COLOR, 0)
            } else {
                null
            },
            displayMode = displayMode,
        )
    }

    fun save(settings: AppSettings) {
        preferences.edit()
            .putString(KEY_THEME_COLOR, settings.themeColor.name)
            .putString(KEY_DISPLAY_MODE, settings.displayMode.name)
            .apply {
                if (settings.customThemeColor == null) {
                    remove(KEY_CUSTOM_THEME_COLOR)
                } else {
                    putInt(KEY_CUSTOM_THEME_COLOR, settings.customThemeColor)
                }
            }
            .remove(KEY_DARK_MODE)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_CUSTOM_THEME_COLOR = "custom_theme_color"
        const val KEY_DISPLAY_MODE = "display_mode"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
