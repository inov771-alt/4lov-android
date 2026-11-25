package ru.fourlov.app.model

data class Listing(
    val id: String,
    val title: String,
    val description: String,
    val city: String,
    val country: String,
    val categoryName: String,
    val subcategoryName: String,
    val publishedAt: String,
    val photos: List<String> = emptyList()
) {
    val location: String
        get() = listOf(city, country).filter { it.isNotBlank() }.distinct().joinToString(", ")

    val categoryPath: String
        get() = listOf(categoryName, subcategoryName)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" · ")
}
