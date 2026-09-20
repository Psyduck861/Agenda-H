package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

object GeminiApiClient {

    // Target model: gemini-flash-latest as requested
    const val MODEL_NAME = "gemini-flash-latest"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    /**
     * Obtains the active Gemini API key following the priority:
     * 1. User input key (if provided and valid)
     * 2. BuildConfig.GEMINI_API_KEY (populated via Secrets plugin / .env)
     * 3. System.getenv("GEMINI_API_KEY") (process.env.GEMINI_API_KEY standard environment)
     * 4. System.getProperty("GEMINI_API_KEY")
     */
    fun resolveApiKey(userKey: String = ""): String {
        val trimmedUser = userKey.trim()
        if (isValidKey(trimmedUser)) {
            return trimmedUser
        }

        val buildConfigKey = try {
            com.example.BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        if (isValidKey(buildConfigKey)) {
            return buildConfigKey
        }

        val envKey = (System.getenv("GEMINI_API_KEY") ?: "").trim()
        if (isValidKey(envKey)) {
            return envKey
        }

        val propKey = (System.getProperty("GEMINI_API_KEY") ?: "").trim()
        if (isValidKey(propKey)) {
            return propKey
        }

        return ""
    }

    private fun isValidKey(key: String): Boolean {
        return key.isNotEmpty() &&
                key != "MY_GEMINI_API_KEY" &&
                key != "PLACEHOLDER" &&
                !key.startsWith("YOUR_")
    }

    /**
     * Executes a prompt call to Gemini flash-latest with robust error handling for connection,
     * timeout, authentication and quota errors.
     */
    suspend fun generateContent(
        prompt: String,
        userKey: String = "",
        bitmaps: List<Bitmap> = emptyList(),
        systemInstruction: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(userKey)
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "⚠️ Chave API do Gemini não configurada!\n\n" +
                    "Configure a chave 'GEMINI_API_KEY' no painel de Segredos (Secrets) do Studio ou " +
                    "insira-a diretamente nas Configurações do app para usar o modelo $MODEL_NAME."
                )
            )
        }

        try {
            val requestObj = JSONObject()

            // System Instruction if provided
            if (systemInstruction.isNotBlank()) {
                val sysPart = JSONObject().put("text", systemInstruction)
                val sysContent = JSONObject().put("parts", JSONArray().put(sysPart))
                requestObj.put("systemInstruction", sysContent)
            }

            // Main Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Text prompt
            partsArray.put(JSONObject().put("text", prompt))

            // Multimodal bitmaps if any
            for (bmp in bitmaps) {
                val imagePart = JSONObject()
                val inlineData = JSONObject()
                val baos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                val base64Str = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", base64Str)
                imagePart.put("inlineData", inlineData)
                partsArray.put(imagePart)
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestObj.put("contents", contentsArray)

            val url = URL("$BASE_URL?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 40000
            conn.readTimeout = 40000

            conn.outputStream.use { os ->
                val bytes = requestObj.toString().toByteArray(Charsets.UTF_8)
                os.write(bytes, 0, bytes.size)
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseText)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val outContent = candidate.optJSONObject("content")
                    val outParts = outContent?.optJSONArray("parts")
                    val text = outParts?.optJSONObject(0)?.optString("text", "") ?: ""
                    if (text.isNotBlank()) {
                        Result.success(text)
                    } else {
                        Result.failure(Exception("O modelo Gemini retornou uma resposta sem texto legível."))
                    }
                } else {
                    Result.failure(Exception("Nenhum resultado retornado pelo modelo Gemini."))
                }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                val friendlyMessage = formatHttpError(responseCode, errorBody)
                Result.failure(Exception(friendlyMessage))
            }
        } catch (e: UnknownHostException) {
            Result.failure(
                Exception(
                    "🌐 Erro de Conexão: Não foi possível alcançar os servidores do Google Gemini.\n" +
                    "Verifique se o seu celular está conectado à internet (Wi-Fi ou dados móveis) e tente novamente."
                )
            )
        } catch (e: SocketTimeoutException) {
            Result.failure(
                Exception(
                    "⏱️ Tempo Limite Esgotado: A requisição ao Gemini demorou muito para responder.\n" +
                    "Verifique a estabilidade da sua conexão de internet e tente novamente."
                )
            )
        } catch (e: ConnectException) {
            Result.failure(
                Exception(
                    "🔌 Falha de Conexão: Não foi possível estabelecer conexão com o servidor da API.\n" +
                    "Verifique sua rede ou tente novamente em alguns instantes."
                )
            )
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    "❌ Falha de comunicação com o Gemini:\n${e.localizedMessage ?: "Erro desconhecido"}\n" +
                    "Verifique sua conexão e tente novamente."
                )
            )
        }
    }

    private fun formatHttpError(code: Int, errorBody: String): String {
        return when (code) {
            400 -> "❌ Requisição Inválida (Erro 400):\nOs dados enviados não puderam ser processados pelo Gemini.\n$errorBody"
            401, 403 -> "❌ Chave de API Inválida (Erro $code):\nA chave GEMINI_API_KEY informada não é válida ou não possui permissões no Google AI Studio.\nRevise suas configurações."
            429 -> "❌ Limite de Cota Excedido (Erro 429):\nVocê atingiu o limite de requisições do modelo $MODEL_NAME. Aguarde um momento e tente novamente."
            500, 502, 503, 504 -> "❌ Servidor do Google Indisponível (Erro $code):\nOs servidores do Gemini estão momentaneamente instáveis. Tente novamente em alguns segundos."
            else -> "❌ Falha na API Gemini (Código $code):\n$errorBody"
        }
    }
}
