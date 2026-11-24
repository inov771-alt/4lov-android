package ru.fourlov.app.data

/**
 * API integration boundary.
 *
 * Intentionally contains no guessed routes, parameters or JSON models. Wire these
 * methods only after checking the real PHP API files and API documentation.
 */
interface FourLovApi {
    suspend fun loadCategories(): ApiResult<Nothing>
    suspend fun loadCountries(): ApiResult<Nothing>
    suspend fun loadCities(countryCode: String, query: String): ApiResult<Nothing>
    suspend fun loadListings(filters: ListingQuery): ApiResult<Nothing>
    suspend fun loadListing(id: String): ApiResult<Nothing>
    suspend fun requestPublicationToken(): ApiResult<Nothing>
    suspend fun createListing(payload: ListingDraft): ApiResult<Nothing>
    suspend fun reportListing(listingId: String, reason: String): ApiResult<Nothing>
}

data class ListingQuery(
    val categorySlug: String? = null,
    val countryCode: String? = null,
    val cityId: String? = null,
    val searchText: String? = null
)

data class ListingDraft(
    val values: Map<String, String> = emptyMap()
)

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>
}
