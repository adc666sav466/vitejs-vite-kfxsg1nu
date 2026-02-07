package com.example.studybet

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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class TimerService : Service() {
    private val serviceJob: Job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var loopJob: Job? = null
    private val random = Random.Default

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val difficultyName = intent.getStringExtra(EXTRA_DIFFICULTY)
                val difficulty = difficultyName?.let { Difficulty.valueOf(it) } ?: Difficulty.EASY
                startForegroundWithNotification("Study Bet 计时中")
                startLoop(difficulty)
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startLoop(difficulty: Difficulty) {
        loopJob?.cancel()
        loopJob = serviceScope.launch {
            while (isActive) {
                val studyMinutes = when (difficulty) {
                    Difficulty.EASY -> random.nextInt(25, 46)
                    Difficulty.NORMAL -> random.nextInt(40, 61)
                    Difficulty.HARD -> random.nextInt(60, 91)
                }
                notifyStage("开始学习：$studyMinutes 分钟", false)
                TimerRepository.update(SessionState(SessionPhase.STUDY, difficulty))
                delay(studyMinutes.minutes.toJavaDuration())

                val restMinutes = when (difficulty) {
                    Difficulty.EASY -> random.nextInt(5, 16)
                    Difficulty.NORMAL -> if (random.nextInt(100) < 20) 0 else random.nextInt(5, 21)
                    Difficulty.HARD -> if (random.nextInt(100) < 40) 0 else random.nextInt(5, 31)
                }

                if (restMinutes == 0) {
                    notifyStage("运气不好！立刻继续学习！", true)
                    TimerRepository.update(SessionState(SessionPhase.STUDY, difficulty))
                    continue
                }

                notifyStage("休息开始：$restMinutes 分钟", true)
                TimerRepository.update(SessionState(SessionPhase.REST, difficulty))
                delay(restMinutes.minutes.toJavaDuration())
            }
        }
    }

    private fun startForegroundWithNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Study Bet",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableVibration(true)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
        val notification = buildNotification(message)
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun notifyStage(message: String, alert: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(message, alert)
        notificationManager.notify(STAGE_NOTIFICATION_ID, notification)
    }

    private fun buildNotification(message: String, alert: Boolean = true): Notification {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Study Bet")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)

        if (alert) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }
        return builder.build()
    }

    companion object {
        const val ACTION_START = "com.example.studybet.action.START"
        const val ACTION_STOP = "com.example.studybet.action.STOP"
        const val EXTRA_DIFFICULTY = "extra_difficulty"
        const val NOTIFICATION_CHANNEL_ID = "study_bet_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val STAGE_NOTIFICATION_ID = 1002
    }
}
