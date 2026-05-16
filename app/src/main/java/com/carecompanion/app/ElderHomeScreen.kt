package com.carecompanion.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.network.ElderMedicineRowDto
import com.carecompanion.app.ui.theme.CareCompanionTheme
import com.carecompanion.app.ui.theme.CareGreen
import kotlinx.coroutines.launch
import java.util.Locale

enum class ElderLanguage { ENGLISH, HINDI, MARATHI, GUJARATI }

private val LocalElderLanguage = compositionLocalOf { ElderLanguage.ENGLISH }

private fun tr(lang: ElderLanguage, key: String): String {
    return when (lang) {
        ElderLanguage.ENGLISH -> when (key) {
            "menu" -> "Menu"
            "home" -> "Home"
            "medicines" -> "Medicines"
            "vitals" -> "Vitals"
            "contacts" -> "Contacts"
            "entertainment" -> "Entertainment"
            "status_at_home" -> "Status: At home"
            "sos_hint" -> "Press and hold for emergency"
            "ott" -> "OTT"
            "tap_open" -> "Tap to open"
            "coming_soon" -> "Coming soon"
            "language" -> "Language"
            "take_medicine_title" -> "Take medicine"
            "todays_medicines" -> "Today's medicines"
            "start_taking" -> "Start taking"
            "medicine_of_fmt" -> "Medicine %1\$d of %2\$d"
            "did_you_take" -> "Did you take this medicine?"
            "not_taken" -> "Not taken"
            "taken" -> "Taken"
            "next_btn" -> "Next"
            "finish_btn" -> "Finish"
            "great_job" -> "Great job!"
            "completed_medicines" -> "You completed all medicines."
            "stats_taken_fmt" -> "Taken: %1\$d   Not taken: %2\$d"
            "watch_entertainment_btn" -> "Watch your favourite show or movie"
            "review_again" -> "Review again"
            "qty_fmt" -> "Qty %d"
            "elder_back" -> "Back"
            "sos_main" -> "SOS"
            else -> key
        }
        ElderLanguage.HINDI -> when (key) {
            "menu" -> "मेन्यू"
            "home" -> "होम"
            "medicines" -> "दवाइयाँ"
            "vitals" -> "वाइटल्स"
            "contacts" -> "संपर्क"
            "entertainment" -> "मनोरंजन"
            "status_at_home" -> "स्थिति: घर पर"
            "sos_hint" -> "आपातकाल के लिए दबाकर रखें"
            "ott" -> "ओटीटी"
            "tap_open" -> "खोलने के लिए टैप करें"
            "coming_soon" -> "जल्द आ रहा है"
            "language" -> "भाषा"
            "take_medicine_title" -> "दवा लें"
            "todays_medicines" -> "आज की दवाइयाँ"
            "start_taking" -> "लेना शुरू करें"
            "medicine_of_fmt" -> "दवा %1\$d में से %2\$d"
            "did_you_take" -> "क्या आपने यह दवा ली?"
            "not_taken" -> "नहीं ली"
            "taken" -> "ली"
            "next_btn" -> "आगे"
            "finish_btn" -> "समाप्त"
            "great_job" -> "बहुत अच्छा!"
            "completed_medicines" -> "आपने सभी दवाइयाँ पूरी कीं।"
            "stats_taken_fmt" -> "ली: %1\$d   नहीं ली: %2\$d"
            "watch_entertainment_btn" -> "अपना पसंदीदा शो या फ़िल्म देखें"
            "review_again" -> "फिर से देखें"
            "qty_fmt" -> "मात्रा %d"
            "elder_back" -> "पीछे"
            "sos_main" -> "SOS"
            else -> key
        }
        ElderLanguage.MARATHI -> when (key) {
            "menu" -> "मेन्यू"
            "home" -> "होम"
            "medicines" -> "औषधे"
            "vitals" -> "वाइटल्स"
            "contacts" -> "संपर्क"
            "entertainment" -> "मनोरंजन"
            "status_at_home" -> "स्थिती: घरी"
            "sos_hint" -> "आपत्कालीनसाठी दाबून ठेवा"
            "ott" -> "ओटीटी"
            "tap_open" -> "उघडण्यासाठी टॅप करा"
            "coming_soon" -> "लवकरच येत आहे"
            "language" -> "भाषा"
            "take_medicine_title" -> "औषध घ्या"
            "todays_medicines" -> "आजची औषधे"
            "start_taking" -> "सुरू करा"
            "medicine_of_fmt" -> "औषध %1\$d पैकी %2\$d"
            "did_you_take" -> "तुम्ही हे औषध घेतले?"
            "not_taken" -> "नाही घेतले"
            "taken" -> "घेतले"
            "next_btn" -> "पुढे"
            "finish_btn" -> "संपले"
            "great_job" -> "छान!"
            "completed_medicines" -> "सर्व औषधे पूर्ण झाली."
            "stats_taken_fmt" -> "घेतले: %1\$d   नाही: %2\$d"
            "watch_entertainment_btn" -> "आवडता शो किंवा चित्रपट पहा"
            "review_again" -> "पुन्हा पहा"
            "qty_fmt" -> "प्रमाण %d"
            "elder_back" -> "मागे"
            "sos_main" -> "SOS"
            else -> key
        }
        ElderLanguage.GUJARATI -> when (key) {
            "menu" -> "મેનુ"
            "home" -> "હોમ"
            "medicines" -> "દવાઓ"
            "vitals" -> "વાઇટલ્સ"
            "contacts" -> "સંપર્કો"
            "entertainment" -> "મનોરંજન"
            "status_at_home" -> "સ્થિતિ: ઘરે"
            "sos_hint" -> "આપત્કાલ માટે દબાવી રાખો"
            "ott" -> "ઓટીટી"
            "tap_open" -> "ખોલવા માટે ટેપ કરો"
            "coming_soon" -> "જલ્દી આવશે"
            "language" -> "ભાષા"
            "take_medicine_title" -> "દવા લો"
            "todays_medicines" -> "આજની દવાઓ"
            "start_taking" -> "શરૂ કરો"
            "medicine_of_fmt" -> "દવા %1\$d માંથી %2\$d"
            "did_you_take" -> "શું તમે આ દવા લીધી?"
            "not_taken" -> "નથી લીધી"
            "taken" -> "લીધી"
            "next_btn" -> "આગળ"
            "finish_btn" -> "પૂર્ણ"
            "great_job" -> "શાબાશ!"
            "completed_medicines" -> "તમે બધી દવાઓ પૂરી કરી."
            "stats_taken_fmt" -> "લીધી: %1\$d   નહીં: %2\$d"
            "watch_entertainment_btn" -> "મનપસંદ શો અથવા ફિલ્મ જુઓ"
            "review_again" -> "ફરી જુઓ"
            "qty_fmt" -> "જથ્થો %d"
            "elder_back" -> "પાછા"
            "sos_main" -> "SOS"
            else -> key
        }
    }
}

