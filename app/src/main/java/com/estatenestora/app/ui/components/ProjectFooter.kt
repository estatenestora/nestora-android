package com.estatenestora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProjectFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F5F7)) // light grey shade matching Swiggy footer
            .padding(start = 24.dp, top = 72.dp, end = 24.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "nestora",
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = Color(0xFFC0C4CC), // light grey shade for branding text
            letterSpacing = (-1.5).sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Crafted with 💙 in Kolkata, India",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF909399) // grey shade for secondary text
        )
    }
}
