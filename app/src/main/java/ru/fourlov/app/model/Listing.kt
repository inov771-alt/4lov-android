package ru.fourlov.app.model

data class Listing(
    val id: String,
    val title: String,
    val description: String,
    val city: String,
    val countryCode: String,
    val categoryName: String,
    val categorySlug: String,
    val subcategoryName: String,
    val photos: List<String>,
    val createdAt: String,
    val publishedAt: String,
    val contactName: String? = null,
    val contactMethod: String? = null,
    val contactValue: String? = null
)
