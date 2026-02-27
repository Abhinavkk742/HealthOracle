package com.healthoracle.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthoracle.core.util.ThemeMode
import com.healthoracle.core.util.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit, // <-- This is the parameter it was looking for!
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentTheme by ThemePreferences.themeMode.collectAsState()

    // Dialog States
    var showDeleteDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val isDeleting by viewModel.isDeleting.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ThemeOptionRow("System Default", Icons.Default.SettingsSystemDaydream, currentTheme == ThemeMode.SYSTEM) {
                        ThemePreferences.setThemeMode(context, ThemeMode.SYSTEM)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ThemeOptionRow("Light Mode", Icons.Default.LightMode, currentTheme == ThemeMode.LIGHT) {
                        ThemePreferences.setThemeMode(context, ThemeMode.LIGHT)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ThemeOptionRow("Dark Mode", Icons.Default.DarkMode, currentTheme == ThemeMode.DARK) {
                        ThemePreferences.setThemeMode(context, ThemeMode.DARK)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Data & Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { Toast.makeText(context, "Local Cache Cleared!", Toast.LENGTH_SHORT).show() }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cached, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Clear Local Cache", style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showDeleteDialog = true }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Delete Account", style = MaterialTheme.typography.bodyLarge, color = Color.Red)
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isDeleting) {
                        showDeleteDialog = false
                        deleteError = null
                        password = ""
                    }
                },
                title = { Text("Delete Account?") },
                text = {
                    Column {
                        Text("This action is permanent and cannot be undone. To confirm, please enter your password to re-authenticate.")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                deleteError = null
                            },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (deleteError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = deleteError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAccount(
                                password = password,
                                onSuccess = {
                                    showDeleteDialog = false
                                    Toast.makeText(context, "Account Deleted Successfully", Toast.LENGTH_SHORT).show()
                                    onNavigateToLogin() // <-- We trigger the navigation here!
                                },
                                onError = { errorMsg -> deleteError = errorMsg }
                            )
                        },
                        enabled = password.isNotBlank() && !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Red, strokeWidth = 2.dp)
                        } else {
                            Text("Delete Forever", color = Color.Red)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            deleteError = null
                            password = ""
                        },
                        enabled = !isDeleting
                    ) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ThemeOptionRow(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
    }
}