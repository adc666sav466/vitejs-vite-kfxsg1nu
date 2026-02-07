package com.example.studybet

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyBetApp(onStart = { difficulty ->
                startStudyService(difficulty)
            })
        }
    }

    private fun startStudyService(difficulty: Difficulty) {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DIFFICULTY, difficulty.name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
private fun StudyBetApp(onStart: (Difficulty) -> Unit) {
    val state by TimerRepository.state.collectAsState()
    val statusText = when (state.phase) {
        SessionPhase.IDLE -> "请选择难度开始"
        SessionPhase.STUDY -> "正在学习中..."
        SessionPhase.REST -> "休息时间！"
    }
    val difficultyText = when (state.difficulty) {
        Difficulty.EASY -> "轻松模式"
        Difficulty.NORMAL -> "中等模式"
        Difficulty.HARD -> "困难模式"
        null -> ""
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = statusText,
                color = Color.White,
                fontSize = 24.sp,
            )
            if (difficultyText.isNotBlank()) {
                Text(
                    text = difficultyText,
                    color = Color(0xFF39FF14),
                    fontSize = 20.sp,
                )
            }

            DifficultyButton(
                label = "轻松",
                onClick = {
                    maybeRequestNotificationPermission()
                    onStart(Difficulty.EASY)
                },
            )
            DifficultyButton(
                label = "中等",
                onClick = {
                    maybeRequestNotificationPermission()
                    onStart(Difficulty.NORMAL)
                },
            )
            DifficultyButton(
                label = "困难",
                onClick = {
                    maybeRequestNotificationPermission()
                    onStart(Difficulty.HARD)
                },
            )
        }
    }
}

@Composable
private fun DifficultyButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1B1B1B),
            contentColor = Color.White,
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