// Screens reachable from the drawer / action cards
sealed class ElderDestination {
    object Home        : ElderDestination()
    object Medicines   : ElderDestination()
    object Vitals      : ElderDestination()
    object Contacts    : ElderDestination()
    object Entertainment : ElderDestination()
}

private data class ContactPerson(
    val name: String,
    val avatarIcon: ImageVector,
    val avatarBg: Color
)

private data class EntertainmentItem(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

data class MedicineItem(
    val name: String,
    val quantity: Int,
    val timeInstruction: String,
    val withInstruction: String,
    val icon: ImageVector,
    val color: Color,
    val photoRes: Int? = null
)

fun elderMedicinesFromRows(rows: List<ElderMedicineRowDto>): List<MedicineItem> =
    rows.mapIndexed { ix, row -> elderRowToMedicineItem(row, ix) }

private fun elderRowToMedicineItem(row: ElderMedicineRowDto, ix: Int): MedicineItem {
    val qty = Regex("""(\d+)""").find(row.dosage)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    val schedule = row.schedules.firstOrNull()
    val timeInstr = schedule?.let { s ->
        val prefix =
            if (s.mealTiming.uppercase() == "AFTER") "After " else "Before "
        val label = if (s.mealLabel.isNotBlank()) s.mealLabel else s.timeLabel
        prefix + label
    }?.trim().orEmpty().ifBlank { row.form.ifBlank { "Today" } }
    val withInstr = when {
        row.schedules.any { it.withWater } -> "With Water"
        else -> "As prescribed"
    }
    val palettes = listOf(Color(0xFFFFF8E1), Color(0xFFE3F2FD), Color(0xFFF3E5F5))
    val icons = listOf(Icons.Outlined.Medication, Icons.Outlined.LocalPharmacy, Icons.Outlined.Vaccines)
    return MedicineItem(
        name = row.name,
        quantity = qty,
        timeInstruction = timeInstr,
        withInstruction = withInstr,
        icon = icons[ix % icons.size],
        color = palettes[ix % palettes.size],
        photoRes = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderHomeScreen(
    elderName: String = "Sunita",
    elderMedicines: List<MedicineItem> = emptyList(),
    onSosPressed: () -> Unit = {},
    onLogout: () -> Unit = {},
    elderContacts: List<ManagedContact> = emptyList()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    var destination by remember { mutableStateOf<ElderDestination>(ElderDestination.Home) }
    var language by remember { mutableStateOf(ElderLanguage.ENGLISH) }

    CompositionLocalProvider(LocalElderLanguage provides language) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ElderDrawer(
                    current = destination,
                    onSelect = { dest ->
                        destination = dest
                        scope.launch { drawerState.close() }
                    }
                )
            },
            scrimColor = Color.Black.copy(alpha = 0.3f)
        ) {
            Scaffold(
                containerColor = Color(0xFFF4F6F4),
                topBar = {
                    ElderTopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onLanguageSelected = { language = it },
                        onLogout = onLogout
                    )
                }
            ) { padding ->
                when (destination) {
                    ElderDestination.Home        -> ElderHomePage(padding, elderName, onSosPressed) { destination = it }
                    ElderDestination.Medicines   -> MedicinesScreen(padding, onSosPressed, elderMedicines) { destination = it }
                    ElderDestination.Vitals      -> VitalsScreen(padding, onSosPressed) { destination = it }
                    ElderDestination.Contacts    -> ContactsScreen(padding, onSosPressed, elderContacts) { destination = it }
                    ElderDestination.Entertainment -> EntertainmentScreen(padding, onSosPressed) { destination = it }
                }
            }
        }
    }
}

