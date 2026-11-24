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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            NavigationBar(containerColor = SurfaceWhite) {
                NavigationBarItem(
                    selected = tab == AppTab.HOME,
                    onClick = { tab = AppTab.HOME },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Главная") }
                )
                NavigationBarItem(
                    selected = tab == AppTab.SEARCH,
                    onClick = { tab = AppTab.SEARCH },
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Поиск") }
                )
                NavigationBarItem(
                    selected = tab == AppTab.PUBLISH,
                    onClick = { tab = AppTab.PUBLISH },
                    icon = { Icon(Icons.Default.AddCircleOutline, null) },
                    label = { Text("Разместить") }
                )
            }
        }
    ) { padding ->
        when (tab) {
            AppTab.HOME -> HomeScreen(
                modifier = Modifier.padding(padding),
                selectedCategory = selectedCategory,
                onCategory = { selectedCategory = if (selectedCategory == it) null else it },
                onSearch = { tab = AppTab.SEARCH }
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
    onSearch: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(Green, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("4♡", color = SurfaceWhite, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("4LOV", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Пространство взаимопомощи", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().background(GreenPale, RoundedCornerShape(24.dp)).padding(20.dp)
            ) {
                Text("Добро начинается рядом", fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                Spacer(Modifier.height(8.dp))
                Text("Отдай то, что не нужно. Найди то, что важно. Помоги тем, кому нужна поддержка.", color = TextPrimary, lineHeight = 21.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSearch, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Найти объявление")
                }
            }
        }
        item { Text("Выбери направление", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
        items(categories.size) { index ->
            val category = categories[index]
            CategoryRow(category, selectedCategory == category) { onCategory(category) }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.FavoriteBorder, null, tint = Green, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text("Объявления появятся после подключения сервера", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("Сейчас мы настраиваем связь приложения с 4lov.ru. Никаких вымышленных объявлений здесь нет.", color = TextSecondary, textAlign = TextAlign.Center, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (selected) GreenPale else SurfaceWhite, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(if (selected) SurfaceWhite else Background, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(category.symbol, fontSize = 23.sp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(category.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
            Text(category.subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Text(if (selected) "✓" else "›", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Поиск объявлений", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Что ищем?") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Text("Страна", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = country == "Россия", onClick = { onCountry("Россия") }, label = { Text("Россия") })
            FilterChip(selected = country == "Беларусь", onClick = { onCountry("Беларусь") }, label = { Text("Беларусь") })
        }
        Text("Категория", fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { item ->
                FilterChip(
                    selected = category == item,
                    onClick = { onCategory(if (category == item) null else item) },
                    label = { Text("${item.symbol}  ${item.title}") }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Фильтры готовы. Получение результатов будет включено после подключения к фактическим методам API.",
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PublishInfoScreen(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(72.dp).background(GreenPale, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AddCircleOutline, null, tint = Green, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("Разместить объявление", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Форму публикации подключим к существующей серверной логике 4LOV. До проверки реального API отправка не имитируется.",
            textAlign = TextAlign.Center,
            color = TextSecondary,
            lineHeight = 21.sp
        )
    }
}
