package com.estatenestora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.ui.theme.*
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.estatenestora.app.util.hasLocationPermission
import com.estatenestora.app.util.isSystemLocationEnabled


@Composable
fun LocationAccessScreen(
    onLocationGranted: () -> Unit,
    onManualLocation: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showManualDialog by remember { mutableStateOf(false) }
    var manualCity by remember { mutableStateOf("") }
    var showEnableLocationDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            if (isSystemLocationEnabled(context)) {
                onLocationGranted()
            } else {
                showEnableLocationDialog = true
            }
        } else {
            Toast.makeText(context, "Location permission is required to detect location automatically.", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0D1A13),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center Content (Illustration + Text)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8FAF4)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB2E8D8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location Pin",
                            tint = NestoraMintDark,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))

                Text(
                    text = "What's your location?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1A13),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "We need your location to show available listings, flats, plumbers, and home services near you.",
                    fontSize = 14.sp,
                    color = NestoraTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Bottom Actions
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (hasLocationPermission(context)) {
                            if (isSystemLocationEnabled(context)) {
                                onLocationGranted()
                            } else {
                                showEnableLocationDialog = true
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Allow location access",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Enter Location Manually",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NestoraMintDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showManualDialog = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Manual Location Input Dialog
        if (showManualDialog) {
            AlertDialog(
                onDismissRequest = { showManualDialog = false },
                title = {
                    Text(
                        text = "Enter City / Area",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0D1A13)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Search city or enter pincode to find services near you.",
                            fontSize = 13.sp,
                            color = NestoraTextMuted
                        )
                        OutlinedTextField(
                            value = manualCity,
                            onValueChange = { manualCity = it },
                            placeholder = { Text("e.g. Newtown, Kolkata", color = NestoraTextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NestoraMint,
                                unfocusedBorderColor = Color(0xFFD4EFE6)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualCity.isNotBlank()) {
                                showManualDialog = false
                                onManualLocation(manualCity)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Search", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualDialog = false }) {
                        Text("Cancel", color = NestoraTextMuted)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Turn on Location Settings Dialog
        if (showEnableLocationDialog) {
            AlertDialog(
                onDismissRequest = { showEnableLocationDialog = false },
                title = { Text("Turn on Location", fontWeight = FontWeight.Bold) },
                text = { Text("Location services are switched off for this device — please turn on GPS/Location in system settings to auto-detect your location.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showEnableLocationDialog = false
                            runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Open Settings", color = Color.White, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showEnableLocationDialog = false }) {
                        Text("Cancel", color = NestoraTextMuted)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
