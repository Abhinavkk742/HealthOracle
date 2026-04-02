package com.healthoracle.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.healthoracle.core.ui.components.AnimatedProgressBar
import com.healthoracle.core.ui.components.GlassCard
import com.healthoracle.core.ui.components.GradientCard
import com.healthoracle.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE SCREEN
// ─────────────────────────────────────────────────────────────────────────────

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

    // ── Form state (same fields as original, no logic change) ─────────────────
    var name           by remember(uiState.profile) { mutableStateOf(uiState.profile.name) }
    var height         by remember(uiState.profile) {
        mutableStateOf(if (uiState.profile.heightCm == 0f) "" else uiState.profile.heightCm.toString())
    }
    var weight         by remember(uiState.profile) {
        mutableStateOf(if (uiState.profile.weightKg == 0f) "" else uiState.profile.weightKg.toString())
    }
    var gender         by remember(uiState.profile) {
        mutableStateOf(if (uiState.profile.gender.isEmpty()) "Male" else uiState.profile.gender)
    }
    var expandedGender by remember { mutableStateOf(false) }
    val genderOptions  = listOf("Male", "Female", "Other")

    var dob              by remember(uiState.profile) { mutableStateOf(uiState.profile.dob) }
    var calculatedAge    by remember(uiState.profile) { mutableIntStateOf(uiState.profile.age) }
    var showDatePicker   by remember { mutableStateOf(false) }
    val datePickerState  = rememberDatePickerState()
    val dobInteraction   = remember { MutableInteractionSource() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadProfilePicture(it) } }

    if (dobInteraction.collectIsPressedAsState().value) showDatePicker = true

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
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
            // ── Avatar Hero ───────────────────────────────────────────────────
            ProfileHero(
                name        = name,
                role        = uiState.profile.role,
                age         = calculatedAge,
                avatarUrl   = uiState.profile.profilePictureUrl,
                isUploading = uiState.isUploadingImage,
                heightCm    = uiState.profile.heightCm,
                weightKg    = uiState.profile.weightKg,
                onAvatar    = { imagePickerLauncher.launch("image/*") }
            )

            Spacer(Modifier.height(20.dp))

            // ── Personal Information Card ──────────────────────────────────────
            ProfileSectionCard(
                title    = "Personal Information",
                icon     = Icons.Outlined.Person,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                ProfileOutlinedField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = "Full Name",
                    leadingIcon   = Icons.Outlined.Person
                )
                Spacer(Modifier.height(14.dp))

                ProfileOutlinedField(
                    value             = if (dob.isNotEmpty()) "$dob (Age: $calculatedAge)" else "",
                    onValueChange     = {},
                    label             = "Date of Birth",
                    leadingIcon       = Icons.Outlined.CalendarToday,
                    readOnly          = true,
                    interactionSource = dobInteraction,
                    trailingIcon      = {
                        Icon(Icons.Default.EditCalendar, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                )
                Spacer(Modifier.height(14.dp))

                // Gender dropdown — preserved exactly
                ExposedDropdownMenuBox(
                    expanded        = expandedGender,
                    onExpandedChange = { expandedGender = !expandedGender }
                ) {
                    ProfileOutlinedField(
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
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileOutlinedField(
                        value           = height,
                        onValueChange   = { height = it },
                        label           = "Height (cm)",
                        leadingIcon     = Icons.Outlined.Height,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier        = Modifier.weight(1f)
                    )
                    ProfileOutlinedField(
                        value           = weight,
                        onValueChange   = { weight = it },
                        label           = "Weight (kg)",
                        leadingIcon     = Icons.Outlined.MonitorWeight,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier        = Modifier.weight(1f)
                    )
                }

                // BMI display if both entered
                val bmi = calculateBmi(height, weight)
                if (bmi != null) {
                    Spacer(Modifier.height(12.dp))
                    BmiIndicator(bmi = bmi)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Feedback banners ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.error != null,
                enter   = slideInVertically() + fadeIn(),
                exit    = slideOutVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.saveSuccess,
                enter   = slideInVertically() + fadeIn(),
                exit    = slideOutVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AccentGreen.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, AccentGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Profile saved successfully!",
                            color      = AccentGreen,
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Action Buttons ────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = {
                        viewModel.saveProfile(
                            name, dob, calculatedAge, gender,
                            height.toFloatOrNull() ?: 0f,
                            weight.toFloatOrNull() ?: 0f
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape   = RoundedCornerShape(14.dp),
                    enabled = !uiState.isSaving,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    AnimatedContent(targetState = uiState.isSaving, label = "saveBtn") { saving ->
                        if (saving) {
                            CircularProgressIndicator(
                                color       = MaterialTheme.colorScheme.onPrimary,
                                modifier    = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save Profile", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick  = onNavigateToMyPosts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Article, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("My Forum Posts", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(20.dp))

            // ── Role-based section ────────────────────────────────────────────
            if (uiState.profile.role == "doctor") {
                DoctorPortalCard(
                    doctorId = uiState.profile.uid,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                PatientCareCard(
                    assignedDoctorId = uiState.profile.assignedDoctorId,
                    linkSuccess      = uiState.linkSuccess,
                    onLink           = { viewModel.linkDoctor(it) },
                    onMessageDoctor  = {
                        onNavigateToChat(uiState.profile.uid, uiState.profile.assignedDoctorId!!, "My Doctor")
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE HERO HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHero(
    name: String,
    role: String,
    age: Int,
    avatarUrl: String?,
    isUploading: Boolean,
    heightCm: Float,
    weightKg: Float,
    onAvatar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(top = 8.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar
            Box(
                modifier         = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, AccentViolet)
                        )
                    )
                    .border(
                        BorderStroke(
                            2.5.dp,
                            Brush.linearGradient(listOf(Primary, Teal))
                        ),
                        CircleShape
                    )
                    .clickable(onClick = onAvatar),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUploading -> CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(30.dp),
                        strokeWidth = 2.5.dp
                    )
                    !avatarUrl.isNullOrEmpty() -> AsyncImage(
                        model              = avatarUrl,
                        contentDescription = "Profile photo",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                    else -> Text(
                        text       = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Tap to change photo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Name + verified badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = name.ifEmpty { "Your Name" },
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                if (role == "doctor") {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector        = Icons.Default.Verified,
                        contentDescription = "Verified Doctor",
                        tint               = Color(0xFF1DA1F2),
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Role pill
            val (pillText, pillBg, pillFg) = if (role == "doctor") {
                Triple("Care Provider", MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary)
            } else {
                Triple("Patient", MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), MaterialTheme.colorScheme.secondary)
            }
            Surface(shape = RoundedCornerShape(50), color = pillBg) {
                Text(
                    text       = pillText,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = pillFg,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Stat strip (age / height / weight)
            if (age > 0 || heightCm > 0f || weightKg > 0f) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier              = Modifier.padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    StatPill("🎂", if (age > 0) "${age}y" else "--", "Age",     modifier = Modifier.weight(1f))
                    VerticalDivider(modifier = Modifier.height(36.dp).padding(vertical = 4.dp), thickness = 0.5.dp)
                    StatPill("📏", if (heightCm > 0f) "${heightCm.toInt()}cm" else "--", "Height", modifier = Modifier.weight(1f))
                    VerticalDivider(modifier = Modifier.height(36.dp).padding(vertical = 4.dp), thickness = 0.5.dp)
                    StatPill("⚖️", if (weightKg > 0f) "${weightKg.toInt()}kg" else "--", "Weight", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatPill(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 14.sp)
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BMI INDICATOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BmiIndicator(bmi: Float) {
    val (label, color) = when {
        bmi < 18.5f -> "Underweight" to AccentAmber
        bmi < 25f   -> "Healthy Weight" to AccentGreen
        bmi < 30f   -> "Overweight" to AccentAmber
        else        -> "Obese" to AccentRose
    }
    val progress = (bmi / 40f).coerceIn(0f, 1f)

    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = color.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "BMI: ${"%.1f".format(bmi)}",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = color
                )
                Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                    Text(
                        text     = label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            AnimatedProgressBar(progress = progress, color = color)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE SECTION CARD WRAPPER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(start = 4.dp, bottom = 10.dp)
        ) {
            Icon(icon, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE OUTLINED TEXT FIELD  (identical API to original)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileOutlinedField(
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
        value             = value,
        onValueChange     = onValueChange,
        label             = { Text(label) },
        leadingIcon       = {
            Icon(leadingIcon, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp))
        },
        trailingIcon      = trailingIcon,
        readOnly          = readOnly,
        keyboardOptions   = keyboardOptions,
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        modifier          = modifier,
        shape             = RoundedCornerShape(12.dp),
        singleLine        = true,
        colors            = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            focusedLabelColor    = MaterialTheme.colorScheme.primary,
            cursorColor          = MaterialTheme.colorScheme.primary
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DOCTOR PORTAL CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DoctorPortalCard(doctorId: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(start = 4.dp, bottom = 10.dp)
        ) {
            Icon(Icons.Default.MedicalServices, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Doctor Portal",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary)
        }

        GradientCard(
            modifier       = Modifier.fillMaxWidth(),
            gradientColors = listOf(Color(0xFF1E1B4B), Color(0xFF13172E)),
            shape          = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Badge, null,
                            tint = PrimaryLight, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Your Doctor ID",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.6f))
                        Spacer(Modifier.height(2.dp))
                        Text(doctorId,
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color      = PrimaryLight,
                            maxLines   = 1)
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Share this ID with your patients so they can link to your care.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PATIENT CARE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PatientCareCard(
    assignedDoctorId: String?,
    linkSuccess: Boolean,
    onLink: (String) -> Unit,
    onMessageDoctor: () -> Unit,
    modifier: Modifier = Modifier
) {
    var doctorIdInput by remember(assignedDoctorId) { mutableStateOf(assignedDoctorId ?: "") }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(start = 4.dp, bottom = 10.dp)
        ) {
            Icon(Icons.Outlined.LocalHospital, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("My Care Provider",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary)
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Current status indicator
                if (!assignedDoctorId.isNullOrEmpty()) {
                    Surface(
                        shape  = RoundedCornerShape(12.dp),
                        color  = AccentGreen.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, AccentGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier          = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = AccentGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Doctor linked",
                                    style      = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = AccentGreen)
                                Text(assignedDoctorId,
                                    style   = MaterialTheme.typography.bodySmall,
                                    color   = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Doctor ID input + link button
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value         = doctorIdInput,
                        onValueChange = { doctorIdInput = it },
                        label         = { Text(if (assignedDoctorId.isNullOrEmpty()) "Enter Doctor ID" else "Update Doctor ID") },
                        modifier      = Modifier.weight(1f),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                    Button(
                        onClick  = { onLink(doctorIdInput) },
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            if (assignedDoctorId.isNullOrEmpty()) "Link" else "Update",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                AnimatedVisibility(visible = linkSuccess) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = AccentGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Doctor linked successfully!",
                            color      = AccentGreen,
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (!assignedDoctorId.isNullOrEmpty()) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick  = onMessageDoctor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Chat, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Message My Doctor", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

private fun calculateBmi(heightStr: String, weightStr: String): Float? {
    val h = heightStr.toFloatOrNull() ?: return null
    val w = weightStr.toFloatOrNull() ?: return null
    if (h <= 0f || w <= 0f) return null
    val hM = h / 100f
    return w / (hM * hM)
}
