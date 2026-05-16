package com.carecompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.carecompanion.app.network.AuthVerifyResponse
import com.carecompanion.app.network.CareApiService
import com.carecompanion.app.network.CareRole
import com.carecompanion.app.network.CareSessionStore
import com.carecompanion.app.network.CreateGuardianProfileBody
import com.carecompanion.app.network.CreateMedicineBody
import com.carecompanion.app.network.OtpRequestBody
import com.carecompanion.app.network.OtpVerifyBody
import com.carecompanion.app.network.buildCareApi
import com.carecompanion.app.network.dtoToMedicine
import com.carecompanion.app.ui.theme.CareCompanionTheme
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class AppScreen {
    object Login : AppScreen()
    data class ElderHome(val name: String) : AppScreen()
    object GuardianHome : AppScreen()
    object GuardianAddProfile : AppScreen()
    data class GuardianManageElder(val profile: GuardianProfile) : AppScreen()
    data class GuardianManageContacts(val profile: GuardianProfile) : AppScreen()
    data class GuardianAddContact(val profile: GuardianProfile) : AppScreen()
    data class GuardianManageMedicines(val profile: GuardianProfile) : AppScreen()
    data class GuardianAddMedicine(val profile: GuardianProfile) : AppScreen()
    data class GuardianDailySchedule(val profile: GuardianProfile) : AppScreen()
    data class GuardianScheduleMedicine(val profile: GuardianProfile) : AppScreen()
    data class GuardianWellnessSos(val profile: GuardianProfile) : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CareCompanionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val scope = rememberCoroutineScope()
                    val sessionStore = remember { CareSessionStore(this@MainActivity) }
                    val api = remember { buildCareApi() }

                    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Login) }
                    val guardianProfiles = remember { mutableStateListOf<GuardianProfile>() }
                    val contactsByProfile = remember { mutableStateMapOf<String, List<ManagedContact>>() }
                    val medicinesByProfile = remember { mutableStateMapOf<String, List<Medicine>>() }

                    var elderMedicines by remember { mutableStateOf<List<MedicineItem>>(emptyList()) }
                    var elderContactsList by remember { mutableStateOf<List<ManagedContact>>(emptyList()) }

                    fun bearer(): String = sessionStore.accessToken?.let { "Bearer $it" } ?: ""
                    fun profileKey(profile: GuardianProfile): String = profile.stableKey()

                    fun logout() {
                        sessionStore.clear()
                        screen = AppScreen.Login
                    }

                    suspend fun guardianReloadProfiles(): List<GuardianProfile> =
                        api.listGuardianProfiles(bearer()).profiles.map { it.toGuardianProfile() }

                    suspend fun guardianMedicinesFor(profileId: String): List<Medicine> {
                        val rows = api.listGuardianMedicines(bearer(), profileId).medicines
                        return rows.map { dto -> dtoToMedicine(dto) }
                    }

                    LaunchedEffect(Unit) {
                        val token = sessionStore.accessToken ?: return@LaunchedEffect
                        try {
                            when (sessionStore.role) {
                                CareRole.GUARDIAN -> {
                                    val fresh = api.listGuardianProfiles("Bearer $token").profiles.map { it.toGuardianProfile() }
                                    guardianProfiles.clear()
                                    guardianProfiles.addAll(fresh)
                                    screen = AppScreen.GuardianHome
                                }
                                CareRole.ELDER -> {
                                    val hdr = "Bearer $token"
                                    val me = api.elderMe(hdr)
                                    elderMedicines =
                                        elderMedicinesFromRows(api.elderMedicines(hdr).medicines)
                                    elderContactsList =
                                        api.elderContacts(hdr).contacts.map { c ->
                                            ManagedContact(
                                                name = c.name,
                                                phone = c.phoneE164.takeLast(10).ifBlank { c.phoneE164 },
                                            )
                                        }
                                    screen = AppScreen.ElderHome(me.profile.displayName)
                                }
                                null -> sessionStore.clear()
                            }
                        } catch (_: Exception) {
                            sessionStore.clear()
                        }
                    }

                    suspend fun bootstrapAfterLogin(role: CareRole, @Suppress("UNUSED_PARAMETER") resp: AuthVerifyResponse) {
                        when (role) {
                            CareRole.GUARDIAN -> {
                                val fresh = guardianReloadProfiles()
                                guardianProfiles.clear()
                                guardianProfiles.addAll(fresh)
                                medicinesByProfile.clear()
                                contactsByProfile.clear()
                                screen = AppScreen.GuardianHome
                            }
                            CareRole.ELDER -> {
                                val hdr = bearer()
                                val me = api.elderMe(hdr)
                                elderMedicines = elderMedicinesFromRows(api.elderMedicines(hdr).medicines)
                                elderContactsList =
                                    api.elderContacts(hdr).contacts.map { c ->
                                        ManagedContact(
                                            name = c.name,
                                            phone = c.phoneE164.takeLast(10).ifBlank { c.phoneE164 },
                                        )
                                    }
                                screen = AppScreen.ElderHome(me.profile.displayName)
                            }
                        }
                    }

                    when (val s = screen) {

                        is AppScreen.Login -> {
                            LoginScreen(
                                api = api,
                                onSuccess = { role, authResp ->
                                    sessionStore.accessToken = authResp.accessToken
                                    sessionStore.role = role
                                    scope.launch {
                                        bootstrapAfterLogin(role, authResp)
                                    }
                                },
                            )
                        }

                        is AppScreen.ElderHome -> {
                            ElderHomeScreen(
                                elderName = s.name,
                                elderMedicines = elderMedicines,
                                onSosPressed = {},
                                onLogout = {
                                    logout()
                                    elderMedicines = emptyList()
                                    elderContactsList = emptyList()
                                },
                                elderContacts = elderContactsList,
                            )
                        }

                        is AppScreen.GuardianHome -> {
                            GuardianHomeScreen(
                                profiles = guardianProfiles,
                                onAddProfile = { screen = AppScreen.GuardianAddProfile },
                                onManageProfile = { profile -> screen = AppScreen.GuardianManageElder(profile) },
                                onLogout = {
                                    guardianProfiles.clear()
                                    contactsByProfile.clear()
                                    medicinesByProfile.clear()
                                    logout()
                                },
                            )
                        }

                        is AppScreen.GuardianAddProfile -> {
                            GuardianAddProfileScreen(
                                onBack = { screen = AppScreen.GuardianHome },
                                elderPhoneOtp =
                                    if (sessionStore.accessToken != null) {
                                        ElderPhoneOtpCallbacks(
                                            requestOtp = { e164 ->
                                                runCatching {
                                                    api.requestOtp(
                                                        OtpRequestBody(phone = e164, role = "ELDER"),
                                                    )
                                                }.map { }
                                            },
                                            verifyOtp = { e164, code ->
                                                runCatching {
                                                    api.verifyOtp(
                                                        OtpVerifyBody(phone = e164, code = code),
                                                    )
                                                    Unit
                                                }.map { }
                                            },
                                        )
                                    } else {
                                        null
                                    },
                                onSaveNext = { profile ->
                                    scope.launch {
                                        try {
                                            if (sessionStore.accessToken == null) {
                                                guardianProfiles.add(profile)
                                                screen = AppScreen.GuardianHome
                                                return@launch
                                            }
                                            val hdr = bearer()
                                            val body = CreateGuardianProfileBody(
                                                displayName = profile.name,
                                                linkedElderPhone =
                                                    profile.linkedElderPhone.takeIf { it.isNotBlank() },
                                                avatarIconKey = profile.avatarIconKey,
                                                avatarBgArgb = profile.avatarBgArgb,
                                                photoUrl = null,
                                            )
                                            api.createGuardianProfile(hdr, body)
                                            val fresh = guardianReloadProfiles()
                                            guardianProfiles.clear()
                                            guardianProfiles.addAll(fresh)
                                            screen = AppScreen.GuardianHome
                                        } catch (_: HttpException) {
                                            guardianProfiles.add(profile)
                                            screen = AppScreen.GuardianHome
                                        } catch (_: Exception) {
                                            guardianProfiles.add(profile)
                                            screen = AppScreen.GuardianHome
                                        }
                                    }
                                },
                            )
                        }

                        is AppScreen.GuardianManageElder -> {
                            GuardianManageElderScreen(
                                profile = s.profile,
                                onBack = { screen = AppScreen.GuardianHome },
                                onSwitchProfiles = { screen = AppScreen.GuardianHome },
                                onLogout = {
                                    guardianProfiles.clear()
                                    contactsByProfile.clear()
                                    medicinesByProfile.clear()
                                    logout()
                                },
                                onOpenContacts = { screen = AppScreen.GuardianManageContacts(s.profile) },
                                onOpenMedicines = { screen = AppScreen.GuardianManageMedicines(s.profile) },
                                onOpenDailySchedule = { screen = AppScreen.GuardianDailySchedule(s.profile) },
                                onOpenWellnessSos = { screen = AppScreen.GuardianWellnessSos(s.profile) },
                            )
                        }

                        is AppScreen.GuardianManageContacts -> {
                            val key = profileKey(s.profile)
                            GuardianManageContactsScreen(
                                profile = s.profile,
                                initialContacts = contactsByProfile[key].orEmpty(),
                                onBack = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onSaveContacts = { updated -> contactsByProfile[key] = updated },
                                onAddContact = { screen = AppScreen.GuardianAddContact(s.profile) },
                                onLogout = {
                                    guardianProfiles.clear()
                                    contactsByProfile.clear()
                                    medicinesByProfile.clear()
                                    logout()
                                },
                            )
                        }

                        is AppScreen.GuardianAddContact -> {
                            val key = profileKey(s.profile)
                            GuardianAddContactScreen(
                                profile = s.profile,
                                onBack = { screen = AppScreen.GuardianManageContacts(s.profile) },
                                onSave = { contact ->
                                    val current = contactsByProfile[key].orEmpty().toMutableList()
                                    current.add(contact)
                                    contactsByProfile[key] = current
                                    screen = AppScreen.GuardianManageContacts(s.profile)
                                },
                            )
                        }

                        is AppScreen.GuardianManageMedicines -> {
                            val key = profileKey(s.profile)
                            val pid = s.profile.backendId
                            LaunchedEffect(pid) {
                                if (pid.isNotBlank()) {
                                    try {
                                        medicinesByProfile[key] =
                                            guardianMedicinesFor(pid)
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                            GuardianManageMedicinesScreen(
                                profile = s.profile,
                                medicines = medicinesByProfile[key].orEmpty(),
                                onBack = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onSaveMedicines = { updated -> medicinesByProfile[key] = updated },
                                onAddMedicine = { screen = AppScreen.GuardianAddMedicine(s.profile) },
                                onNavigateHome = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onNavigateSos = { screen = AppScreen.GuardianWellnessSos(s.profile) },
                            )
                        }

                        is AppScreen.GuardianAddMedicine -> {
                            val key = profileKey(s.profile)
                            GuardianAddMedicineScreen(
                                profile = s.profile,
                                onBack = { screen = AppScreen.GuardianManageMedicines(s.profile) },
                                onSave = { medicine ->
                                    scope.launch {
                                        val pid = s.profile.backendId
                                        if (pid.isNotBlank() && bearer().isNotBlank()) {
                                            try {
                                                api.createGuardianMedicine(
                                                    bearer(),
                                                    pid,
                                                    CreateMedicineBody(
                                                        name = medicine.name,
                                                        dosage =
                                                            medicine.dosage.ifBlank { null },
                                                        form =
                                                            medicine.form.ifBlank { null },
                                                        isActive = true,
                                                        pillImageUrl = null,
                                                        packetFrontUrl = null,
                                                        packetBackUrl = null,
                                                        schedules = emptyList(),
                                                    ),
                                                )
                                                medicinesByProfile[key] =
                                                    guardianMedicinesFor(pid)
                                            } catch (_: Exception) {
                                                val current =
                                                    medicinesByProfile[key].orEmpty()
                                                        .toMutableList()
                                                current.add(medicine)
                                                medicinesByProfile[key] = current
                                            }
                                        } else {
                                            val current =
                                                medicinesByProfile[key].orEmpty().toMutableList()
                                            current.add(medicine)
                                            medicinesByProfile[key] = current
                                        }
                                        screen =
                                            AppScreen.GuardianManageMedicines(s.profile)
                                    }
                                },
                            )
                        }

                        is AppScreen.GuardianDailySchedule -> {
                            val key = profileKey(s.profile)
                            LaunchedEffect(s.profile.backendId) {
                                val pid = s.profile.backendId
                                if (pid.isNotBlank()) {
                                    try {
                                        medicinesByProfile[key] =
                                            guardianMedicinesFor(pid)
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                            GuardianDailyScheduleScreen(
                                profile = s.profile,
                                medicines = medicinesByProfile[key].orEmpty(),
                                onBack = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onSaveMedicines = { updated -> medicinesByProfile[key] = updated },
                                onAddSchedule = { screen = AppScreen.GuardianScheduleMedicine(s.profile) },
                                onNavigateHome = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onNavigateSos = { screen = AppScreen.GuardianWellnessSos(s.profile) },
                            )
                        }

                        is AppScreen.GuardianScheduleMedicine -> {
                            val key = profileKey(s.profile)
                            GuardianScheduleMedicineScreen(
                                medicines = medicinesByProfile[key].orEmpty(),
                                onBack = { screen = AppScreen.GuardianDailySchedule(s.profile) },
                                onSave = { updated ->
                                    medicinesByProfile[key] = updated
                                    scope.launch {
                                        val pid = s.profile.backendId
                                        if (pid.isNotBlank()) {
                                            try {
                                                medicinesByProfile[key] =
                                                    guardianMedicinesFor(pid)
                                            } catch (_: Exception) {
                                            }
                                        }
                                    }
                                    screen = AppScreen.GuardianDailySchedule(s.profile)
                                },
                            )
                        }

                        is AppScreen.GuardianWellnessSos -> {
                            GuardianWellnessSosScreen(
                                profile = s.profile,
                                onBack = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onNavigateHome = { screen = AppScreen.GuardianManageElder(s.profile) },
                            )
                        }
                    }
                }
            }
        }
    }
}
