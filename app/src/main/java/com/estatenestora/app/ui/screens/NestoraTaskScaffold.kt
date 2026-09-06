package com.estatenestora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val NestoraFilterPanelSpacing = 12.dp

/**
 * Standard shell for focused provider work. It deliberately has no global
 * location header or bottom navigation, keeping one clear way back to the
 * provider dashboard.
 */
@Composable
fun NestoraTaskScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                androidx.compose.foundation.layout.Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to dashboard",
                                tint = Color(0xFF10231B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF10231B),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(color = Color(0xFFE8ECEA), thickness = 1.dp)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8F7))
                .padding(innerPadding)
        ) {
            content()
        }
    }
}

/** Compact identity header for a root provider workspace. Root workspaces do
 * not show a back action because the persistent navigation owns switching. */
@Composable
fun NestoraWorkspaceHeader(
    icon: ImageVector,
    title: String,
    description: String? = null,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF075D45),
            modifier = Modifier.size(20.dp)
        )
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(start = 9.dp).weight(1f)
        ) {
            Text(
                text = title,
                color = Color(0xFF10231B),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            TextButton(onClick = onAction) {
                if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = Color(0xFF075D45),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(start = if (actionIcon == null) 0.dp else 4.dp),
                    color = Color(0xFF075D45),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** One consistent, clearly visible boundary between top-level page sections. */
@Composable
fun NestoraSectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Color(0xFFE8ECEA))
    )
}
