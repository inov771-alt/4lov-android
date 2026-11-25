package ru.fourlov.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import ru.fourlov.app.model.Listing
import java.util.concurrent.TimeUnit

data class ListingsPage(val items: List<Listing>, val totalCount: Int)

class ListingsRepository {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS).build()

    suspend fun loadListings(page: Int = 1, pageSize: Int = 50): Result<ListingsPage> = withContext(Dispatchers.IO) {
        runCatching {
            val safeLimit = pageSize.coerceIn(1, 5000)
            val offset = 0
            val request = Request.Builder().url(ApiConfig.BASE_URL + "api/listings.php?limit=$safeLimit&offset=$offset")
                .header("Accept", "application/json").get().build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Сервер ответил: HTTP ${response.code}" }
                val body = response.body?.string().orEmpty()
                require(body.isNotBlank()) { "Сервер вернул пустой ответ" }
                val root = JSONObject(body)
                if (!root.optBoolean("success", true)) {
                    error(root.optString("message").ifBlank { root.optString("error").ifBlank { "Не удалось получить объявления" } })
                }
                if (root.has("error") && !root.isNull("error")) error(root.optString("error"))
                val payload = root.opt("data")
                val dataObject = payload as? JSONObject
                val array = when (payload) {
                    is JSONArray -> payload
                    is JSONObject -> payload.optJSONArray("items")
                        ?: payload.optJSONArray("listings")
                        ?: dataObject.optJSONArray("results")
                        ?: JSONArray().put(payload)
                    else -> root.optJSONArray("listings")
                        ?: root.optJSONArray("items")
                        ?: root.optJSONArray("results")
                        ?: JSONArray()
                }
                val items = buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val photos = mutableListOf<String>()
                        val photoArray = item.optJSONArray("photos") ?: item.optJSONArray("images")
                        if (photoArray != null) for (p in 0 until photoArray.length()) {
                            val photo = photoArray.opt(p)
                            val path = when (photo) {
                                is JSONObject -> photo.optString("file_path").ifBlank { photo.optString("url") }
                                is String -> photo
                                else -> ""
                            }
                            if (path.isNotBlank()) photos += absoluteUrl(path)
                        }
                        add(Listing(
                            id = item.optString("id", i.toString()),
                            title = item.optString("title").ifBlank { "Объявление" },
                            description = item.optString("description"), city = item.optString("city"),
                            country = when (item.optString("country").ifBlank { item.optString("country_code") }.uppercase()) {
                                "RU" -> "Россия"
                                "BY" -> "Беларусь"
                                else -> item.optString("country")
                            }, categoryName = item.optString("category_name"),
                            subcategoryName = item.optString("subcategory_name"),
                            publishedAt = item.optString("published_at"), photos = photos
                        ))
                    }
                }
                val pagination = root.optJSONObject("pagination") ?: root.optJSONObject("meta")
                    ?: dataObject?.optJSONObject("pagination") ?: dataObject?.optJSONObject("meta")
                val reportedTotal = root.optInt(
                    "total_count",
                    root.optInt(
                        "totalCount",
                        root.optInt(
                            "total",
                            pagination?.optInt("total", -1) ?: -1
                        )
                    )
                ).takeIf { it >= 0 }
                    ?: dataObject?.optInt("total_count", -1)?.takeIf { it >= 0 }
                    ?: dataObject?.optInt("total", -1)?.takeIf { it >= 0 }
                    ?: root.optInt("count", items.size)
                ListingsPage(items, maxOf(reportedTotal, items.size))
            }
        }
    }

    private fun absoluteUrl(path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path
        else ApiConfig.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
}
