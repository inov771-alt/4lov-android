package ru.fourlov.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.fourlov.app.data.ListingRepository
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
    var searchText by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Россия") }
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var openedListing by remember { mutableStateOf<Listing?>(null) }
    var isDetailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }
    val repository = remember { ListingRepository() }
    val scope = rememberCoroutineScope()

    fun openListing(listing: Listing) {
        openedListing = listing
        isDetailLoading = true
        detailError = null
        scope.launch {
            repository.loadListing(listing.id)
                .onSuccess { openedListing = it }
                .onFailure { detailError = it.message ?: "Не удалось загрузить объявление" }
            isDetailLoading = false
        }
    }

    fun refreshListings() {
        scope.launch {
            isLoading = true
            loadError = null
            repository.loadListings()
                .onSuccess { listings = it }
                .onFailure { loadError = it.message ?: "Не удалось загрузить объявления" }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refreshListings() }

    val filteredListings = listings.filter { listing ->
        val countryMatches = listing.countryCode.equals(if (country == "Россия") "RU" else "BY", true)
        val categoryMatches = selectedCategory == null || listing.categorySlug == selectedCategory?.slug
        val queryMatches = searchText.isBlank() ||
            listing.title.contains(searchText, true) ||
            listing.description.contains(searchText, true) ||
            listing.city.contains(searchText, true)
        countryMatches && categoryMatches && queryMatches
    }

    if (openedListing != null) {
        ListingDialog(
            listing = openedListing!!,
            isLoading = isDetailLoading,
            error = detailError,
            onDismiss = { openedListing = null }
        )
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                contentColor = GreenDark,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = tab == AppTab.HOME,
                    onClick = { tab = AppTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
                    label = { Text("Главная", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = tab == AppTab.SEARCH,
                    onClick = { tab = AppTab.SEARCH },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                    label = { Text("Поиск", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = tab == AppTab.PUBLISH,
                    onClick = { tab = AppTab.PUBLISH },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Разместить") },
                    label = { Text("Разместить", fontSize = 11.sp) }
                )
            }
        }
    ) { padding ->
        when (tab) {
            AppTab.HOME -> HomeScreen(
                modifier = Modifier.padding(padding),
                selectedCategory = selectedCategory,
                onCategory = {
                    selectedCategory = if (selectedCategory == it) null else it
                    tab = AppTab.SEARCH
                },
                onSearch = { tab = AppTab.SEARCH },
                country = "Россия и Беларусь",
                listings = listings,
                isLoading = isLoading,
                error = loadError,
                onRetry = ::refreshListings,
                onListing = ::openListing
            )
            AppTab.SEARCH -> SearchScreen(
                modifier = Modifier.padding(padding),
                query = searchText,
                onQuery = { searchText = it },
                country = country,
                onCountry = { country = it },
                category = selectedCategory,
                onCategory = { selectedCategory = it },
                listings = filteredListings,
                isLoading = isLoading,
                error = loadError,
                onRetry = ::refreshListings,
                onListing = ::openListing
            )
            AppTab.PUBLISH -> PublishInfoScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    selectedCategory: Category?,
    onCategory: (Category) -> Unit,
    onSearch: () -> Unit,
    country: String,
    listings: List<Listing>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onListing: (Listing) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Green),
                    contentAlignment = Alignment.Center
                ) {
                    Text("4♡", color = SurfaceWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("4LOV", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                    Text("Добро начинается рядом", color = TextSecondary, fontSize = 11.sp)
                }
                Row(
                    Modifier.clip(CircleShape).background(SurfaceWhite).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Green, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(country, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceWhite)
                    .clickable(onClick = onSearch).padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = TextSecondary)
                Spacer(Modifier.width(10.dp))
                Text("Что ищем?", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowForward, null, tint = Green, modifier = Modifier.size(18.dp))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Направления", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Каждый может помочь", fontSize = 12.sp, color = TextSecondary)
                }
                Text("5 категорий", color = GreenDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        items(categories.chunked(2)) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { category ->
                    CategoryTile(
                        category = category,
                        selected = selectedCategory == category,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategory(category) }
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Свежие объявления", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Реальные предложения сообщества", fontSize = 12.sp, color = TextSecondary)
                }
                Icon(Icons.Default.FavoriteBorder, null, tint = Green, modifier = Modifier.size(21.dp))
            }
        }
        if (isLoading && listings.isEmpty()) {
            item { LoadingState() }
        } else if (error != null && listings.isEmpty()) {
            item { ErrorState(error, onRetry) }
        } else if (listings.isEmpty()) {
            item { EmptyState("Пока нет объявлений", "Когда на сайте появятся опубликованные объявления, они отобразятся здесь.") }
        } else {
            items(listings.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { listing ->
                        ListingCard(listing, Modifier.weight(1f), onListing)
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(category: Category, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(if (selected) GreenPale else SurfaceWhite)
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) SurfaceWhite else Background), contentAlignment = Alignment.Center) {
                Text(category.symbol, fontSize = 21.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowForward, null, tint = Green, modifier = Modifier.size(16.dp))
        }
        Text(category.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(category.subtitle, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp, minLines = 2)
    }
}

@Composable
private fun ListingCard(listing: Listing, modifier: Modifier = Modifier, onClick: (Listing) -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceWhite).clickable { onClick(listing) }
    ) {
        val photo = listing.photos.firstOrNull()?.let {
            if (it.startsWith("http")) it else "https://4lov.ru$it"
        }
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = listing.title,
                modifier = Modifier.fillMaxWidth().height(116.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxWidth().height(116.dp).background(GreenPale), contentAlignment = Alignment.Center) {
                Text("4♡", color = Green, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(listing.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(listing.categoryName, color = GreenDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Text(listing.city, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SearchScreen(
    modifier: Modifier,
    query: String,
    onQuery: (String) -> Unit,
    country: String,
    onCountry: (String) -> Unit,
    category: Category?,
    onCategory: (Category?) -> Unit,
    listings: List<Listing>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onListing: (Listing) -> Unit
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Text("Поиск", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("Найди нужное или предложи помощь", color = TextSecondary, fontSize = 13.sp)
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Название, описание или город") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(15.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green, unfocusedBorderColor = BorderSoft,
                    focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite
                )
            )
        }
        item { Text("Страна", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = country == "Россия", onClick = { onCountry("Россия") }, label = { Text("Россия") })
                FilterChip(selected = country == "Беларусь", onClick = { onCountry("Беларусь") }, label = { Text("Беларусь") })
            }
        }
        item { Text("Категория", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { onCategory(null) }, label = { Text("Все") })
            }
        }
        items(categories) { item ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (category == item) GreenPale else SurfaceWhite)
                    .clickable { onCategory(if (category == item) null else item) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.symbol, fontSize = 20.sp)
                Spacer(Modifier.width(11.dp))
                Text(item.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (category == item) Text("✓", color = Green, fontWeight = FontWeight.Bold)
            }
        }
        item { Text("Найдено: ${listings.size}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
        if (isLoading && listings.isEmpty()) item { LoadingState() }
        else if (error != null && listings.isEmpty()) item { ErrorState(error, onRetry) }
        else if (listings.isEmpty()) item { EmptyState("Ничего не найдено", "Попробуй изменить запрос или выбрать другую категорию.") }
        else items(listings) { ListingCard(it, Modifier.fillMaxWidth(), onListing) }
    }
}

@Composable
private fun ListingDialog(
    listing: Listing,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(listing.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val photo = listing.photos.firstOrNull()?.let { if (it.startsWith("http")) it else "https://4lov.ru$it" }
                if (photo != null) AsyncImage(photo, listing.title, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                Text(listing.description, color = TextPrimary)
                Text("${listing.categoryName} · ${listing.subcategoryName}", color = GreenDark, fontSize = 12.sp)
                Text(listing.city, color = TextSecondary, fontSize = 12.sp)
                listing.address?.let { Text(it, color = TextSecondary, fontSize = 12.sp) }
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Green, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Загружаем контакты…", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                listing.contactName?.let {
                    Text("Контакт: $it", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                listing.contactMethod?.let {
                    val method = when (it.lowercase()) {
                        "phone", "телефон" -> "Телефон"
                        "email", "почта" -> "Электронная почта"
                        "telegram" -> "Telegram"
                        else -> it
                    }
                    Text("Способ связи: $method", color = TextSecondary, fontSize = 12.sp)
                }
                listing.contactValue?.let { value ->
                    val context = LocalContext.current
                    val method = listing.contactMethod.orEmpty().lowercase()
                    val target = when (method) {
                        "phone", "телефон" -> "tel:${value.filter { it.isDigit() || it == '+' }}"
                        "email", "почта" -> "mailto:$value"
                        "telegram" -> if (value.startsWith("http")) value else "https://t.me/${value.removePrefix("@")}"
                        "max" -> if (value.startsWith("http")) value else "https://max.ru/$value"
                        else -> null
                    }
                    if (target != null) {
                        Button(
                            onClick = {
                                val action = if (method == "phone" || method == "телефон") Intent.ACTION_DIAL else Intent.ACTION_VIEW
                                runCatching { context.startActivity(Intent(action, Uri.parse(target))) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(when (method) {
                                "phone", "телефон" -> "Позвонить"
                                "email", "почта" -> "Написать на почту"
                                "telegram" -> "Открыть Telegram"
                                "max" -> "Открыть MAX"
                                else -> value
                            })
                        }
                    } else {
                        Text(value, color = GreenDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
        containerColor = SurfaceWhite
    )
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Green, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SurfaceWhite).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Не удалось загрузить объявления", color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(message, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) { Text("Повторить", color = GreenDark) }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SurfaceWhite).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.FavoriteBorder, null, tint = Green, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(message, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PublishInfoScreen(modifier: Modifier) {
    val context = LocalContext.current
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(68.dp).clip(CircleShape).background(GreenPale), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = Green, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Новое объявление", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Заполни форму на сайте 4LOV. Объявление отправится на обычную проверку перед публикацией.",
            textAlign = TextAlign.Center, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://4lov.ru/create.php"))
                runCatching { context.startActivity(intent) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Green),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Открыть форму публикации")
        }
    }
}
