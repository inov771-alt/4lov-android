package ru.fourlov.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

private val Green = UiColor(0xFF278B55)
private val Dark = UiColor(0xFF244230)
private val Background = UiColor(0xFFF7FAF5)
private val Grey = UiColor(0xFF718575)

private data class Listing(
    val id: String, val title: String, val city: String, val date: String,
    val category: String, val image: String, val url: String, val searchText: String
)
private data class Category(val slug: String, val label: String)
private enum class Screen { HOME, DETAIL, PUBLISH }

class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private lateinit var photoPicker: ActivityResultLauncher<Intent>
    private var photoCallback: ValueCallback<Array<Uri>>? = null
    private var publishData: ((List<Listing>, List<Category>) -> Unit)? = null
    private var publishError: (() -> Unit)? = null
    private val homeUrl = "https://4lov.ru/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = photoCallback
            photoCallback = null
            val data = result.data
            val uris = if (result.resultCode == RESULT_OK) {
                val clip = data?.clipData
                when {
                    clip != null -> Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
                    data?.data != null -> arrayOf(data.data!!)
                    else -> null
                }
            } else null
            callback?.onReceiveValue(uris)
        }
        window.statusBarColor = Color.rgb(247, 250, 245)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        web = WebView(this).apply {
            setBackgroundColor(Color.rgb(247, 250, 245))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url != null && url.substringBefore("#").trimEnd('/') == "https://4lov.ru") {
                        extractListings()
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    photoCallback?.onReceiveValue(null)
                    photoCallback = filePathCallback
                    val chooser = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    return try {
                        photoPicker.launch(Intent.createChooser(chooser, "Выберите фотографии"))
                        true
                    } catch (_: Exception) {
                        photoCallback = null
                        false
                    }
                }
            }
        }

        setContent {
            var screen by remember { mutableStateOf(Screen.HOME) }
            var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
            var categories by remember { mutableStateOf(listOf(Category("all", "Все"))) }
            var selected by remember { mutableStateOf("all") }
            var query by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(true) }
            var failed by remember { mutableStateOf(false) }

            SideEffect {
                publishData = { newListings, newCategories ->
                    listings = newListings
                    categories = newCategories
                    loading = false
                    failed = false
                }
                publishError = {
                    loading = false
                    failed = true
                }
            }

            BackHandler(screen != Screen.HOME) {
                screen = Screen.HOME
                web.loadUrl(homeUrl)
            }

            MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Background, surface = UiColor.White)) {
                Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
                    Row(
                        Modifier.fillMaxWidth().background(UiColor.White).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(Modifier.size(44.dp), color = Green, shape = RoundedCornerShape(13.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("4♡", color = UiColor.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            when (screen) { Screen.HOME -> "ForLove"; Screen.DETAIL -> "Объявление"; Screen.PUBLISH -> "Разместить" },
                            Modifier.weight(1f).padding(start = 12.dp),
                            color = Dark, fontSize = 19.sp, fontWeight = FontWeight.Bold
                        )
                        if (screen != Screen.HOME) {
                            Text("Назад", Modifier.clickable {
                                screen = Screen.HOME
                                web.loadUrl(homeUrl)
                            }.padding(8.dp), color = Green, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            factory = { web },
                            modifier = Modifier.fillMaxSize(),
                            update = { it.visibility = if (screen == Screen.HOME) View.INVISIBLE else View.VISIBLE }
                        )
                        if (screen == Screen.HOME) {
                            Column(Modifier.fillMaxSize().background(Background)) {
                                Text(
                                    "Хорошие вещи находят новых хозяев",
                                    Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 4.dp),
                                    color = Dark, fontSize = 21.sp, fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Объявления людей, готовых делиться и помогать",
                                    Modifier.padding(horizontal = 18.dp),
                                    color = Grey, fontSize = 13.sp
                                )
                                OutlinedTextField(
                                    value = query, onValueChange = { query = it },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                                    singleLine = true, shape = RoundedCornerShape(15.dp),
                                    placeholder = { Text("Что ищем?") },
                                    leadingIcon = { Text("⌕", color = Green, fontSize = 22.sp) }
                                )
                                if (categories.size > 1) {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        items(categories, key = { it.slug }) { item ->
                                            Surface(
                                                color = if (selected == item.slug) Green else UiColor.White,
                                                shape = RoundedCornerShape(30.dp),
                                                modifier = Modifier.clickable { selected = item.slug }
                                            ) {
                                                Text(
                                                    item.label, Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                                                    color = if (selected == item.slug) UiColor.White else Dark,
                                                    fontSize = 13.sp, fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                                val shown = listings.filter {
                                    (selected == "all" || it.category == selected) &&
                                    (query.isBlank() || it.searchText.contains(query, ignoreCase = true))
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Свежие объявления", color = Dark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(shown.size.toString(), color = Grey, fontSize = 12.sp)
                                }
                                when {
                                    loading && listings.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = Green)
                                    }
                                    failed && listings.isEmpty() -> EmptyState("Не удалось загрузить объявления", "Проверьте подключение и попробуйте ещё раз") {
                                        loading = true; failed = false; web.reload()
                                    }
                                    shown.isEmpty() -> EmptyState("Пока ничего не найдено", "Попробуйте изменить запрос или раздел", null)
                                    else -> LazyColumn(
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(shown, key = { it.id }) { item ->
                                            ListingCard(item, categories.firstOrNull { category -> category.slug == item.category }?.label ?: item.categoryLabel()) {
                                                screen = Screen.DETAIL
                                                web.loadUrl(item.url)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().background(UiColor.White).navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        NavItem("⌂", "Главная", screen == Screen.HOME) {
                            screen = Screen.HOME
                            if (web.url?.substringBefore("#")?.trimEnd('/') != homeUrl.trimEnd('/')) web.loadUrl(homeUrl)
                        }
                        NavItem("⌕", "Поиск", false) {
                            screen = Screen.HOME
                            selected = "all"
                            query = ""
                        }
                        NavItem("+", "Разместить", screen == Screen.PUBLISH) {
                            screen = Screen.PUBLISH
                            web.loadUrl("https://4lov.ru/create.php")
                        }
                    }
                }
            }
        }
        web.loadUrl(homeUrl)
    }

    private fun extractListings() {
        val js = """
            (function() {
              const cards = Array.from(document.querySelectorAll('.listing-card')).map(card => {
                const href = card.getAttribute('data-url') || '';
                const title = card.querySelector('.listing-title')?.textContent?.trim() || '';
                return {
                  id: (href.match(/[?&]id=([0-9]+)/) || [,''])[1],
                  title: title,
                  city: card.querySelector('.listing-city')?.textContent?.trim() || '',
                  date: card.querySelector('.listing-meta span:last-child')?.textContent?.trim() || '',
                  category: card.getAttribute('data-category') || 'other',
                  image: card.querySelector('.gallery-main-image')?.src || '',
                  href: href,
                  searchText: card.innerText || ''
                };
              }).filter(x => x.id && x.title && x.href);
              const categories = Array.from(document.querySelectorAll('.filter-button')).map(b => ({
                slug: b.getAttribute('data-filter') || 'all',
                label: (b.textContent || '').trim()
              })).filter(x => x.label);
              return JSON.stringify({cards: cards, categories: categories});
            })();
        """.trimIndent()
        web.evaluateJavascript(js) { raw ->
            try {
                val jsonText = JSONTokener(raw ?: "null").nextValue() as? String ?: ""
                val root = JSONObject(jsonText)
                val cards = root.optJSONArray("cards") ?: JSONArray()
                val filters = root.optJSONArray("categories") ?: JSONArray()
                val data = mutableListOf<Listing>()
                for (i in 0 until cards.length()) {
                    val item = cards.optJSONObject(i) ?: continue
                    val href = item.optString("href")
                    data += Listing(
                        item.optString("id"), item.optString("title"), item.optString("city"),
                        item.optString("date"), item.optString("category"), item.optString("image"),
                        if (href.startsWith("http")) href else "https://4lov.ru" + href,
                        item.optString("searchText")
                    )
                }
                val categoryData = mutableListOf(Category("all", "Все"))
                for (i in 0 until filters.length()) {
                    val item = filters.optJSONObject(i) ?: continue
                    val slug = item.optString("slug")
                    val label = item.optString("label")
                    if (slug.isNotBlank() && slug != "all" && label.isNotBlank()) categoryData += Category(slug, label)
                }
                runOnUiThread { publishData?.invoke(data, categoryData) }
            } catch (_: Exception) {
                runOnUiThread { publishError?.invoke() }
            }
        }
    }

    override fun onDestroy() {
        photoCallback?.onReceiveValue(null)
        photoCallback = null
        if (::web.isInitialized) { web.stopLoading(); web.destroy() }
        super.onDestroy()
    }
}

@Composable
private fun ListingCard(item: Listing, categoryLabel: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = UiColor.White, shape = RoundedCornerShape(18.dp), shadowElevation = 2.dp
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.image.isNotBlank()) {
                AsyncImage(item.image, contentDescription = item.title, modifier = Modifier.size(104.dp).clip(RoundedCornerShape(13.dp)))
            } else {
                Box(Modifier.size(104.dp).clip(RoundedCornerShape(13.dp)).background(UiColor(0xFFEAF3E9)), contentAlignment = Alignment.Center) {
                    Text("♡", color = Green, fontSize = 34.sp)
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(categoryLabel, color = Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(item.title, color = Dark, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                Text(item.city, color = Grey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.date, color = Grey, fontSize = 11.sp)
            }
            Text("›", color = Green, fontSize = 25.sp)
        }
    }
}

private fun Listing.categoryLabel(): String = when (category) {
    "give" -> "Отдам"
    "receive" -> "Приму в дар"
    "need" -> "Нужна помощь"
    "help" -> "Могу помочь"
    "animals" -> "Животные"
    else -> "Объявление"
}

@Composable
private fun EmptyState(title: String, subtitle: String, retry: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("♡", color = Green, fontSize = 42.sp)
        Text(title, color = Dark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Grey, fontSize = 13.sp)
        if (retry != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = retry) { Text("Повторить") }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, color = if (active) Green else Grey, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (active) Green else Grey, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}
