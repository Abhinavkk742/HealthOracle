package com.healthoracle.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMyPosts: () -> Unit,
    onNavigateToChat: (patientId: String, doctorId: String, contactName: String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── Form state (mirrors original exactly) ────────────────────────────────
    var name    by remember(uiState.profile) { mutableStateOf(uiState.profile.name) }
    var height  by remember(uiState.profile) { mutableStateOf(if (uiState.profile.heightCm == 0f) "" else uiState.profile.heightCm.toString()) }
    var weight  by remember(uiState.profile) { mutableStateOf(if (uiState.profile.weightKg == 0f) "" else uiState.profile.weightKg.toString()) }
    var gender  by remember(uiState.profile) { mutableStateOf(if (uiState.profile.gender.isEmpty()) "Male" else uiState.profile.gender) }
    var expandedGender by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other")

    var dob            by remember(uiState.profile) { mutableStateOf(uiState.profile.dob) }
    var calculatedAge  by remember(uiState.profile) { mutableIntStateOf(uiState.profile.age) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val dobInteractionSource = remember { MutableInteractionSource() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadProfilePicture(it) } }

    if (dobInteractionSource.collectIsPressedAsState().value) {
        showDatePicker = true
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        dob = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                        val today = Calendar.getInstance()
                        var age = today.get(Calendar.YEAR) - cal.get(Calendar.YEAR)
                        if (today.get(Calendar.DAY_OF_YEAR) < cal.get(Calendar.DAY_OF_YEAR)) age--
                        calculatedAge = age.coerceAtLeast(0)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.logout(onLogoutComplete = onNavigateToLogin) }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Avatar hero header ────────────────────────────────────────────
            ProfileAvatarHeader(
                name               = name,
                role               = uiState.profile.role,
                avatarUrl          = uiState.profile.profilePictureUrl,
                isUploading        = uiState.isUploadingImage,
                onAvatarClick      = { imagePickerLauncher.launch("image/*") }
            )

            // ── Personal info card ────────────────────────────────────────────
            ProfileSection(title = "Personal Information") {

                ProfileTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = "Full Name",
                    leadingIcon   = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value                = if (dob.isNotEmpty()) "$dob (Age: $calculatedAge)" else "",
                    onValueChange        = {},
                    label                = "Date of Birth",
                    leadingIcon          = Icons.Outlined.CalendarToday,
                    readOnly             = true,
                    interactionSource    = dobInteractionSource,
                    trailingIcon         = {
                        Icon(Icons.Default.EditCalendar, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gender dropdown
                ExposedDropdownMenuBox(
                    expanded        = expandedGender,
                    onExpandedChange = { expandedGender = !expandedGender }
                ) {
                    ProfileTextField(
                        value         = gender,
                        onValueChange = {},
                        label         = "Gender",
                        leadingIcon   = Icons.Outlined.People,
                        readOnly      = true,
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGender) },
                        modifier      = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded        = expandedGender,
                        onDismissRequest = { expandedGender = false }
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text    = { Text(option) },
                                onClick = { gender = option; expandedGender = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileTextField(
                        value         = height,
                        onValueChange = { height = it },
                        label         = "Height (cm)",
                        leadingIcon   = Icons.Outlined.Height,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier      = Modifier.weight(1f)
                    )
                    ProfileTextField(
                        value         = weight,
                        onValueChange = { weight = it },
                        label         = "Weight (kg)",
                        leadingIcon   = Icons.Outlined.MonitorWeight,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier      = Modifier.weight(1f)
                    )
                }
            }

            // ── Feedback banners ──────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(uiState.error ?: "", color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
            AnimatedVisibility(visible = uiState.saveSuccess) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profile saved!", color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save + My Posts ────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick  = {
                        viewModel.saveProfile(
                            name, dob, calculatedAge, gender,
                            height.toFloatOrNull() ?: 0f,
                            weight.toFloatOrNull() ?: 0f
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    enabled  = !uiState.isSaving
                ) {
                    AnimatedContent(targetState = uiState.isSaving, label = "saveBtnContent") { saving ->
                        if (saving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Text("Save Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick  = onNavigateToMyPosts,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Article, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View My Posts", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            // ── Doctor / Patient portal section ───────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.profile.role == "doctor") {
                DoctorPortalSection(doctorId = uiState.profile.uid)
            } else {
                PatientCareSection(
                    assignedDoctorId = uiState.profile.assignedDoctorId,
                    linkSuccess      = uiState.linkSuccess,
                    onLink           = { viewModel.linkDoctor(it) },
                    onMessageDoctor  = {
                        onNavigateToChat(uiState.profile.uid, uiState.profile.assignedDoctorId!!, "My Doctor")
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE AVATAR HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileAvatarHeader(
    name: String,
    role: String,
    avatarUrl: String?,
    isUploading: Boolean,
    onAvatarClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 24.dp)) {
            Box(
                modifier         = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUploading -> CircularProgressIndicator(
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.5.dp
                    )
                    !avatarUrl.isNullOrEmpty() -> AsyncImage(
                        model              = avatarUrl,
                        contentDescription = "Profile Picture",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                    else -> Text(
                        text       = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "Tap to change photo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = name.ifEmpty { "Your Name" },
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                if (role == "doctor") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector        = Icons.Default.Verified,
                        contentDescription = "Verified Doctor",
                        tint               = Color(0xFF1DA1F2),
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            if (role == "doctor") {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text     = "Care Provider",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE SECTION CARD WRAPPER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        ElevatedCard(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(18.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE TEXT FIELD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    interactionSource: MutableInteractionSource? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        leadingIcon          = {
            Icon(leadingIcon, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        },
        trailingIcon         = trailingIcon,
        readOnly             = readOnly,
        keyboardOptions      = keyboardOptions,
        interactionSource    = interactionSource ?: remember { MutableInteractionSource() },
        modifier             = modifier,
        shape                = RoundedCornerShape(12.dp),
        singleLine           = true,
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DOCTOR PORTAL SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DoctorPortalSection(doctorId: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            "Doctor Portal",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        ElevatedCard(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(18.dp),
            elevation = CardDefaults.elevatedCardElevation(1.dp),
            colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Your Doctor ID", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(doctorId, style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Share this ID with your patients so they can link their account to yours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PATIENT CARE SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PatientCareSection(
    assignedDoctorId: String?,
    linkSuccess: Boolean,
    onLink: (String) -> Unit,
    onMessageDoctor: () -> Unit
) {
    var doctorIdInput by remember(assignedDoctorId) { mutableStateOf(assignedDoctorId ?: "") }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            "My Care Provider",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        ElevatedCard(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(18.dp),
            elevation = CardDefaults.elevatedCardElevation(1.dp),
            colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value         = doctorIdInput,
                        onValueChange = { doctorIdInput = it },
                        label         = { Text("Enter Doctor ID") },
                        modifier      = Modifier.weight(1f),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                    Button(
                        onClick  = { onLink(doctorIdInput) },
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(if (assignedDoctorId.isNullOrEmpty()) "Link" else "Update",
                            fontWeight = FontWeight.SemiBold)
                    }
                }

                AnimatedVisibility(visible = linkSuccess) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Doctor linked!", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (!assignedDoctorId.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick  = onMessageDoctor,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message My Doctor", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
