package com.medlens.app.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

data class OpenFdaResponse(val results: List<OpenFdaRecord> = emptyList())

data class OpenFdaRecord(
    @SerializedName("set_id") val setId: String? = null,
    @SerializedName("effective_time") val effectiveTime: String? = null,
    @SerializedName("indications_and_usage") val indications: List<String>? = null,
    val purpose: List<String>? = null,
    @SerializedName("how_supplied") val howSupplied: List<String>? = null,
    @SerializedName("storage_and_handling") val storage: List<String>? = null,
    @SerializedName("boxed_warning") val boxedWarning: List<String>? = null,
    val warnings: List<String>? = null,
    @SerializedName("warnings_and_precautions") val warningsAndPrecautions: List<String>? = null,
    val contraindications: List<String>? = null,
    @SerializedName("adverse_reactions") val adverseReactions: List<String>? = null,
    @SerializedName("drug_interactions") val drugInteractions: List<String>? = null,
    @SerializedName("drug_and_or_laboratory_test_interactions") val labInteractions: List<String>? = null,
    @SerializedName("mechanism_of_action") val mechanism: List<String>? = null,
    val precautions: List<String>? = null,
    @SerializedName("general_precautions") val generalPrecautions: List<String>? = null,
    @SerializedName("ask_doctor") val askDoctor: List<String>? = null,
    @SerializedName("ask_doctor_or_pharmacist") val askDoctorOrPharmacist: List<String>? = null,
    @SerializedName("stop_use") val stopUse: List<String>? = null,
    @SerializedName("active_ingredient") val activeIngredient: List<String>? = null,
    val openfda: OpenFdaMetadata? = null,
)

data class OpenFdaMetadata(
    @SerializedName("generic_name") val genericNames: List<String>? = null,
    @SerializedName("product_type") val productTypes: List<String>? = null,
    val route: List<String>? = null,
)

interface OpenFdaApi {
    @GET("drug/label.json")
    suspend fun findLabels(@Query("search") search: String, @Query("limit") limit: Int = 10): OpenFdaResponse
}
