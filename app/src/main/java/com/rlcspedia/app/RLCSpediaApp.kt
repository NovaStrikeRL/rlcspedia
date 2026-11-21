package com.rlcspedia.app

import android.app.Application
import com.rlcspedia.app.data.DataSource
import com.rlcspedia.app.data.FeedWorker
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.SettingsStore
import com.rlcspedia.app.notify.Notifier
import com.rlcspedia.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RLCSpediaApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsStore(this)
        val notifier = Notifier(this, settings)
        notifier.createChannels()

        appScope.launch { LiveRepository.events.collect { notifier.onEvent(it) } }
        LiveRepository.init(this, appScope, settings.value.dataSource)
        WidgetUpdater.start(this, appScope, settings)
        applyBackgroundWork(settings.value.dataSource)
        // Cambiar de fuente desde Ajustes reinicia el repositorio
        appScope.launch {
            settings.state.map { it.dataSource }.distinctUntilChanged().drop(1).collect {
                LiveRepository.setSource(it); applyBackgroundWork(it)
            }
        }
    }

    private fun applyBackgroundWork(source: DataSource) =
        if (source == DataSource.LIQUIPEDIA) FeedWorker.schedule(this) else FeedWorker.cancel(this)

    companion object {
        lateinit var instance: RLCSpediaApp
            private set
    }
}
