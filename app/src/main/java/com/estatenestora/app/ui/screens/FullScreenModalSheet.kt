package com.estatenestora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

internal fun shouldDismissFilterDrawer(
    offsetY: Float,
    dismissThresholdPx: Float,
    velocityY: Float
): Boolean = offsetY >= dismissThresholdPx || velocityY > 1_800f

internal val FilterPaneDividerColor = Color(0xFFDDE3E0)

/** Standard full-screen content sheet. Compact confirmation dialogs remain AlertDialogs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenModalSheet(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF111814),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F3F2))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF17201C),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
            content()
        }
    }
}

/**
 * Filter sheets keep a short, dimmed glimpse of the current page above the
 * rounded surface. It preserves context while still giving filter controls a
 * full-height workspace and one obvious close action.
 */
@Composable
fun FilterOverlaySheet(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val drawerTopGap = 54.dp
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 120.dp.toPx() }
    var drawerOffsetY by remember { mutableFloatStateOf(0f) }
    val drawerDragState = rememberDraggableState { delta ->
        drawerOffsetY = (drawerOffsetY + delta).coerceAtLeast(0f)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    // The close control ends at statusBarHeight + 46.dp.
                    // Starting the drawer at +54.dp leaves a compact 8.dp gap.
                    .padding(top = statusBarHeight + drawerTopGap)
                    .offset { IntOffset(0, drawerOffsetY.roundToInt()) }
                    .draggable(
                        state = drawerDragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            if (shouldDismissFilterDrawer(drawerOffsetY, dismissThresholdPx, velocity)) {
                                onDismissRequest()
                            } else {
                                animate(
                                    initialValue = drawerOffsetY,
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 180)
                                ) { value, _ -> drawerOffsetY = value }
                            }
                        }
                    ),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 9.dp, bottom = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(38.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFCCD2CF))
                            )
                        }
                        Text(
                            text = title,
                            color = Color(0xFF111814),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 20.dp, top = 7.dp, bottom = 14.dp)
                        )
                    }
                    HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
                    content()
                }
            }

            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 6.dp, end = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close filters",
                    tint = Color(0xFF17201C),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