// ── Top bar ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElderTopBar(
    onMenuClick: () -> Unit,
    onLanguageSelected: (ElderLanguage) -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {},
        modifier = Modifier.padding(horizontal = 4.dp),
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = Color(0xFF333333),
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Outlined.Translate,
                        contentDescription = "Language",
                        tint = Color(0xFF4F4F4F),
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("English") },
                        onClick = { onLanguageSelected(ElderLanguage.ENGLISH); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("हिन्दी") },
                        onClick = { onLanguageSelected(ElderLanguage.HINDI); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("मराठी") },
                        onClick = { onLanguageSelected(ElderLanguage.MARATHI); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("ગુજરાતી") },
                        onClick = { onLanguageSelected(ElderLanguage.GUJARATI); expanded = false }
                    )
                }
            }
            IconButton(onClick = {}) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color(0xFF4F4F4F),
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    Icons.Outlined.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFFB42318),
                    modifier = Modifier.size(23.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F9F8))
    )
}

// ── Side drawer ──────────────────────────────────────────────────────
@Composable
private fun ElderDrawer(
    current: ElderDestination,
    onSelect: (ElderDestination) -> Unit
) {
    val lang = LocalElderLanguage.current
    ModalDrawerSheet(
        modifier = Modifier.width(260.dp),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = Color.White
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = tr(lang, "menu"),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFAAAAAA),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        DrawerItem(
            icon  = Icons.Outlined.MedicalServices,
            label = tr(lang, "medicines"),
            selected = current == ElderDestination.Medicines,
            onClick = { onSelect(ElderDestination.Medicines) }
        )
        DrawerItem(
            icon  = Icons.Outlined.MonitorHeart,
            label = tr(lang, "vitals"),
            selected = current == ElderDestination.Vitals,
            onClick = { onSelect(ElderDestination.Vitals) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = Color(0xFFEEEEEE)
        )

        DrawerItem(
            icon  = Icons.Outlined.Home,
            label = tr(lang, "home"),
            selected = current == ElderDestination.Home,
            onClick = { onSelect(ElderDestination.Home) }
        )
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor   = if (selected) CareGreen.copy(alpha = 0.1f) else Color.Transparent
    val iconColor = if (selected) CareGreen else Color(0xFF555555)
    val txtColor  = if (selected) CareGreen else Color(0xFF333333)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) CareGreen.copy(alpha = 0.15f) else Color(0xFFF2F2F2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = txtColor)
    }
}

