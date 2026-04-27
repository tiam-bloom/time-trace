package com.timetrace.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timetrace.data.local.entity.UnlockRecordEntity
import com.timetrace.data.repository.UnlockRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class UnlockReceiver : BroadcastReceiver() {

    @Inject
    lateinit var unlockRepository: UnlockRepository

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val record = UnlockRecordEntity(
                    timestamp = now,
                    date = dateFormat.format(Date(now))
                )
                unlockRepository.insertRecord(record)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
