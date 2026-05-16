package com.carecompanion.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.carecompanion.app.network.normalizeToE164Digits
import com.carecompanion.app.ui.theme.CareGreen
import kotlinx.coroutines.launch
import retrofit2.HttpException

private val PageBg = Color(0xFFF5F6F4)
private val CardStroke = Color(0xFFE8EBE9)
private val TextPrimary = Color(0xFF1C1C1C)
private val TextMuted = Color(0xFF6B7280)
private val AccentLink = CareGreen

private data class AvatarOption(val icon: ImageVector, val bg: Color)

private val avatarOptions: List<AvatarOption>
    get() = GuardianAvatars.options().map { AvatarOption(it.first, it.second) }

/** Real OTP for elder primary phone (same API as login, role ELDER). Guardian session is unchanged on verify. */
data class ElderPhoneOtpCallbacks(
    val requestOtp: suspend (phoneE164Digits: String) -> Result<Unit>,
    val verifyOtp: suspend (phoneE164Digits: String, code: String) -> Result<Unit>,
)

@Composable
fun GuardianAddProfileScreen(
    onBack: () -> Unit,
    onSaveNext: (GuardianProfile) -> Unit,
    elderPhoneOtp: ElderPhoneOtpCallbacks? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var avatarIndex by remember { mutableIntStateOf(0) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    var elderOtpSent by remember { mutableStateOf(false) }
    var elderOtpVerified by remember { mutableStateOf(false) }
    var elderOtpCode by remember { mutableStateOf("") }
    var elderOtpBusy by remember { mutableStateOf(false) }
    LaunchedEffect(phone) {
        elderOtpSent = false
        elderOtpVerified = false
        elderOtpCode = ""
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        photoUri = uri
        if (uri != null) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.guardian_photo_added))
            }
        }
    }

    val pickContact = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val (n, p) = ContactImport.readNameAndPhone(context, uri)
        if (!n.isNullOrBlank()) emergencyName = n
        if (!p.isNullOrBlank()) {
            emergencyPhone = p
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.guardian_contact_imported))
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.guardian_contact_no_phone))
            }
        }
    }

    val requestContactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pickContact.launch(null)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.guardian_contacts_permission))
            }
        }
    }

    fun openContactPicker() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED -> pickContact.launch(null)
            else -> requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = CareGreen.copy(alpha = 0.55f),
        unfocusedBorderColor = CardStroke,
        cursorColor = CareGreen
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = PageBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, CardStroke)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            val rawDigits = phone.filter { it.isDigit() }
                            val needsOtp = elderPhoneOtp != null && rawDigits.length >= 10
                            if (needsOtp && !elderOtpVerified) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.guardian_elder_phone_must_verify),
                                    )
                                }
                                return@Button
                            }
                            val list = avatarOptions
                            val pick = list[avatarIndex.coerceIn(0, list.lastIndex)]
                            val linked =
                                rawDigits.takeIf { it.length >= 10 }
                                    ?.let { normalizeToE164Digits(it) }
                                    .orEmpty()
                            onSaveNext(
                                GuardianProfile(
                                    backendId = "",
                                    name = name.trim().ifBlank {
                                        context.getString(R.string.guardian_unnamed_profile)
                                    },
                                    linkedElderPhone = linked,
                                    avatarIconKey = GuardianAvatars.keyFromIndex(avatarIndex),
                                    avatarBgArgb = GuardianAvatars.argbFromColor(pick.bg),
                                    icon = pick.icon,
                                    bg = pick.bg,
                                    photoUri = photoUri
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CareGreen,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            stringResource(R.string.guardian_save_next),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = onBack) {
                        Text(
                            stringResource(R.string.guardian_cancel),
                            fontSize = 16.sp,
                            color = AccentLink,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, CardStroke, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.guardian_back),
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.guardian_add_profile_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            color = TextPrimary,
                            letterSpacing = (-0.3).sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                    TextButton(onClick = onBack) {
                        Text(
                            stringResource(R.string.guardian_cancel),
                            fontSize = 15.sp,
                            color = AccentLink,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                MinimalCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = CareGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.guardian_personal_info),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFF1F8F2))
                                .border(1.dp, Color(0xFFE0E4E0), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val picked = photoUri
                            if (picked != null) {
                                UriBitmapImage(
                                    uri = picked,
                                    contentDescription = stringResource(R.string.guardian_photo_preview),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = avatarOptions[avatarIndex].icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp),
                                    tint = CareGreen
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                                singleLine = true,
                                label = {
                                    Text(
                                        stringResource(R.string.guardian_full_name),
                                        fontSize = 14.sp,
                                        color = TextMuted
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    color = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        pickImage.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFFF0FDF4),
                                        contentColor = Color(0xFF166534)
                                    ),
                                    elevation = ButtonDefaults.filledTonalButtonElevation(
                                        defaultElevation = 0.dp
                                    )
                                ) {
                                    Icon(
                                        Icons.Outlined.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.guardian_upload_photo),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showAvatarDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CardStroke),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = TextPrimary
                                    )
                                ) {
                                    Text(
                                        stringResource(R.string.guardian_choose_avatar),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (photoUri != null) {
                                TextButton(
                                    onClick = { photoUri = null },
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Text(
                                        stringResource(R.string.guardian_remove_photo),
                                        fontSize = 14.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                MinimalCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Call,
                            contentDescription = null,
                            tint = CareGreen,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.guardian_contact_info),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    Text(
                        stringResource(R.string.guardian_primary_phone_label),
                        fontSize = 13.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = TextMuted
                                )
                            },
                            label = {
                                Text(
                                    stringResource(R.string.guardian_primary_phone),
                                    fontSize = 14.sp
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors
                        )
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val digits = phone.filter { it.isDigit() }
                                    if (digits.length < 10) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.guardian_elder_phone_too_short),
                                        )
                                        return@launch
                                    }
                                    val bridge = elderPhoneOtp
                                    if (bridge == null) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.guardian_elder_otp_login_required),
                                        )
                                        return@launch
                                    }
                                    elderOtpBusy = true
                                    val e164 = normalizeToE164Digits(digits)
                                    val result = bridge.requestOtp(e164)
                                    elderOtpBusy = false
                                    result.fold(
                                        onSuccess = {
                                            elderOtpSent = true
                                            elderOtpCode = ""
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.guardian_elder_otp_sent),
                                            )
                                        },
                                        onFailure = { e ->
                                            val msg =
                                                (e as? HttpException)?.response()?.errorBody()?.string()
                                                    ?: e.message
                                                    ?: context.getString(R.string.guardian_elder_otp_failed)
                                            snackbarHostState.showSnackbar(msg.take(280))
                                        },
                                    )
                                }
                            },
                            enabled = !elderOtpBusy,
                            modifier = Modifier.heightIn(min = 56.dp),
                        ) {
                            Text(
                                stringResource(R.string.guardian_get_otp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentLink,
                            )
                        }
                    }

                    if (elderPhoneOtp != null && elderOtpSent && !elderOtpVerified) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = elderOtpCode,
                            onValueChange = { v ->
                                elderOtpCode = v.filter { ch -> ch.isDigit() }.take(8)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(R.string.guardian_elder_otp_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val bridge = elderPhoneOtp ?: return@launch
                                    val digits = phone.filter { it.isDigit() }
                                    if (elderOtpCode.trim().length < 4) return@launch
                                    elderOtpBusy = true
                                    val e164 = normalizeToE164Digits(digits)
                                    val result = bridge.verifyOtp(e164, elderOtpCode.trim())
                                    elderOtpBusy = false
                                    result.fold(
                                        onSuccess = {
                                            elderOtpVerified = true
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.guardian_elder_otp_verified),
                                            )
                                        },
                                        onFailure = { e ->
                                            val msg =
                                                (e as? HttpException)?.response()?.errorBody()?.string()
                                                    ?: e.message
                                                    ?: context.getString(R.string.guardian_elder_otp_failed)
                                            snackbarHostState.showSnackbar(msg.take(280))
                                        },
                                    )
                                }
                            },
                            enabled = !elderOtpBusy && elderOtpCode.trim().length >= 4,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CareGreen),
                        ) {
                            Text(
                                stringResource(R.string.guardian_verify_otp),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else if (elderPhoneOtp != null && elderOtpVerified) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.guardian_elder_otp_verified),
                            fontSize = 13.sp,
                            color = CareGreen,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        stringResource(R.string.guardian_emergency_section),
                        fontSize = 13.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = emergencyName,
                            onValueChange = { emergencyName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            singleLine = true,
                            label = {
                                Text(
                                    stringResource(R.string.guardian_field_name),
                                    fontSize = 13.sp
                                )
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.guardian_emergency_name),
                                    fontSize = 15.sp,
                                    color = TextMuted.copy(alpha = 0.7f)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors
                        )
                        OutlinedTextField(
                            value = emergencyPhone,
                            onValueChange = { emergencyPhone = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            label = {
                                Text(
                                    stringResource(R.string.guardian_field_phone),
                                    fontSize = 13.sp
                                )
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.guardian_emergency_phone),
                                    fontSize = 15.sp,
                                    color = TextMuted.copy(alpha = 0.7f)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { openContactPicker() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardStroke),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(
                            Icons.Outlined.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = TextMuted
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.guardian_import_contacts),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = {
                Text(
                    stringResource(R.string.guardian_pick_avatar_title),
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    itemsIndexed(avatarOptions) { index, option ->
                        val selected = index == avatarIndex
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(option.bg)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) CareGreen else CardStroke,
                                    shape = CircleShape
                                )
                                .clickable {
                                    avatarIndex = index
                                    photoUri = null
                                    showAvatarDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
                                tint = Color(0xFF4B5563)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text(stringResource(android.R.string.ok), color = AccentLink)
                }
            }
        )
    }
}

@Composable
private fun MinimalCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, CardStroke)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            content()
        }
    }
}
