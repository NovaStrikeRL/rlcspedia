package com.rlcspedia.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ContrastLevel { STANDARD, HIGH }

data class NotifyPrefs(
    val seriesStart: Boolean = true,
    val goals: Boolean = true,
    val gameEnd: Boolean = true,
    val seriesEnd: Boolean = true,
    val favoritesOnly: Boolean = false,
)

data class AppSettings(
    val dataSource: DataSource = DataSource.DEMO,
    val favorites: Set<String> = emptySet(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val contrast: ContrastLevel = ContrastLevel.STANDARD,
    val fontScale: Float = 1f,
    val legibleFont: Boolean = false,
    val reduceMotion: Boolean = false,
    val notify: NotifyPrefs = NotifyPrefs(),
)

/** Favoritos y ajustes persistidos en SharedPreferences, expuestos como StateFlow. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("rlcspedia", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    val value: AppSettings get() = _state.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        _state.value = next
        save(next)
    }

    fun toggleFavorite(id: String) = update {
        it.copy(favorites = if (id in it.favorites) it.favorites - id else it.favorites + id)
    }

    fun isFavorite(id: String) = id in _state.value.favorites

    private fun load() = AppSettings(
        dataSource = runCatching { DataSource.valueOf(prefs.getString("dataSource.v2", "DEMO")!!) }.getOrDefault(DataSource.DEMO),
        favorites = prefs.getStringSet("favorites", emptySet())!!.toSet(),
        theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM),
        dynamicColor = prefs.getBoolean("dynamicColor", false),
        contrast = runCatching { ContrastLevel.valueOf(prefs.getString("contrast", "STANDARD")!!) }.getOrDefault(ContrastLevel.STANDARD),
        fontScale = prefs.getFloat("fontScale", 1f),
        legibleFont = prefs.getBoolean("legibleFont", false),
        reduceMotion = prefs.getBoolean("reduceMotion", false),
        notify = NotifyPrefs(
            seriesStart = prefs.getBoolean("n.seriesStart", true),
            goals = prefs.getBoolean("n.goals", true),
            gameEnd = prefs.getBoolean("n.gameEnd", true),
            seriesEnd = prefs.getBoolean("n.seriesEnd", true),
            favoritesOnly = prefs.getBoolean("n.favoritesOnly", false),
        ),
    )

    private fun save(s: AppSettings) = prefs.edit {
        putString("dataSource.v2", s.dataSource.name)
        putStringSet("favorites", s.favorites)
        putString("theme", s.theme.name)
        putBoolean("dynamicColor", s.dynamicColor)
        putString("contrast", s.contrast.name)
        putFloat("fontScale", s.fontScale)
        putBoolean("legibleFont", s.legibleFont)
        putBoolean("reduceMotion", s.reduceMotion)
        putBoolean("n.seriesStart", s.notify.seriesStart)
        putBoolean("n.goals", s.notify.goals)
        putBoolean("n.gameEnd", s.notify.gameEnd)
        putBoolean("n.seriesEnd", s.notify.seriesEnd)
        putBoolean("n.favoritesOnly", s.notify.favoritesOnly)
    }
}
