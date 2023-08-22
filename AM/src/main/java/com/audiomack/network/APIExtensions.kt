package com.audiomack.network

import android.net.Uri
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.model.AMArtist
import com.audiomack.model.AMComment
import com.audiomack.model.AMCommentVote
import com.audiomack.model.AMCommentsResponse
import com.audiomack.model.AMFeaturedSpot
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMVoteStatus
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CommentSort
import com.audiomack.model.Credentials
import com.audiomack.model.ErrorCodes
import com.audiomack.model.OnboardingArtist
import com.audiomack.model.PlaylistCategory
import com.audiomack.model.ReportContentReason
import com.audiomack.model.ReportContentType
import com.audiomack.model.ReportType
import com.audiomack.network.AuthInterceptor.Companion.TAG_DO_NOT_AUTHENTICATE
import com.audiomack.network.AuthInterceptor.Companion.TAG_DO_NOT_REFRESH_TOKEN_ON_401
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.utils.buildUrl
import com.audiomack.utils.getIntOrNull
import com.audiomack.utils.getJSONObjectOrNull
import com.audiomack.utils.getStringOrNull
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Observable
import io.reactivex.Single
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

fun API.removeDownload(musicId: String): Observable<Boolean> {
    return Observable.create { emitter ->
        val request = Request.Builder()
            .url(baseUrl + "user/downloads/" + musicId)
            .delete()
            .build()

        val call = client.newCall(request).apply {
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    emitter.onNext(true)
                    emitter.onComplete()
                    response.close()
                }
            })
        }

        emitter.setCancellable { call.cancel() }
    }
}

fun API.onboardingItems(): Observable<List<OnboardingArtist>> {
    return Observable.create { emitter ->
        val request = Request.Builder()
            .url(baseUrl + "onboarding-items")
            .get()
            .build()

        val call = client.newCall(request).apply {
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val jsonArray = JSONArray(response.body!!.string())
                        val results = (0 until jsonArray.length())
                            .mapNotNull { jsonArray.optJSONObject(it) }
                            .map { OnboardingArtist(it) }
                            .filter { it.isValid() }
                        emitter.onNext(results)
                        emitter.onComplete()
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    } finally {
                        response.close()
                    }
                }
            })
        }

        emitter.setCancellable { call.cancel() }
    }
}

fun API.playlistCategories(): Observable<List<PlaylistCategory>> {
    return Observable.create { emitter ->
        val request: Request = Request.Builder()
            .url(baseUrl + "playlist/categories?featured=yes&limit=999")
            .get()
            .build()
        val call: Call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body
                    val jsonObject = JSONObject(responseBody?.string() ?: "")
                    jsonObject.optJSONObject("results")?.optJSONArray("categories")?.let { array ->
                        val categories = (0 until array.length()).map { PlaylistCategory(array.optJSONObject(it)) }
                        emitter.onNext(categories)
                        emitter.onComplete()
                    } ?: emitter.tryOnError(Exception("Unsuccessful or malformed response"))
                } catch (e: Exception) {
                    emitter.tryOnError(e)
                } finally {
                    response.close()
                }
            }
        })
        emitter.setCancellable { call.cancel() }
    }
}

fun API.playlistsForCategory(categorySlug: String, page: Int, ignoreGeorestrictedMusic: Boolean): APIRequestData {
    val url = baseUrl + "playlist/categories?slug=$categorySlug&page=${page + 1}"
    val observable: Observable<APIResponseData> = Observable.create { emitter ->
        val request: Request = Request.Builder()
            .url(url)
            .get()
            .build()
        val call: Call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body
                    val jsonObject = JSONObject(responseBody?.string() ?: "")
                    jsonObject.optJSONObject("results")?.optJSONArray("playlists")?.let { array ->
                        val playlists = (0 until array.length()).map { AMResultItem.fromJson(array.optJSONObject(it), ignoreGeorestrictedMusic, null) }
                        emitter.onNext(APIResponseData(playlists as List<Any>, null))
                        emitter.onComplete()
                    } ?: emitter.onNext(APIResponseData(emptyList(), null))
                } catch (e: Exception) {
                    emitter.tryOnError(e)
                } finally {
                    response.close()
                }
            }
        })
        emitter.setCancellable { call.cancel() }
    }
    return APIRequestData(observable, url)
}

