package ru.fourlov.app.model

data class Category(
    val slug: String,
    val title: String,
    val subtitle: String,
    val symbol: String,
    val subcategories: List<Category> = emptyList()
)

val categories = listOf(
    Category("give", "Отдам", "Вещи, которые могут пригодиться", "🎁"),
    Category("receive", "Приму в дар", "То, что сейчас очень нужно", "💚"),
    Category("need", "Нужна помощь", "Можно попросить — это нормально", "🤝"),
    Category("help", "Могу помочь", "Знания, время и добрые руки", "🌱"),
    Category("animals", "Животные", "Забота и дом для хвостатых", "🐾"),
    Category("together", "ВМЕСТЕ", "Общие дела и добрые инициативы", "🫶")
)
