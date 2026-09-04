package com.estatenestora.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun NestoraMoneyScreen(
    onBack: () -> Unit,
    onAddBalanceClick: () -> Unit,
    getWalletBalance: suspend () -> Double
) {
    val context = LocalContext.current
    val strings = com.estatenestora.app.ui.theme.LocalNestoraStrings.current
    var balance by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            balance = getWalletBalance()
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F7)),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.moneyTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "powered by Nestora",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F7F7))
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Green Wallet Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0F7855), Color(0xFF1EAD7E))
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = strings.moneyAvailableBalance,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "₹${String.format("%.0f", balance)}",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = strings.moneyUsedAt,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Promo Card 1
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEAEAEA))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Share love through e-gift vouchers!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Celebrate special occasions with your loved ones with e-gift vouchers.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Buy a gift voucher",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFF3E0),
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }

                // Promo Card 2
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEAEAEA))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Get instant refunds with Nestora Money!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Select refund method", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }

            // Bottom CTA section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onAddBalanceClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7855))
                    ) {
                        Text(strings.moneyAddBalance, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Have a gift voucher? Redeem Now",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F7855)
                    )
                }
            }
        }
    }
}

@Composable
fun AddBalanceScreen(
    onBack: () -> Unit,
    onBalanceAdded: (Double) -> Unit,
    getWalletBalance: suspend () -> Double,
    addWalletBalance: suspend (Double) -> Double
) {
    val context = LocalContext.current
    val strings = com.estatenestora.app.ui.theme.LocalNestoraStrings.current
    var currentBalance by remember { mutableStateOf(0.0) }
    var enterAmount by remember { mutableStateOf("250") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            currentBalance = getWalletBalance()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F7)),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = strings.moneyAddBalance,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Available balance: ₹${String.format("%.2f", currentBalance)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F7F7))
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Enter Amount Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEAEAEA))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(strings.moneyEnterAmount, fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = enterAmount,
                            onValueChange = { enterAmount = it.filter { char -> char.isDigit() } },
                            leadingIcon = { Text("₹", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE65100),
                                unfocusedBorderColor = Color(0xFFEAEAEA)
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf("250", "500", "2000", "5000")
                            presets.forEach { preset ->
                                val isSelected = enterAmount == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFFFF3E0) else Color.White)
                                        .border(1.dp, if (isSelected) Color(0xFFE65100) else Color(0xFFEAEAEA), RoundedCornerShape(8.dp))
                                        .clickable { enterAmount = preset }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "₹$preset",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFFE65100) else Color.Black
                                        )
                                        if (preset == "500") {
                                            Text(
                                                text = "Most Popular",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFE65100))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Have a gift voucher? Redeem Now",
                            fontSize = 12.sp,
                            color = Color(0xFF0F7855),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Notes Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEAEAEA))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(strings.moneyNoteTitle, fontSize = 12.sp, color = Color(0xFFC53030), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))

                        val bullets = listOf(
                            strings.moneyNote1,
                            strings.moneyNote2,
                            strings.moneyNote3,
                            strings.moneyNote4
                        )

                        bullets.forEach { bullet ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                Text(
                                    text = bullet,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Proceed button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val amountVal = enterAmount.toDoubleOrNull() ?: 0.0
                        if (amountVal > 0) {
                            val upiUriString = "upi://pay" +
                                    "?pa=ritesh249@slc" +
                                    "&pn=${Uri.encode("Nestora")}" +
                                    "&am=${amountVal}" +
                                    "&cu=INR" +
                                    "&tn=${Uri.encode("Load Nestora Money")}" +
                                    "&tr=txn_${System.currentTimeMillis()}"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUriString))
                                val chooser = Intent.createChooser(intent, "Pay via UPI App")
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No UPI app found on this device.", Toast.LENGTH_LONG).show()
                            }

                            // Trigger backend update to persist balance change
                            scope.launch {
                                val newBal = addWalletBalance(amountVal)
                                Toast.makeText(context, "₹$amountVal added to Nestora Money!", Toast.LENGTH_LONG).show()
                                onBalanceAdded(newBal)
                            }
                        } else {
                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7855))
                ) {
                    Text(strings.moneyProceedToAdd, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
