package com.carecompanion.app.network

import com.google.gson.annotations.SerializedName
import com.carecompanion.app.BuildConfig
import com.carecompanion.app.MealTiming
import com.carecompanion.app.Medicine
import com.carecompanion.app.MedicineSchedule
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class OtpRequestBody(
    val phone: String,
    val role: String,
)

data class OtpVerifyBody(
    val phone: String,
    val code: String,
)

data class OtpSentResponse(val sent: Boolean)

data class AuthVerifyResponse(
    @SerializedName("accessToken") val accessToken: String,
    val user: AuthUserDto,
)

data class AuthUserDto(
    val id: String,
    val phoneE164: String,
    val role: String,
)

data class GuardianProfilesResponse(val profiles: List<GuardianProfileDto>)

data class GuardianProfileDto(
    val id: String,
    val displayName: String,
    @SerializedName("linkedElderPhoneE164") val linkedElderPhoneE164: String?,
    val avatarIconKey: String?,
    val avatarBgArgb: Int?,
    val photoUrl: String?,
)

data class CreateGuardianProfileBody(
    val displayName: String,
    val linkedElderPhone: String?,
    val avatarIconKey: String?,
    val avatarBgArgb: Int?,
    val photoUrl: String?,
)

data class CreateGuardianProfileResponse(val profile: CreatedProfileDto)

data class CreatedProfileDto(
    val id: String,
    val displayName: String,
    val linkedElderPhoneE164: String?,
)

data class GuardianMedicinesResponse(val medicines: List<GuardianMedicineDto>)

data class GuardianMedicineDto(
    val id: String,
    val name: String,
    val dosage: String,
    val form: String,
    val isActive: Boolean,
    val pillImageUrl: String?,
    val packetFrontUrl: String?,
    val packetBackUrl: String?,
    val schedules: List<GuardianScheduleDto>,
)

data class GuardianScheduleDto(
    val id: String,
    val mealLabel: String,
    val timeLabel: String,
    val mealTiming: String,
    val withWater: Boolean,
    val enabled: Boolean,
)

data class CreateMedicineBody(
    val name: String,
    val dosage: String?,
    val form: String?,
    val isActive: Boolean?,
    val pillImageUrl: String?,
    val packetFrontUrl: String?,
    val packetBackUrl: String?,
    val schedules: List<CreateScheduleBody>?,
)

data class CreateScheduleBody(
    val mealLabel: String,
    val timeLabel: String,
    val mealTiming: String,
    val withWater: Boolean,
    val enabled: Boolean,
)

data class ElderMeResponse(val profile: ElderMeProfileDto)

data class ElderMeProfileDto(
    val id: String,
    val displayName: String,
)

data class ElderMedicinesPayload(val medicines: List<ElderMedicineRowDto>)

data class ElderMedicineRowDto(
    val id: String,
    val name: String,
    val dosage: String,
    val form: String,
    val schedules: List<ElderScheduleDto>,
)

data class ElderScheduleDto(
    val mealLabel: String,
    val timeLabel: String,
    val mealTiming: String,
    val withWater: Boolean,
)

data class ContactsPayload(val contacts: List<ContactDto>)

data class MedicineCreatedResponse(val medicine: GuardianMedicineDto)

data class ContactDto(
    val id: String,
    val name: String,
    val phoneE164: String,
    val photoUrl: String?,
)

interface CareApiService {
    @POST("v1/auth/otp/request")
    suspend fun requestOtp(@Body body: OtpRequestBody): OtpSentResponse

    @POST("v1/auth/otp/verify")
    suspend fun verifyOtp(@Body body: OtpVerifyBody): AuthVerifyResponse

    @GET("v1/guardian/profiles")
    suspend fun listGuardianProfiles(@Header("Authorization") authorization: String): GuardianProfilesResponse

    @POST("v1/guardian/profiles")
    suspend fun createGuardianProfile(
        @Header("Authorization") authorization: String,
        @Body body: CreateGuardianProfileBody,
    ): CreateGuardianProfileResponse

    @GET("v1/guardian/profiles/{profileId}/medicines")
    suspend fun listGuardianMedicines(
        @Header("Authorization") authorization: String,
        @Path("profileId") profileId: String,
    ): GuardianMedicinesResponse

    @POST("v1/guardian/profiles/{profileId}/medicines")
    suspend fun createGuardianMedicine(
        @Header("Authorization") authorization: String,
        @Path("profileId") profileId: String,
        @Body body: CreateMedicineBody,
    ): MedicineCreatedResponse

    @GET("v1/elder/me")
    suspend fun elderMe(@Header("Authorization") authorization: String): ElderMeResponse

    @GET("v1/elder/medicines")
    suspend fun elderMedicines(@Header("Authorization") authorization: String): ElderMedicinesPayload

    @GET("v1/elder/contacts")
    suspend fun elderContacts(@Header("Authorization") authorization: String): ContactsPayload
}

fun buildCareApi(): CareApiService {
    val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()
    val base = BuildConfig.API_BASE_URL.trimEnd('/') + "/"
    val retrofit = Retrofit.Builder()
        .baseUrl(base)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    return retrofit.create(CareApiService::class.java)
}

fun digitsOnlyPhone(input: String): String = input.filter { it.isDigit() }

/** Match server: 10-digit India local → prefix 91 */
fun normalizeToE164Digits(digits: String): String =
    when {
        digits.startsWith('0') -> digits.trimStart('0')
        digits.length == 10 -> "91$digits"
        else -> digits
    }

fun dtoToMedicine(d: GuardianMedicineDto): Medicine =
    Medicine(
        id = d.id,
        name = d.name,
        dosage = d.dosage,
        form = d.form,
        isActive = d.isActive,
        schedules = d.schedules
            .filter { it.enabled }
            .sortedBy { it.timeLabel }
            .map { s ->
                MedicineSchedule(
                    label = s.mealLabel.ifBlank { s.timeLabel },
                    time = s.timeLabel,
                    enabled = s.enabled,
                    withWater = s.withWater,
                    mealTiming = if (s.mealTiming.uppercase() == "AFTER") MealTiming.After else MealTiming.Before,
                )
            },
    )
