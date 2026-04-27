package com.timetrace

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.timetrace.data.repository.UnlockRepository
import com.timetrace.service.UnlockReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TimeTraceApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var unlockRepository: UnlockRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerReceiver(
            UnlockReceiver(unlockRepository),
            IntentFilter(Intent.ACTION_USER_PRESENT)
        )
    }
}
