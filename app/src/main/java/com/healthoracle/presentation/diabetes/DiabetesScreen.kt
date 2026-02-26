package com.healthoracle.presentation.diabetes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiabetesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiSuggestion: (String) -> Unit,
    viewModel: DiabetesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Input states
    var highBP        by remember { mutableStateOf("0") }
    var highChol      by remember { mutableStateOf("0") }
    var cholCheck     by remember { mutableStateOf("1") }
    var bmi           by remember { mutableStateOf("25") }
    var smoker        by remember { mutableStateOf("0") }
    var stroke        by remember { mutableStateOf("0") }
    var heartDisease  by remember { mutableStateOf("0") }
    var physActivity  by remember { mutableStateOf("1") }
    var fruits        by remember { mutableStateOf("1") }
    var veggies       by remember { mutableStateOf("1") }
    var hvyAlcohol    by remember { mutableStateOf("0") }
    var anyHealthcare by remember { mutableStateOf("1") }
    var noDocCost     by remember { mutableStateOf("0") }
    var genHlth       by remember { mutableStateOf("3") }
    var mentHlth      by remember { mutableStateOf("0") }
    var physHlth      by remember { mutableStateOf("0") }
    var diffWalk      by remember { mutableStateOf("0") }
    var sex           by remember { mutableStateOf("0") }
    var age           by remember { mutableStateOf("5") }
    var education     by remember { mutableStateOf("4") }
    var income        by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diabetes Predictor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Enter Health Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Binary fields: 0 = No, 1 = Yes",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Binary Yes/No fields ──────────────────────────────────
            SectionHeader("Basic Health")
            DiabetesInputField("High Blood Pressure (0/1)", highBP) { highBP = it }
            DiabetesInputField("High Cholesterol (0/1)", highChol) { highChol = it }
            DiabetesInputField("Cholesterol Check in 5 Years (0/1)", cholCheck) { cholCheck = it }
            DiabetesInputField("BMI (e.g. 25)", bmi) { bmi = it }

            SectionHeader("Lifestyle")
            DiabetesInputField("Smoker (0/1)", smoker) { smoker = it }
            DiabetesInputField("Physical Activity (0/1)", physActivity) { physActivity = it }
            DiabetesInputField("Fruits Daily (0/1)", fruits) { fruits = it }
            DiabetesInputField("Vegetables Daily (0/1)", veggies) { veggies = it }
            DiabetesInputField("Heavy Alcohol Consumption (0/1)", hvyAlcohol) { hvyAlcohol = it }

            SectionHeader("Medical History")
            DiabetesInputField("Stroke (0/1)", stroke) { stroke = it }
            DiabetesInputField("Heart Disease or Attack (0/1)", heartDisease) { heartDisease = it }
            DiabetesInputField("Difficulty Walking (0/1)", diffWalk) { diffWalk = it }

            SectionHeader("Healthcare Access")
            DiabetesInputField("Any Healthcare Coverage (0/1)", anyHealthcare) { anyHealthcare = it }
            DiabetesInputField("Couldn't See Doctor Due to Cost (0/1)", noDocCost) { noDocCost = it }

            SectionHeader("General Health (Scales)")
            DiabetesInputField("General Health (1=Excellent to 5=Poor)", genHlth) { genHlth = it }
            DiabetesInputField("Mental Health (Days bad in last 30, 0-30)", mentHlth) { mentHlth = it }
            DiabetesInputField("Physical Health (Days bad in last 30, 0-30)", physHlth) { physHlth = it }

            SectionHeader("Demographics")
            DiabetesInputField("Sex (0=Female, 1=Male)", sex) { sex = it }
            DiabetesInputField("Age Group (1-13, e.g. 5=40-44)", age) { age = it }
            DiabetesInputField("Education Level (1-6)", education) { education = it }
            DiabetesInputField("Income Level (1-8)", income) { income = it }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Predict Button ────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.predict(
                        highBP        = highBP.toFloatOrNull() ?: 0f,
                        highChol      = highChol.toFloatOrNull() ?: 0f,
                        cholCheck     = cholCheck.toFloatOrNull() ?: 1f,
                        bmi           = bmi.toFloatOrNull() ?: 25f,
                        smoker        = smoker.toFloatOrNull() ?: 0f,
                        stroke        = stroke.toFloatOrNull() ?: 0f,
                        heartDisease  = heartDisease.toFloatOrNull() ?: 0f,
                        physActivity  = physActivity.toFloatOrNull() ?: 1f,
                        fruits        = fruits.toFloatOrNull() ?: 1f,
                        veggies       = veggies.toFloatOrNull() ?: 1f,
                        hvyAlcohol    = hvyAlcohol.toFloatOrNull() ?: 0f,
                        anyHealthcare = anyHealthcare.toFloatOrNull() ?: 1f,
                        noDocCost     = noDocCost.toFloatOrNull() ?: 0f,
                        genHlth       = genHlth.toFloatOrNull() ?: 3f,
                        mentHlth      = mentHlth.toFloatOrNull() ?: 0f,
                        physHlth      = physHlth.toFloatOrNull() ?: 0f,
                        diffWalk      = diffWalk.toFloatOrNull() ?: 0f,
                        sex           = sex.toFloatOrNull() ?: 0f,
                        age           = age.toFloatOrNull() ?: 5f,
                        education     = education.toFloatOrNull() ?: 4f,
                        income        = income.toFloatOrNull() ?: 5f
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Predict Diabetes Risk", fontSize = 16.sp)
                }
            }

            // ── Result Card ───────────────────────────────────────────
            uiState.result?.let { result ->
                ResultCard(
                    result = result,
                    onGetAiSuggestion = {
                        onNavigateToAiSuggestion(result.riskLevel)
                    }
                )
            }

            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ResultCard(
    result: com.healthoracle.data.local.DiabetesResult,
    onGetAiSuggestion: () -> Unit
) {
    val containerColor = when {
        !result.isDiabetic && result.confidence > 0.7f -> Color(0xFF4CAF50)
        result.riskLevel == "Low Risk"      -> Color(0xFF4CAF50)
        result.riskLevel == "Moderate Risk" -> Color(0xFFFF9800)
        result.riskLevel == "High Risk"     -> Color(0xFFF44336)
        else                                -> Color(0xFFB71C1C)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Prediction Result",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (result.isDiabetic) "Diabetic Risk Detected" else "No Diabetes Detected",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = containerColor
            )
            Text(
                text = result.riskLevel,
                fontSize = 16.sp,
                color = containerColor
            )
            Text(
                text = "Confidence: ${"%.1f".format(result.confidence * 100)}%",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onGetAiSuggestion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get AI Suggestions")
            }

            Text(
                text = "⚠️ This is an AI-based prediction for informational purposes only. Please consult a licensed doctor for an accurate diagnosis.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun DiabetesInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