// ── Home page body ───────────────────────────────────────────────────
@Composable
private fun ElderHomePage(
    padding: PaddingValues,
    elderName: String,
    onSosPressed: () -> Unit,
    navigate: (ElderDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        AvatarCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp),
            name = elderName,
            status = tr(LocalElderLanguage.current, "status_at_home")
        )

        // SOS below profile as requested.
        Spacer(modifier = Modifier.height(20.dp))
        SosButton(onSosPressed)
        Spacer(modifier = Modifier.height(10.dp))
        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActionCard(
                icon  = Icons.Outlined.Contacts,
                label = tr(LocalElderLanguage.current, "contacts"),
                tint  = Color(0xFF1565C0),
                bg    = Color(0xFFE3F2FD),
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp),
                onClick = { navigate(ElderDestination.Contacts) }
            )
            ActionCard(
                icon  = Icons.Outlined.Movie,
                label = tr(LocalElderLanguage.current, "entertainment"),
                tint  = Color(0xFF6A1B9A),
                bg    = Color(0xFFF3E5F5),
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp),
                onClick = { navigate(ElderDestination.Entertainment) }
            )
        }
    }
}

// ── Avatar card ──────────────────────────────────────────────────────
@Composable
private fun AvatarCard(
    modifier: Modifier = Modifier,
    name: String,
    status: String
) {
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.05f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar oval
            Box(
                modifier = Modifier
                    .size(width = 138.dp, height = 162.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF64B5F6), Color(0xFF1976D2)),
                            center = Offset(0.5f, 0.3f),
                            radius = 400f
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    imageVector = Icons.Outlined.Elderly,
                    contentDescription = "Avatar",
                    tint = Color.White,
                    modifier = Modifier
                        .size(120.dp)
                        .offset(y = 10.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = name,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1A1A)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFF1F8F2))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(CareGreen)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = status,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF3F5C45)
                )
            }
        }
    }
}

// ── SOS button ───────────────────────────────────────────────────────
@Composable
private fun SosButton(onClick: () -> Unit) {
    val lang = LocalElderLanguage.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .scale(if (pressed) 0.98f else 1f)
            .shadow(
                elevation = if (pressed) 6.dp else 12.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFFD32F2F).copy(alpha = 0.30f),
                spotColor = Color(0xFFD32F2F).copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFF24141), Color(0xFFD62323))
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = "SOS",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = tr(lang, "sos_main"),
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 5.sp
            )
        }
        Text(
            text = tr(lang, "sos_hint"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 11.dp)
        )
    }
}

// ── Action card (Contacts / Entertainment) ───────────────────────────
@Composable
private fun ActionCard(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(0.05f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2F2F2F))
        }
    }
}

