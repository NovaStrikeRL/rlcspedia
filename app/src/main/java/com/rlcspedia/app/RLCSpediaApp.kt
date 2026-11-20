package com.rlcspedia.app

import android.app.Application
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.SettingsStore
import com.rlcspedia.app.notify.Notifier
import com.rlcspedia.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

        LiveRepository.start(appScope)
        appScope.launch { LiveRepository.events.collect { notifier.onEvent(it) } }
        WidgetUpdater.start(this, appScope, settings)
    }

    companion object {
        lateinit var instance: RLCSpediaApp
            private set
    }
}
