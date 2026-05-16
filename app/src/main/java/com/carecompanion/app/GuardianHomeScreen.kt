package com.carecompanion.app

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.carecompanion.app.network.GuardianProfileDto
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen

data class GuardianProfile(
    val backendId: String = "",
    val name: String,
    val linkedElderPhone: String = "",
    /** Saved with profile for API create; echoes server keys after fetch */
    val avatarIconKey: String? = null,
    val avatarBgArgb: Int? = null,
    val icon: ImageVector,
    val bg: Color,
    val photoUri: Uri? = null
)

fun GuardianProfile.stableKey(): String =
    backendId.ifBlank { "local:${name.trim().lowercase()}" }

fun GuardianProfileDto.toGuardianProfile(): GuardianProfile {
    val (iconPick, fbBg) = GuardianAvatars.pairFromKey(avatarIconKey)
    val bgColor = avatarBgArgb?.takeIf { it != 0 }?.let { Color(it) } ?: fbBg
    return GuardianProfile(
        backendId = id,
        name = displayName,
        linkedElderPhone = linkedElderPhoneE164.orEmpty(),
        avatarIconKey = avatarIconKey,
        avatarBgArgb = avatarBgArgb,
        icon = iconPick,
        bg = bgColor,
        photoUri = null
    )
}

@Composable
fun GuardianHomeScreen(
    profiles: List<GuardianProfile>,
    onAddProfile: () -> Unit = {},
    onManageProfile: (GuardianProfile) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedProfile by remember { mutableStateOf<GuardianProfile?>(null) }

    LaunchedEffect(profiles.size) {
        if (profiles.isEmpty()) {
            selectedProfile = null
        } else {
            val sel = selectedProfile
            if (sel == null || profiles.none { it.stableKey() == sel.stableKey() }) {
                selectedProfile = profiles.firstOrNull()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F4))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onLogout) {
                    Text(stringResource(R.string.common_logout), color = Color(0xFFB42318))
                }
            }
            Text(
                text = "Care\nCompanion",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-0.5).sp,
                    color = Color(0xFF1C1C1C)
                ),
                modifier = Modifier.padding(bottom = 30.dp)
            )

            Text(
                text = "Select Profile",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                if (profiles.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .background(Color(0xFFEAF5EA), CircleShape)
                                .clickable {
                                    onAddProfile()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add Profile",
                                tint = CareGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Add Profile",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2A2A2A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No profiles yet. Tap to add one.",
                            fontSize = 16.sp,
                            color = Color(0xFF777777)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEAF6EC), RoundedCornerShape(10.dp))
                                    .clickable(onClick = onAddProfile)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.PersonAdd,
                                        contentDescription = "Add Profile",
                                        tint = CareGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Profile", color = CareGreen, fontSize = 14.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            profiles.forEach { profile ->
                                ProfileChoiceCard(
                                    profile = profile,
                                    selected = selectedProfile?.stableKey() == profile.stableKey(),
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedProfile = profile }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                selectedProfile?.let(onManageProfile)
                            },
                            enabled = selectedProfile != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CareGreen)
                        ) {
                            Text(
                                text = if (selectedProfile == null) {
                                    "Manage Profile"
                                } else {
                                    "Manage ${selectedProfile!!.name}"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileChoiceCard(
    profile: GuardianProfile,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) CareGreen.copy(alpha = 0.6f) else Color(0xFFEAEAEA)
    val bg = if (selected) Color(0xFFF7FFF8) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(profile.bg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (profile.photoUri != null) {
                UriBitmapImage(
                    uri = profile.photoUri,
                    contentDescription = profile.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = profile.icon,
                    contentDescription = profile.name,
                    tint = Color(0xFF424242),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = profile.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2E2E2E)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 4.dp)
                .background(borderColor, RoundedCornerShape(10.dp))
        )
    }
}