// ── Stub sub-screens ─────────────────────────────────────────────────
@Composable
fun MedicinesScreen(
    padding: PaddingValues,
    onSosPressed: () -> Unit,
    medicines: List<MedicineItem>,
    onNavigate: (ElderDestination) -> Unit,
) {

    var step by rememberSaveable { mutableStateOf(0) } // 0=list, 1=details, 2=done
    var index by rememberSaveable { mutableStateOf(0) }
    var tookCurrent by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var takenCount by rememberSaveable { mutableStateOf(0) }
    var skippedCount by rememberSaveable { mutableStateOf(0) }
    val lang = LocalElderLanguage.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(ElderDestination.Home) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = tr(lang, "elder_back"),
                    tint = Color(0xFF404040)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = tr(lang, "take_medicine_title"),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1C)
            )
        }

        val contentModifier = if (step == 0) {
            Modifier
                .fillMaxWidth()
                .height(470.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .weight(1f)
        }

        Box(
            modifier = contentModifier
                .shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(0.06f))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(12.dp)
        ) {
            when (step) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = tr(lang, "todays_medicines"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2A2A2A)
                        )
                        if (medicines.isEmpty()) {
                            Text(
                                text = "No medicines from your caregiver yet.",
                                fontSize = 16.sp,
                                color = Color(0xFF666666)
                            )
                        } else {
                            medicines.forEachIndexed { i, med ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF8F8F8))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(med.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = med.icon,
                                        contentDescription = med.name,
                                        tint = Color(0xFF6B4D00),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${i + 1}. ${med.name}",
                                    fontSize = 18.sp,
                                    color = Color(0xFF2E2E2E),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), tr(lang, "qty_fmt"), med.quantity),
                                    fontSize = 16.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                index = 0
                                tookCurrent = null
                                takenCount = 0
                                skippedCount = 0
                                step = 1
                            },
                            enabled = medicines.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C8BD9))
                        ) {
                            Text(tr(lang, "start_taking"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                1 -> {
                    if (medicines.isEmpty()) {
                        Text("No medicines to take.", modifier = Modifier.padding(16.dp))
                    } else {
                    AnimatedContent(
                        targetState = index,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(260)) togetherWith
                                fadeOut(animationSpec = tween(220))
                        },
                        label = "medicine_slide"
                    ) { animatedIndex ->
                        val med = medicines[animatedIndex]
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    tr(lang, "medicine_of_fmt"),
                                    animatedIndex + 1,
                                    medicines.size
                                ),
                                fontSize = 20.sp,
                                color = Color(0xFF666666)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .shadow(2.dp, RoundedCornerShape(16.dp))
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(med.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (med.photoRes != null) {
                                        Image(
                                            painter = painterResource(id = med.photoRes),
                                            contentDescription = med.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = med.icon,
                                            contentDescription = med.name,
                                            tint = Color(0xFF5B4300),
                                            modifier = Modifier.size(96.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = med.name,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            String.format(Locale.getDefault(), tr(lang, "qty_fmt"), med.quantity),
                                            fontSize = 18.sp
                                        )
                                    }
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text(med.timeInstruction, fontSize = 18.sp) }
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(med.withInstruction, fontSize = 18.sp) }
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tr(lang, "did_you_take"),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2A2A2A)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { tookCurrent = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (tookCurrent == false) Color(0xFFD32F2F) else Color(0xFFFDECEC),
                                        contentColor = if (tookCurrent == false) Color.White else Color(0xFFD32F2F)
                                    )
                                ) {
                                    Text(tr(lang, "not_taken"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { tookCurrent = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (tookCurrent == true) Color(0xFF2E7D32) else Color(0xFFEAF6EC),
                                        contentColor = if (tookCurrent == true) Color.White else Color(0xFF2E7D32)
                                    )
                                ) {
                                    Text(tr(lang, "taken"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    if (tookCurrent == true) takenCount += 1 else skippedCount += 1
                                    if (index < medicines.lastIndex) {
                                        index += 1
                                        tookCurrent = null
                                    } else {
                                        step = 2
                                    }
                                },
                                enabled = tookCurrent != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C8BD9))
                            ) {
                                Text(
                                    text = if (animatedIndex < medicines.lastIndex) {
                                        tr(lang, "next_btn")
                                    } else {
                                        tr(lang, "finish_btn")
                                    },
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    }
                }

                else -> {
                    val confetti = rememberInfiniteTransition(label = "confetti")
                    val bounce by confetti.animateFloat(
                        initialValue = 0f,
                        targetValue = 14f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "celebrate_bounce"
                    )
                    val particleA by confetti.animateFloat(
                        initialValue = -8f,
                        targetValue = 42f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "particle_a"
                    )
                    val particleB by confetti.animateFloat(
                        initialValue = -20f,
                        targetValue = 48f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "particle_b"
                    )
                    val particleC by confetti.animateFloat(
                        initialValue = -14f,
                        targetValue = 50f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1600),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "particle_c"
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF3CD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .graphicsLayer {
                                        translationX = -42f
                                        translationY = particleA
                                    }
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF6F61))
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .graphicsLayer {
                                        translationX = 46f
                                        translationY = particleB
                                    }
                                    .clip(CircleShape)
                                    .background(Color(0xFF42A5F5))
                            )
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .graphicsLayer {
                                        translationX = 0f
                                        translationY = particleC
                                    }
                                    .clip(CircleShape)
                                    .background(Color(0xFF66BB6A))
                            )
                            Icon(
                                imageVector = Icons.Outlined.SentimentVerySatisfied,
                                contentDescription = "Done",
                                tint = Color(0xFFE6A800),
                                modifier = Modifier
                                    .size(76.dp)
                                    .scale(1f + (bounce / 150f))
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = tr(lang, "great_job"),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF222222)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = tr(lang, "completed_medicines"),
                            fontSize = 20.sp,
                            color = Color(0xFF555555)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                tr(lang, "stats_taken_fmt"),
                                takenCount,
                                skippedCount
                            ),
                            fontSize = 18.sp,
                            color = Color(0xFF4A4A4A)
                        )
                        Spacer(Modifier.height(26.dp))
                        Button(
                            onClick = { onNavigate(ElderDestination.Entertainment) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
                        ) {
                            Text(tr(lang, "watch_entertainment_btn"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                step = 0
                                index = 0
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(tr(lang, "review_again"), fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VitalsScreen(
    padding: PaddingValues,
    onSosPressed: () -> Unit,
    onNavigate: (ElderDestination) -> Unit
) = StubScreen(
    padding = padding,
    title = tr(LocalElderLanguage.current, "vitals"),
    icon = Icons.Outlined.MonitorHeart,
    color = Color(0xFFC62828),
    onSosPressed = onSosPressed,
    current = ElderDestination.Vitals,
    onNavigate = onNavigate
)

@Composable
fun ContactsScreen(
    padding: PaddingValues,
    onSosPressed: () -> Unit,
    elderContacts: List<ManagedContact> = emptyList(),
    onNavigate: (ElderDestination) -> Unit
) = ContactListScreen(padding, onSosPressed, elderContacts, onNavigate)

@Composable
fun EntertainmentScreen(
    padding: PaddingValues,
    onSosPressed: () -> Unit,
    onNavigate: (ElderDestination) -> Unit
) = EntertainmentListScreen(
    padding = padding,
    onNavigate = onNavigate
)

@Composable
private fun ContactListScreen(
    padding: PaddingValues,
    onSosPressed: () -> Unit,
    elderContacts: List<ManagedContact>,
    onNavigate: (ElderDestination) -> Unit
) {
    val lang = LocalElderLanguage.current
    val contacts = remember(elderContacts) {
        if (elderContacts.isNotEmpty()) {
            elderContacts.mapIndexed { i, c ->
                val colors = listOf(
                    Color(0xFFE3F2FD), Color(0xFFFCE4EC), Color(0xFFE8F5E9),
                    Color(0xFFF3E5F5), Color(0xFFFFF3E0), Color(0xFFE0F2F1)
                )
                ContactPerson(c.name, Icons.Outlined.Person, colors[i % colors.size])
            }
        } else {
            listOf(
                ContactPerson("Aarav", Icons.Outlined.Person, Color(0xFFE3F2FD)),
                ContactPerson("Riya", Icons.Outlined.Person, Color(0xFFFCE4EC)),
                ContactPerson("Meera", Icons.Outlined.Person, Color(0xFFE8F5E9)),
                ContactPerson("Rahul", Icons.Outlined.Person, Color(0xFFF3E5F5))
            )
        }
    }
    val contactRows = remember(contacts) { contacts.chunked(2) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(ElderDestination.Home) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = tr(lang, "elder_back"),
                    tint = Color(0xFF404040)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = tr(lang, "contacts"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1C)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(0.06f))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(contactRows) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ContactCard(
                            person = row[0],
                            modifier = Modifier.weight(1f),
                            onCallClick = {}
                        )
                        if (row.size > 1) {
                            ContactCard(
                                person = row[1],
                                modifier = Modifier.weight(1f),
                                onCallClick = {}
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        SharedBottomNav(
            current = ElderDestination.Contacts,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun EntertainmentListScreen(
    padding: PaddingValues,
    onNavigate: (ElderDestination) -> Unit
) {
    val lang = LocalElderLanguage.current
    val items = remember {
        listOf(
            EntertainmentItem("YouTube", Icons.Outlined.PlayCircle, Color(0xFFFFEBEE)),
            EntertainmentItem("Music", Icons.Outlined.LibraryMusic, Color(0xFFE3F2FD)),
            EntertainmentItem("Movies", Icons.Outlined.Movie, Color(0xFFF3E5F5)),
            EntertainmentItem("TV Shows", Icons.Outlined.LiveTv, Color(0xFFE8F5E9)),
            EntertainmentItem("News", Icons.Outlined.Newspaper, Color(0xFFFFF8E1)),
            EntertainmentItem("Podcasts", Icons.Outlined.Podcasts, Color(0xFFE0F7FA))
        )
    }
    val rows = remember(items) { items.chunked(2) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(ElderDestination.Home) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = tr(lang, "elder_back"),
                    tint = Color(0xFF404040)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = tr(lang, "ott"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1C)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(0.06f))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(rows) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EntertainmentCard(
                            item = row[0],
                            modifier = Modifier.weight(1f)
                        )
                        if (row.size > 1) {
                            EntertainmentCard(
                                item = row[1],
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        SharedBottomNav(
            current = ElderDestination.Entertainment,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun ContactCard(
    person: ContactPerson,
    modifier: Modifier = Modifier,
    onCallClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(182.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(0.05f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFBFBFB))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(person.avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = person.avatarIcon,
                    contentDescription = person.name,
                    tint = Color(0xFF3D3D3D),
                    modifier = Modifier.size(56.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "Call ${person.name}",
                        tint = CareGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = person.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E1E1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

    }
}

@Composable
private fun EntertainmentCard(
    item: EntertainmentItem,
    modifier: Modifier = Modifier
) {
    val lang = LocalElderLanguage.current
    Box(
        modifier = modifier
            .height(182.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(0.05f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFBFBFB))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.name,
                    tint = Color(0xFF5A2D82),
                    modifier = Modifier.size(56.dp)
                )
            }
            Text(
                text = item.name,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E1E1E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tr(lang, "tap_open"),
                fontSize = 14.sp,
                color = Color(0xFF888888)
            )
        }
    }
}

@Composable
private fun StubScreen(
    padding: PaddingValues,
    title: String,
    icon: ImageVector,
    color: Color,
    onSosPressed: () -> Unit,
    current: ElderDestination,
    onNavigate: (ElderDestination) -> Unit
) {
    val lang = LocalElderLanguage.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
        Spacer(Modifier.height(8.dp))
        Text(
            tr(lang, "coming_soon"),
            fontSize = 14.sp,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        SosButton(onSosPressed)
        Spacer(Modifier.height(12.dp))
        SharedBottomNav(current = current, onNavigate = onNavigate)
    }
}

@Composable
private fun SharedBottomNav(
    current: ElderDestination,
    onNavigate: (ElderDestination) -> Unit
) {
    val left = when (current) {
        ElderDestination.Contacts -> ElderDestination.Home
        else -> ElderDestination.Contacts
    }
    val right = when (current) {
        ElderDestination.Entertainment -> ElderDestination.Home
        else -> ElderDestination.Entertainment
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FooterNavCard(
            destination = left,
            modifier = Modifier.weight(1f),
            onNavigate = onNavigate
        )
        FooterNavCard(
            destination = right,
            modifier = Modifier.weight(1f),
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun FooterNavCard(
    destination: ElderDestination,
    modifier: Modifier = Modifier,
    onNavigate: (ElderDestination) -> Unit
) {
    val lang = LocalElderLanguage.current
    data class NavMeta(
        val label: String,
        val icon: ImageVector,
        val tint: Color,
        val bg: Color
    )
    val meta = when (destination) {
        ElderDestination.Home -> NavMeta(tr(lang, "home"), Icons.Outlined.Home, Color(0xFF2F2F2F), Color(0xFFEDEDED))
        ElderDestination.Contacts -> NavMeta(tr(lang, "contacts"), Icons.Outlined.Contacts, Color(0xFF1565C0), Color(0xFFE3F2FD))
        ElderDestination.Entertainment -> NavMeta(tr(lang, "entertainment"), Icons.Outlined.Movie, Color(0xFF6A1B9A), Color(0xFFF3E5F5))
        ElderDestination.Medicines -> NavMeta(tr(lang, "medicines"), Icons.Outlined.MedicalServices, Color(0xFF2E7D32), Color(0xFFE8F5E9))
        ElderDestination.Vitals -> NavMeta(tr(lang, "vitals"), Icons.Outlined.MonitorHeart, Color(0xFFC62828), Color(0xFFFFEBEE))
    }
    ActionCard(
        icon = meta.icon,
        label = meta.label,
        tint = meta.tint,
        bg = meta.bg,
        modifier = modifier.height(170.dp),
        onClick = { onNavigate(destination) }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ElderHomePreview() {
    CareCompanionTheme { ElderHomeScreen() }
}