fun API.addSongToPlaylist(playlistId: String, musicIds: String, mixpanelPage: String): Completable {
    return Completable.create { emitter ->

        val body = FormBody.Builder()
            .add("music_id", musicIds)
            .add("section", mixpanelPage)
            .build()

        val request = Request.Builder()
            .url(baseUrl + "playlist/" + playlistId + "/track")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        emitter.onComplete()
                    } else {
                        emitter.tryOnError(Throwable("Response code is not successful"))
                    }
                } catch (e: Exception) {
                    emitter.tryOnError(e)
                } finally {
                    response.close()
                }
            }
        })
    }
}

fun API.deleteSongFromPlaylist(playlistId: String, musicIds: String): Completable {
    return Completable.create { emitter ->

        val request = Request.Builder()
            .url(baseUrl + "playlist/" + playlistId + "/track/" + musicIds)
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        emitter.onComplete()
                    } else {
                        emitter.tryOnError(Throwable("Response code is not successful"))
                    }
                } catch (e: Exception) {
                    emitter.tryOnError(e)
                } finally {
                    response.close()
                }
            }
        })
    }
}

private fun getAdvertisingId(): Single<String> {
    return Single.create { emitter ->
        try {
            emitter.onSuccess(AdvertisingIdClient.getAdvertisingIdInfo(MainApplication.context).id)
        } catch (e: Exception) {
            emitter.onSuccess("")
        }
    }
}

/** Emits [Credentials] in case of success, otherwise a [APILoginException] will be thrown **/
fun API.login(providerData: LoginProviderData, socialEmail: String?): Single<Credentials> {
    return getAdvertisingId()
        .subscribeOn(AMSchedulersProvider().io)
        .flatMap { advertisingId ->
            Single.create<Credentials> { emitter ->

                val bodyBuilder = FormBody.Builder()
                    .add("os_type", "android")
                    .add("advertising_id", advertisingId)

                when (providerData) {
                    is LoginProviderData.Facebook -> {
                        bodyBuilder.add("fb_token", providerData.token)
                    }
                    is LoginProviderData.Google -> {
                        bodyBuilder.add("g_token", providerData.token)
                    }
                    is LoginProviderData.Twitter -> {
                        bodyBuilder
                            .add("t_token", providerData.token)
                            .add("t_secret", providerData.secret)
                    }
                    is LoginProviderData.Apple -> {
                        bodyBuilder.add("id_token", providerData.token)
                    }
                    is LoginProviderData.UsernamePassword -> {
                        bodyBuilder
                            .add("x_auth_username", providerData.username)
                            .add("x_auth_password", providerData.password)
                            .add("x_auth_mode", "client_auth")
                    }
                }

                socialEmail?.takeIf { it.isNotBlank() }?.let {
                    bodyBuilder.add("u_auth_email", it)
                }

                val url = baseUrl + (if (providerData is LoginProviderData.Apple) "auth/apple" else "access_token")

                val request = Request.Builder()
                    .url(url)
                    .post(bodyBuilder.build())
                    .tag(TAG_DO_NOT_REFRESH_TOKEN_ON_401)
                    .build()

                val callback = object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (e is SocketTimeoutException) {
                            emitter.onError(APILoginException("", null, 999, true))
                        } else {
                            emitter.onError(APILoginException("", null, 999, false))
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {

                        val responseBody = response.body
                        var jsonObject: JSONObject? = null
                        try {
                            jsonObject = JSONObject(responseBody?.string() ?: "")

                            Credentials.saveFromJson(MainApplication.context!!, jsonObject)

                            val credentials = Credentials.load(MainApplication.context)!!

                            when (providerData) {
                                is LoginProviderData.Facebook -> {
                                    credentials.facebookId = providerData.id
                                }
                                is LoginProviderData.Google -> {
                                    credentials.googleToken = providerData.token
                                }
                                is LoginProviderData.Twitter -> {
                                    credentials.twitterToken = providerData.token
                                    credentials.twitterSecret = providerData.secret
                                }
                                is LoginProviderData.Apple -> {
                                    credentials.appleIdToken = providerData.token
                                }
                                is LoginProviderData.UsernamePassword -> {
                                    credentials.email = providerData.username
                                    credentials.password = providerData.password
                                }
                            }

                            Credentials.save(credentials, MainApplication.context!!)

                            emitter.onSuccess(credentials)
                        } catch (e: Exception) {

                            var errorMessage: String? = jsonObject?.getStringOrNull("description") ?: jsonObject?.getStringOrNull("message")
                            val errorCode = jsonObject?.getIntOrNull("errorcode")

                            if (errorMessage?.toLowerCase(Locale.US)?.startsWith("expired timestamp") == true) {
                                errorMessage = MainApplication.context!!.getString(R.string.login_error_timestamp)
                            }
                            if (errorCode == ErrorCodes.MISSING_SOCIAL_DATA) {
                                errorMessage = MainApplication.context!!.getString(R.string.login_error_1037)
                            }
                            emitter.onError(APILoginException(errorMessage ?: "", errorCode, response.code, false))
                        } finally {
                            response.close()
                        }
                    }
                }

                client.newCall(request).enqueue(callback)
            }
        }
}

