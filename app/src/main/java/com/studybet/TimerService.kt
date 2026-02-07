package com.studybet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class TimerService : Service() {
    private val serviceJob: Job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var activeDifficulty: String = "-"
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            activeDifficulty = intent.getStringExtra(EXTRA_DIFFICULTY) ?: "-"
            startForeground(NOTIFICATION_ID, buildOngoingNotification("准备中..."))
            loopJob?.cancel()
            loopJob = serviceScope.launch {
                runBetLoop(activeDifficulty)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    private suspend fun runBetLoop(difficulty: String) {
        while (serviceScope.isActive) {
            val studyMinutes = randomStudyMinutes(difficulty)
            updateStatus("正在学习中...", difficulty, notify = true)
            delay(minutesToMillis(studyMinutes))

            val restMinutes = randomRestMinutes(difficulty)
            if (restMinutes == 0) {
                sendAlert("运气不好！立刻继续学习！")
                updateStatus("正在学习中...", difficulty, notify = true)
                continue
            }
            updateStatus("休息时间！", difficulty, notify = true)
            delay(minutesToMillis(restMinutes))
            sendAlert("休息结束，继续学习！")
        }
    }

    private fun updateStatus(status: String, difficulty: String, notify: Boolean) {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_STATUS_TEXT, status)
            putExtra(EXTRA_DIFFICULTY, difficulty)
        }
        sendBroadcast(intent)
        if (notify) {
            val notification = buildOngoingNotification(status)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
            sendAlert(status)
        }
    }

    private fun buildOngoingNotification(content: String): Notification {
        createNotificationChannelIfNeeded()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Study Bet")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun sendAlert(message: String) {
        createNotificationChannelIfNeeded()
        val alert = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Study Bet")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Random.nextInt(1000, 9999), alert)
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Study Bet",
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.description = "Study Bet timer alerts"
                channel.enableVibration(true)
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun randomStudyMinutes(difficulty: String): Int = when (difficulty) {
        Difficulty.EASY.displayName -> Random.nextInt(25, 46)
        Difficulty.NORMAL.displayName -> Random.nextInt(40, 61)
        Difficulty.HARD.displayName -> Random.nextInt(60, 91)
        else -> Random.nextInt(25, 46)
    }

    private fun randomRestMinutes(difficulty: String): Int = when (difficulty) {
        Difficulty.EASY.displayName -> Random.nextInt(5, 16)
        Difficulty.NORMAL.displayName -> if (Random.nextInt(100) < 20) 0 else Random.nextInt(5, 21)
        Difficulty.HARD.displayName -> if (Random.nextInt(100) < 40) 0 else Random.nextInt(5, 31)
        else -> Random.nextInt(5, 16)
    }

    private fun minutesToMillis(minutes: Int): Long = minutes * 60_000L

    companion object {
        const val ACTION_START = "com.studybet.action.START"
        const val ACTION_STATUS_UPDATE = "com.studybet.action.STATUS_UPDATE"
        const val EXTRA_DIFFICULTY = "com.studybet.extra.DIFFICULTY"
        const val EXTRA_STATUS_TEXT = "com.studybet.extra.STATUS_TEXT"
        private const val CHANNEL_ID = "study_bet_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
