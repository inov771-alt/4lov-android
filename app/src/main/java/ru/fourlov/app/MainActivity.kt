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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fourlov.app.model.Category
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

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                contentColor = GreenDark,
                tonalElevation = 8.dp,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                NavigationBarItem(
                    selected = tab == AppTab.HOME,
                    onClick = { tab = AppTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
                    label = { Text("Главная", fontSize = 11.sp) },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = tab == AppTab.SEARCH,
                    onClick = { tab = AppTab.SEARCH },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                    label = { Text("Поиск", fontSize = 11.sp) },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = tab == AppTab.PUBLISH,
                    onClick = { tab = AppTab.PUBLISH },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Разместить") },
                    label = { Text("Разместить", fontSize = 11.sp) },
                    alwaysShowLabel = true
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
                country = country
            )
            AppTab.SEARCH -> SearchScreen(
                modifier = Modifier.padding(padding),
                query = searchText,
                onQuery = { searchText = it },
                country = country,
                onCountry = { country = it },
                category = selectedCategory,
                onCategory = { selectedCategory = it }
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
    country: String
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Green),
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
                    modifier = Modifier.clip(CircleShape).background(SurfaceWhite).padding(horizontal = 10.dp, vertical = 8.dp),
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
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceWhite)
                    .clickable(onClick = onSearch)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
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
                    Text("Хорошие вещи находят новых хозяев", fontSize = 12.sp, color = TextSecondary)
                }
                Icon(Icons.Default.FavoriteBorder, null, tint = Green, modifier = Modifier.size(21.dp))
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceWhite)
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(GreenPale),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FavoriteBorder, null, tint = Green, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Пока здесь тихо", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Как только подключим получение объявлений с 4lov.ru, они появятся здесь.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: Category,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) GreenPale else SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (selected) SurfaceWhite else Background),
                contentAlignment = Alignment.Center
            ) {
                Text(category.symbol, fontSize = 21.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowForward, null, tint = Green, modifier = Modifier.size(16.dp))
        }
        Text(
            category.title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            category.subtitle,
            color = TextSecondary,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            minLines = 2
        )
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
    onCategory: (Category?) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Text("Поиск", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("Найди нужное или предложи помощь", color = TextSecondary, fontSize = 13.sp)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Что ищем?") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(15.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green,
                    unfocusedBorderColor = BorderSoft,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )
        }
        item { Text("Страна", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = country == "Россия",
                    onClick = { onCountry("Россия") },
                    label = { Text("Россия") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPale, selectedLabelColor = GreenDark)
                )
                FilterChip(
                    selected = country == "Беларусь",
                    onClick = { onCountry("Беларусь") },
                    label = { Text("Беларусь") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPale, selectedLabelColor = GreenDark)
                )
            }
        }
        item { Text("Категория", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp) }
        items(categories) { item ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (category == item) GreenPale else SurfaceWhite)
                    .clickable { onCategory(if (category == item) null else item) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.symbol, fontSize = 20.sp)
                Spacer(Modifier.width(11.dp))
                Text(item.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (category == item) Text("✓", color = Green, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Text(
                "Результаты появятся после подключения реального API сайта.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun PublishInfoScreen(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(68.dp).clip(CircleShape).background(GreenPale),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = Green, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Новое объявление", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Форму публикации подключим к существующей логике 4LOV. Пока не будем показывать форму, которая не может отправить объявление.",
            textAlign = TextAlign.Center,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}
