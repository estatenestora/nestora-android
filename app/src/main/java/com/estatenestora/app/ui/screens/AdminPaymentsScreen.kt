package com.estatenestora.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.data.model.AdminPaymentReview
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentsScreen(
    loadQueue: suspend () -> List<AdminPaymentReview>,
    approve: suspend (String) -> String?,
    reject: suspend (String) -> String?,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf<List<AdminPaymentReview>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var processingId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmApprove by remember { mutableStateOf<AdminPaymentReview?>(null) }
    var confirmReject by remember { mutableStateOf<AdminPaymentReview?>(null) }

    suspend fun refreshQueue(showLoading: Boolean) {
        if (showLoading) loading = true
        queue = runCatching { loadQueue() }
            .onFailure { message = "Could not load payment reviews. Please try again." }
            .getOrDefault(emptyList())
        if (showLoading) loading = false
    }
    LaunchedEffect(Unit) {
        refreshQueue(showLoading = true)
        while (isActive) {
            delay(5_000)
            refreshQueue(showLoading = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment verification") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    IconButton(onClick = { scope.launch { refreshQueue(showLoading = true) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh payment queue")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(SnackbarHostState()) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Review customer advance payments. Updates every 5 seconds.", fontSize = 14.sp, color = Color(0xFF52665C))
            Spacer(Modifier.height(12.dp))
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                queue.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No payments awaiting review") }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(queue, key = { it.bookingId }) { item ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.listingTitle.orEmpty().ifBlank { "Service booking" }, fontWeight = FontWeight.Bold)
                                Text("Order #${item.referenceCode} · ₹${"%.2f".format(item.advanceAmount)} advance", fontSize = 13.sp)
                                Text("Customer: ${item.customerName}\nProvider: ${item.providerName}", fontSize = 13.sp, color = Color(0xFF52665C))
                                if (item.paymentScreenshot.orEmpty().isNotBlank()) Text("Payment receipt uploaded", fontSize = 12.sp, color = Color(0xFF137333))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = { confirmReject = item }, enabled = processingId == null, modifier = Modifier.weight(1f)) { Text("Reject") }
                                    Button(onClick = { confirmApprove = item }, enabled = processingId == null, modifier = Modifier.weight(1f)) { Text("Settle") }
                                }
                            }
                        }
                    }
                }
            }
            message?.let { Text(it, color = Color(0xFF137333), modifier = Modifier.padding(top = 8.dp)) }
        }
    }
    fun decide(item: AdminPaymentReview, approved: Boolean) = scope.launch {
        processingId = item.bookingId
        message = if (approved) approve(item.bookingId) else reject(item.bookingId)
        processingId = null
        confirmApprove = null; confirmReject = null
        refreshQueue(showLoading = false)
    }
    confirmApprove?.let { item -> AlertDialog(onDismissRequest = { confirmApprove = null }, title = { Text("Settle advance?") }, text = { Text("Confirm ₹${"%.2f".format(item.advanceAmount)} advance for #${item.referenceCode}.") }, confirmButton = { TextButton(onClick = { decide(item, true) }) { Text("Settle") } }, dismissButton = { TextButton(onClick = { confirmApprove = null }) { Text("Cancel") } }) }
    confirmReject?.let { item -> AlertDialog(onDismissRequest = { confirmReject = null }, title = { Text("Reject payment?") }, text = { Text("The customer will be asked to submit payment again.") }, confirmButton = { TextButton(onClick = { decide(item, false) }) { Text("Reject") } }, dismissButton = { TextButton(onClick = { confirmReject = null }) { Text("Cancel") } }) }
}
