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
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}api/listings.php")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Сервер ответил с кодом ${response.code}")
                }
                val body = response.body?.string()
                    ?: throw IOException("Сервер вернул пустой ответ")
                val json = JSONObject(body)
                if (!json.optBoolean("success")) {
                    throw IOException(json.optString("message", "Не удалось загрузить объявления"))
                }
                val data = json.optJSONArray("data")
                    ?: throw IOException("В ответе нет списка объявлений")

                buildList {
                    for (index in 0 until data.length()) {
                        val item = data.optJSONObject(index) ?: continue
                        val photosJson = item.optJSONArray("photos")
                        val photos = buildList {
                            if (photosJson != null) {
                                for (photoIndex in 0 until photosJson.length()) {
                                    val photo = photosJson.optString(photoIndex)
                                    if (photo.isNotBlank() && photo != "null") add(photo)
                                }
                            }
                        }
                        add(
                            Listing(
                                id = item.optString("id"),
                                title = item.optString("title"),
                                description = item.optString("description"),
                                city = item.optString("city"),
                                countryCode = item.optString("country_code"),
                                categoryName = item.optString("category_name"),
                                categorySlug = item.optString("category_slug"),
                                subcategoryName = item.optString("subcategory_name"),
                                photos = photos,
                                createdAt = item.optString("created_at"),
                                publishedAt = item.optString("published_at")
                            )
                        )
                    }
                }
            }
        }
    }
}
