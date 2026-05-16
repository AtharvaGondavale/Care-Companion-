package com.carecompanion.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.network.AuthVerifyResponse
import com.carecompanion.app.network.CareApiService
import com.carecompanion.app.network.CareRole
import com.carecompanion.app.network.OtpRequestBody
import com.carecompanion.app.network.OtpVerifyBody
import com.carecompanion.app.network.normalizeToE164Digits
import com.carecompanion.app.ui.theme.CareCompanionTheme
import com.carecompanion.app.ui.theme.CareGreen
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class UserRole(val label: String, val icon: ImageVector)

private val roles = listOf(
    UserRole("Elder User", Icons.Outlined.Person),
    UserRole("Guardian User", Icons.Outlined.Security)
)

// Palette
private val PageBg      = Color(0xFFF4F6F4)
private val CardBg      = Color(0xFFFFFFFF)
private val FieldBg     = Color(0xFFF8F8F8)
private val FieldBorder = Color(0xFFE8E8E8)
private val TextPrimary = Color(0xFF1C1C1C)
private val TextHint    = Color(0xFFAAAAAA)
private val TextSub     = Color(0xFF888888)
private val GreenLight  = Color(0xFFEAF5EA)

@Composable
fun LoginScreen(
    api: CareApiService,
    onSuccess: (role: CareRole, response: AuthVerifyResponse) -> Unit,
) {
    var selectedRole  by remember { mutableStateOf<UserRole?>(null) }
    var phone         by remember { mutableStateOf("") }
    var otp           by remember { mutableStateOf("") }
    var otpRequested  by remember { mutableStateOf(false) }
    var busy          by remember { mutableStateOf(false) }
    var errorText     by remember { mutableStateOf<String?>(null) }
    val scope         = rememberCoroutineScope()

    fun roleForSelection(): CareRole? =
        when {
            selectedRole?.label?.contains("Elder", ignoreCase = true) == true -> CareRole.ELDER
            selectedRole?.label?.contains("Guardian", ignoreCase = true) == true -> CareRole.GUARDIAN
            else -> null
        }

    fun phoneDigitsNormalized(): String {
        val d = phone.filter { it.isDigit() }
        require(d.length >= 10) { "Enter a valid mobile number" }
        return normalizeToE164Digits(d)
    }

    fun showError(t: String) { errorText = t }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.Start
        ) {

            errorText?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        err,
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFB71C1C),
                        fontSize = 14.sp
                    )
                }
            }

            // ── App title ──────────────────────────────────────────
            Text(
                text = "Care\nCompanion",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = TextPrimary,
                    lineHeight = 54.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // ── Login card ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBg)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    Text(
                        text = "Login",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.sp
                    )

                    // Role cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        roles.forEach { role ->
                            RoleCard(
                                role = role,
                                selected = selectedRole == role,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedRole = role
                                    otpRequested = false
                                    otp = ""
                                    phone = ""
                                }
                            )
                        }
                    }

                    // Animated step
                    AnimatedContent(
                        targetState = otpRequested,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "step"
                    ) { showOtp ->
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (showOtp) {
                                StepField(
                                    value = otp,
                                    onValueChange = { if (it.length <= 6) otp = it },
                                    placeholder = "Enter OTP",
                                    keyboardType = KeyboardType.NumberPassword
                                )

                                ActionButton(
                                    label = "Login",
                                    enabled = !busy && selectedRole != null && otp.length >= 4,
                                    onClick = {
                                        scope.launch {
                                            busy = true
                                            errorText = null
                                            try {
                                                val r = roleForSelection()
                                                    ?: throw IllegalStateException("Select a role")
                                                val p = phoneDigitsNormalized()
                                                val code = otp.trim()
                                                require(code.length >= 4) { "Enter the code from SMS" }
                                                val body = OtpVerifyBody(phone = p, code = code)
                                                val resp = api.verifyOtp(body)
                                                onSuccess(r, resp)
                                            } catch (e: HttpException) {
                                                showError(e.response()?.errorBody()?.string() ?: e.message())
                                            } catch (e: Exception) {
                                                showError(e.message ?: "Something went wrong")
                                            } finally {
                                                busy = false
                                            }
                                        }
                                    }
                                )

                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Resend OTP?",
                                        color = CareGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable(
                                            enabled = !busy,
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            scope.launch {
                                                busy = true
                                                errorText = null
                                                try {
                                                    val r = roleForSelection()
                                                    ?: throw IllegalStateException("Select a role")
                                                val p = phoneDigitsNormalized()
                                                api.requestOtp(OtpRequestBody(phone = p, role = r.name))
                                                } catch (e: HttpException) {
                                                    showError(e.response()?.errorBody()?.string() ?: e.message())
                                                } catch (e: Exception) {
                                                    showError(e.message ?: "Could not resend")
                                                } finally {
                                                    busy = false
                                                }
                                            }
                                        }
                                    )
                                }
                            } else {
                                StepField(
                                    value = phone,
                                    onValueChange = { if (it.length <= 15) phone = it },
                                    placeholder = "Enter Phone Number",
                                    keyboardType = KeyboardType.Phone
                                )

                                ActionButton(
                                    label = "Get OTP",
                                    enabled = !busy && selectedRole != null && phone.length >= 10,
                                    onClick = {
                                        scope.launch {
                                            busy = true
                                            errorText = null
                                            try {
                                                val r = roleForSelection()
                                                    ?: throw IllegalStateException("Select a role")
                                                val p = phoneDigitsNormalized()
                                                api.requestOtp(OtpRequestBody(phone = p, role = r.name))
                                                otpRequested = true
                                                otp = ""
                                            } catch (e: HttpException) {
                                                showError(e.response()?.errorBody()?.string() ?: e.message())
                                            } catch (e: Exception) {
                                                showError(e.message ?: "Could not send OTP")
                                            } finally {
                                                busy = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Role selection card ────────────────────────────────────────────
@Composable
private fun RoleCard(
    role: UserRole,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor    = if (selected) GreenLight else FieldBg
    val iconColor  = if (selected) CareGreen  else Color(0xFF777777)
    val textColor  = if (selected) CareGreen  else TextPrimary
    val borderMod  = if (selected)
        Modifier.border(1.5.dp, CareGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
    else
        Modifier

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(borderMod)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (selected) CareGreen.copy(alpha = 0.12f) else Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = role.icon,
                    contentDescription = role.label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = role.label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// ── Generic text input ─────────────────────────────────────────────
@Composable
private fun StepField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = TextPrimary),
        cursorBrush = SolidColor(CareGreen),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, FieldBorder, RoundedCornerShape(12.dp))
            .background(FieldBg)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, color = TextHint, fontSize = 15.sp)
            inner()
        }
    )
}

// ── Primary CTA button ─────────────────────────────────────────────
@Composable
private fun ActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CareGreen,
            disabledContainerColor = Color(0xFFB2DFDB)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    CareCompanionTheme { Text("Install on device/emulator for live login.") }
}
