package com.example.service

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FocusGuardApplication
import com.example.ui.components.PinEntryDialog
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class BlockingOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: "Blocked App"
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This Application"
        val limitMins = intent.getIntExtra(EXTRA_LIMIT_MINS, 30)
        val usedMins = intent.getIntExtra(EXTRA_USED_MINS, limitMins)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                BlockingOverlayContent(
                    appName = appName,
                    packageName = blockedPackage,
                    limitMins = limitMins,
                    usedMins = usedMins,
                    onGoHome = { navigateToHome() },
                    onTempUnlockSuccess = { unlockMins ->
                        tempUnlockApp(blockedPackage, unlockMins)
                    }
                )
            }
        }
    }

    private fun navigateToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun tempUnlockApp(packageName: String, minutes: Int) {
        val app = FocusGuardApplication.instance
        val scope = CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            app.appLimitRepository.tempUnlockForMinutes(packageName, minutes)
            runOnUiThread {
                Toast.makeText(this@BlockingOverlayActivity, "Unlocked for $minutes minutes!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onBackPressed() {
        navigateToHome()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_LIMIT_MINS = "extra_limit_mins"
        const val EXTRA_USED_MINS = "extra_used_mins"
    }
}

@Composable
fun BlockingOverlayContent(
    appName: String,
    packageName: String,
    limitMins: Int,
    usedMins: Int,
    onGoHome: () -> Unit,
    onTempUnlockSuccess: (Int) -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    val preferences = FocusGuardApplication.instance.userPreferencesRepository

    val quotes = listOf(
        "\"Focus is a muscle. Every time you resist distraction, you build mental strength.\"",
        "\"You have reached your daily limit for $appName. Take a breath and focus on what matters.\"",
        "\"Small daily choices determine your future. Stay in control.\"",
        "\"Disconnect to reconnect with your goals.\""
    )
    val quote = remember { quotes.random() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Deep twilight background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E1B4B),
                            Color(0xFF020617)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Shield / Lock Hero Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF312E81)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Focus Shield",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Daily Limit Reached",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF87171),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Usage pill card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Used $usedMins mins today (Limit: $limitMins mins)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Motivational Quote Box
                Text(
                    text = quote,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Buttons
                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Return to Home Screen", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showPinDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1))
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFFA5B4FC))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with PIN", fontSize = 16.sp, color = Color(0xFFA5B4FC))
                }
            }
        }
    }

    if (showPinDialog) {
        PinEntryDialog(
            title = "Enter PIN to Unlock $appName",
            onVerifyPin = { pin -> preferences.verifyPin(pin) },
            onDismiss = { showPinDialog = false },
            onSuccess = {
                showPinDialog = false
                onTempUnlockSuccess(15) // Grant 15 mins temporary access
            }
        )
    }
}
