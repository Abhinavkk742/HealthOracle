package com.healthoracle.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context      = LocalContext.current
    val currentTheme by ThemePreferences.themeMode.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var password         by remember { mutableStateOf("") }
    var deleteError      by remember { mutableStateOf<String?>(null) }
    val isDeleting       by viewModel.isDeleting.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Settings", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            // ── Appearance ────────────────────────────────────────────────────
            SettingsSectionTitle("Appearance")
            Spacer(modifier = Modifier.height(8.dp))
            ElevatedCard(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                elevation = CardDefaults.elevatedCardElevation(1.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    ThemeOptionRow("System Default", Icons.Outlined.SettingsSystemDaydream,
                        currentTheme == ThemeMode.SYSTEM) {
                        ThemePreferences.setThemeMode(context, ThemeMode.SYSTEM)
                    }
                    SettingsDivider()
                    ThemeOptionRow("Light Mode", Icons.Outlined.LightMode,
                        currentTheme == ThemeMode.LIGHT) {
                        ThemePreferences.setThemeMode(context, ThemeMode.LIGHT)
                    }
                    SettingsDivider()
                    ThemeOptionRow("Dark Mode", Icons.Outlined.DarkMode,
                        currentTheme == ThemeMode.DARK) {
                        ThemePreferences.setThemeMode(context, ThemeMode.DARK)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Data & Account ────────────────────────────────────────────────
            SettingsSectionTitle("Data & Account")
            Spacer(modifier = Modifier.height(8.dp))
            ElevatedCard(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                elevation = CardDefaults.elevatedCardElevation(1.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    SettingsActionRow(
                        label    = "Clear Local Cache",
                        icon     = Icons.Outlined.CleaningServices,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick  = { Toast.makeText(context, "Cache cleared!", Toast.LENGTH_SHORT).show() }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        label    = "Delete Account",
                        icon     = Icons.Outlined.DeleteForever,
                        iconTint = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.error,
                        onClick  = { showDeleteDialog = true }
                    )
                }
            }
        }

        // ── Delete account dialog (logic unchanged) ───────────────────────────
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDeleting) { showDeleteDialog = false; deleteError = null; password = "" } },
                title            = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
                text             = {
                    Column {
                        Text("This is permanent and cannot be undone. Enter your password to confirm.",
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value                = password,
                            onValueChange        = { password = it; deleteError = null },
                            label                = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier             = Modifier.fillMaxWidth(),
                            singleLine           = true,
                            shape                = RoundedCornerShape(12.dp)
                        )
                        if (deleteError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(deleteError!!, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                confirmButton    = {
                    TextButton(
                        onClick  = {
                            viewModel.deleteAccount(password,
                                onSuccess = {
                                    showDeleteDialog = false
                                    Toast.makeText(context, "Account Deleted", Toast.LENGTH_SHORT).show()
                                    onNavigateToLogin()
                                },
                                onError   = { deleteError = it }
                            )
                        },
                        enabled  = password.isNotBlank() && !isDeleting
                    ) {
                        if (isDeleting) CircularProgressIndicator(modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.error, strokeWidth = 2.dp)
                        else Text("Delete Forever", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton    = {
                    TextButton(onClick = { showDeleteDialog = false; deleteError = null; password = "" },
                        enabled = !isDeleting) { Text("Cancel") }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(start = 4.dp))
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp)
}

@Composable
fun ThemeOptionRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier             = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null,
                tint     = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = isSelected, onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}