sealed class LoginProviderData {
    data class UsernamePassword(val username: String, val password: String) : LoginProviderData()
    data class Facebook(val id: String, val token: String) : LoginProviderData()
    data class Google(val token: String) : LoginProviderData()
    data class Twitter(val token: String, val secret: String) : LoginProviderData()
    data class Apple(val token: String) : LoginProviderData()
    data class Instagram(val token: String) : LoginProviderData()
}

/** Can throw a [LinkSocialException] in case of errors **/
fun API.linkSocialProvider(providerData: LoginProviderData): Completable {
    return Completable.create { emitter ->
        val bodyBuilder = FormBody.Builder()

        when (providerData) {
            is LoginProviderData.Twitter -> {
                bodyBuilder
                    .add("t_token", providerData.token)
                    .add("t_secret", providerData.secret)
            }
            is LoginProviderData.Instagram -> {
                bodyBuilder
                    .add("instagram_token", providerData.token)
            }
            else -> {
                emitter.onError(LinkSocialException.SocialNotSupported)
            }
        }

        val request = Request.Builder()
            .url(baseUrl + "access_token")
            .post(bodyBuilder.build())
            .build()

        val callback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e is SocketTimeoutException) {
                    emitter.onError(LinkSocialException.Timeout)
                } else {
                    emitter.onError(LinkSocialException.Generic)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    emitter.onComplete()
                } else {
                    try {
                        val json = JSONObject(response.body?.string() ?: "")
                        if (json.getIntOrNull("errorcode") == ErrorCodes.SOCIAL_ID_ALREADY_REGISTERED) {
                            emitter.onError(LinkSocialException.SocialIDAlreadyLinked)
                        } else {
                            emitter.onError(LinkSocialException.Generic)
                        }
                    } catch (e: Exception) {
                        emitter.onError(LinkSocialException.Generic)
                    }
                }
                response.close()
            }
        }

        client.newCall(request).enqueue(callback)
    }
}

fun API.fetchFeaturedMusic(): Single<List<AMFeaturedSpot>> {
    return Single.create { emitter ->
        val request = Request.Builder()
            .url(baseUrl + "featured")
            .get()
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body
                        val jsonObject = JSONObject(responseBody?.string() ?: "")
                        val featuredSpots = jsonObject.optJSONArray("results")?.let { array ->
                            (0 until array.length())
                                .mapNotNull { array.getJSONObjectOrNull(it) }
                                .mapNotNull { AMFeaturedSpot.fromJSON(it) }
                        } ?: emptyList()
                        emitter.onSuccess(featuredSpots)
                    } else {
                        emitter.onSuccess(emptyList())
                    }
                } catch (e: Exception) {
                    emitter.onSuccess(emptyList())
                } finally {
                    response.close()
                }
            }
        })
        emitter.setCancellable(call::cancel)
    }
}

fun API.fetchRecommendations(): Single<List<AMResultItem>> {
    return Single.create { emitter ->
        val request = Request.Builder()
            .url(baseUrl + "recommendations/songs")
            .get()
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body
                        val music = JSONArray(responseBody?.string()).let { array ->
                            (0 until array.length())
                                .mapNotNull { array.getJSONObjectOrNull(it) }
                                .mapNotNull { AMResultItem.fromJson(it, true, null) }
                        }
                        emitter.onSuccess(music)
                    } else {
                        emitter.onSuccess(emptyList())
                    }
                } catch (e: Exception) {
                    emitter.onSuccess(emptyList())
                } finally {
                    response.close()
                }
            }
        })
        emitter.setCancellable(call::cancel)
    }
}

