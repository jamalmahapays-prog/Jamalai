package com.example.data.search

import com.example.data.model.WebSearchSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebSearchClient {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun searchLiveSources(query: String, preferArabic: Boolean): List<WebSearchSource> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<WebSearchSource>()
            val hasArabicChars = query.any { it in '\u0600'..'\u06FF' }
            val wikiLang = if (hasArabicChars || preferArabic) "ar" else "en"
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")

            // 1. Live Wikipedia Search API
            runCatching {
                val wikiUrl =
                    "https://$wikiLang.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1&srlimit=4"
                val request = Request.Builder()
                    .url(wikiUrl)
                    .header("User-Agent", "MyAIProAndroid/1.0")
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        val searchArr = json.optJSONObject("query")?.optJSONArray("search")
                        if (searchArr != null) {
                            for (i in 0 until searchArr.length()) {
                                val item = searchArr.getJSONObject(i)
                                val title = item.optString("title")
                                val rawSnippet = item.optString("snippet")
                                    .replace(Regex("<[^>]*>"), "")
                                    .replace("&quot;", "\"")
                                    .replace("&amp;", "&")
                                val pageUrl =
                                    "https://$wikiLang.wikipedia.org/wiki/${URLEncoder.encode(title.replace(" ", "_"), "UTF-8")}"
                                if (title.isNotBlank()) {
                                    results.add(
                                        WebSearchSource(
                                            title = title,
                                            snippet = rawSnippet,
                                            url = pageUrl
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Live DuckDuckGo Instant Answer API
            runCatching {
                val ddgUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
                val request = Request.Builder()
                    .url(ddgUrl)
                    .header("User-Agent", "MyAIProAndroid/1.0")
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        val abstractText = json.optString("AbstractText")
                        val abstractUrl = json.optString("AbstractURL")
                        val heading = json.optString("Heading")
                        if (abstractText.isNotBlank() && abstractUrl.isNotBlank()) {
                            results.add(
                                0,
                                WebSearchSource(
                                    title = heading.ifBlank { query },
                                    snippet = abstractText,
                                    url = abstractUrl
                                )
                            )
                        }
                    }
                }
            }

            results.distinctBy { it.url }.take(6)
        }
}
