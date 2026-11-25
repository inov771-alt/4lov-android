package ru.fourlov.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
private sealed interface HomeRoute {
    data object Root : HomeRoute
    data class CategoryLevel(val category: Category) : HomeRoute
    data class ListingsLevel(val category: Category?, val subcategory: String? = null) : HomeRoute
}

@Composable
private fun FourLovApp() {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    val stack = remember { mutableStateListOf<HomeRoute>(HomeRoute.Root) }
    var query by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Россия") }
    var searchCategory by remember { mutableStateOf<Category?>(null) }
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val repository = remember { ListingsRepository() }
    val scope = rememberCoroutineScope()

    fun refresh(targetPage: Int = 1) {
        scope.launch {
            loading = true
            error = null
            repository.loadListings(1, 50).onSuccess {
                listings = it.items
                totalCount = maxOf(it.totalCount, it.items.size)
                page = targetPage.coerceIn(1, pageCount(totalCount))
            }.onFailure { error = it.message ?: "Не удалось загрузить объявления" }
            loading = false
        }
    }
    fun back() {
        page = 1
        if (tab != AppTab.HOME) tab = AppTab.HOME
        else if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
    LaunchedEffect(Unit) { refresh() }
    BackHandler(enabled = tab != AppTab.HOME || stack.size > 1) { back() }

    val route = stack.last()
    val searchResults = filterListings(listings, query, country, searchCategory, null)

    Scaffold(
        containerColor = Background,
        topBar = {
            if (tab == AppTab.HOME && route != HomeRoute.Root) {
                Column(Modifier.fillMaxWidth().statusBarsPadding().background(SurfaceWhite)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { back() }) { Icon(Icons.Default.ArrowBack, "Назад") }
                        val title = when (route) {
                            is HomeRoute.CategoryLevel -> route.category.title
                            is HomeRoute.ListingsLevel -> route.subcategory ?: route.category?.title ?: "Все объявления"
                            HomeRoute.Root -> "ForLove"
                        }
                        Text(title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Обновить") }
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Главная", Modifier.clickable { stack.clear(); stack.add(HomeRoute.Root) }, color = GreenDark, fontSize = 12.sp)
                        when (route) {
                            is HomeRoute.CategoryLevel -> {
                                Text("›", color = TextSecondary)
                                Text(route.category.title, color = TextSecondary, fontSize = 12.sp)
                            }
                            is HomeRoute.ListingsLevel -> {
                                Text("›", color = TextSecondary)
                                Text(route.category?.title ?: "Все объявления", Modifier.clickable {
                                    val c = route.category
                                    stack.clear(); stack.add(HomeRoute.Root)
                                    if (c != null) stack.add(HomeRoute.CategoryLevel(c))
                                }, color = GreenDark, fontSize = 12.sp)
                                if (route.subcategory != null) {
                                    Text("›", color = TextSecondary)
                                    Text(route.subcategory, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            HomeRoute.Root -> Unit
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceWhite) {
                NavigationBarItem(tab == AppTab.HOME, {
                    tab = AppTab.HOME; page = 1; stack.clear(); stack.add(HomeRoute.Root)
                }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Главная") })
                NavigationBarItem(tab == AppTab.SEARCH, { tab = AppTab.SEARCH },
                    icon = { Icon(Icons.Default.Search, null) }, label = { Text("Поиск") })
                NavigationBarItem(tab == AppTab.PUBLISH, { tab = AppTab.PUBLISH },
                    icon = { Icon(Icons.Default.AddCircleOutline, null) }, label = { Text("Разместить") })
            }
        }
    ) { padding ->
        when (tab) {
            AppTab.HOME -> when (route) {
                HomeRoute.Root -> HomeScreen(Modifier.padding(padding), listings, totalCount, loading, error,
                    page = page, onPage = { page = it },
                    onCategory = { page = 1; stack.add(HomeRoute.CategoryLevel(it)) },
                    onAll = { page = 1; stack.add(HomeRoute.ListingsLevel(null)) },
                    onSearch = { page = 1; tab = AppTab.SEARCH }, onRefresh = { refresh() })
                is HomeRoute.CategoryLevel -> CategoryScreen(Modifier.padding(padding), route.category, listings, loading, error, page, { page = it },
                    onSubcategory = { page = 1; stack.add(HomeRoute.ListingsLevel(route.category, it)) },
                    onAll = { page = 1; stack.add(HomeRoute.ListingsLevel(route.category)) }, onRefresh = { refresh() })
                is HomeRoute.ListingsLevel -> ListingsScreen(Modifier.padding(padding),
                    filterListings(listings, "", "", route.category, route.subcategory),
                    if (route.category == null && route.subcategory == null) totalCount else filterListings(listings, "", "", route.category, route.subcategory).size,
                    route.category?.title ?: "Все объявления", route.subcategory, loading, error, page,
                    { page = it }, { refresh() })
            }
            AppTab.SEARCH -> SearchScreen(Modifier.padding(padding), query, { query = it; page = 1 }, country, { country = it; page = 1 },
                searchCategory, { searchCategory = it; page = 1 }, searchResults, loading, error, page, { page = it }, { refresh() })
            AppTab.PUBLISH -> PublishScreen(Modifier.padding(padding))
        }
    }
}


private const val LISTINGS_PER_PAGE = 50

private fun pageCount(totalItems: Int): Int =
    maxOf(1, (totalItems + LISTINGS_PER_PAGE - 1) / LISTINGS_PER_PAGE)

private fun <T> pageSlice(items: List<T>, page: Int): List<T> {
    val safePage = page.coerceIn(1, pageCount(items.size))
    return items.drop((safePage - 1) * LISTINGS_PER_PAGE).take(LISTINGS_PER_PAGE)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageNavigation(currentPage: Int, totalPages: Int, onPage: (Int) -> Unit) {
    if (totalPages <= 1) return
    val safePage = currentPage.coerceIn(1, totalPages)
    val visiblePages = (1..totalPages).toList()

    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Страница $safePage из $totalPages", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PageControl("‹", Modifier.weight(1f), safePage > 1) { onPage(safePage - 1) }
            PageControl("›", Modifier.weight(1f), safePage < totalPages) { onPage(safePage + 1) }
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            visiblePages.forEach { number ->
                PageControl(number.toString(), Modifier.width(34.dp), true,
                    selected = number == safePage) { onPage(number) }
            }
        }
    }
}

@Composable
private fun PageControl(
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(38.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(10.dp),
        colors = if (selected) ButtonDefaults.buttonColors(
            containerColor = Green,
            contentColor = SurfaceWhite
        ) else ButtonDefaults.buttonColors(
            containerColor = SurfaceWhite,
            contentColor = GreenDark,
            disabledContainerColor = SurfaceWhite,
            disabledContentColor = TextSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 2.dp else 0.dp)
    ) {
        Text(label, fontSize = 13.sp, maxLines = 1)
    }
}

private fun filterListings(source: List<Listing>, query: String, country: String,
    category: Category?, subcategory: String?): List<Listing> = source.filter { item ->
    val textOk = query.isBlank() || item.title.contains(query, true) ||
        item.description.contains(query, true) || item.location.contains(query, true)
    val countryOk = country.isBlank() || item.country.isBlank() ||
        (country == "Россия" && (item.country.contains("Россия", true) || item.country.equals("RU", true))) ||
        (country == "Беларусь" && (item.country.contains("Беларусь", true) || item.country.equals("BY", true)))
    val categoryOk = category == null || if (category.slug == "together")
        item.categoryName.contains("вмест", true) || item.categoryName.equals("together", true)
    else item.categoryName.contains(category.title, true) || item.categoryName.equals(category.slug, true) ||
        item.categoryPath.contains(category.title, true)
    val subcategoryOk = subcategory == null || item.subcategoryName.equals(subcategory, true)
    textOk && countryOk && categoryOk && subcategoryOk
}

@Composable
private fun HomeScreen(modifier: Modifier, listings: List<Listing>, totalCount: Int, loading: Boolean, error: String?,
    page: Int, onPage: (Int) -> Unit, onCategory: (Category) -> Unit, onAll: () -> Unit, onSearch: () -> Unit, onRefresh: () -> Unit) {
    val totalPages = pageCount(totalCount)
    val visibleListings = listings.take(50)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Green, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                    Text("4♡", color = SurfaceWhite, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ForLove", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Пространство взаимопомощи", color = TextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Обновить") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(GreenPale, RoundedCornerShape(22.dp)).padding(19.dp)) {
                Text("Добро начинается рядом", fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                Spacer(Modifier.height(7.dp))
                Text("Отдай то, что не нужно. Найди то, что важно. Помоги тем, кому нужна поддержка.", color = TextPrimary, lineHeight = 20.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onSearch, shape = RoundedCornerShape(13.dp)) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Найти объявление")
                }
            }
        }
        item { Text("Разделы", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
        items(categories, key = { it.slug }) { category -> CategoryRow(category) { onCategory(category) } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Свежие объявления", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                TextButton(onClick = onAll) { Text("Все") }
            }
        }
        when {
            loading && listings.isEmpty() -> item { LoadingPanel() }
            error != null && listings.isEmpty() -> item { InfoPanel("Не удалось загрузить объявления", error, onRefresh) }
            listings.isEmpty() -> item { InfoPanel("Пока нет объявлений", "Опубликованные объявления 4lov.ru появятся здесь.", onRefresh) }
            else -> {
                items(visibleListings, key = { "home-${it.id}" }) { ListingCard(it) }
            }
        }
    }
}

@Composable
private fun CategoryScreen(modifier: Modifier, category: Category, listings: List<Listing>,
    loading: Boolean, error: String?, page: Int, onPage: (Int) -> Unit,
    onSubcategory: (String) -> Unit, onAll: () -> Unit, onRefresh: () -> Unit) {
    val categoryListings = filterListings(listings, "", "", category, null)
    val visibleListings = pageSlice(categoryListings, page)
    val children = listings.filter { item ->
        if (category.slug == "together") item.categoryName.contains("вмест", true) || item.categoryName.equals("together", true)
        else item.categoryName.contains(category.title, true) || item.categoryName.equals(category.slug, true) ||
            item.categoryPath.contains(category.title, true)
    }.map { it.subcategoryName.trim() }.filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(GreenPale, RoundedCornerShape(18.dp)).padding(17.dp)) {
                Text(category.symbol, fontSize = 27.sp)
                Text(category.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                Text(category.subtitle, fontSize = 13.sp, color = TextSecondary)
            }
        }
        item { Text("Подразделы", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
        when {
            loading && listings.isEmpty() -> item { LoadingPanel() }
            error != null && listings.isEmpty() -> item { InfoPanel("Не удалось загрузить подразделы", error, onRefresh) }
            children.isEmpty() -> item { InfoPanel("Подразделы пока пусты",
                "Пока нет опубликованных объявлений с подразделами в этой категории.", onRefresh) }
            else -> items(children, key = { it }) { child -> NavigationRow(child) { onSubcategory(child) } }
        }
        item {
            Button(onClick = onAll, Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                Text("Все объявления раздела")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Объявления раздела", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${categoryListings.size}", color = TextSecondary, fontSize = 12.sp)
            }
        }
        if (categoryListings.isNotEmpty()) {
            item { PageNavigation(page, pageCount(categoryListings.size), onPage) }
            items(visibleListings, key = { "category-${it.id}" }) { ListingCard(it) }
            item { PageNavigation(page, pageCount(categoryListings.size), onPage) }
        }
    }
}

@Composable
private fun ListingsScreen(modifier: Modifier, listings: List<Listing>, totalCount: Int, title: String, subcategory: String?,
    loading: Boolean, error: String?, page: Int, onPage: (Int) -> Unit, onRefresh: () -> Unit) {
    val totalPages = pageCount(totalCount)
    val visibleListings = pageSlice(listings, page)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            Column {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (subcategory != null) Text(subcategory, fontSize = 13.sp, color = TextSecondary)
                Text("Объявлений: $totalCount", fontSize = 12.sp, color = TextSecondary)
            }
        }
        when {
            loading && listings.isEmpty() -> item { LoadingPanel() }
            error != null && listings.isEmpty() -> item { InfoPanel("Не удалось загрузить объявления", error, onRefresh) }
            listings.isEmpty() -> item { InfoPanel("Пока нет объявлений", "В этом разделе пока нет опубликованных объявлений.", onRefresh) }
            else -> {
                item { PageNavigation(page, totalPages, onPage) }
                items(visibleListings, key = { "list-${it.id}" }) { ListingCard(it) }
                item { PageNavigation(page, totalPages, onPage) }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(17.dp)).clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(45.dp).background(Background, CircleShape), contentAlignment = Alignment.Center) {
            Text(category.symbol, fontSize = 22.sp)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(category.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
            Text(category.subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Text("›", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NavigationRow(title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text("›", color = Green, fontSize = 24.sp)
    }
}

@Composable
private fun SearchScreen(modifier: Modifier, query: String, onQuery: (String) -> Unit,
    country: String, onCountry: (String) -> Unit, category: Category?, onCategory: (Category?) -> Unit,
    listings: List<Listing>, loading: Boolean, error: String?, page: Int, onPage: (Int) -> Unit, onRefresh: () -> Unit) {
    val totalPages = pageCount(listings.size)
    val visibleListings = pageSlice(listings, page)
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Поиск объявлений", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
        item {
            OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Что ищем?") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(15.dp), singleLine = true)
        }
        item { Text("Страна", fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(country == "Россия", { onCountry("Россия") }, label = { Text("Россия") })
                FilterChip(country == "Беларусь", { onCountry("Беларусь") }, label = { Text("Беларусь") })
                FilterChip(country.isBlank(), { onCountry("") }, label = { Text("Все") })
            }
        }
        item { Text("Раздел", fontWeight = FontWeight.SemiBold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                categories.forEach { c ->
                    FilterChip(category == c, { onCategory(if (category == c) null else c) },
                        label = { Text("${c.symbol}  ${c.title}") })
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Результаты: ${listings.size}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                TextButton(onClick = onRefresh) { Text("Обновить") }
            }
        }
        when {
            loading && listings.isEmpty() -> item { LoadingPanel() }
            error != null && listings.isEmpty() -> item { InfoPanel("Ошибка загрузки", error, onRefresh) }
            listings.isEmpty() -> item { Text("Ничего не найдено", color = TextSecondary) }
            else -> {
                item { PageNavigation(page, totalPages, onPage) }
                items(visibleListings, key = { "search-${it.id}" }) { ListingCard(it) }
                item { PageNavigation(page, totalPages, onPage) }
            }
        }
    }
}

@Composable
private fun ListingCard(item: Listing) {
    Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(17.dp)).padding(13.dp)) {
        item.photos.firstOrNull()?.let { photo ->
            AsyncImage(photo, item.title, Modifier.fillMaxWidth().height(175.dp), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(8.dp))
        }
        if (item.categoryPath.isNotBlank()) Text(item.categoryPath, color = GreenDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (item.description.isNotBlank()) Text(item.description, color = TextSecondary, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (item.location.isNotBlank()) Text("⌖  ${item.location}", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun LoadingPanel() {
    Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green) }
}

@Composable
private fun InfoPanel(title: String, message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(17.dp)).padding(17.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(5.dp))
        Text(message, color = TextSecondary, fontSize = 13.sp)
        TextButton(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
private fun PublishScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AddCircleOutline, null, tint = Green, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(16.dp))
        Text("Разместить объявление", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Для публикации откроется существующая форма 4lov.ru.", color = TextSecondary)
    }
}
