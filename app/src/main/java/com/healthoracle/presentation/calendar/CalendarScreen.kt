package com.healthoracle.presentation.calendar

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                },
                actions = {
                    // NEW: Download from Cloud Button
                    IconButton(
                        onClick = {
                            viewModel.downloadFromCloud { success, message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Download from Cloud")
                    }

                    IconButton(
                        onClick = {
                            viewModel.syncToCloud { success, message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Sync to Cloud")
                    }

                    IconButton(
                        onClick = {
                            scheduler.scheduleTestNotification()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Test notification arriving in 5 seconds...")
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "Test Notification")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
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
            Spacer(modifier = Modifier.height(8.dp))

            CalendarHeader(
                currentMonth = currentMonth,
                onMonthChange = { currentMonth = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DaysOfWeekHeader()

            Spacer(modifier = Modifier.height(8.dp))

            CalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                appointments = appointments,
                onDateSelected = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            SelectedDateDetails(
                selectedDate = selectedDate,
                dailyAppointments = appointments[selectedDate] ?: emptyList(),
                onDeleteAppointment = { appointment ->
                    viewModel.deleteAppointment(appointment) { alarmId ->
                        scheduler.cancel(alarmId)
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = newAppointmentTitle,
                            onValueChange = { newAppointmentTitle = it },
                            label = { Text("Title (e.g., Checkup)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
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
                                    date = selectedDate
                                ) { alarmId ->
                                    scheduler.schedule(
                                        appointmentId = alarmId,
                                        title = newAppointmentTitle,
                                        timeStr = formattedTime,
                                        date = selectedDate
                                    )
                                }
                                newAppointmentTitle = ""
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
fun CalendarHeader(currentMonth: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
        }

        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        Text(
            text = currentMonth.format(formatter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

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
                style = MaterialTheme.typography.bodyMedium,
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
                val hasAppointments = appointments.containsKey(date) && appointments[date]!!.isNotEmpty()

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
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
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                        )

                        if (hasAppointments) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedDateDetails(
    selectedDate: LocalDate,
    dailyAppointments: List<AppointmentEntity>,
    onDeleteAppointment: (AppointmentEntity) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
            Text(
                text = selectedDate.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (dailyAppointments.isEmpty()) {
                Text(
                    text = "No appointments scheduled for this date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                dailyAppointments.forEachIndexed { index, appointment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appointment.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = appointment.time,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteAppointment(appointment) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Appointment",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (index < dailyAppointments.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}