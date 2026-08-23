package com.estatenestora.app.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.R
import com.estatenestora.app.data.telegram.TdLibManager
import com.estatenestora.app.ui.theme.*
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.estatenestora.app.data.telegram.SmsReceiver
import com.estatenestora.app.data.telegram.OtpNotificationListener


// ─── ROOT ENTRY POINT ────────────────────────────────────────────────────────
@Composable
fun TelegramAuthScreen(authState: TdLibManager.AuthState, onSkip: () -> Unit) {
    var splashShown by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = !splashShown,
        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
        label = "auth_transition"
    ) { showSplash ->
        if (showSplash) {
            NestoraSplashScreen(onContinue = { splashShown = true })
        } else {
            NestoraAuthFlow(
                authState = authState,
                onSkip = onSkip,
                onBackToWelcome = { splashShown = false }
            )
        }
    }
}

// ─── SPLASH / WELCOME SCREEN (2-screen flow inspired by Swiggy) ────────────────
@Composable
fun NestoraSplashScreen(onContinue: () -> Unit) {
    var currentScreen by remember { mutableStateOf(0) }

    // Auto transition Screen 1 (White Logo) -> Screen 2 (Green Info) after 2 seconds
    LaunchedEffect(currentScreen) {
        if (currentScreen == 0) {
            kotlinx.coroutines.delay(2000)
            currentScreen = 1
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(tween(500)) togetherWith fadeOut(tween(400))
        },
        label = "splash_screen_transition"
    ) { screen ->
        if (screen == 0) {
            // SCREEN 1: SPLASH (Pure White background)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .clickable { currentScreen = 1 },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nestora_logo),
                        contentDescription = "Nestora Logo",
                        modifier = Modifier.size(190.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "NESTORA",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00966B),
                        letterSpacing = 3.sp
                    )
                }
            }
        } else {
            // SCREEN 2: ONBOARDING (Nestora Mint background)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NestoraMint)
            ) {
                // Background decorative circles
                Box(
                    modifier = Modifier
                        .size(340.dp)
                        .offset(x = (-100).dp, y = (-100).dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 80.dp, y = 80.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top logo + NESTORA wordmark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 56.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.nestora_logo),
                            contentDescription = "Nestora Logo White",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "NESTORA",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.5.sp
                        )
                    }

                    // Center context
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Hyperlocal Services\nat Doorstep",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 44.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Find verified flats, rooms, and local services in your area.",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(28.dp))

                        // Bullets in card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OnboardingBullet("🏠", "Verified Flats & Room Rentals")
                                OnboardingBullet("🧹", "Professional Maids & Cleaners")
                                OnboardingBullet("🔧", "Experienced Plumbers & Technicians")
                            }
                        }
                    }

                    // Bottom CTA
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 52.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = "GET STARTED",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.5.sp,
                                color = NestoraMintDark
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "By continuing, you agree to our Terms & Conditions",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingBullet(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ─── AUTH FLOW — key fix: use lastStableState so the UI NEVER shows a blank
// full-screen spinner. The current step stays visible while request is in-flight,
// with the button itself showing a spinner.
@Composable
fun NestoraAuthFlow(
    authState: TdLibManager.AuthState,
    onSkip: () -> Unit,
    onBackToWelcome: () -> Unit
) {
    var phoneNumber  by remember { mutableStateOf("") }
    var otpCode      by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Track the last "real" step so LoggingIn doesn't replace the current form
    var lastStableState by remember { mutableStateOf<TdLibManager.AuthState>(authState) }

    LaunchedEffect(authState) {
        when (authState) {
            is TdLibManager.AuthState.LoggingIn -> { /* keep showing last step with spinner on button */ }
            else -> {
                lastStableState = authState
                isSubmitting = false   // reset spinner once new state arrives
            }
        }
    }

    // Loading = state is LoggingIn (in-flight request)
    val loading = authState is TdLibManager.AuthState.LoggingIn || isSubmitting

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Render the last stable step directly on the full-screen layout!
            when (val step = lastStableState) {
                is TdLibManager.AuthState.Uninitialized -> {
                    InitializingStep()
                }
                is TdLibManager.AuthState.WaitPhoneNumber -> {
                    PhoneNumberStep(
                        phoneNumber = phoneNumber,
                        onPhoneChange = { phoneNumber = it },
                        isLoading = loading,
                        onSubmit = {
                            if (!isSubmitting && phoneNumber.trim().length == 10) {
                                isSubmitting = true
                                TdLibManager.submitPhoneNumber("+91${phoneNumber.trim()}")
                            }
                        },
                        onSkip = onSkip,
                        onBack = onBackToWelcome
                    )
                }
                is TdLibManager.AuthState.WaitCode -> {
                    OtpStep(
                        phoneNumber = phoneNumber,
                        code = otpCode,
                        onCodeChange = { otpCode = it },
                        isLoading = loading,
                        onSubmit = {
                            if (!isSubmitting && otpCode.trim().length == 5) {
                                isSubmitting = true
                                TdLibManager.submitCode(otpCode.trim())
                            }
                        },
                        onBack = { otpCode = ""; TdLibManager.resetPhoneAuth() }
                    )
                }
                is TdLibManager.AuthState.WaitPassword -> {
                    PasswordStep(
                        hint = step.hint,
                        password = password,
                        onPasswordChange = { password = it },
                        isLoading = loading,
                        onSubmit = {
                            if (!isSubmitting && password.isNotBlank()) {
                                isSubmitting = true
                                TdLibManager.submitPassword(password)
                            }
                        },
                        onBack = { password = ""; TdLibManager.resetPhoneAuth() }
                    )
                }
                is TdLibManager.AuthState.Error -> {
                    ErrorStep(
                        message = step.message,
                        onRetry = { isSubmitting = false; TdLibManager.retry() },
                        onStartOver = {
                            phoneNumber = ""; otpCode = ""; password = ""
                            isSubmitting = false
                            TdLibManager.resetPhoneAuth()
                        }
                    )
                }
                is TdLibManager.AuthState.Ready -> { ReadyStep() }
                else -> { InitializingStep() }
            }
        }
    }
}

