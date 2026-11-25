package ru.fourlov.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import ru.fourlov.app.model.Listing
import java.util.concurrent.TimeUnit

/**
 * Read-only client for the existing public listings endpoint.
 * It deliberately sends no guessed filters or publication requests.
 */
class ListingsRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun loadListings(): Result<List<Listing>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(ApiConfig.BASE_URL + "api/listings.php")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Сервер ответил: HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                require(body.isNotBlank()) { "Сервер вернул пустой ответ" }
                val root = JSONObject(body)
                val payload = root.opt("data")
                val array = when (payload) {
                    is JSONArray -> payload
                    is JSONObject -> JSONArray().put(payload)
                    else -> root.optJSONArray("listings") ?: JSONArray()
                }
                if (root.has("error") && !root.isNull("error")) {
                    error(root.optString("error", "Не удалось загрузить объявления"))
                }
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val photos = mutableListOf<String>()
                        val photoArray = item.optJSONArray("photos")
                            ?: item.optJSONArray("images")
                        if (photoArray != null) {
                            for (photoIndex in 0 until photoArray.length()) {
                                val photo = photoArray.opt(photoIndex)
                                val path = when (photo) {
                                    is JSONObject -> photo.optString("file_path")
                                        .ifBlank { photo.optString("url") }
                                    is String -> photo
                                    else -> ""
                                }
                                if (path.isNotBlank()) photos += absoluteUrl(path)
                            }
                        }
                        add(
                            Listing(
                                id = item.optString("id", index.toString()),
                                title = item.optString("title").ifBlank { "Объявление" },
                                description = item.optString("description"),
                                city = item.optString("city"),
                                country = item.optString("country"),
                                categoryName = item.optString("category_name"),
                                subcategoryName = item.optString("subcategory_name"),
                                publishedAt = item.optString("published_at"),
                                photos = photos
                            )
                        )
                    }
                }
            }
        }
    }

    private fun absoluteUrl(path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path
        else ApiConfig.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
}
