package com.thiarara.myapplicatio.api

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class GeminiService(private val apiKey: String) {
    private val client = OkHttpClient()
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent"
    
    companion object {
        private const val MAX_REQUESTS_PER_MINUTE = 10
        private val requestCount = AtomicInteger(0)
        private var lastResetTime = System.currentTimeMillis()
        
        @Synchronized
        fun getRemainingRequests(): Int {
            checkAndResetCounter()
            return MAX_REQUESTS_PER_MINUTE - requestCount.get()
        }
        
        @Synchronized
        fun getTimeUntilReset(): Long {
            val currentTime = System.currentTimeMillis()
            return maxOf(0L, TimeUnit.MINUTES.toMillis(1) - (currentTime - lastResetTime))
        }
        
        @Synchronized
        private fun checkAndResetCounter() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastResetTime >= TimeUnit.MINUTES.toMillis(1)) {
                requestCount.set(0)
                lastResetTime = currentTime
            }
        }
        
        @Synchronized
        private fun canMakeRequest(): Boolean {
            checkAndResetCounter()
            return if (requestCount.get() < MAX_REQUESTS_PER_MINUTE) {
                requestCount.incrementAndGet()
                true
            } else {
                false
            }
        }
    }

    data class RequestStatus(
        val success: Boolean,
        val message: String,
        val remainingRequests: Int,
        val timeUntilReset: Long
    )

    suspend fun identifyPlant(bitmap: Bitmap): RequestStatus = withContext(Dispatchers.IO) {
        if (!canMakeRequest()) {
            val waitTime = getTimeUntilReset()
            return@withContext RequestStatus(
                success = false,
                message = "Rate limit exceeded. Please wait ${waitTime / 1000} seconds before trying again.",
                remainingRequests = getRemainingRequests(),
                timeUntilReset = waitTime
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", generatePrompt(base64Image))
                            })
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw Exception("API call failed: ${response.code}")
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            val content = if (candidates.length() > 0) {
                candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } else {
                "Unable to identify the plant"
            }

            RequestStatus(
                success = true,
                message = content,
                remainingRequests = getRemainingRequests(),
                timeUntilReset = getTimeUntilReset()
            )
        } catch (e: Exception) {
            RequestStatus(
                success = false,
                message = "Error: ${e.message}",
                remainingRequests = getRemainingRequests(),
                timeUntilReset = getTimeUntilReset()
            )
        }
    }

    suspend fun identifyPlantDisease(bitmap: Bitmap): RequestStatus = withContext(Dispatchers.IO) {
        if (!canMakeRequest()) {
            val waitTime = getTimeUntilReset()
            return@withContext RequestStatus(
                success = false,
                message = "Rate limit exceeded. Please wait ${waitTime / 1000} seconds before trying again.",
                remainingRequests = getRemainingRequests(),
                timeUntilReset = waitTime
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                You are a plant and disease expert. Analyze this image and provide a comprehensive plant and disease analysis report in the following format:

                Common name: [Plant's common name]
                Scientific name: [Plant's scientific name]
                Disease name: [Name of the disease if present, or "No disease detected"]
                Severity: [Mild/Moderate/Severe, or "Not applicable" if no disease]
                
                Symptoms:
                • [List the visible symptoms]
                • [Additional symptoms if present]
                
                Causes:
                • [List the causes of the disease]
                • [Additional causes if applicable]
                
                Control Measures:
                • [List treatment options]
                • [Additional control measures]
                
                Prevention:
                • [List prevention methods]
                • [Additional prevention tips]
                
                Additional Notes:
                • [Any other relevant information about the plant's health]
                • [Growing recommendations if applicable]

                Provide detailed analysis of both the plant and any potential diseases.
            """.trimIndent()

            val response = makeGeminiRequest(base64Image, prompt)
            decrementRemainingRequests()
            
            return@withContext RequestStatus(
                success = true,
                message = response,
                remainingRequests = getRemainingRequests(),
                timeUntilReset = getTimeUntilReset()
            )
        } catch (e: Exception) {
            return@withContext RequestStatus(
                success = false,
                message = e.message ?: "An error occurred during plant and disease analysis",
                remainingRequests = getRemainingRequests(),
                timeUntilReset = getTimeUntilReset()
            )
        }
    }

    suspend fun identifyDiseaseOnly(bitmap: Bitmap): RequestStatus = withContext(Dispatchers.IO) {
        if (!canMakeRequest()) {
            val waitTime = getTimeUntilReset()
            return@withContext RequestStatus(
                success = false,
                message = "Rate limit exceeded. Please wait ${waitTime / 1000} seconds before trying again.",
                remainingRequests = getRemainingRequests(),
                timeUntilReset = waitTime
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                You are a plant disease expert. Analyze this image and provide a detailed disease analysis report in the following format:

                Common name: [Plant's common name]
                Scientific name: [Plant's scientific name]
                Disease name: [Name of the disease if present, or "No disease detected"]
                Severity: [Mild/Moderate/Severe, or "Not applicable" if no disease]
                
                Symptoms:
                • [List the visible symptoms]
                • [Additional symptoms if present]
                
                Causes:
                • [List the causes of the disease]
                • [Additional causes if applicable]
                
                Control Measures:
                • [List treatment options]
                • [Additional control measures]
                
                Prevention:
                • [List prevention methods]
                • [Additional prevention tips]
                
                Additional Notes:
                • [Any other relevant information]

                Focus only on disease identification and analysis. Be specific and detailed.
            """.trimIndent()

            val response = makeGeminiRequest(base64Image, prompt)
            decrementRemainingRequests()
            
            return@withContext RequestStatus(
                success = true,
                message = response,
                remainingRequests = getRemainingRequests(),
                timeUntilReset = getTimeUntilReset()
            )
        } catch (e: Exception) {
            return@withContext RequestStatus(
                success = false,
                message = e.message ?: "An error occurred during disease analysis",
                remainingRequests = getRemainingRequests(),
                timeUntilReset = getTimeUntilReset()
            )
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
    }

    private fun generatePrompt(base64Image: String): String {
        return """
            Analyze this image and provide detailed information in the following format:

            If it's a plant product (fruit, vegetable, processed item):
            Product identification:
            • Identify what the product is
            • Describe its appearance and characteristics
            • Mention its typical uses
            • Note any nutritional highlights

            Common English name: [plant name]
            Scientific name: [scientific name]

            Plant characteristics:
            • Detailed description of the plant that produces this product
            • Notable features
            • Growth habits

            Growing conditions:
            • Ideal climate
            • Soil requirements
            • Sunlight needs
            • Water requirements

            Care instructions:
            • Planting guidelines
            • Maintenance tips
            • Harvesting information

            Common Issues and Diseases:
            • List potential problems
            • Prevention methods
            • Treatment options

            If it's a plant (not a product):
            Common English name: [name]
            Scientific name: [scientific name]

            Plant characteristics:
            [Detailed bullet points about appearance, size, etc.]

            Growing conditions:
            [Bullet points about ideal growing conditions]

            Care instructions:
            [Bullet points about care requirements]

            Common Issues and Diseases:
            [Bullet points about problems and solutions]

            Please be specific and detailed in your analysis.
        """.trimIndent()
    }

    private fun generateDiseasePrompt(base64Image: String): String {
        return """
            You are an expert plant pathologist. Analyze this image with extreme attention to detail for disease identification. Look for even subtle signs of disease or stress. Provide a comprehensive analysis in the following format:

            Plant Information:
            Common name: [plant name]
            Scientific name: [scientific name]

            Disease Identification:
            Disease name: [name of the disease]
            Severity: [mild/moderate/severe]

            Symptoms (be extremely detailed):
            Leaves:
            • Color changes (yellowing, browning, etc.)
            • Spots (size, color, pattern, borders)
            • Wilting (partial/complete, pattern)
            • Texture changes (crispy, soft, mushy)
            • Deformities or growth issues
            
            Stems/Branches:
            • Discoloration
            • Lesions or cankers
            • Wilting patterns
            • Unusual growth
            
            Flowers/Fruit (if present):
            • Discoloration
            • Rot or decay
            • Deformities
            • Premature drop

            Pattern and Progression:
            • Location of symptoms (top/bottom/scattered)
            • Progression pattern (bottom-up, top-down, random)
            • Speed of symptom development
            • Percentage of plant affected

            Environmental Factors:
            • Recent weather conditions impact
            • Humidity levels influence
            • Soil moisture relevance
            • Light exposure effects

            Causes:
            Primary:
            • [Main pathogen/cause]
            • [Disease classification]
            
            Contributing Factors:
            • Environmental conditions
            • Cultural practices
            • Plant stress factors
            • Seasonal influences

            Control Measures:
            Immediate Actions:
            • [Urgent steps to take]
            • [Isolation needs]
            • [Pruning requirements]
            
            Treatment Options:
            • Cultural controls
            • Biological controls
            • Chemical controls (if necessary)
            • Treatment timing considerations

            Prevention:
            Cultural Practices:
            • Spacing and air circulation
            • Watering practices
            • Sanitation measures
            • Pruning techniques
            
            Environmental Management:
            • Temperature control
            • Humidity management
            • Light adjustment
            • Soil management

            Additional Notes:
            • Disease spread potential
            • Similar disease differentiation
            • Long-term plant health impact
            • Recovery prognosis
            • Monitoring recommendations

            Please be extremely specific in your analysis, noting even subtle symptoms that might indicate early stage or complex disease issues. Consider multiple possibilities if symptoms are ambiguous.
        """.trimIndent()
    }

    private suspend fun makeGeminiRequest(base64Image: String, prompt: String): String {
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("$baseUrl?key=$apiKey")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful || responseBody == null) {
            throw Exception("API call failed: ${response.code}")
        }

        val jsonResponse = JSONObject(responseBody)
        val candidates = jsonResponse.getJSONArray("candidates")
        return if (candidates.length() > 0) {
            candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } else {
            throw Exception("No response from API")
        }
    }

    @Synchronized
    private fun decrementRemainingRequests() {
        requestCount.incrementAndGet()
    }
} 