package ru.fourlov.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.fourlov.app.data.ListingsRepository
import ru.fourlov.app.model.Category
import ru.fourlov.app.model.Listing
import ru.fourlov.app.model.categories
import ru.fourlov.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FourLovTheme { FourLovApp() } }
    }
}

private enum class AppTab { HOME, SEARCH, PUBLISH }

@Composable
private fun FourLovApp() {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSubcategory by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Россия") }
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val repository = remember { ListingsRepository() }
    val scope = rememberCoroutineScope()

    fun refreshListings() {
        scope.launch {
            loading = true
            error = null
            repository.loadListings().onSuccess { listings = it }
                .onFailure { error = it.message ?: "Не удалось загрузить объявления" }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refreshListings() }

    val visibleListings = listings.filter { listing ->
        val matchesText = searchText.isBlank() || listing.title.contains(searchText, true) ||
            listing.description.contains(searchText, true) || listing.city.contains(searchText, true)
        val matchesCountry = country.isBlank() || listing.country.isBlank() ||
            listing.country.contains(country, true) ||
            (country == "Россия" && listing.country.contains("RU", true)) ||
            (country == "Беларусь" && listing.country.contains("BY", true))
        val matchesCategory = selectedCategory == null ||
            listing.categoryName.contains(selectedCategory!!.title, true) ||
            listing.categoryPath.contains(selectedCategory!!.title, true) ||
            (selectedCategory!!.slug == "together" && listing.categoryName.contains("вмест", true))
        val matchesSubcategory = selectedSubcategory == null ||
            listing.subcategoryName.equals(selectedSubcategory, true)
        matchesText && matchesCountry && matchesCategory && matchesSubcategory
    }

    Scaffold(containerColor = Background, bottomBar = {
        NavigationBar(containerColor = SurfaceWhite) {
            NavigationBarItem(tab == AppTab.HOME, { tab = AppTab.HOME },
                icon = { Icon(Icons.Default.Home, null) }, label = { Text("Главная") })
            NavigationBarItem(tab == AppTab.SEARCH, { tab = AppTab.SEARCH },
                icon = { Icon(Icons.Default.Search, null) }, label = { Text("Поиск") })
            NavigationBarItem(tab == AppTab.PUBLISH, { tab = AppTab.PUBLISH },
                icon = { Icon(Icons.Default.AddCircleOutline, null) }, label = { Text("Разместить") })
        }
    }) { padding ->
        when (tab) {
            AppTab.HOME -> HomeScreen(Modifier.padding(padding), visibleListings, loading, error,
                selectedCategory, selectedSubcategory,
                { selectedCategory = it; selectedSubcategory = null },
                { selectedSubcategory = it }, ::refreshListings, { tab = AppTab.SEARCH })
            AppTab.SEARCH -> SearchScreen(Modifier.padding(padding), searchText, { searchText = it },
                country, { country = it }, selectedCategory,
                { selectedCategory = it; selectedSubcategory = null },
                visibleListings, loading, error, ::refreshListings)
            AppTab.PUBLISH -> PublishInfoScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier, listings: List<Listing>, loading: Boolean, error: String?,
    selectedCategory: Category?, selectedSubcategory: String?,
    onCategory: (Category?) -> Unit, onSubcategory: (String?) -> Unit,
    onRefresh: () -> Unit, onSearch: () -> Unit
) {
    val subcategories = listings.map { it.subcategoryName }.filter { it.isNotBlank() }.distinct().sorted()
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Green, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center) {
                    Text("4♡", color = SurfaceWhite, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("4LOV", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Пространство взаимопомощи", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Обновить") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(GreenPale, RoundedCornerShape(24.dp)).padding(20.dp)) {
                Text("Добро начинается рядом", fontSize = 24.sp, lineHeight = 29.sp,
                    fontWeight = FontWeight.Bold, color = GreenDark)
                Spacer(Modifier.height(8.dp))
                Text("Отдай то, что не нужно. Найди то, что важно. Помоги тем, кому нужна поддержка.",
                    color = TextPrimary, lineHeight = 21.sp)
                Spacer(Modifier.height(14.dp))
                Button(onClick = onSearch, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text("Найти объявление")
                }
            }
        }
        item { Text("Разделы", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
        items(categories) { category ->
            CategoryRow(category, selectedCategory == category) {
                onCategory(if (selectedCategory == category) null else category)
            }
        }
        if (selectedCategory != null && subcategories.isNotEmpty()) {
            item { Text("Подразделы", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subcategories.forEach { sub ->
                        FilterChip(selectedSubcategory == sub,
                            { onSubcategory(if (selectedSubcategory == sub) null else sub) },
                            label = { Text(sub) })
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Объявления", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = onRefresh) { Text("Обновить") }
            }
        }
        when {
            loading && listings.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
            }
            error != null && listings.isEmpty() -> item { InfoPanel("Не удалось загрузить объявления", error, onRefresh) }
            listings.isEmpty() -> item { InfoPanel("Пока нет объявлений",
                "Когда на 4lov.ru появятся опубликованные объявления, они отобразятся здесь.", onRefresh) }
            else -> items(listings, key = { it.id }) { ListingCard(it) }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth()
        .background(if (selected) GreenPale else SurfaceWhite, RoundedCornerShape(18.dp))
        .clickable(onClick = onClick).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).background(if (selected) SurfaceWhite else Background, CircleShape),
            contentAlignment = Alignment.Center) { Text(category.symbol, fontSize = 22.sp) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(category.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
            Text(category.subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Text(if (selected) "✓" else "›", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SearchScreen(
    modifier: Modifier, query: String, onQuery: (String) -> Unit,
    country: String, onCountry: (String) -> Unit, category: Category?,
    onCategory: (Category?) -> Unit, listings: List<Listing>, loading: Boolean,
    error: String?, onRefresh: () -> Unit
) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Поиск объявлений", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
        item {
            OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Что ищем?") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(16.dp), singleLine = true)
        }
        item { Text("Страна", fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(country == "Россия", { onCountry("Россия") }, label = { Text("Россия") })
                FilterChip(country == "Беларусь", { onCountry("Беларусь") }, label = { Text("Беларусь") })
            }
        }
        item { Text("Раздел", fontWeight = FontWeight.SemiBold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { categoryItem ->
                    FilterChip(category == categoryItem,
                        { onCategory(if (category == categoryItem) null else categoryItem) },
                        label = { Text("${categoryItem.symbol}  ${categoryItem.title}") })
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Результаты: ${listings.size}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                TextButton(onClick = onRefresh) { Text("Обновить") }
            }
        }
        if (loading && listings.isEmpty()) item { CircularProgressIndicator(color = Green) }
        if (error != null && listings.isEmpty()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        items(listings, key = { it.id }) { ListingCard(it) }
    }
}

@Composable
private fun ListingCard(listing: Listing) {
    Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(18.dp)).padding(16.dp)) {
        if (listing.categoryPath.isNotBlank()) {
            Text(listing.categoryPath, color = GreenDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
        }
        Text(listing.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (listing.description.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(listing.description, color = TextSecondary, lineHeight = 19.sp, maxLines = 4)
        }
        if (listing.location.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text("⌖  ${listing.location}", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoPanel(title: String, message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(18.dp)).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp)); Text(message, color = TextSecondary, fontSize = 13.sp)
        TextButton(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
private fun PublishInfoScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(72.dp).background(GreenPale, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AddCircleOutline, null, tint = Green, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("Разместить объявление", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Публикация будет подключена к существующей форме 4LOV после проверки обязательных полей и загрузки фотографий.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = TextSecondary, lineHeight = 21.sp)
    }
}
