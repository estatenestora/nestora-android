package com.estatenestora.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.*
import com.estatenestora.app.ui.theme.NestoraMint
import kotlinx.coroutines.launch

private data class AdminMediaDestination(val scope: String, val id: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMediaScreen(
    categories: List<Category>,
    loadServiceTypes: suspend () -> List<ServiceType>,
    onBack: () -> Unit,
    loadAssets: suspend (String, String) -> AndroidBridgeResponse?,
    upload: suspend (Uri, String, String, String, String, String, String, String, Int) -> AndroidBridgeResponse,
    archive: suspend (String) -> AndroidBridgeResponse?,
    resolveMedia: suspend (String) -> String?
) {
    val scope = rememberCoroutineScope()
    var serviceTypes by remember { mutableStateOf<List<ServiceType>>(emptyList()) }
    LaunchedEffect(Unit) { serviceTypes = loadServiceTypes() }
    val destinations = remember(categories, serviceTypes) {
        buildList {
            add(AdminMediaDestination("APP_CAROUSEL", "", "App carousel"))
            categories.filter { it.backendId.isNotBlank() }.forEach { add(AdminMediaDestination("CATEGORY", it.backendId, "Category: ${it.name}")) }
            serviceTypes.filter { it.backendId.isNotBlank() }.forEach { add(AdminMediaDestination("SERVICE_TYPE", it.backendId, "Service: ${it.name}")) }
        }
    }
    var selected by remember(destinations) { mutableStateOf(destinations.firstOrNull()) }
    var assets by remember { mutableStateOf<List<MediaAsset>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var actionLabel by remember { mutableStateOf("") }
    var actionValue by remember { mutableStateOf("") }
    var destinationMenu by remember { mutableStateOf(false) }

    fun reload() {
        val target = selected ?: return
        scope.launch {
            loading = true
            val response = loadAssets(target.scope, target.id)
            assets = response?.mediaAssets.orEmpty().filter { it.status == "ACTIVE" }
            feedback = if (response?.ok == true) null else response?.reply ?: "Could not load app images."
            loading = false
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = selected
        if (uri != null && target != null) scope.launch {
            loading = true
            feedback = "Optimizing and saving image..."
            val role = if (target.scope == "APP_CAROUSEL") "HERO" else "PRIMARY"
            val response = upload(uri, target.scope, target.id, role, title, subtitle, actionLabel, actionValue, assets.size)
            feedback = response.reply
            loading = false
            if (response.ok) {
                title = ""; subtitle = ""; actionLabel = ""; actionValue = ""
                reload()
            }
        }
    }
    LaunchedEffect(selected) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App media", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        containerColor = Color(0xFFF6F8F7)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Admin-managed imagery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Manage carousel, category and service images without shipping a new APK. The original is not retained; Nestora stores optimized display sizes.", color = Color(0xFF60756B), style = MaterialTheme.typography.bodySmall)
            }
            item {
                Box {
                    OutlinedButton(onClick = { destinationMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selected?.label ?: "Choose section", modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(expanded = destinationMenu, onDismissRequest = { destinationMenu = false }) {
                        destinations.forEach { target ->
                            DropdownMenuItem(text = { Text(target.label) }, onClick = { selected = target; destinationMenu = false })
                        }
                    }
                }
            }
            if (selected?.scope == "APP_CAROUSEL") {
                item { OutlinedTextField(title, { title = it.take(160) }, label = { Text("Banner title") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(subtitle, { subtitle = it.take(500) }, label = { Text("Banner message") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(actionLabel, { actionLabel = it.take(80) }, label = { Text("Action label") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(actionValue, { actionValue = it.take(500) }, label = { Text("Action target") }, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Button(onClick = { picker.launch("image/*") }, enabled = !loading && selected != null, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose and upload image", fontWeight = FontWeight.Bold)
                }
            }
            if (loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NestoraMint) }
            feedback?.let { message -> item { Text(message, color = Color(0xFF60756B), style = MaterialTheme.typography.bodySmall) } }
            if (!loading && assets.isEmpty()) item { Text("No image configured. The packaged fallback remains visible to users.", color = Color(0xFF60756B)) }
            items(assets, key = { it.id }) { asset ->
                AdminMediaAssetRow(asset, resolveMedia) {
                    scope.launch {
                        loading = true
                        feedback = archive(asset.id)?.reply ?: "Could not remove image."
                        loading = false
                        reload()
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMediaAssetRow(asset: MediaAsset, resolveMedia: suspend (String) -> String?, onRemove: () -> Unit) {
    val fileId = remember(asset.id) { asset.fileIdFor(if (asset.scope == "APP_CAROUSEL") "HERO" else "THUMBNAIL") }
    var path by remember(fileId) { mutableStateOf<String?>(null) }
    LaunchedEffect(fileId) { path = fileId?.let { resolveMedia(it) } }
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFDCE8E3))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(width = 112.dp, height = 72.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE7F1ED)), contentAlignment = Alignment.Center) {
                if (!path.isNullOrBlank()) AsyncImage(path, asset.title.ifBlank { asset.scope }, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f)) {
                Text(asset.title.ifBlank { asset.role.lowercase().replaceFirstChar(Char::uppercase) }, fontWeight = FontWeight.Bold)
                Text("${asset.variants.size} optimized sizes", color = Color(0xFF60756B), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onRemove) { Text("Remove", color = Color(0xFFB3261E)) }
        }
    }
}
