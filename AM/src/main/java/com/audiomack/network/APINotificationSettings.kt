package com.audiomack.network

import com.audiomack.model.NotificationPreferenceType
import com.audiomack.model.NotificationPreferenceTypeValue
import io.reactivex.Single
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

class APINotificationSettings(
    private val client: OkHttpClient,
    private val baseUrl: String
) : APIInterface.NotificationSettingsInterface {

    override fun getNotificationPreferences(): Single<List<NotificationPreferenceTypeValue>> {
        return Single.create { emitter ->
            val request = Request.Builder()
                .url(baseUrl + "user/setting/notification")
                .get()
                .build()

            val call = client.newCall(request).apply {
                enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        emitter.tryOnError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            val responseBody = response.body
                            val json = JSONObject(responseBody!!.string())
                            val results = json.keys().asSequence().toList().mapNotNull {
                                val type = NotificationPreferenceType.fromApiCode(it)
                                val value = json.optBoolean(it)
                                if (type == null) {
                                    null
                                } else {
                                    NotificationPreferenceTypeValue(type, value)
                                }
                            }
                            emitter.onSuccess(results)
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

    override fun setNotificationPreference(typeValue: NotificationPreferenceTypeValue): Single<Boolean> {
        return Single.create { emitter ->

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("settings", JSONObject().apply {
                        put(typeValue.type.apiCode, typeValue.value)
                    }.toString()
                )
                .build()

            val request = Request.Builder()
                .url(baseUrl + "user/setting/notification")
                .post(body)
                .build()

            client.newCall(request).apply {
                enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        emitter.tryOnError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            emitter.onSuccess(response.isSuccessful)
                        } catch (e: Exception) {
                            emitter.tryOnError(e)
                        } finally {
                            response.close()
                        }
                    }
                })
            }
        }
    }
}