fun API.getCommentListSingle(kind: String, id: String, uuid: String, threadId: String?): Single<AMCommentsResponse> {
    return Single.create { emitter ->
        try {

            val url = Uri.parse(baseUrl + "comments/single")
                .buildUpon()
                .apply {
                    appendQueryParameter("kind", kind)
                    appendQueryParameter("id", id)
                    appendQueryParameter("uuid", uuid)
                    threadId?.let {
                        appendQueryParameter("thread", threadId)
                    }
                }
                .build()
                .toString()
            val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body
                        val jsonObject = JSONObject(responseBody?.string() ?: "")
                        val commentsResponse = AMCommentsResponse.fromJSON(jsonObject)
                        emitter.onSuccess(commentsResponse)
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    } finally {
                        response.close()
                    }
                }
            })
            emitter.setCancellable(call::cancel)
        } catch (e: Exception) {
            emitter.tryOnError(e)
        }
    }
}

fun API.getCommentList(kind: String, id: String, limit: String, offset: String, sort: String): Single<AMCommentsResponse> {
    return Single.create { emitter ->
        try {
            val url = Uri.parse(baseUrl + "comments")
                .buildUpon()
                .apply {
                    appendQueryParameter("kind", kind)
                    appendQueryParameter("id", id)
                    appendQueryParameter("order_by", if (sort.isNotEmpty()) sort else CommentSort.Top.stringValue())
                    if (limit.isNotEmpty()) appendQueryParameter("limit", limit)
                    if (offset.isNotEmpty()) appendQueryParameter("offset", offset)
                }
                .build()
                .toString()
            val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body
                        val jsonObject = JSONObject(responseBody?.string() ?: "")
                        val commentsResponse = AMCommentsResponse.fromJSON(jsonObject)
                        emitter.onSuccess(commentsResponse)
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    } finally {
                        response.close()
                    }
                }
            })
            emitter.setCancellable(call::cancel)
        } catch (e: Exception) {
            emitter.tryOnError(e)
        }
    }
}

fun API.postCommentSend(content: String, kind: String, id: String, thread: String?): Single<AMComment> {
    return Single.create { emitter ->
        try {
            val jsonObject = JSONObject().apply {
                put("id", id)
                put("kind", kind)
                put("content", content)
                thread?.let {
                    put("thread", it)
                }
            }
            val jsonString = jsonObject.toString()
            val body = jsonString.toRequestBody(MEDIA_TYPE_JSON)
            val request: Request = Request.Builder()
                    .url(baseUrl + "comments")
                    .post(body)
                    .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            val responseBody = response.body
                            val bodyObject = JSONObject(responseBody?.string() ?: "")
                            val resultObject = bodyObject.getJSONObject("result")
                            val commentsResponse = AMComment.fromJSON(resultObject)
                            emitter.onSuccess(commentsResponse)
                        } else {
                            emitter.tryOnError(genericThrowable)
                        }
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    } finally {
                        response.close()
                    }
                }
            })
            emitter.setCancellable(call::cancel)
        } catch (e: Exception) {
            emitter.tryOnError(e)
        }
    }
}

fun API.postCommentReport(kind: String, id: String, uuid: String, thread: String?): Single<Boolean> {
    return Single.create { emitter ->
        try {
            val requestJson = JSONObject().apply {
                put("id", id)
                put("kind", kind)
                put("uuid", uuid)
                put("thread", thread ?: JSONObject.NULL)
            }
            val jsonString = requestJson.toString()
            val body = jsonString.toRequestBody(MEDIA_TYPE_JSON)
            val request: Request = Request.Builder()
                    .url(baseUrl + "comments/reports")
                    .post(body)
                    .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            emitter.onSuccess(true)
                        } else {
                            emitter.tryOnError(genericThrowable)
                        }
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    } finally {
                        response.close()
                    }
                }
            })
            emitter.setCancellable(call::cancel)
        } catch (e: Exception) {
            emitter.tryOnError(e)
        }
    }
}

