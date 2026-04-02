package com.healthoracle.presentation.diabetes

// ─── DiabetesScreen ───────────────────────────────────────────────────────────
// All prediction logic, sliders, switches, tabs, SpeedometerGauge — unchanged.
// UI: cleaner section cards, improved header for upload tab, polished result card.

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthoracle.data.local.DiabetesResult
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiabetesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiSuggestion: (String) -> Unit = {},
    viewModel: DiabetesViewModel = hiltViewModel()
) {
    val uiState            by viewModel.uiState.collectAsState()
    var selectedTabIndex   by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diabetes Risk Check", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 },
                    text = { Text("Manual Entry", fontWeight = FontWeight.SemiBold) })
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 },
                    text = { Text("Upload Report", fontWeight = FontWeight.SemiBold) })
            }

            if (selectedTabIndex == 0) {
                ManualEntryView(uiState, viewModel, onNavigateToAiSuggestion)
            } else {
                UploadReportView(uiState, viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UPLOAD REPORT VIEW  (logic identical)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UploadReportView(uiState: DiabetesUiState, viewModel: DiabetesViewModel) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.analyzeReport(bitmap)
            }
        }
    )

    Column(
        modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Illustration box
        Box(
            modifier         = Modifier.size(120.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DocumentScanner, null,
                modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("AI Report Analyzer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Upload a photo of your blood test or lab report. Our AI will extract glucose and HbA1c levels to assess your diabetes risk.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick  = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.FileUpload, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Report Image", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isReportLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Scanning document...", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        uiState.reportError?.let { error ->
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()) {
                Text("$error", color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
        }

        AnimatedVisibility(visible = uiState.reportResult != null, enter = fadeIn() + slideInVertically()) {
            uiState.reportResult?.let { resultText ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.elevatedCardElevation(2.dp),
                    colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(resultText, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        modifier = Modifier.padding(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MANUAL ENTRY VIEW  (all slider/switch state + logic identical)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ManualEntryView(
    uiState: DiabetesUiState,
    viewModel: DiabetesViewModel,
    onNavigateToAiSuggestion: (String) -> Unit
) {
    var highBP         by remember { mutableFloatStateOf(0f) }
    var highChol       by remember { mutableFloatStateOf(0f) }
    var cholCheck      by remember { mutableFloatStateOf(1f) }
    var bmi            by remember { mutableFloatStateOf(26f) }
    var smoker         by remember { mutableFloatStateOf(0f) }
    var stroke         by remember { mutableFloatStateOf(0f) }
    var heartDisease   by remember { mutableFloatStateOf(0f) }
    var physActivity   by remember { mutableFloatStateOf(1f) }
    var fruits         by remember { mutableFloatStateOf(1f) }
    var veggies        by remember { mutableFloatStateOf(1f) }
    var hvyAlcohol     by remember { mutableFloatStateOf(0f) }
    var anyHealthcare  by remember { mutableFloatStateOf(1f) }
    var noDocbcCost    by remember { mutableFloatStateOf(0f) }
    var genHlth        by remember { mutableFloatStateOf(2f) }
    var mentHlth       by remember { mutableFloatStateOf(0f) }
    var physHlth       by remember { mutableFloatStateOf(0f) }
    var diffWalk       by remember { mutableFloatStateOf(0f) }
    var sex            by remember { mutableFloatStateOf(0f) }
    var age            by remember { mutableFloatStateOf(5f) }
    var education      by remember { mutableFloatStateOf(5f) }
    var income         by remember { mutableFloatStateOf(5f) }

    LaunchedEffect(uiState.userProfile) {
        uiState.userProfile?.let { p ->
            if (p.gender.equals("Male", ignoreCase = true))   sex = 1f
            else if (p.gender.equals("Female", ignoreCase = true)) sex = 0f
            if (p.weightKg > 0 && p.heightCm > 0) {
                val h = p.heightCm / 100f
                bmi = (p.weightKg / (h * h)).coerceIn(10f, 80f)
            }
            if (p.age > 0) age = when (p.age) {
                in 0..24  -> 1f; in 25..29 -> 2f; in 30..34 -> 3f; in 35..39 -> 4f
                in 40..44 -> 5f; in 45..49 -> 6f; in 50..54 -> 7f; in 55..59 -> 8f
                in 60..64 -> 9f; in 65..69 -> 10f; in 70..74 -> 11f; in 75..79 -> 12f
                else      -> 13f
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SectionHeader("Cardiovascular")
        DiabetesCard {
            BinarySwitch("High Blood Pressure",            highBP)       { highBP = it }
            BinarySwitch("High Cholesterol",               highChol)     { highChol = it }
            BinarySwitch("Cholesterol Check (last 5 yrs)", cholCheck)    { cholCheck = it }
            BinarySwitch("Heart Disease or Attack",        heartDisease) { heartDisease = it }
            BinarySwitch("Stroke History",                 stroke)       { stroke = it }
        }

        SectionHeader("Lifestyle")
        DiabetesCard {
            ContinuousSlider("BMI", bmi, 10f, 80f, "%.0f") { bmi = it }
            BinarySwitch("Smoker (100+ cigarettes lifetime)", smoker)      { smoker = it }
            BinarySwitch("Heavy Alcohol Consumption",         hvyAlcohol)  { hvyAlcohol = it }
            BinarySwitch("Physical Activity (last 30 days)", physActivity) { physActivity = it }
            BinarySwitch("Fruits (1+ per day)",               fruits)      { fruits = it }
            BinarySwitch("Vegetables (1+ per day)",           veggies)     { veggies = it }
        }

        SectionHeader("Health Status")
        DiabetesCard {
            SteppedSlider("General Health", genHlth,
                listOf("Excellent","Very Good","Good","Fair","Poor"), 1f..5f) { genHlth = it }
            ContinuousSlider("Mental Unhealthy Days (last 30)", mentHlth, 0f, 30f, "%.0f") { mentHlth = it }
            ContinuousSlider("Physical Unhealthy Days (last 30)", physHlth, 0f, 30f, "%.0f") { physHlth = it }
            BinarySwitch("Difficulty Walking / Climbing Stairs", diffWalk) { diffWalk = it }
        }

        SectionHeader("Demographics")
        DiabetesCard {
            BinarySwitch("Any Healthcare Coverage",          anyHealthcare) { anyHealthcare = it }
            BinarySwitch("Couldn't See Doctor Due to Cost",  noDocbcCost)   { noDocbcCost = it }
            BinarySwitch("Male (turn off for Female)",       sex)           { sex = it }
            SteppedSlider("Age Group", age,
                listOf("18-24","25-29","30-34","35-39","40-44","45-49","50-54","55-59","60-64","65-69","70-74","75-79","80+"),
                1f..13f) { age = it }
            SteppedSlider("Education Level", education,
                listOf("None","Grades 1–8","Grades 9–11","Grade 12","College 1–3 yrs","College 4+ yrs"),
                1f..6f) { education = it }
            SteppedSlider("Income Level", income,
                listOf("<\$10K","\$10-15K","\$15-20K","\$20-25K","\$25-35K","\$35-50K","\$50-75K",">\$75K"),
                1f..8f) { income = it }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick  = {
                viewModel.predict(highBP, highChol, cholCheck, bmi, smoker, stroke, heartDisease,
                    physActivity, fruits, veggies, hvyAlcohol, anyHealthcare, noDocbcCost, genHlth,
                    mentHlth, physHlth, diffWalk, sex, age, education, income)
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.Analytics, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Predict Diabetes Risk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        AnimatedVisibility(visible = uiState.result != null, enter = fadeIn() + slideInVertically()) {
            uiState.result?.let { result ->
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    ResultCard(result)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick  = {
                            onNavigateToAiSuggestion(
                                if (result.isDiabetic) "High Risk of Diabetes" else "Low Risk of Diabetes")
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Psychology, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get AI Health Suggestions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Text("Error: $error", color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CARD WRAPPER FOR DIABETES SECTIONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiabetesCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), content = content)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text     = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp, start = 4.dp),
        letterSpacing = 0.8.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BINARY SWITCH  (logic unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BinarySwitch(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier             = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment    = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked        = value >= 0.5f,
            onCheckedChange = { onValueChange(if (it) 1f else 0f) },
            colors         = SwitchDefaults.colors(
                checkedThumbColor  = MaterialTheme.colorScheme.primary,
                checkedTrackColor  = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTINUOUS SLIDER  (logic unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ContinuousSlider(label: String, value: Float, min: Float, max: Float, format: String, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(String.format(format, value), style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = min..max,
            modifier = Modifier.fillMaxWidth())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEPPED SLIDER  (logic unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SteppedSlider(label: String, value: Float, steps: List<String>, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    val index = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start) * (steps.size - 1))
        .toInt().coerceIn(0, steps.size - 1)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(steps[index], style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange,
            steps = steps.size - 2, modifier = Modifier.fillMaxWidth())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RESULT CARD  (logic/canvas unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ResultCard(result: DiabetesResult) {
    val isDiabetic       = result.isDiabetic
    val containerColor   = if (isDiabetic) MaterialTheme.colorScheme.errorContainer
                           else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    val riskPercentage   = if (isDiabetic) (result.confidence * 100f).coerceIn(60f, 100f)
                           else ((1f - result.confidence) * 100f).coerceIn(0f, 40f)
    val resultTitle      = if (isDiabetic) "High Risk Detected" else "Low Risk Detected"

    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        colors    = CardDefaults.elevatedCardColors(containerColor = containerColor)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SpeedometerGauge(riskPercentage = riskPercentage, resultText = resultTitle)
            Text("Risk Level: ${result.riskLevel}", style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold)
            Text("This is a screening tool only. Consult a doctor for diagnosis.",
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SPEEDOMETER GAUGE  (canvas logic unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SpeedometerGauge(riskPercentage: Float, resultText: String, modifier: Modifier = Modifier) {
    val animatedRisk = animateFloatAsState(
        targetValue   = riskPercentage,
        animationSpec = tween(durationMillis = 1000),
        label         = "RiskAnimation"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(220.dp).padding(16.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 18.dp.toPx()
                val center      = Offset(size.width / 2, size.height / 2)
                val radius      = (size.minDimension - strokeWidth) / 2

                drawArc(color = Color.LightGray.copy(alpha = 0.25f),
                    startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size    = Size(radius * 2, radius * 2),
                    style   = Stroke(width = strokeWidth, cap = StrokeCap.Round))

                val sweepAngle = (animatedRisk.value / 100f) * 180f
                val color = when {
                    animatedRisk.value < 40f -> Color(0xFF4CAF50)
                    animatedRisk.value < 70f -> Color(0xFFFFC107)
                    else                     -> Color(0xFFF44336)
                }
                drawArc(color = color, startAngle = 180f, sweepAngle = sweepAngle, useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size    = Size(radius * 2, radius * 2),
                    style   = Stroke(width = strokeWidth, cap = StrokeCap.Round))

                val needleAngle  = Math.toRadians((180.0 + sweepAngle))
                val needleLength = radius - 8.dp.toPx()
                drawLine(color = Color.DarkGray,
                    start       = center,
                    end         = Offset(center.x + needleLength * cos(needleAngle).toFloat(),
                        center.y + needleLength * sin(needleAngle).toFloat()),
                    strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(color = Color.DarkGray, radius = 7.dp.toPx(), center = center)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = 16.dp)) {
                Text("${animatedRisk.value.toInt()}%", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text("RISK", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(resultText, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
