package com.healthoracle.presentation.calendar

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthoracle.data.local.entity.AppointmentEntity
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scheduler = remember { NotificationScheduler(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val appointments by viewModel.appointments.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newAppointmentTitle by remember { mutableStateOf("") }
    var newAppointmentDescription by remember { mutableStateOf("") }

    val categories = listOf("Doctor Visit", "Medication", "Lab Test", "Routine")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    val currentTime = remember { LocalTime.now() }
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.hour,
        initialMinute = currentTime.minute,
        is24Hour = false
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Appointments") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Appointment")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            CalendarHeader(
                currentMonth = currentMonth,
                onMonthChange = { currentMonth = it },
                onJumpToToday = {
                    currentMonth = YearMonth.now()
                    selectedDate = LocalDate.now()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            DaysOfWeekHeader()

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = currentMonth,
                transitionSpec = {
                    if (targetState.isAfter(initialState)) {
                        slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut()
                    }
                },
                label = "CalendarAnimation"
            ) { animatedMonth ->
                CalendarGrid(
                    currentMonth = animatedMonth,
                    selectedDate = selectedDate,
                    appointments = appointments,
                    onDateSelected = { selectedDate = it }
                )
            }

            // Tightened up the spacing between the calendar and the card to give maximum room
            Spacer(modifier = Modifier.height(12.dp))

            SelectedDateDetails(
                selectedDate = selectedDate,
                dailyAppointments = appointments[selectedDate] ?: emptyList(),
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp),
                onDeleteAppointment = { appointment ->
                    viewModel.deleteAppointment(appointment) { alarmId ->
                        scheduler.cancel(alarmId)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Appointment deleted")
                        }
                    }
                }
            )
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Appointment") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = newAppointmentTitle,
                            onValueChange = { newAppointmentTitle = it },
                            label = { Text("Title (e.g., Dentist)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newAppointmentDescription,
                            onValueChange = { newAppointmentDescription = it },
                            label = { Text("Notes/Details (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Select Time",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeInput(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newAppointmentTitle.isNotBlank()) {
                                val hour = timePickerState.hour
                                val minute = timePickerState.minute
                                val isPm = hour >= 12
                                val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                                val amPm = if (isPm) "PM" else "AM"
                                val formattedTime = String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm)

                                viewModel.addAppointment(
                                    title = newAppointmentTitle,
                                    time = formattedTime,
                                    date = selectedDate,
                                    category = selectedCategory,
                                    description = newAppointmentDescription
                                ) { alarmId ->
                                    scheduler.schedule(
                                        appointmentId = alarmId,
                                        title = newAppointmentTitle,
                                        timeStr = formattedTime,
                                        date = selectedDate
                                    )
                                }
                                newAppointmentTitle = ""
                                newAppointmentDescription = ""
                                selectedCategory = categories[0]
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            newAppointmentTitle = ""
                            newAppointmentDescription = ""
                            showAddDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CalendarHeader(currentMonth: YearMonth, onMonthChange: (YearMonth) -> Unit, onJumpToToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
            Text(
                text = currentMonth.format(formatter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onJumpToToday) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Jump to Today",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    appointments: Map<LocalDate, List<AppointmentEntity>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value

    val emptyDaysBefore = firstDayOfMonth - 1
    val totalCells = emptyDaysBefore + daysInMonth

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(totalCells) { index ->
            if (index < emptyDaysBefore) {
                Spacer(modifier = Modifier.size(40.dp))
            } else {
                val day = index - emptyDaysBefore + 1
                val date = currentMonth.atDay(day)
                val isSelected = date == selectedDate
                val isToday = date == LocalDate.now()
                val dailyAppts = appointments[date] ?: emptyList()

                val hasDoctorVisit = dailyAppts.any { it.category == "Doctor Visit" }
                val importantAppts = dailyAppts.filter { it.category != "Routine" }

                Box(
                    modifier = Modifier
                        // REMOVED .aspectRatio(1f) and replaced with a fixed, compact height
                        .height(48.dp)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp)) // Sleeker rounded rectangles instead of circles
                        .background(
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                hasDoctorVisit -> Color(0xFFE53935).copy(alpha = 0.1f)
                                else -> Color.Transparent
                            }
                        )
                        .then(
                            if (hasDoctorVisit && !isSelected) {
                                Modifier.border(1.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day.toString(),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                hasDoctorVisit -> Color(0xFFE53935)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isToday || hasDoctorVisit) FontWeight.Bold else FontWeight.Normal
                        )

                        if (importantAppts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val distinctCategories = importantAppts.map { it.category }.distinct().take(3)
                                distinctCategories.forEach { category ->
                                    val (_, color) = getCategoryStyle(category)
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else color
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getCategoryStyle(category: String): Pair<ImageVector, Color> {
    return when(category) {
        "Medication" -> Icons.Default.Medication to Color(0xFF4CAF50)
        "Lab Test" -> Icons.Default.Science to Color(0xFF9C27B0)
        "Doctor Visit" -> Icons.Default.LocalHospital to Color(0xFFE53935)
        "Routine" -> Icons.Default.CheckCircle to Color(0xFF607D8B)
        else -> Icons.Default.MedicalServices to Color(0xFF006688)
    }
}

@Composable
fun SelectedDateDetails(
    selectedDate: LocalDate,
    dailyAppointments: List<AppointmentEntity>,
    modifier: Modifier = Modifier,
    onDeleteAppointment: (AppointmentEntity) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val mainAppointments = dailyAppointments.filter { it.category != "Routine" }
    val routineTasks = dailyAppointments.filter { it.category == "Routine" }

    val activeList = if (selectedTabIndex == 0) mainAppointments else routineTasks

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp) // Tightened internal padding
        ) {
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
            Text(
                text = selectedDate.format(formatter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Appointments", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Daily Tasks", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = "Clear",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedTabIndex == 0) "No appointments today!" else "No tasks today!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeList.forEach { appointment ->
                        val (icon, tintColor) = getCategoryStyle(category = appointment.category)
                        val isDoctorVisit = appointment.category == "Doctor Visit"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(tintColor.copy(alpha = if (isDoctorVisit) 0.15f else 0.1f))
                                .then(
                                    if (isDoctorVisit) Modifier.border(1.5.dp, tintColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                    else Modifier
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(tintColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tintColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appointment.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = appointment.time,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (isDoctorVisit) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(tintColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "DOCTOR",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tintColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                if (appointment.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = appointment.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteAppointment(appointment) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Appointment",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}