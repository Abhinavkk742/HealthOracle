package com.healthoracle.presentation.diabetes

import com.healthoracle.data.local.DiabetesResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiabetesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiSuggestion: (String) -> Unit = {},
    viewModel: DiabetesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diabetes Predictor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {  // ✅ FIXED
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader("❤️ Cardiovascular")
            BinarySlider("High Blood Pressure", highBP) { highBP = it }
            BinarySlider("High Cholesterol", highChol) { highChol = it }
            BinarySlider("Cholesterol Check (last 5 yrs)", cholCheck) { cholCheck = it }
            BinarySlider("Heart Disease or Attack", heartDisease) { heartDisease = it }
            BinarySlider("Stroke History", stroke) { stroke = it }

            SectionHeader("🏃 Lifestyle")
            ContinuousSlider("BMI", bmi, 10f, 80f, "%.0f") { bmi = it }
            BinarySlider("Smoker (100+ cigarettes lifetime)", smoker) { smoker = it }
            BinarySlider("Heavy Alcohol Consumption", hvyAlcohol) { hvyAlcohol = it }
            BinarySlider("Physical Activity (last 30 days)", physActivity) { physActivity = it }
            BinarySlider("Fruits (1+ per day)", fruits) { fruits = it }
            BinarySlider("Vegetables (1+ per day)", veggies) { veggies = it }

            SectionHeader("🏥 Health Status")
            SteppedSlider(
                label = "General Health",
                value = genHlth,
                steps = listOf("Excellent", "Very Good", "Good", "Fair", "Poor"),
                valueRange = 1f..5f
            ) { genHlth = it }
            ContinuousSlider("Mental Unhealthy Days (last 30)", mentHlth, 0f, 30f, "%.0f") { mentHlth = it }
            ContinuousSlider("Physical Unhealthy Days (last 30)", physHlth, 0f, 30f, "%.0f") { physHlth = it }
            BinarySlider("Difficulty Walking / Climbing Stairs", diffWalk) { diffWalk = it }

            SectionHeader("👤 Demographics")
            BinarySlider("Any Healthcare Coverage", anyHealthcare) { anyHealthcare = it }
            BinarySlider("Couldn't See Doctor Due to Cost", noDocbcCost) { noDocbcCost = it }
            BinarySlider("Sex (0 = Female, 1 = Male)", sex) { sex = it }
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
                        highBP, highChol, cholCheck, bmi,
                        smoker, stroke, heartDisease, physActivity,
                        fruits, veggies, hvyAlcohol, anyHealthcare,
                        noDocbcCost, genHlth, mentHlth, physHlth,
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
                    ResultCard(result)
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

@Composable
fun BinarySlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    val displayValue = if (value >= 0.5f) "Yes" else "No"
    val displayColor = if (value >= 0.5f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(displayValue, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = displayColor)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 0,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ContinuousSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    format: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                String.format(format, value),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SteppedSlider(
    label: String,
    value: Float,
    steps: List<String>,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val index = ((value - valueRange.start) /
            (valueRange.endInclusive - valueRange.start) * (steps.size - 1))
        .toInt().coerceIn(0, steps.size - 1)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                steps[index],
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps.size - 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ResultCard(result: DiabetesResult) {
    val isDiabetic = result.isDiabetic
    val bgColor = if (isDiabetic) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
    val textColor = if (isDiabetic) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32)

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isDiabetic) "⚠️ Diabetic Risk Detected" else "✅ No Diabetes Detected",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = "Confidence: ${"%.1f".format(result.confidence * 100)}%",
                fontSize = 14.sp,
                color = textColor
            )
            Text(
                text = "Risk Level: ${result.riskLevel}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = "⚕️ This is a screening tool only. Consult a doctor for diagnosis.",
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}