// ─── STEP: INITIALIZING ──────────────────────────────────────────────────────
@Composable
fun InitializingStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 24.dp)) {
        CircularProgressIndicator(color = NestoraMint, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(16.dp))
        Text("Connecting securely...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D1A13))
        Spacer(Modifier.height(6.dp))
        Text("Setting up your encrypted session", fontSize = 12.sp, color = NestoraTextMuted, textAlign = TextAlign.Center)
    }
}

// ─── STEP: PHONE NUMBER ──────────────────────────────────────────────────────
@Composable
fun PhoneNumberStep(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    val isValid = phoneNumber.trim().length == 10
    val focusManager = LocalFocusManager.current

    // Header Row with Back Button and Skip Button (matches Swiggy Figma Page 3)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF0D1A13)
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF5F5F5),
            modifier = Modifier.clickable(enabled = !isLoading) { onSkip() }
        ) {
            Text(
                text = "Skip",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D1A13),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    Text(
        text = "Enter your mobile number\nto get OTP",
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF0D1A13),
        lineHeight = 30.sp,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(28.dp))

    // Country code + phone input
    Row(
        modifier = Modifier.fillMaxWidth().height(62.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // India flag box
        Box(
            modifier = Modifier
                .width(86.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF0FDF8))
                .border(1.5.dp, NestoraMint.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("🇮🇳", fontSize = 24.sp)
                Text("+91", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = NestoraMintDark)
            }
        }

        // Phone field
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(
                    1.5.dp,
                    if (phoneNumber.isNotEmpty()) NestoraMint else Color(0xFFD4EFE6),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (phoneNumber.isEmpty()) {
                Text("00000  00000", color = Color(0xFFADC5BE), fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = phoneNumber,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }
                    if (digits.length <= 10) onPhoneChange(digits)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (isValid && !isLoading) onSubmit()
                }),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1A13),
                    letterSpacing = 3.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(Modifier.height(22.dp))

    Button(
        onClick = { focusManager.clearFocus(); onSubmit() },
        enabled = isValid && !isLoading,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint, disabledContainerColor = Color(0xFFB2E8D8)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Text("Getting OTP...", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
            }
        } else {
            Text("Get OTP", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White, letterSpacing = 0.3.sp)
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("By clicking, I accept the terms of service and privacy policy", fontSize = 11.sp, color = NestoraTextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(4.dp))
}