fun API.postCommentDelete(kind: String, id: String, uuid: String, thread: String?): Single<Boolean> {
    return Single.create { emitter ->
        try {
            val requestJson = JSONObject().apply {
                put("id", id)
                put("kind", kind)
                put("uuid", uuid)
                thread?.let {
                    put("thread", thread)
                }
            }
            val jsonString = requestJson.toString()
            val body = jsonString.toRequestBody(MEDIA_TYPE_JSON)
            val request: Request = Request.Builder()
                    .url(baseUrl + "comments")
                    .delete(body)
                    .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            emitter.onSuccess(true)
                        } else {
                            emitter.tryOnError(genericThrowable)
                        }
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    } finally {
                        response.close()
                    }
                }
            })
            emitter.setCancellable(call::cancel)
        } catch (e: Exception) {
            emitter.tryOnError(e)
        }
    }
}

fun API.getStatusVote(kind: String, id: String): Single<ArrayList<AMVoteStatus>> {
    return Single.create { emitter ->
        val items = ArrayList<AMVoteStatus>()
        try {
            val url = Uri.parse(baseUrl + "comments/votes")
                .buildUpon()
                .apply {
                    appendQueryParameter("kind", kind)
                    appendQueryParameter("id", id)
                }
                .build()
                .toString()
            val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
            val call: Call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            val responseBody = response.body
                            val responseObject = JSONObject(responseBody?.string() ?: "")
                            val resultsArray = responseObject.getJSONArray("result")
                            for (i in 0 until resultsArray.length()) {
                                try {
                                    val item = AMVoteStatus.fromJSON(resultsArray.optJSONObject(i))
                                    items.add(item)
                                } catch (e: Exception) {
                                    emitter.tryOnError(e)
                                }
                            }
                            emitter.onSuccess(items)
                        } else {
                            emitter.tryOnError(genericThrowable)
                        }
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    } finally {
                        response.close()
                    }
                }
            })
            emitter.setCancellable(call::cancel)
        } catch (e: Exception) {
            emitter.tryOnError(e)
        }
    }
}

fun API.postCommentVote(comment: AMComment, isUpVote: Boolean?, kind: String, id: String): Single<AMCommentVote> {
    return Single.create { emitter ->
        try {
            val entities = JSONArray()
            val jsonObject = JSONObject().apply {
                put("uuid", comment.uuid)
                put("vote_up", isUpVote)
                put("thread", if (comment.threadUuid.isNullOrEmpty()) JSONObject.NULL else comment.threadUuid)
            }
            entities.put(jsonObject)
            val bodyJson = JSONObject().apply {
                put("id", id)
                put("kind", kind)
                put("votes", entities)
            }
            val jsonString = bodyJson.toString()
            val body = jsonString.toRequestBody(MEDIA_TYPE_JSON)
            val request: Request = Request.Builder()
                .url(baseUrl + "comments/votes")
                .post(body)
                .build()
            val call: Call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            val responseBody = response.body
                            val responseObject = JSONObject(responseBody?.string() ?: "")
                            val resultsArray = responseObject.getJSONArray("result")
                            val resultObject = resultsArray.optJSONObject(0)
                            val commentVoteObject = AMCommentVote.fromJSON(resultObject)
                            emitter.onSuccess(commentVoteObject)
                        } else {
                            emitter.tryOnError(genericThrowable)
                        }
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    } finally {
                        response.close()
                    }
                }
            })
            emitter.setCancellable(call::cancel)
        } catch (e: java.lang.Exception) {
            emitter.tryOnError(e)
        }
    }
}

fun API.addDownloads(musicIds: String, mixpanelPage: String) {

    if (!Credentials.isLogged(MainApplication.context) || musicIds.isBlank()) {
        return
    }

    val callback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {}
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                AMResultItem.markMusicAsSynced(musicIds)
            }
            response.close()
        }
    }

    val bodyBuilder = FormBody.Builder()
        .add("music_id", musicIds)
        .add("section", mixpanelPage)

    val request = Request.Builder()
        .url(baseUrl + "user/downloads")
        .post(bodyBuilder.build())
        .build()

    client.newCall(request).enqueue(callback)
}

fun API.reportOrBlock(reportType: ReportType, contentId: String, contentType: ReportContentType, reason: ReportContentReason): Completable {
    return Completable.create { emitter ->

        val body = JSONObject().apply {
            put("id", contentId)
            put("reason", reason.key)
        }.toString().toRequestBody(MEDIA_TYPE_JSON)

        val request = Request.Builder()
            .url(baseUrl + reportType.type + "/content/" + contentType.type)
            .post(body)
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        emitter.onComplete()
                    } else {
                        emitter.tryOnError(Throwable("Response code is not successful"))
                    }
                } catch (e: Exception) {
                    emitter.tryOnError(e)
                } finally {
                    response.close()
                }
            }
        })
        emitter.setCancellable(call::cancel)
    }
}

