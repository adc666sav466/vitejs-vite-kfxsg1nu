package com.studybet

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var statusText by remember { mutableStateOf("请选择难度开始") }
                var currentDifficulty by remember { mutableStateOf("-") }
                val context = this

                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            if (intent?.action == TimerService.ACTION_STATUS_UPDATE) {
                                statusText = intent.getStringExtra(TimerService.EXTRA_STATUS_TEXT)
                                    ?: statusText
                                currentDifficulty = intent.getStringExtra(TimerService.EXTRA_DIFFICULTY)
                                    ?: currentDifficulty
                            }
                        }
                    }
                    val filter = IntentFilter(TimerService.ACTION_STATUS_UPDATE)
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    onDispose {
                        context.unregisterReceiver(receiver)
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPermission) {
                            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Study Bet",
                        color = Color(0xFF00FF7F),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "当前难度：$currentDifficulty",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { startTimer(context, Difficulty.EASY) },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF7F))
                    ) {
                        Text(text = "轻松", color = Color.Black, fontSize = 20.sp)
                    }
                    Button(
                        onClick = { startTimer(context, Difficulty.NORMAL) },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = "中等", color = Color.Black, fontSize = 20.sp)
                    }
                    Button(
                        onClick = { startTimer(context, Difficulty.HARD) },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F00FF))
                    ) {
                        Text(text = "困难", color = Color.White, fontSize = 20.sp)
                    }
                }
            }
        }
    }

    private fun startTimer(context: Context, difficulty: Difficulty) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DIFFICULTY, difficulty.displayName)
        }
        ContextCompat.startForegroundService(context, intent)
    }
}

enum class Difficulty(val displayName: String) {
    EASY("轻松"),
    NORMAL("中等"),
    HARD("困难")
}
