package ru.fourlov.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import ru.fourlov.app.model.Listing
import java.io.IOException
import java.util.concurrent.TimeUnit

class ListingRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun loadListings(): Result<List<Listing>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = requestJson("${ApiConfig.BASE_URL}api/listings.php")
            if (!json.optBoolean("success")) {
                throw IOException(json.optString("message", "Не удалось загрузить объявления"))
            }
            val data = json.optJSONArray("data")
                ?: throw IOException("В ответе нет списка объявлений")

            buildList {
                for (index in 0 until data.length()) {
                    val item = data.optJSONObject(index) ?: continue
                    add(item.toListing())
                }
            }
        }
    }

    suspend fun loadListing(id: String): Result<Listing> = withContext(Dispatchers.IO) {
        runCatching {
            val json = requestJson("${ApiConfig.BASE_URL}api/listings.php?id=${id}")
            if (!json.optBoolean("success")) {
                throw IOException(json.optString("message", "Не удалось загрузить объявление"))
            }
            val data = json.optJSONObject("data")
                ?: throw IOException("В ответе нет данных объявления")
            data.toListing()
        }
    }

    private fun requestJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Сервер ответил с кодом ${response.code}")
            }
            val body = response.body?.string()
                ?: throw IOException("Сервер вернул пустой ответ")
            return JSONObject(body)
        }
    }

    private fun JSONObject.toListing(): Listing {
        val photosJson = optJSONArray("photos")
        val photos = buildList {
            if (photosJson != null) {
                for (index in 0 until photosJson.length()) {
                    val photo = photosJson.optString(index)
                    if (photo.isNotBlank() && photo != "null") add(photo)
                }
            }
        }

        return Listing(
            id = optString("id"),
            title = optString("title"),
            description = optString("description"),
            city = optString("city"),
            countryCode = optString("country_code"),
            categoryName = optString("category_name"),
            categorySlug = optString("category_slug"),
            subcategoryName = optString("subcategory_name"),
            photos = photos,
            createdAt = optString("created_at"),
            publishedAt = optString("published_at"),
            address = optString("address").takeIf { it.isNotBlank() && it != "null" },
            contactName = optString("contact_name").takeIf { it.isNotBlank() && it != "null" },
            contactMethod = optString("contact_method").takeIf { it.isNotBlank() && it != "null" },
            contactValue = optString("contact_value").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}
