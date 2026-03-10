package com.healthoracle.presentation.diabetes

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diabetes Predictor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Manual Entry", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Upload Report", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTabIndex == 0) {
                ManualEntryView(uiState, viewModel, onNavigateToAiSuggestion)
            } else {
                UploadReportView(uiState, viewModel)
            }
        }
    }
}

@Composable
fun UploadReportView(uiState: DiabetesUiState, viewModel: DiabetesViewModel) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            imageUri = uri
            uri?.let {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.analyzeReport(bitmap)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.Default.DocumentScanner,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AI Report Analyzer",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Upload a photo of your blood test or lab report, and our AI will extract your glucose and HbA1c levels to assess your risk.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Select Report Image", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isReportLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Scanning document and analyzing metrics...")
        }

        uiState.reportError?.let { error ->
            Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
        }

        AnimatedVisibility(
            visible = uiState.reportResult != null,
            enter = fadeIn() + slideInVertically()
        ) {
            uiState.reportResult?.let { resultText ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ManualEntryView(
    uiState: DiabetesUiState,
    viewModel: DiabetesViewModel,
    onNavigateToAiSuggestion: (String) -> Unit
) {
    var highBP by remember { mutableFloatStateOf(0f) }
    var highChol by remember { mutableFloatStateOf(0f) }
    var cholCheck by remember { mutableFloatStateOf(1f) }
    var bmi by remember { mutableFloatStateOf(26f) }
    var smoker by remember { mutableFloatStateOf(0f) }
    var stroke by remember { mutableFloatStateOf(0f) }
    var heartDisease by remember { mutableFloatStateOf(0f) }
    var physActivity by remember { mutableFloatStateOf(1f) }
    var fruits by remember { mutableFloatStateOf(1f) }
    var veggies by remember { mutableFloatStateOf(1f) }
    var hvyAlcohol by remember { mutableFloatStateOf(0f) }
    var anyHealthcare by remember { mutableFloatStateOf(1f) }
    var noDocbcCost by remember { mutableFloatStateOf(0f) }
    var genHlth by remember { mutableFloatStateOf(2f) }
    var mentHlth by remember { mutableFloatStateOf(0f) }
    var physHlth by remember { mutableFloatStateOf(0f) }
    var diffWalk by remember { mutableFloatStateOf(0f) }
    var sex by remember { mutableFloatStateOf(0f) }
    var age by remember { mutableFloatStateOf(5f) }
    var education by remember { mutableFloatStateOf(5f) }
    var income by remember { mutableFloatStateOf(5f) }

    LaunchedEffect(uiState.userProfile) {
        uiState.userProfile?.let { profile ->
            if (profile.gender.equals("Male", ignoreCase = true)) sex = 1f
            else if (profile.gender.equals("Female", ignoreCase = true)) sex = 0f

            if (profile.weightKg > 0 && profile.heightCm > 0) {
                val heightM = profile.heightCm / 100f
                val calculatedBmi = profile.weightKg / (heightM * heightM)
                bmi = calculatedBmi.coerceIn(10f, 80f)
            }

            if (profile.age > 0) {
                age = when (profile.age) {
                    in 0..24 -> 1f
                    in 25..29 -> 2f
                    in 30..34 -> 3f
                    in 35..39 -> 4f
                    in 40..44 -> 5f
                    in 45..49 -> 6f
                    in 50..54 -> 7f
                    in 55..59 -> 8f
                    in 60..64 -> 9f
                    in 65..69 -> 10f
                    in 70..74 -> 11f
                    in 75..79 -> 12f
                    else -> 13f
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader("❤️ Cardiovascular")
        BinarySwitch("High Blood Pressure", highBP) { highBP = it }
        BinarySwitch("High Cholesterol", highChol) { highChol = it }
        BinarySwitch("Cholesterol Check (last 5 yrs)", cholCheck) { cholCheck = it }
        BinarySwitch("Heart Disease or Attack", heartDisease) { heartDisease = it }
        BinarySwitch("Stroke History", stroke) { stroke = it }

        SectionHeader("🏃 Lifestyle")
        ContinuousSlider("BMI", bmi, 10f, 80f, "%.0f") { bmi = it }
        BinarySwitch("Smoker (100+ cigarettes lifetime)", smoker) { smoker = it }
        BinarySwitch("Heavy Alcohol Consumption", hvyAlcohol) { hvyAlcohol = it }
        BinarySwitch("Physical Activity (last 30 days)", physActivity) { physActivity = it }
        BinarySwitch("Fruits (1+ per day)", fruits) { fruits = it }
        BinarySwitch("Vegetables (1+ per day)", veggies) { veggies = it }

        SectionHeader("🏥 Health Status")
        SteppedSlider(
            label = "General Health",
            value = genHlth,
            steps = listOf("Excellent", "Very Good", "Good", "Fair", "Poor"),
            valueRange = 1f..5f
        ) { genHlth = it }
        ContinuousSlider("Mental Unhealthy Days (last 30)", mentHlth, 0f, 30f, "%.0f") { mentHlth = it }
        ContinuousSlider("Physical Unhealthy Days (last 30)", physHlth, 0f, 30f, "%.0f") { physHlth = it }
        BinarySwitch("Difficulty Walking / Climbing Stairs", diffWalk) { diffWalk = it }

        SectionHeader("👤 Demographics")
        BinarySwitch("Any Healthcare Coverage", anyHealthcare) { anyHealthcare = it }
        BinarySwitch("Couldn't See Doctor Due to Cost", noDocbcCost) { noDocbcCost = it }
        BinarySwitch("Male (Turn off for Female)", sex) { sex = it }
        SteppedSlider(
            label = "Age Group",
            value = age,
            steps = listOf("18-24","25-29","30-34","35-39","40-44","45-49",
                "50-54","55-59","60-64","65-69","70-74","75-79","80+"),
            valueRange = 1f..13f
        ) { age = it }
        SteppedSlider(
            label = "Education Level",
            value = education,
            steps = listOf("None","Grades 1-8","Grades 9-11","Grade 12","College 1-3 yrs","College 4+ yrs"),
            valueRange = 1f..6f
        ) { education = it }
        SteppedSlider(
            label = "Income Level",
            value = income,
            steps = listOf("<$10K","$10-15K","$15-20K","$20-25K","$25-35K","$35-50K","$50-75K",">$75K"),
            valueRange = 1f..8f
        ) { income = it }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.predict(
                    highBP, highChol, cholCheck, bmi, smoker, stroke, heartDisease, physActivity,
                    fruits, veggies, hvyAlcohol, anyHealthcare, noDocbcCost, genHlth, mentHlth, physHlth,
                    diffWalk, sex, age, education, income
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Predict Diabetes Risk", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        AnimatedVisibility(
            visible = uiState.result != null,
            enter = fadeIn() + slideInVertically()
        ) {
            uiState.result?.let { result ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    ResultCard(result)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val conditionLabel = if (result.isDiabetic) "High Risk of Diabetes" else "Low Risk of Diabetes"
                            onNavigateToAiSuggestion(conditionLabel)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get AI Health Suggestions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Text(
                text = "Error: $error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.primaryContainer)
}

// --- NEW: Binary Switch for a much better user experience! ---
@Composable
fun BinarySwitch(label: String, value: Float, onValueChange: (Float) -> Unit) {
    val isChecked = value >= 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = isChecked,
            onCheckedChange = { onValueChange(if (it) 1f else 0f) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
fun ContinuousSlider(label: String, value: Float, min: Float, max: Float, format: String, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(String.format(format, value), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = min..max, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun SteppedSlider(label: String, value: Float, steps: List<String>, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    val index = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start) * (steps.size - 1)).toInt().coerceIn(0, steps.size - 1)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(steps[index], fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps.size - 2, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ResultCard(result: DiabetesResult) {
    val isDiabetic = result.isDiabetic
    val bgColor = if (isDiabetic) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
    val textColor = if (isDiabetic) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32)

    val riskPercentage = if (isDiabetic) {
        (result.confidence * 100).toFloat().coerceIn(60f, 100f)
    } else {
        ((1f - result.confidence) * 100).toFloat().coerceIn(0f, 40f)
    }

    val resultTitle = if (isDiabetic) "High Risk Detected" else "Low Risk Detected"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SpeedometerGauge(
                riskPercentage = riskPercentage,
                resultText = resultTitle
            )

            Text(
                text = "Risk Level: ${result.riskLevel}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )

            Text(
                text = "⚕️ This is a screening tool only. Consult a doctor for diagnosis.",
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SpeedometerGauge(
    riskPercentage: Float,
    resultText: String,
    modifier: Modifier = Modifier
) {
    val animatedRisk = animateFloatAsState(
        targetValue = riskPercentage,
        animationSpec = tween(durationMillis = 1000),
        label = "RiskAnimation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 20.dp.toPx()
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension - strokeWidth) / 2

                // Background Arc
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Colored Arc
                val sweepAngle = (animatedRisk.value / 100f) * 180f
                val color = when {
                    animatedRisk.value < 40f -> Color(0xFF4CAF50)
                    animatedRisk.value < 70f -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }

                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Needle
                val needleAngle = 180f + sweepAngle
                val needleLength = radius - 10.dp.toPx()
                val needleRad = Math.toRadians(needleAngle.toDouble())
                val endX = center.x + needleLength * cos(needleRad).toFloat()
                val endY = center.y + needleLength * sin(needleRad).toFloat()

                drawLine(
                    color = Color.DarkGray,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = Color.DarkGray,
                    radius = 8.dp.toPx(),
                    center = center
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = 20.dp)
            ) {
                Text(
                    text = "${animatedRisk.value.toInt()}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "RISK",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = resultText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}