// ─── STEP: OTP ───────────────────────────────────────────────────────────────
@Composable
fun OtpStep(
    phoneNumber: String,
    code: String,
    onCodeChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isValid = code.trim().length == 5
    var showNotifPermissionDialog by remember { mutableStateOf(false) }
    var hasPromptedNotifAccess by remember { mutableStateOf(false) }

    fun isNotifListenerEnabled(ctx: Context): Boolean {
        val cn = ComponentName(ctx, OtpNotificationListener::class.java)
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        }
        if (!isNotifListenerEnabled(context) && !hasPromptedNotifAccess) {
            showNotifPermissionDialog = true
        }
    }

    DisposableEffect(Unit) {
        val intentFilter = IntentFilter().apply {
            addAction("android.provider.Telephony.SMS_RECEIVED")
            addAction("com.estatenestora.app.OTP_RECEIVED")
            priority = 999
        }
        val receiver = SmsReceiver { otp ->
            onCodeChange(otp)
            if (otp.length == 5 && !isLoading) {
                onSubmit()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    if (showNotifPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotifPermissionDialog = false },
            title = { Text("Auto-fetch from Notifications", fontWeight = FontWeight.Bold) },
            text = { Text("Nestora can read the login code directly from Telegram notifications to auto-fill it for you. Enable notification access for Nestora in settings?") },
            confirmButton = {
                Button(
                    onClick = {
                        showNotifPermissionDialog = false
                        hasPromptedNotifAccess = true
                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        runCatching { context.startActivity(intent) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                ) { Text("Enable Access", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNotifPermissionDialog = false }) { Text("Not now", color = NestoraTextMuted) }
            }
        )
    }

    // Header Row with Back Button (matches Swiggy Figma Page 5)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF0D1A13)
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    Text(
        text = "Verify with OTP send to\n+91 $phoneNumber",
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF0D1A13),
        lineHeight = 30.sp,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(28.dp))

    // Real invisible BasicTextField stacked on top of visual boxes
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Visual digit display row (5 boxes as per Telegram 5-digit OTP!)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 5) {
                val char = code.getOrNull(i)?.toString() ?: ""
                val isCurrentPos = i == code.length
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (char.isNotEmpty()) Color(0xFFF0FDF8) else Color.White)
                        .border(
                            1.5.dp,
                            when {
                                char.isNotEmpty() -> NestoraMint
                                isCurrentPos      -> NestoraMint.copy(alpha = 0.6f)
                                else              -> Color(0xFFD4EFE6)
                            },
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(char, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = NestoraMintDark)
                }
            }
        }

        // Invisible text field stretched over the entire visual area (5 digit limit)
        androidx.compose.foundation.text.BasicTextField(
            value = code,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }
                if (digits.length <= 5) onCodeChange(digits)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (isValid && !isLoading) onSubmit() }),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Transparent), // ensure text is fully hidden
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .alpha(0.01f)
        )
    }

    Spacer(Modifier.height(16.dp))

    // Auto fetching OTP row (matching Swiggy Page 5 UI)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        CircularProgressIndicator(
            color = NestoraMint,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Auto fetching OTP...",
            fontSize = 13.sp,
            color = NestoraTextMuted,
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(Modifier.height(28.dp))

    Button(
        onClick = { if (!isLoading && isValid) onSubmit() },
        enabled = isValid && !isLoading,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint, disabledContainerColor = Color(0xFFB2E8D8)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Text("Continuing...", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
            }
        } else {
            Text("Continue", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
        }
    }

    Spacer(Modifier.height(20.dp))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Didn't receive it? Retry in 00:30",
            fontSize = 12.sp,
            color = NestoraTextMuted,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Change Phone Number", color = NestoraMint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(4.dp))
}

// ─── STEP: PASSWORD ──────────────────────────────────────────────────────────
@Composable
fun PasswordStep(
    hint: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Text("2-Step Verification", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1A13))
    Spacer(Modifier.height(6.dp))
    if (hint.isNotBlank()) {
        Text("Hint: $hint", fontSize = 12.sp, color = NestoraTextMuted, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(20.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Security Password") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NestoraMint) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (password.isNotBlank() && !isLoading) onSubmit() }),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NestoraMint, unfocusedBorderColor = Color(0xFFD4EFE6),
            focusedLabelColor = NestoraMint, focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF7FDFA),
            focusedTextColor = Color(0xFF0D1A13), unfocusedTextColor = Color(0xFF0D1A13)
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(20.dp))

    Button(
        onClick = { if (!isLoading && password.isNotBlank()) onSubmit() },
        enabled = password.isNotBlank() && !isLoading,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint, disabledContainerColor = Color(0xFFB2E8D8)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Text("Unlocking...", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
            }
        } else {
            Text("Unlock Account  →", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onBack() }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Change Phone Number", color = NestoraMint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(4.dp))
}

// ─── STEP: ERROR ─────────────────────────────────────────────────────────────
@Composable
fun ErrorStep(message: String, onRetry: () -> Unit, onStartOver: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Icon(Icons.Default.Warning, contentDescription = null, tint = NestoraError, modifier = Modifier.size(44.dp))
    Spacer(Modifier.height(10.dp))
    Text("Authentication Error", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1A13))
    Spacer(Modifier.height(6.dp))
    Text(message, fontSize = 13.sp, color = NestoraTextMuted, textAlign = TextAlign.Center, lineHeight = 19.sp)
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Try Again", fontWeight = FontWeight.Bold, color = Color.White)
    }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onStartOver() }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Start Over", color = NestoraMint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(8.dp))
}

// ─── STEP: READY ─────────────────────────────────────────────────────────────
@Composable
fun ReadyStep() {
    Spacer(Modifier.height(16.dp))
    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(52.dp))
    Spacer(Modifier.height(12.dp))
    Text("Verified & Connected!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1A13))
    Spacer(Modifier.height(16.dp))
}

@Composable
fun BadgeChip(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NestoraMint.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NestoraMint.copy(alpha = 0.3f))
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NestoraMintDark,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}
