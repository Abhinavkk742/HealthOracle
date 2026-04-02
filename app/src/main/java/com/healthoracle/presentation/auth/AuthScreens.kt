package com.healthoracle.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.healthoracle.R

// ─────────────────────────────────────────────────────────────────────────────
// LOGIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToDoctorDashboard: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    viewModel.signInWithGoogle(token) { isDoctor ->
                        if (isDoctor) onNavigateToDoctorDashboard() else onNavigateToHome()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    AuthScaffold {
        // ── Branding ─────────────────────────────────────────────────────────
        AuthBrandHeader()

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text       = "Welcome back",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text  = "Sign in to your account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        // ── Error banner ──────────────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.error != null) {
            Surface(
                modifier      = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape         = RoundedCornerShape(12.dp),
                color         = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text     = uiState.error ?: "",
                    color    = MaterialTheme.colorScheme.onErrorContainer,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // ── Fields ────────────────────────────────────────────────────────────
        AuthTextField(
            value           = email,
            onValueChange   = { email = it },
            label           = "Email address",
            leadingIcon     = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthTextField(
            value               = password,
            onValueChange       = { password = it },
            label               = "Password",
            leadingIcon         = Icons.Outlined.Lock,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon        = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector        = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "Toggle password",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Primary CTA ───────────────────────────────────────────────────────
        PrimaryAuthButton(
            text      = "Sign In",
            isLoading = uiState.isLoading,
            onClick   = {
                viewModel.login(email, password) { isDoctor ->
                    if (isDoctor) onNavigateToDoctorDashboard() else onNavigateToHome()
                }
            }
        )

        // ── Divider ───────────────────────────────────────────────────────────
        OrDivider()

        // ── Google sign-in ────────────────────────────────────────────────────
        GoogleAuthButton(
            isLoading = uiState.isLoading,
            onClick   = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                client.signOut().addOnCompleteListener {
                    googleSignInLauncher.launch(client.signInIntent)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Switch to sign-up ─────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Text("Don't have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text     = "Sign Up",
                color    = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToSignUp() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SIGN-UP SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignUpScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToDoctorDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isDoctorAccount by remember { mutableStateOf(false) }

    AuthScaffold {
        AuthBrandHeader()

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text       = "Create account",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text  = "Start your health journey today",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        // ── Error banner ──────────────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape    = RoundedCornerShape(12.dp),
                color    = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text     = uiState.error ?: "",
                    color    = MaterialTheme.colorScheme.onErrorContainer,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // ── Fields ────────────────────────────────────────────────────────────
        AuthTextField(
            value           = email,
            onValueChange   = { email = it },
            label           = "Email address",
            leadingIcon     = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthTextField(
            value                = password,
            onValueChange        = { password = it },
            label                = "Password (min. 6 characters)",
            leadingIcon          = Icons.Outlined.Lock,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon         = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector        = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "Toggle password",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Doctor toggle ─────────────────────────────────────────────────────
        DoctorRoleToggle(
            checked   = isDoctorAccount,
            onToggle  = { isDoctorAccount = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryAuthButton(
            text      = "Create Account",
            isLoading = uiState.isLoading,
            onClick   = {
                val role = if (isDoctorAccount) "doctor" else "patient"
                viewModel.signUp(email, password, role) { isDoctor ->
                    if (isDoctor) onNavigateToDoctorDashboard() else onNavigateToHome()
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text       = "Sign In",
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED AUTH COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

/** Scrollable centered container used by both auth screens. */
@Composable
private fun AuthScaffold(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 72.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.Start,
            content = content
        )
    }
}

/** App icon + wordmark at the top of auth screens. */
@Composable
private fun AuthBrandHeader() {
    Box(
        modifier         = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.MonitorHeart,
            contentDescription = null,
            tint               = Color.White,
            modifier           = Modifier.size(30.dp)
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text       = "HealthOracle",
        style      = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.primary
    )
}

/** Styled OutlinedTextField consistent across auth screens. */
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        leadingIcon          = {
            Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon         = trailingIcon,
        modifier             = Modifier.fillMaxWidth(),
        keyboardOptions      = keyboardOptions,
        visualTransformation = visualTransformation,
        shape                = RoundedCornerShape(14.dp),
        singleLine           = true,
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )
}

/** Full-width primary CTA button with loading state. */
@Composable
private fun PrimaryAuthButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape    = RoundedCornerShape(14.dp),
        enabled  = !isLoading,
        colors   = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        AnimatedContent(targetState = isLoading, label = "btnContent") { loading ->
            if (loading) {
                CircularProgressIndicator(
                    color    = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

/** "OR" divider row. */
@Composable
private fun OrDivider() {
    Row(
        verticalAlignment   = Alignment.CenterVertically,
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "  or  ",
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            style    = MaterialTheme.typography.labelMedium
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Google sign-in outlined button. */
@Composable
private fun GoogleAuthButton(isLoading: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape    = RoundedCornerShape(14.dp),
        enabled  = !isLoading,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors   = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        // Simple "G" badge since we can't ship a Drawable
        Box(
            modifier         = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF4285F4)),
            contentAlignment = Alignment.Center
        ) {
            Text("G", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** Role selection card for the sign-up form. */
@Composable
private fun DoctorRoleToggle(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) },
        shape     = RoundedCornerShape(14.dp),
        color     = if (checked)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.MedicalServices,
                contentDescription = null,
                tint               = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Register as a Doctor",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Care providers & physicians",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked         = checked,
                onCheckedChange = onToggle,
                colors          = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
