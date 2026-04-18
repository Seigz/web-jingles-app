package com.seigz.webjingles.data.repository

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest

class NewPipeDownloaderImpl(client: OkHttpClient) : Downloader() {

    companion object {
        private const val TAG = "NewPipeDownloader"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; rv:131.0) Gecko/20100101 Firefox/131.0"
    }

    // Cookie jar that persists cookies across requests (handles consent flow automatically)
    private val cookieJar = object : CookieJar {
        private val store = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store.getOrPut(url.host) { mutableListOf() }.apply {
                cookies.forEach { newCookie ->
                    removeAll { it.name == newCookie.name }
                    add(newCookie)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return store[url.host] ?: emptyList()
        }
    }

    private val httpClient: OkHttpClient

    init {
        // Pre-seed YouTube consent cookies
        val ytUrl = "https://www.youtube.com/".toHttpUrlOrNull()!!
        cookieJar.saveFromResponse(ytUrl, listOf(
            Cookie.Builder()
                .domain("youtube.com")
                .path("/")
                .name("SOCS")
                .value("CAISNQgDEitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjMwODI5LjA3X3AxGgJlbiACGgYIgJnBpwY")
                .secure()
                .build(),
            Cookie.Builder()
                .domain("youtube.com")
                .path("/")
                .name("CONSENT")
                .value("PENDING+999")
                .secure()
                .build()
        ))

        httpClient = client.newBuilder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    override fun execute(request: NewPipeRequest): Response {
        val url = request.url()
        val requestBuilder = Request.Builder().url(url)

        // Always set a browser User-Agent and language
        requestBuilder.header("User-Agent", USER_AGENT)
        requestBuilder.header("Accept-Language", "en-US,en;q=0.5")
        requestBuilder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

        // Add headers from the request (may override defaults above)
        request.headers()?.forEach { (name, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(name, value)
            }
        }

        // Set HTTP method
        val dataToSend = request.dataToSend()
        when (request.httpMethod()) {
            "GET" -> requestBuilder.get()
            "HEAD" -> requestBuilder.head()
            "POST" -> {
                val body = dataToSend?.toRequestBody()
                    ?: ByteArray(0).toRequestBody(null)
                requestBuilder.post(body)
            }
            else -> requestBuilder.method(
                request.httpMethod(),
                dataToSend?.toRequestBody()
            )
        }

        val okhttpResponse = httpClient.newCall(requestBuilder.build()).execute()

        val responseBody = okhttpResponse.body?.string()
        val responseHeaders = okhttpResponse.headers.toMultimap()

        Log.d(TAG, "${request.httpMethod()} ${url.take(80)} -> ${okhttpResponse.code}")

        return Response(
            okhttpResponse.code,
            okhttpResponse.message,
            responseHeaders,
            responseBody,
            okhttpResponse.request.url.toString()
        )
    }
}
