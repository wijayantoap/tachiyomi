package eu.kanade.tachiyomi.data.track.kitsu

import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.int
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.string
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import okhttp3.FormBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rx.Observable

class KitsuApi(private val client: OkHttpClient, interceptor: KitsuInterceptor) {

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    private val rest = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(authClient)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build()
        .create(Rest::class.java)

    private val searchRest = Retrofit.Builder()
        .baseUrl(algoliaKeyUrl)
        .client(authClient)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build()
        .create(SearchKeyRest::class.java)

    private val algoliaRest = Retrofit.Builder()
        .baseUrl(algoliaUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build()
        .create(AgoliaSearchRest::class.java)

    fun addLibManga(track: Track, userId: String): Observable<Track> {
        return Observable.defer {
            // @formatter:off
            val data = jsonObject(
                "type" to "libraryEntries",
                "attributes" to jsonObject(
                    "status" to track.toKitsuStatus(),
                    "progress" to track.last_chapter_read
                ),
                "relationships" to jsonObject(
                    "user" to jsonObject(
                        "data" to jsonObject(
                            "id" to userId,
                            "type" to "users"
                        )
                    ),
                    "media" to jsonObject(
                        "data" to jsonObject(
                            "id" to track.media_id,
                            "type" to "manga"
                        )
                    )
                )
            )

            rest.addLibManga(jsonObject("data" to data))
                .map { json ->
                    track.media_id = json["data"]["id"].int
                    track
                }
        }
    }

    fun updateLibManga(track: Track): Observable<Track> {
        return Observable.defer {
            // @formatter:off
            val data = jsonObject(
                "type" to "libraryEntries",
                "id" to track.media_id,
                "attributes" to jsonObject(
                    "status" to track.toKitsuStatus(),
                    "progress" to track.last_chapter_read,
                    "ratingTwenty" to track.toKitsuScore()
                )
            )
            // @formatter:on

            rest.updateLibManga(track.media_id, jsonObject("data" to data))
                .map { track }
        }
    }

    fun search(query: String): Observable<List<TrackSearch>> {
        return searchRest
            .getKey().map { json ->
                json["media"].asJsonObject["key"].string
            }.flatMap { key ->
                algoliaSearch(key, query)
            }
    }

    private fun algoliaSearch(key: String, query: String): Observable<List<TrackSearch>> {
        val jsonObject = jsonObject("params" to "query=$query$algoliaFilter")
        return algoliaRest
            .getSearchQuery(algoliaAppId, key, jsonObject)
            .map { json ->
                val data = json["hits"].array
                data.map { KitsuSearchManga(it.obj) }
                    .filter { it.subType != "novel" }
                    .map { it.toTrack() }
            }
    }

    fun findLibManga(track: Track, userId: String): Observable<Track?> {
        return rest.findLibManga(track.media_id, userId)
            .map { json ->
                val data = json["data"].array
                if (data.size() > 0) {
                    val manga = json["included"].array[0].obj
                    KitsuLibManga(data[0].obj, manga).toTrack()
                } else {
                    null
                }
            }
    }

    fun getLibManga(track: Track): Observable<Track> {
        return rest.getLibManga(track.media_id)
            .map { json ->
                val data = json["data"].array
                if (data.size() > 0) {
                    val manga = json["included"].array[0].obj
                    KitsuLibManga(data[0].obj, manga).toTrack()
                } else {
                    throw Exception("Could not find manga")
                }
            }
    }

    fun login(username: String, password: String): Observable<OAuth> {
        return Retrofit.Builder()
            .baseUrl(loginUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build()
            .create(LoginRest::class.java)
            .requestAccessToken(username, password)
    }

    fun getCurrentUser(): Observable<String> {
        return rest.getCurrentUser().map { it["data"].array[0]["id"].string }
    }

    private interface Rest {

        @Headers("Content-Type: application/vnd.api+json")
        @POST("library-entries")
        fun addLibManga(
            @Body data: JsonObject
        ): Observable<JsonObject>

        @Headers("Content-Type: application/vnd.api+json")
        @PATCH("library-entries/{id}")
        fun updateLibManga(
            @Path("id") remoteId: Int,
            @Body data: JsonObject
        ): Observable<JsonObject>

        @GET("library-entries")
        fun findLibManga(
            @Query("filter[manga_id]", encoded = true) remoteId: Int,
            @Query("filter[user_id]", encoded = true) userId: String,
            @Query("include") includes: String = "manga"
        ): Observable<JsonObject>

        @GET("library-entries")
        fun getLibManga(
            @Query("filter[id]", encoded = true) remoteId: Int,
            @Query("include") includes: String = "manga"
        ): Observable<JsonObject>

        @GET("users")
        fun getCurrentUser(
            @Query("filter[self]", encoded = true) self: Boolean = true
        ): Observable<JsonObject>
    }

    private interface SearchKeyRest {
        @GET("media/")
        fun getKey(): Observable<JsonObject>
    }

    private interface AgoliaSearchRest {
        @POST("query/")
        fun getSearchQuery(@Header("X-Algolia-Application-Id") appid: String, @Header("X-Algolia-API-Key") key: String, @Body json: JsonObject): Observable<JsonObject>
    }

    private interface LoginRest {

        @FormUrlEncoded
        @POST("oauth/token")
        fun requestAccessToken(
            @Field("username") username: String,
            @Field("password") password: String,
            @Field("grant_type") grantType: String = "password",
            @Field("client_id") client_id: String = clientId,
            @Field("client_secret") client_secret: String = clientSecret
        ): Observable<OAuth>
    }

    companion object {
        private const val clientId = "dd031b32d2f56c990b1425efe6c42ad847e7fe3ab46bf1299f05ecd856bdb7dd"
        private const val clientSecret = "54d7307928f63414defd96399fc31ba847961ceaecef3a5fd93144e960c0e151"
        private const val baseUrl = "https://kitsu.io/api/edge/"
        private const val loginUrl = "https://kitsu.io/api/"
        private const val baseMangaUrl = "https://kitsu.io/manga/"
        private const val algoliaKeyUrl = "https://kitsu.io/api/edge/algolia-keys/"
        private const val algoliaUrl = "https://AWQO5J657S-dsn.algolia.net/1/indexes/production_media/"
        private const val algoliaAppId = "AWQO5J657S"
        private const val algoliaFilter = "&facetFilters=%5B%22kind%3Amanga%22%5D&attributesToRetrieve=%5B%22synopsis%22%2C%22canonicalTitle%22%2C%22chapterCount%22%2C%22posterImage%22%2C%22startDate%22%2C%22subtype%22%2C%22endDate%22%2C%20%22id%22%5D"

        fun mangaUrl(remoteId: Int): String {
            return baseMangaUrl + remoteId
        }

        fun refreshTokenRequest(token: String) = POST(
            "${loginUrl}oauth/token",
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", token)
                .build()
        )
    }
}
