package com.estatenestora.app.ui.screens

import com.estatenestora.app.ui.components.ProjectFooter
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.GeocodePlace
import com.estatenestora.app.data.model.UserProfile
import com.estatenestora.app.ui.theme.*

fun isValidImageUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return true
    // Accept Android content:// and file:// URIs from the device gallery/picker
    if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) return true
    // Accept Telegram file_id (length >= 20, no whitespace)
    val isTelegramFileId = trimmed.length >= 20 && trimmed.none { it.isWhitespace() }
    if (isTelegramFileId) return true
    val lower = trimmed.lowercase()
    return lower.startsWith("http://") || lower.startsWith("https://") ||
           lower.endsWith(".jpg") ||
           lower.endsWith(".jpeg") ||
           lower.endsWith(".png") ||
           lower.endsWith(".webp") ||
           lower.endsWith(".gif") ||
           lower.endsWith(".bmp")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onUploadPhoto: suspend (android.net.Uri) -> String?,
    onResolvePhoto: suspend (String) -> String?,
    onSearchAddress: suspend (String, Double?, Double?) -> List<GeocodePlace>,
    onReverseGeocode: suspend (Double, Double) -> GeocodePlace?,
    onAdminPayments: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // States for Edit Profile Form
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember(profile) { mutableStateOf(profile.name) }
    var editPhone by remember(profile) { mutableStateOf(profile.phone) }
    var editEmail by remember(profile) { mutableStateOf(profile.email) }
    var editAddress by remember(profile) { mutableStateOf(profile.address ?: "") }
    var editPicUrl by remember(profile) { mutableStateOf(profile.profilePicUrl ?: "") }
    var editUpiId by remember(profile) { mutableStateOf(profile.upiId ?: "") }
    // Holds the local URI for preview immediately after picking (before or during upload)
    var displayPicUri by remember { mutableStateOf<android.net.Uri?>(null) }
    // Resolved local file path for displaying saved profile photo (from Telegram file_id)
    var resolvedPhotoPath by remember(profile.profilePicUrl) { mutableStateOf<String?>(null) }

    // Whenever the profile photo file_id changes, resolve it to a local cached path
    LaunchedEffect(profile.profilePicUrl) {
        val picUrl = profile.profilePicUrl
        if (!picUrl.isNullOrBlank() && picUrl.length >= 20) {
            resolvedPhotoPath = onResolvePhoto(picUrl)
        } else {
            resolvedPhotoPath = null
        }
    }
    
    var menuExpanded by remember { mutableStateOf(false) }
    var showBackConfirmationDialog by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    // Snackbar for styled error/success alerts
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var snackbarIsError by remember { mutableStateOf(false) }

    fun showSnack(message: String, isError: Boolean = true) {
        snackbarIsError = isError
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            displayPicUri = pickedUri  // Show picked image immediately in the avatar
            coroutineScope.launch {
                isUploadingPhoto = true
                showSnack("Uploading photo… please wait", isError = false)
                val fileId = try { onUploadPhoto(pickedUri) } catch (e: Exception) { null }
                isUploadingPhoto = false
                if (fileId != null) {
                    editPicUrl = fileId
                    showSnack("✅ Profile photo uploaded successfully!", isError = false)
                } else {
                    showSnack("❌ Photo upload failed. Please try again.")
                }
            }
        }
    }

    val isFormChanged = editName != profile.name ||
            editPhone != profile.phone ||
            editEmail != profile.email ||
            editAddress != (profile.address ?: "") ||
            editPicUrl != (profile.profilePicUrl ?: "") ||
            editUpiId != (profile.upiId ?: "")

    // Validation runner for saving
    fun runValidationAndSave(onSuccess: () -> Unit) {
        val trimmedName = editName.trim()
        val trimmedPhone = editPhone.trim()
        val trimmedEmail = editEmail.trim()
        val trimmedUpi = editUpiId.trim()
        val trimmedPic = editPicUrl.trim()

        val nameRegex = Regex("^[a-zA-Z\\s]{2,50}$")
        val phoneRegex = Regex("^(\\+91)?[6-9]\\d{9}$")
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        // Standard UPI VPA format: alphanumeric/.-_ before @, bank handle after
        val upiRegex = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$")

        when {
            !nameRegex.matches(trimmedName) -> {
                showSnack("\u274C Invalid Name: Should contain only letters (min 2 characters)")
            }
            trimmedPhone.isNotEmpty() && !phoneRegex.matches(trimmedPhone) -> {
                showSnack("\u274C Invalid Phone: Please enter a valid 10-digit mobile number")
            }
            trimmedEmail.isNotEmpty() && !emailRegex.matches(trimmedEmail) -> {
                showSnack("\u274C Invalid Email: Please enter a valid email address")
            }
            trimmedUpi.isNotEmpty() && !upiRegex.matches(trimmedUpi) -> {
                showSnack("\u274C Invalid UPI Number: Format should be yourname@bankhandle (e.g. 9876543210@okaxis)")
            }
            trimmedPic.isNotEmpty() && !isValidImageUrl(trimmedPic) -> {
                showSnack("\u274C Invalid profile image — please pick a photo from your gallery")
            }
            else -> {
                try {
                    val updated = profile.copy(
                        name = trimmedName,
                        phone = trimmedPhone,
                        email = trimmedEmail,
                        address = editAddress.trim(),
                        profilePicUrl = trimmedPic,
                        upiId = trimmedUpi
                    )
                    onUpdateProfile(updated)
                    onSuccess()
                    showSnack("\u2705 Profile updated successfully!", isError = false)
                } catch (e: Exception) {
                    showSnack("\u274C Failed to save profile: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    if (showLocationPicker) {
        MapLocationPickerScreen(
            initialAddress = editAddress,
            onSearchAddress = onSearchAddress,
            onReverseGeocode = onReverseGeocode,
            onLocationConfirmed = { title, subtitle, _, _ ->
                editAddress = if (subtitle.isNotBlank()) "$title, $subtitle" else title
                showLocationPicker = false
            },
            onBack = { showLocationPicker = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Styled Snackbar — shown for all validation errors and save success/failure
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding(),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (snackbarIsError) Color(0xFFB71C1C) else Color(0xFF1B5E20),
                    contentColor = Color.White,
                    actionColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        )

        if (isEditing) {
            // ─── EDIT PROFILE VIEW (Clean White background, no curves, no card wrapping) ───
            
            // Confirmation dialog on back pressed
            if (showBackConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showBackConfirmationDialog = false },
                    title = { Text("Save Changes?", fontWeight = FontWeight.Bold) },
                    text = { Text("You have unsaved changes. Do you want to save them before leaving?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                runValidationAndSave {
                                    showBackConfirmationDialog = false
                                    isEditing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                showBackConfirmationDialog = false
                                isEditing = false
                                // reset form state
                                editName = profile.name
                                editPhone = profile.phone
                                editEmail = profile.email
                                editAddress = profile.address ?: ""
                                editPicUrl = profile.profilePicUrl ?: ""
                                editUpiId = profile.upiId ?: ""
                            }
                        ) {
                            Text("Discard", color = NestoraTextMuted)
                        }
                    }
                )
            }



            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // 1. Top action bar (Clean White header, matching Swiggy back layouts)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isFormChanged) {
                                    showBackConfirmationDialog = true
                                } else {
                                    isEditing = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0D1A13)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Edit Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1A13),
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Small "Save" word button
                        TextButton(
                            enabled = isFormChanged,
                            onClick = {
                                runValidationAndSave {
                                    isEditing = false
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = NestoraMint,
                                disabledContentColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "Save",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // 2. Profile body inputs (direct edit form, no cards, no borders)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item { Spacer(Modifier.height(16.dp)) }

                    // Avatar Circle container
                    item {
                        val isAadharUploaded = profile.verificationStatus == "VERIFIED" || profile.verificationStatus == "AADHAR_VERIFIED"
                        
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0FDF8))
                                .clickable {
                                    if (isAadharUploaded) {
                                        imagePickerLauncher.launch("image/*")
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Bad Request: Aadhaar verification required to update profile picture!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (displayPicUri != null) {
                                AsyncImage(
                                    model = displayPicUri,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!resolvedPhotoPath.isNullOrBlank()) {
                                AsyncImage(
                                    model = java.io.File(resolvedPhotoPath!!),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("👤", fontSize = 44.sp)
                            }
                            
                            // overlay: spinner while uploading, pencil icon otherwise
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = if (isUploadingPhoto) 0.45f else 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingPhoto) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Change avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap photo to change",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NestoraTextMuted
                        )
                    }

                    // Fields form inputs (direct editing)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProfileInputField(
                                label = "Full Name",
                                value = editName,
                                onValueChange = { editName = it }
                            )

                            ProfileInputField(
                                label = "Phone Number",
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                keyboardType = KeyboardType.Phone
                            )

                            ProfileInputField(
                                label = "Email",
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                keyboardType = KeyboardType.Email
                            )

                            // Address field - clickable to open Map picker
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Address",
                                    fontSize = 12.sp,
                                    color = NestoraTextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = editAddress,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showLocationPicker = true },
                                    enabled = false, // disable typing, clicks go to map picker
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = Color(0xFFE2EAF2),
                                        disabledTextColor = Color(0xFF0D1A13)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Pick on map",
                                            tint = NestoraMint,
                                            modifier = Modifier.clickable { showLocationPicker = true }
                                        )
                                    }
                                )
                            }

                            ProfileInputField(
                                label = "UPI Number",
                                value = editUpiId,
                                placeholder = "e.g. 9876543210@okaxis",
                                onValueChange = { editUpiId = it }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                    item { ProjectFooter() }
                }
            }
        } else {
            // ─── ORIGINAL DASHBOARD VIEW ───
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Item 1: Swiggy-style top header block (curved gradient card)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF00382B), Color(0xFF00382B))
                                )
                            )
                            .statusBarsPadding()
                            .padding(top = 8.dp, bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Top Actions: Back Arrow (Left), Help (Right), Vertical Overflow Dots (Right)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left: White back arrow icon
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                // Right: Help pill and vertical three-dots icon
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                        modifier = Modifier.clickable {
                                            Toast.makeText(context, "Support Contact: support@nestora.app", Toast.LENGTH_LONG).show()
                                        }
                                    ) {
                                        Text(
                                            text = "Help",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                    
                                    Spacer(Modifier.width(4.dp))
                                    
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // User Identity Block (Avatar restored, Name, Phone, Email stacked)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                // Profile photo avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable {
                                            displayPicUri = null
                                            isEditing = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!resolvedPhotoPath.isNullOrBlank()) {
                                        AsyncImage(
                                            model = java.io.File(resolvedPhotoPath!!),
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text("👤", fontSize = 32.sp)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = profile.name,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = profile.phone,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = profile.email,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Item 2: Promotion Banner (Swiggy One Style Banner)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp, bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEAEAEA))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Free",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F7855)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "ACTIVE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F7855))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text("⌄", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                            Text(
                                text = "Explore all Nestora Free benefits",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Item 3: Quick Action 4-Grid Cards
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(Icons.Outlined.Place, "Address", modifier = Modifier.weight(1f)) {
                            Toast.makeText(context, profile.address?.ifEmpty { "Salt Lake, Sector V, Kolkata" } ?: "Salt Lake, Sector V, Kolkata", Toast.LENGTH_LONG).show()
                        }
                        QuickActionCard(Icons.Outlined.ShoppingCart, "Payment Modes", modifier = Modifier.weight(1f)) {
                            Toast.makeText(context, "UPI, Card, Net Banking supported", Toast.LENGTH_SHORT).show()
                        }
                        QuickActionCard(Icons.Outlined.Refresh, "My Refunds", modifier = Modifier.weight(1f)) {
                            Toast.makeText(context, "Refund history is empty", Toast.LENGTH_SHORT).show()
                        }
                        QuickActionCard(Icons.Outlined.Lock, "Nestora Wallet", modifier = Modifier.weight(1f)) {
                            Toast.makeText(context, "Nestora Wallet Balance: ₹0.00", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // Item 4: Vertical List of Premium options inside a single large white rounded card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEAEAEA))
                    ) {
                        Column {
                            VerticalListMenuItem(Icons.Outlined.Star, "Nestora Platinum Membership") {
                                Toast.makeText(context, "Nestora Platinum features active!", Toast.LENGTH_SHORT).show()
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            
                            VerticalListMenuItem(Icons.Outlined.Notifications, "My Vouchers") {
                                Toast.makeText(context, "Vouchers list is empty", Toast.LENGTH_SHORT).show()
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            
                            VerticalListMenuItem(Icons.Outlined.List, "Account Statement") {
                                Toast.makeText(context, "Downloading PDF account statement...", Toast.LENGTH_SHORT).show()
                            }
                            if (profile.role.equals("ADMIN", ignoreCase = true)) {
                                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                VerticalListMenuItem(Icons.Outlined.Notifications, "Payment Verification Queue") {
                                    onAdminPayments()
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            
                            VerticalListMenuItem(Icons.Outlined.FavoriteBorder, "My Shortlists") {
                                Toast.makeText(context, "Shortlisted items list is empty", Toast.LENGTH_SHORT).show()
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            
                            VerticalListMenuItem(Icons.Outlined.FavoriteBorder, "Favourites") {
                                Toast.makeText(context, "Favourite services and providers", Toast.LENGTH_SHORT).show()
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            
                            VerticalListMenuItem(Icons.Outlined.Share, "Partner Rewards") {
                                Toast.makeText(context, "Nestora partners overview", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
                item { ProjectFooter() }
            }
        }

        // Invisible click interceptor to close top-right overlay options menu when clicking anywhere else
        if (menuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { menuExpanded = false }
            )
        }

        // Dropdown Card drawn on top of the click interceptor
        if (menuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 36.dp, end = 16.dp)
                        .width(IntrinsicSize.Max),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181C24)),
                    border = BorderStroke(1.dp, Color(0xFF2C3545)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    menuExpanded = false
                                    displayPicUri = null
                                    isEditing = true
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "Edit Profile",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                        HorizontalDivider(color = Color(0xFF2C3545), thickness = 0.8.dp)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    menuExpanded = false
                                    Toast.makeText(context, "App Settings loaded", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "Settings",
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                        HorizontalDivider(color = Color(0xFF2C3545), thickness = 0.8.dp)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    menuExpanded = false
                                    onLogout()
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "Logout",
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = NestoraTextMuted,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = NestoraTextMuted) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NestoraMint,
                unfocusedBorderColor = Color(0xFFE2EAF2),
                focusedLabelColor = NestoraMint,
                unfocusedLabelColor = NestoraTextMuted
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEAEAEA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2A2A2A),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VerticalListMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF2A2A2A),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2A2A2A),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFB0B0B0),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun GuestProfileScreen(onLoginClick: () -> Unit, onBack: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FDFA))
            .statusBarsPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("👤", fontSize = 72.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Guest Mode",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D1A13)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Log in to view your profile, manage bookings, and access full marketplace features.",
                fontSize = 14.sp,
                color = NestoraTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
            ) {
                Text(
                    text = "Log In",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Go Back", color = NestoraTextMuted)
            }
        }
    }
}