fun API.completeProfileAgeGender(name: String, birthday: Date, gender: AMArtist.Gender) = Completable.create { emitter ->

    val bodyBuilder = FormBody.Builder()
        .add("name", name)
        .add("gender", gender.toString())
        .add("birthday", SimpleDateFormat(APIInterface.DATE_FORMAT, Locale.US).format(birthday))

    val request = Request.Builder()
        .url("${baseUrl}user")
        .put(bodyBuilder.build())
        .build()

    val call = client.newCall(request)
    emitter.setCancellable(call::cancel)
    call.enqueue(CompletableCallback(emitter))
}

fun API.reportUnplayable(item: AMResultItem): Completable = Completable.create { emitter ->
    val url = baseUrl.buildUrl {
        appendPath("music")
        appendPath("song")
        appendPath(item.uploaderSlug)
        appendPath(item.urlSlug)
        appendQueryParameter("type", "song")
        appendQueryParameter("status", "unplayable")
    }

    val request = Request.Builder()
        .url(url)
        .patch(FormBody.Builder().build())
        .tag(TAG_DO_NOT_AUTHENTICATE)
        .build()

    val call = client.newCall(request)
    emitter.setCancellable(call::cancel)
    call.enqueue(CompletableCallback(emitter))
}

fun API.deleteTokenForLogout() = Completable.create { emitter ->
    val request = Request.Builder()
        .url("${baseUrl}access_token")
        .delete()
        .build()

    val call = client.newCall(request)
    emitter.setCancellable(call::cancel)
    call.enqueue(CompletableCallback(emitter))
}

fun API.updatePassword(oldPassword: String, newPassword: String) = Completable.create { emitter ->
    val bodyBuilder = FormBody.Builder()
        .add("old_password", oldPassword)
        .add("new_password", newPassword)

    val request = Request.Builder()
        .url("${baseUrl}user")
        .patch(bodyBuilder.build())
        .tag(TAG_DO_NOT_REFRESH_TOKEN_ON_401)
        .build()

    val call = client.newCall(request)
    emitter.setCancellable(call::cancel)
    call.enqueue(CompletableCallback(emitter))
}

fun API.runVerifyForgotPasswordToken(token: String) = Completable.create { emitter ->
    val request = Request.Builder()
        .url("${baseUrl}user/verify-forgot-token?token=$token")
        .get()
        .tag(TAG_DO_NOT_AUTHENTICATE)
        .build()

    val call = client.newCall(request)
    emitter.setCancellable(call::cancel)
    call.enqueue(CompletableCallback(emitter))
}

fun API.runResetPassword(token: String, newPassword: String) = Completable.create { emitter ->
    val bodyBuilder = FormBody.Builder()
        .add("token", token)
        .add("password", newPassword)
        .add("password2", newPassword)

    val request = Request.Builder()
        .url("${baseUrl}user/recover-account")
        .post(bodyBuilder.build())
        .tag(TAG_DO_NOT_AUTHENTICATE)
        .build()

    val call = client.newCall(request)
    emitter.setCancellable(call::cancel)
    call.enqueue(CompletableCallback(emitter))
}

class CompletableCallback(private val emitter: CompletableEmitter) : Callback {
    override fun onFailure(call: Call, e: IOException) {
        emitter.tryOnError(e)
    }

    override fun onResponse(call: Call, response: Response) {
        response.use {
            if (it.isSuccessful) {
                emitter.onComplete()
            } else {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    val errorTitle = json.getStringOrNull("message")
                    var errorMessage = ""
                    json.optJSONObject("errors")?.let { errorsDict ->
                        errorsDict.keys().iterator().forEach { key ->
                            (errorsDict.get(key) as? String)?.let { string -> errorMessage += string + "\n" }
                            (errorsDict.get(key) as? JSONObject)?.let { errorsSubDict ->
                                errorsSubDict.keys().iterator().forEach { subKey ->
                                    errorsSubDict.getStringOrNull(subKey)
                                        ?.let { string -> errorMessage += string + "\n" }
                                }
                            }
                        }
                    }
                    emitter.tryOnError(APIDetailedException(errorTitle, errorMessage.trim()))
                } catch (e: Exception) {
                    emitter.tryOnError(e)
                }
            }
        }
    }
}
