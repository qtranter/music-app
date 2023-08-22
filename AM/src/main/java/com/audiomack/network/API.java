package com.audiomack.network;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.audiomack.BuildConfig;
import com.audiomack.ConstantsKt;
import com.audiomack.MainApplication;
import com.audiomack.R;
import com.audiomack.data.database.ArtistDAOImpl;
import com.audiomack.data.database.MusicDAOImpl;
import com.audiomack.data.device.DeviceRepository;
import com.audiomack.data.premium.PremiumRepository;
import com.audiomack.data.reachability.Reachability;
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl;
import com.audiomack.data.socialauth.SocialAuthManagerImpl;
import com.audiomack.data.tracking.TrackingRepository;
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource;
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository;
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource;
import com.audiomack.data.tracking.mixpanel.MixpanelRepository;
import com.audiomack.data.user.AccountSaveException;
import com.audiomack.data.user.UserDataSource;
import com.audiomack.data.user.UserRepository;
import com.audiomack.data.user.UserSlugSaveException;
import com.audiomack.model.AMArtist;
import com.audiomack.model.AMComment;
import com.audiomack.model.AMCommentVote;
import com.audiomack.model.AMCommentsResponse;
import com.audiomack.model.AMFeaturedSpot;
import com.audiomack.model.AMNotification;
import com.audiomack.model.AMResultItem;
import com.audiomack.model.AMVoteStatus;
import com.audiomack.model.APIRequestData;
import com.audiomack.model.APIResponseData;
import com.audiomack.model.Credentials;
import com.audiomack.model.ErrorCodes;
import com.audiomack.model.NextPageData;
import com.audiomack.model.ReportContentReason;
import com.audiomack.model.ReportContentType;
import com.audiomack.model.ReportType;
import com.audiomack.data.user.UserData;
import com.audiomack.network.retrofitApi.WorldPostService;
import com.audiomack.onesignal.OneSignalRepository;
import com.audiomack.rx.AMSchedulersProvider;
import com.audiomack.utils.GeneralPreferencesHelper;
import com.audiomack.utils.StethoUtils;
import com.audiomack.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

import static com.audiomack.network.AuthInterceptor.TAG_DO_NOT_AUTHENTICATE;

public class API implements APIInterface.AuthenticationInterface, APIInterface.SocialLinkInterface,
        APIInterface.FeaturedSpotsInterface, APIInterface.UserInterface, APIInterface.DownloadsInterface,
        APIInterface.CommentsInterface, APIInterface.ReportInterface, APIInterface.SearchInterface,
        APIInterface.SettingsInterface, APIInterface.AccountsInterface, APIInterface.FeedInterface {

    public final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    public Throwable genericThrowable = new Throwable("API error");

    private final int REGULAR_TIMEOUT = 30;
    private final int SHORT_TIMEOUT = 10;

    private static API instance;

    public static API getInstance() {
        if (instance == null) {
            instance = new API();
        }
        return instance;
    }

    private final OkHttpClient client;
    protected String baseUrl;
    private Handler mainHandler;

    private final MixpanelDataSource mixpanelDataSource = new MixpanelRepository();
    private final AppsFlyerDataSource appsFlyerDataSource = new AppsFlyerRepository();

    private final APIEmailVerification emailVerificationAPI;
    private final APINotificationSettings notificationSettingsAPI;
    private final WorldPostService worldPostService;

    private API() {
        super();

        updateEnvironment();

        String userAgent = Utils.INSTANCE.getUserAgent(MainApplication.getContext());

        Interceptor AuthInterceptor = new AuthInterceptor(userAgent, DeviceRepository.INSTANCE, new TrackingRepository(), new SocialAuthManagerImpl(), new AMSchedulersProvider());

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(REGULAR_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(REGULAR_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(REGULAR_TIMEOUT, TimeUnit.SECONDS)
                .followRedirects(true)
                .addInterceptor(AuthInterceptor);

        if (BuildConfig.AUDIOMACK_DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC));
        }
        StethoUtils.INSTANCE.addNetworkInterceptor(builder);

        this.client = builder.build();

        this.emailVerificationAPI = new APIEmailVerification(client, baseUrl);
        this.notificationSettingsAPI = new APINotificationSettings(client, baseUrl);
        this.worldPostService = WorldPostService.Companion.create(client);
    }

    public APIInterface.EmailVerificationInterface getEmailVerificationAPI() {
        return emailVerificationAPI;
    }

    public APINotificationSettings getNotificationSettingsAPI() {
        return notificationSettingsAPI;
    }

    public WorldPostService getWorldPostService() {
        return worldPostService;
    }

    private Handler getMainHandler() {
        if (mainHandler == null) {
            this.mainHandler = new Handler(Looper.getMainLooper());
        }
        return mainHandler;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public interface SignupListener {
        void onSuccess(@NonNull Credentials credentials);

        void onTimeout();

        void onFailure(String errorMessage);
    }

    public interface GetStreamURLListener {
        void onSuccess(String streamUrl);

        void onFailure(@NonNull final Exception exception);
    }

    public interface FavoriteListener {
        void onSuccess();

        void onAlreadyFavorite();

        void onFailure();
    }

    public interface GetInfoListener {
        void onSuccess(AMResultItem item);

        void onFailure(int statusCode);
    }

    public interface FollowListener {
        void onSuccess();

        void onFailure();
    }

    public interface ArrayListener<T> {
        void onSuccess(List<T> results);

        void onFailure();
    }

    public interface ForgotPasswordListener {
        void onSuccess();

        void onFailure(@Nullable String errorMessage, boolean emailNotFound);
    }

    // APIInterface.AuthenticationInterface

    @NotNull public Single<Credentials> loginWithEmailPassword(@NonNull String username, @NonNull String password) {
        return APIExtensionsKt.login(this, new LoginProviderData.UsernamePassword(username, password), null);
    }

    @NotNull public Single<Credentials> loginWithFacebook(@NonNull String fbId, @NonNull String fbToken, @Nullable String socialEmail) {
        return APIExtensionsKt.login(this, new LoginProviderData.Facebook(fbId, fbToken), socialEmail);
    }

    @NotNull public Single<Credentials> loginWithGoogle(@NonNull String googleToken, @Nullable String socialEmail) {
        return APIExtensionsKt.login(this, new LoginProviderData.Google(googleToken), socialEmail);
    }

    @NotNull public Single<Credentials> loginWithTwitter(@NotNull String twitterToken, @NotNull String twitterSecret, @Nullable String socialEmail) {
        return APIExtensionsKt.login(this, new LoginProviderData.Twitter(twitterToken, twitterSecret), socialEmail);
    }

    @NotNull public Single<Credentials> loginWithAppleId(@NotNull String appleIdToken, @Nullable String socialEmail) {
        return APIExtensionsKt.login(this, new LoginProviderData.Apple(appleIdToken), socialEmail);
    }

    @Override
    public void signup(@NonNull String username,
                       @NonNull String email,
                       @NonNull String password,
                       @Nullable String advertisingId,
                       @NonNull Date birthday,
                       @NonNull AMArtist.Gender gender,
                       @NonNull final SignupListener listener) {
        Credentials credentials = new Credentials();
        credentials.setUserScreenName(username);
        credentials.setEmail(email);
        credentials.setPassword(password);

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (e instanceof SocketTimeoutException) {
                    getMainHandler().post(listener::onTimeout);
                } else {
                    getMainHandler().post(() -> listener.onFailure(null));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, Response response) {

                ResponseBody responseBody = response.body();
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(responseBody.string());

                    Credentials.saveFromJson(MainApplication.getContext(), jsonObject);

                    getMainHandler().post(() -> listener.onSuccess(credentials));

                } catch (Exception e) {

                    String errorMessage = null;
                    try {
                        errorMessage = jsonObject.optString("message");
                        JSONObject errorsObject = jsonObject.optJSONObject("errors");
                        for (Iterator<String> keysIterator = errorsObject.keys(); keysIterator.hasNext(); ) {
                            JSONObject errorObject = errorsObject.optJSONObject(keysIterator.next());
                            for (Iterator<String> errorKeysIterator = errorObject.keys(); errorKeysIterator.hasNext(); ) {
                                errorMessage = errorObject.optString(errorKeysIterator.next());
                                break;
                            }
                            break;
                        }
                    } catch (Exception ee) {
                        Timber.w(ee);
                    }

                    final String errorMsg = errorMessage;
                    getMainHandler().post(() -> listener.onFailure(errorMsg));

                } finally {
                    response.close();
                }
            }
        };

        FormBody.Builder bodyBuilder = new FormBody.Builder()
                .add("email", credentials.getEmail())
                .add("artist_name", credentials.getUserScreenName())
                .add("password", credentials.getPassword())
                .add("password2", credentials.getPassword())
                .add("gender", gender.toString())
                .add("birthday", new SimpleDateFormat(APIInterface.DATE_FORMAT, Locale.US).format(birthday))
                .add("os_type", "android");

        if (advertisingId != null) {
            bodyBuilder.add("advertising_id", advertisingId);
        }

        RequestBody body = bodyBuilder.build();

        Request request = new Request.Builder()
                .url(baseUrl + "user/register")
                .post(body)
                .tag(TAG_DO_NOT_AUTHENTICATE)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public Observable<Boolean> checkEmailExistence(@Nullable String email, @Nullable String slug) {
        return Observable.create(emitter -> {

            if (email == null && slug == null) {
                emitter.tryOnError(genericThrowable);
                return;
            }

            String url = baseUrl + "identity_check";
            if (email != null) {
                url += (url.contains("?") ? "&" : "?") + "email=" + URLEncoder.encode(email, "UTF-8");
            }
            if (slug != null) {
                url += (url.contains("?") ? "&" : "?") + "slug=" + URLEncoder.encode(slug, "UTF-8");
            }

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();


            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    emitter.tryOnError(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try {
                        ResponseBody responseBody = response.body();
                        JSONObject jsonObject = new JSONObject(responseBody.string());
                        boolean emailTaken = jsonObject.optJSONObject("email").optBoolean("taken");
                        emitter.onNext(emailTaken);
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.tryOnError(e);
                    } finally {
                        response.close();
                    }
                }
            });
            emitter.setCancellable(call::cancel);
        });
    }

    @NotNull
    @Override
    public Completable logout() {
        return APIExtensionsKt.deleteTokenForLogout(this);
    }

    @NotNull
    @Override
    public Completable changePassword(@NotNull String oldPassword, @NotNull String newPassword) {
        return APIExtensionsKt.updatePassword(this, oldPassword, newPassword);
    }

    @NotNull
    @Override
    public Completable verifyForgotPasswordToken(@NotNull String token) {
        return APIExtensionsKt.runVerifyForgotPasswordToken(this, token);
    }

    @NotNull
    @Override
    public Completable resetPassword(@NotNull String token, @NotNull String newPassword) {
        return APIExtensionsKt.runResetPassword(this, token, newPassword);
    }

    // APIInterface.SocialLinkInterface

    @NotNull public Completable linkSocial(@NotNull LoginProviderData providerData) {
        return APIExtensionsKt.linkSocialProvider(this, providerData);
    }

	// APIInterface.FeaturedSpotsInterface

    @NotNull public Single<List<AMFeaturedSpot>> getFeaturedMusic() {
        return APIExtensionsKt.fetchFeaturedMusic(this);
    }

    // APIInterface.CommentsInterface

    @NotNull public Single<AMCommentsResponse> getSingleComments(@NonNull String kind, @NonNull String id, @NonNull String uuid, @Nullable String threadId) {
        return APIExtensionsKt.getCommentListSingle(this, kind, id, uuid, threadId);
    }

    @NotNull public Single<AMCommentsResponse> getComments(@NonNull String kind, @NonNull String id, @NonNull String limit, @NonNull String offset, @NonNull String sort) {
        return APIExtensionsKt.getCommentList(this, kind, id, limit, offset, sort);
    }

    @NotNull public Single<AMComment> postComment(@NonNull String content, @NonNull String kind, @NonNull String id, @Nullable String thread) {
        return APIExtensionsKt.postCommentSend(this, content, kind, id, thread);
    }

    @NotNull public Single<Boolean> reportComment(@NonNull String kind, @NonNull String id, @NonNull String uuid, @Nullable String thread) {
        return APIExtensionsKt.postCommentReport(this, kind, id, uuid, thread);
    }

    @NotNull public Single<Boolean> deleteComment(@NonNull String kind, @NonNull String id, @NonNull String uuid, @Nullable String thread) {
        return APIExtensionsKt.postCommentDelete(this, kind, id, uuid, thread);
    }

    @NotNull public Single<ArrayList<AMVoteStatus>> getVoteStatus(@NonNull String kind, @NonNull String id) {
        return APIExtensionsKt.getStatusVote(this, kind, id);
    }

    @NotNull public Single<AMCommentVote> voteComment(@NotNull AMComment comment, @Nullable Boolean isUpVote, @NotNull String kind, @NotNull String id) {
        return APIExtensionsKt.postCommentVote(this, comment, isUpVote, kind, id);
    }

    // APIInterface.DownloadsInterface

    public void addDownload(@NotNull String musicId, @NotNull String mixpanelPage) {
        APIExtensionsKt.addDownloads(this, musicId, mixpanelPage);
    }

    // APIInterface.ReportInterface

    @NotNull public Completable reportBlock(@NotNull ReportType reportType, @NotNull String contentId, @NotNull ReportContentType contentType, @NotNull ReportContentReason reportReason) {
        return APIExtensionsKt.reportOrBlock(this, reportType, contentId, contentType, reportReason);
	}

    // APIInterface.SearchDataSource

    @NotNull public Single<List<AMResultItem>> getRecommendations() {
        return APIExtensionsKt.fetchRecommendations(this);
    }

    // APIInterface.UserInterface

    @NotNull public Observable<AMArtist> getUserData() {
        return Observable.create(emitter -> {
            Callback callback = new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    emitter.tryOnError(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NotNull Response response) {

                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());

                        AMArtist artist = AMArtist.fromJSON(true, jsonObject);

                        JSONArray favoritePlaylistIDs = jsonObject.optJSONArray("favorite_playlists");
                        if (favoritePlaylistIDs != null) {
                            for (int i = 0; i < favoritePlaylistIDs.length(); i++) {
                                UserData.INSTANCE.addItemToFavoritePlaylists(favoritePlaylistIDs.optString(i));
                            }
                        }
                        JSONArray favoriteMusicIDs = jsonObject.optJSONArray("favorite_music");
                        if (favoriteMusicIDs != null) {
                            for (int i = 0; i < favoriteMusicIDs.length(); i++) {
                                UserData.INSTANCE.addItemToFavoriteMusic(favoriteMusicIDs.optString(i));
                            }
                        }
                        JSONArray followingArtistIDs = jsonObject.optJSONArray("following");
                        if (followingArtistIDs != null) {
                            for (int i = 0; i < followingArtistIDs.length(); i++) {
                                UserData.INSTANCE.addArtistToFollowing(followingArtistIDs.optString(i));
                            }
                        }
                        JSONArray myPlaylistIDs = jsonObject.optJSONArray("playlists");
                        if (myPlaylistIDs != null) {
                            for (int i = 0; i < myPlaylistIDs.length(); i++) {
                                UserData.INSTANCE.addPlaylistToMyPlaylists(myPlaylistIDs.optString(i));
                            }
                        }
                        JSONArray reupIDs = jsonObject.optJSONArray("reups");
                        if (reupIDs != null) {
                            for (int i = 0; i < reupIDs.length(); i++) {
                                UserData.INSTANCE.addItemToReups(reupIDs.optString(i));
                            }
                        }
                        JSONArray pinned = jsonObject.optJSONArray("pinned");
                        if (pinned != null) {
                            UserData.INSTANCE.clearHighlights();
                            for (int i = 0; i < pinned.length(); i++) {
                                JSONObject musicJsonObject = pinned.optJSONObject(i);
                                if (musicJsonObject != null) {
                                    AMResultItem music = AMResultItem.fromJson(musicJsonObject, false, null);
                                    UserData.INSTANCE.addItemToHighlights(music);
                                }
                            }
                        }
                        Credentials credentials = Credentials.load(MainApplication.getContext());
                        Credentials.save(credentials, MainApplication.getContext());

                        mixpanelDataSource.trackIdentity(UserRepository.getInstance(), PremiumRepository.getInstance());
                        appsFlyerDataSource.trackIdentity(UserRepository.getInstance());

                        emitter.onNext(artist);
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.tryOnError(e);
                    } finally {
                        response.close();
                    }
                }
            };

            Request request = new Request.Builder()
                    .url(baseUrl + "user")
                    .get()
                    .build();
            Call call = client.newCall(request);
            call.enqueue(callback);
            emitter.setCancellable(call::cancel);
        });
    }

    @NotNull public Completable completeProfile(@NotNull String name, @NotNull Date birthday, @NotNull AMArtist.Gender gender) {
        return APIExtensionsKt.completeProfileAgeGender(this, name, birthday, gender);
    }

    @NotNull public Single<AMArtist> editUserAccountInfo(@NonNull AMArtist artist) {
        return Single.create(emitter -> {

            String messageString = MainApplication.getContext().getString(R.string.generic_api_error);
            AccountSaveException genericException = new AccountSaveException(null, messageString);

            try {
                FormBody.Builder bodyBuilder = new FormBody.Builder()
                        .add("name", (artist.getName() != null) ? artist.getName() : "")
                        .add("label", (artist.getLabel() != null) ? artist.getLabel() : "")
                        .add("hometown", (artist.getHometown() != null) ? artist.getHometown() : "")
                        .add("url", (artist.getUrl() != null) ? artist.getUrl() : "")
                        .add("bio", (artist.getBio() != null) ? artist.getBio() : "")
                        .add("facebook", (artist.getFacebook() != null) ? artist.getFacebook() : "")
                        .add("genre", (artist.getGenre() != null) ? artist.getGenre() : "")
                        .add("youtube", (artist.getYoutube() != null) ? artist.getYoutube() : "")
                        .add("gender", (artist.getGender() != null) ? artist.getGender().toString() : "")
                        .add("birthday", (artist.getBirthdayString() != null) ? artist.getBirthdayString() : "");
                if (artist.getImageBase64() != null) {
                    bodyBuilder.add("image", artist.getImageBase64());
                }
                if (artist.getBannerBase64() != null) {
                    bodyBuilder.add("image_banner", artist.getBannerBase64());
                }

                Request request = new Request.Builder()
                        .url(baseUrl + "user")
                        .put(bodyBuilder.build())
                        .build();

                Call call = client.newCall(request);
                call.timeout().timeout(SHORT_TIMEOUT, TimeUnit.SECONDS);
                Response response = call.execute();

                try (ResponseBody responseBody = response.body()) {

                    if (responseBody != null) {

                        String responseString = responseBody.string();
                        JSONObject responseObject = new JSONObject(responseString);
                        responseBody.close();

                        if (response.isSuccessful() && responseObject.has("id")) {

                            AMArtist freshArtist = AMArtist.fromJSON(true, responseObject);
                            emitter.onSuccess(freshArtist);

                        } else {

                            String errorTitle = null;
                            String errorMessage = "";

                            try {
                                if (!responseObject.isNull("message")) {
                                    errorTitle = responseObject.getString("message");
                                }
                                JSONObject errorsDict = responseObject.optJSONObject("errors");
                                if (errorsDict != null) {
                                    Iterator<String> iterator = errorsDict.keys();
                                    while (iterator.hasNext()) {
                                        String key = iterator.next();
                                        Object obj = errorsDict.get(key);

                                        if (obj instanceof String) {
                                            errorMessage += obj + " ";
                                        } else if (obj instanceof JSONObject) {
                                            Iterator<String> it = ((JSONObject) obj).keys();
                                            while (it.hasNext()) {
                                                String k = it.next();
                                                errorMessage += ((JSONObject) obj).getString(k) + " ";
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ee) {
                                Timber.w(ee);
                            }

                            if (!TextUtils.isEmpty(errorMessage)) {
                                emitter.onError(new AccountSaveException(errorTitle, errorMessage));
                            }
                        }
                    }

                    emitter.onError(genericException);

                } catch (Exception e) {
                    emitter.onError(e);
                }

            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    @NotNull public Single<AMArtist> editUserUrlSlug(@NonNull AMArtist artist) {
        return Single.create(emitter -> {

            String messageString = MainApplication.getContext().getString(R.string.generic_api_error);
            UserSlugSaveException genericException = new UserSlugSaveException(null, messageString);

            try {

                FormBody.Builder bodyBuilder = new FormBody.Builder()
                        .add("url_slug", (artist.getUrlSlug() != null) ? artist.getUrlSlug() : "");

                Request request = new Request.Builder()
                        .url(baseUrl + "user")
                        .patch(bodyBuilder.build())
                        .build();


                Call call = client.newCall(request);
                call.timeout().timeout(SHORT_TIMEOUT, TimeUnit.SECONDS);
                Response response = call.execute();

                try (ResponseBody responseBody = response.body()) {

                    if (responseBody != null) {

                        String responseString = responseBody.string();
                        JSONObject responseObject = new JSONObject(responseString);
                        responseBody.close();

                        if (response.isSuccessful() && responseObject.has("id")) {

                            AMArtist freshArtist = AMArtist.fromJSON(true, responseObject);
                            emitter.onSuccess(freshArtist);

                        } else {

                            String errorMessage;

                            if (!responseObject.isNull("message")) {
                                errorMessage = responseObject.getString("message");
                                if (!TextUtils.isEmpty(errorMessage)) {
                                    emitter.onError(new UserSlugSaveException("", errorMessage));
                                }
                            }

                        }
                    }

                    emitter.onError(genericException);

                } catch (Exception e) {
                    emitter.onError(e);
                }

            } catch (Exception e) {
                emitter.onError(e);
            }

        });

    }

    // APIInterface.SettingsInterface

    public void updateEnvironment() {
        this.baseUrl = (GeneralPreferencesHelper.getInstance(MainApplication.getContext()).isLiveEnvironment(MainApplication.getContext()) ? BuildConfig.AM_WS_URL_LIVE : BuildConfig.AM_WS_URL_DEV);
    }

    // Other

    public APIRequestData getRecent(String genre, int page, boolean ignoreGeorestrictedMusic) {
        String url = baseUrl + "music/";
        if (!TextUtils.isEmpty(genre) && !genre.equals("all")) {
            url += genre + "/";
        }
        url += "recent" + "?page=" + (page + 1);
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), url);
    }

    public APIRequestData getTrending(String genre, int page, boolean ignoreGeorestrictedMusic) {
        if (!TextUtils.isEmpty(genre) && !genre.equals("all")) {
            genre = genre + "/";
        } else {
            genre = "";
        }
        String url = baseUrl + "music/" + genre + "trending" + "?page=" + (page + 1);
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), url);
    }

    public APIRequestData search(String query, String category, String sort, boolean verifiedOnly, String genre, int page, boolean ignoreGeorestrictedMusic) {
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            Timber.tag(API.class.getSimpleName()).w("Failed to URL encode the query");
        }
        String url = baseUrl + "search?q=" + query + "&sort=" + sort + "&show=" + category + (verifiedOnly ? "&verified=on" : "") + ((genre != null && !genre.equals("all")) ? "&genre=" + genre : "") + "&page=" + (page + 1);
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), url);
    }

    public APIRequestData getItems(String genre, String category, String period, int page, boolean ignoreGeorestrictedMusic) {
        if (!TextUtils.isEmpty(genre) && !genre.equals("all")) {
            genre = genre + "/";
        } else {
            genre = "";
        }
        String url = baseUrl + genre + "chart/" + category + "/" + period + "?page=" + (page + 1);
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), url);
    }

    public APIRequestData getDownloads(String type, String pagingToken, boolean ignoreGeorestrictedMusic) {
        String url = baseUrl + "user/downloads?type=" + type + "&limit=50";
        if (pagingToken != null) {
            url += "&paging_token=" + pagingToken;
        }
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), url);
    }

    public APIRequestData getArtistUploads(String userSlug, int page, boolean ignoreGeorestrictedMusic) {
        String url = baseUrl + "artist/" + userSlug + "/uploads" + "?page=" + (page + 1);
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), url);
    }

    public APIRequestData getArtistFavorites(String userSlug, int page, String category, boolean ignoreGeorestrictedMusic) {
        String url = baseUrl + "artist/" + userSlug + "/favorites" + "?show=" + category + "&page=" + (page + 1);
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), url);
    }

    public APIRequestData getArtistPlaylists(String userSlug, int page, boolean ignoreGeorestrictedMusic) {
        String url = baseUrl + "artist/" + userSlug + "/playlists" + "?page=" + (page + 1);
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), url);
    }

    public APIRequestData getArtistFollowing(String userSlug, String pagingToken) {
        String url = baseUrl + "artist/" + userSlug + "/following" + "?paging_token=" + (pagingToken != null ? pagingToken : "false");
        return new APIRequestData(getArtists(url, "results"), null);
    }

    public APIRequestData getArtistFollowers(String userSlug, String pagingToken) {
        String url = baseUrl + "artist/" + userSlug + "/follows" + "?paging_token=" + (pagingToken != null ? pagingToken : "false");
        return new APIRequestData(getArtists(url, "results"), null);
    }

    @NotNull public Observable<APIResponseData> getSuggestedFollows(int page) {
        String url = baseUrl + "user/follow?page=" + (page + 1);
        return getArtists(url, "results");
    }

	@NotNull public Observable<APIResponseData> getArtistsRecommendations() {
        String url = baseUrl + "recommendations/artists";
        return getArtists(url, null);
    }

    @NotNull public APIRequestData getMyFeed(int page, boolean excludeReups, boolean ignoreGeorestrictedMusic) {
        String url = baseUrl + "user/feed?page=" + (page + 1);
        if (excludeReups) {
            url += "&only_uploads=1";
        }
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), url);
    }

    public APIRequestData getMyPlaylists(int page, String genre, @Nullable String biasedWithMusicId, boolean ignoreGeorestrictedMusic) {
        String url = baseUrl + "user/playlists";
        if (genre.equals("all")) {
            url += "?";
        } else {
            url += "?genre=" + genre + "&";
        }
        url += "page=" + (page + 1);
        if (biasedWithMusicId != null) {
            url += "&music_id=" + biasedWithMusicId;
        }
        return new APIRequestData(getMusicAsObservable(url, null, ignoreGeorestrictedMusic), null);
    }

    public APIRequestData searchUserAccount(String query, String category, int page, boolean ignoreGeorestrictedMusic) {
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            Timber.tag(API.class.getSimpleName()).w("Failed to URL encode the query");
        }
        String url = baseUrl + "user/search?q=" + query + "&show=" + category + "&page=" + (page + 1);
        return new APIRequestData(getMusicAsObservable(url, category, ignoreGeorestrictedMusic), url);
    }

    public Observable<APIResponseData> getAllMusicPages(NextPageData nextPageData) {
        return getSingleMusicPage(nextPageData.getNextPageUrl(), 1, null, true);
    }

    private Observable<APIResponseData> getSingleMusicPage(String url, int page, APIResponseData previousResponse, boolean ignoreGeorestrictedMusic) {
        String newUrl = url.replace("page=" + page, "page=" + (page + 1));
        return getMusicAsObservable(newUrl, null, ignoreGeorestrictedMusic)
                .flatMap((Function<APIResponseData, ObservableSource<APIResponseData>>) apiResponseData -> {
                    if (apiResponseData.getObjects().size() > 0) {
                        APIResponseData data;
                        if (previousResponse != null) {
                            data = previousResponse;
                            List<Object> objects = data.getObjects();
                            objects.addAll(apiResponseData.getObjects());
                        } else {
                            data = apiResponseData;
                        }
                        return getSingleMusicPage(newUrl, page + 1, data, ignoreGeorestrictedMusic);
                    } else {
                        return Observable.just(previousResponse);
                    }
                });
    }

    public Observable<APIResponseData> getNextPage(NextPageData nextPageData) {
        return getMusicAsObservable(nextPageData.getNextPageUrl(), null, true);
    }

    public Observable<AMArtist> getArtistInfo(String artistSlug) {
        return Observable.create(emitter -> {
            Request request = new Request.Builder()
                    .url(baseUrl + "artist/" + artistSlug)
                    .get()
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    emitter.tryOnError(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            ResponseBody responseBody = response.body();
                            JSONObject jsonObject = new JSONObject(responseBody.string());
                            final AMArtist artist = AMArtist.fromJSON(false, jsonObject.optJSONObject("results"));
                            emitter.onNext(artist);
                            emitter.onComplete();
                        } else {
                            emitter.tryOnError(new APIException(response.code()));
                        }
                    } catch (Exception e) {
                        emitter.tryOnError(e);
                    } finally {
                        response.close();
                    }
                }
            });
            emitter.setCancellable(call::cancel);
        });
    }

    public APIRequestData searchArtists(String query, String category, String sort, boolean verifiedOnly, String genre, int page) {
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            Timber.tag(API.class.getSimpleName()).w("Failed to URL encode the query");
        }
        String url = baseUrl + "search?q=" + query + "&sort=" + sort + "&show=" + category + (verifiedOnly ? "&verified=on" : "") + (genre != null ? "&genre=" + genre : "") + "&page=" + (page + 1);
        return new APIRequestData(getArtists(url, "results"), url);
    }

    public void favorite(AMResultItem item, final @NonNull FavoriteListener listener, @NonNull String mixpanelPage) {

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getMainHandler().post(listener::onFailure);
            }

            @Override
            public void onResponse(@NonNull Call call, Response response) {
                if (response.code() == 204) {
                    getMainHandler().post(listener::onSuccess);
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        if (jsonObject.optInt("errorcode") == ErrorCodes.ALREADY_FAVORITED) {
                            getMainHandler().post(listener::onAlreadyFavorite);
                        } else {
                            getMainHandler().post(listener::onFailure);
                        }
                    } catch (Exception e) {
                        Timber.w(e);
                    }
                }
                response.close();
            }
        };

        RequestBody body = new FormBody.Builder()
                .add("section", mixpanelPage)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "music/" + item.getItemId() + "/favorite")
                .put(body)
                .build();
        Call call = client.newCall(request);
        call.timeout().timeout(SHORT_TIMEOUT, TimeUnit.SECONDS);
        call.enqueue(callback);
    }

    public void unfavorite(AMResultItem item, final @NonNull FavoriteListener listener) {

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getMainHandler().post(listener::onFailure);
            }

            @Override
            public void onResponse(@NonNull Call call, Response response) {
                if (response.code() == 204) {
                    getMainHandler().post(listener::onSuccess);
                }else{
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        if(jsonObject.optInt("errorcode") == ErrorCodes.ALREADY_UNFAVORITED){
                            getMainHandler().post(listener::onAlreadyFavorite);
                        }else{
                            getMainHandler().post(listener::onFailure);
                        }
                    }catch (Exception e){
                        Timber.w(e);
                    }
                }
                response.close();
            }
        };

        Request request = new Request.Builder()
                .url(baseUrl + "music/" + item.getItemId() + "/favorite")
                .delete()
                .build();
        Call call = client.newCall(request);
        call.timeout().timeout(SHORT_TIMEOUT, TimeUnit.SECONDS);
        call.enqueue(callback);
    }

    // The boolean is true if item was reposted
    public Observable<Boolean> repost(AMResultItem item, @NonNull String mixpanelPage) {
        item.setRepostStatus(AMResultItem.ItemAPIStatus.Loading);
        return Observable.create(emitter -> {
            try {
                RequestBody body = new FormBody.Builder()
                        .add("section", mixpanelPage)
                        .build();

                Request request = new Request.Builder()
                        .url(baseUrl + "music/" + item.getItemId() + "/repost")
                        .put(body)
                        .build();
                Call call = client.newCall(request);
                call.timeout().timeout(SHORT_TIMEOUT, TimeUnit.SECONDS);

                Response response = call.execute();
                if (response.code() == 204) {
                    UserData.INSTANCE.addItemToReups(item.getItemId());
                    item.setRepostStatus(AMResultItem.ItemAPIStatus.On);
                    emitter.onNext(true);
                    emitter.onComplete();
                } else {
                    try (ResponseBody responseBody = response.body()) {
                        JSONObject jsonObject = new JSONObject(responseBody.string());
                        if (jsonObject.optInt("errorcode") == ErrorCodes.ALREADY_FAVORITED) {
                            UserData.INSTANCE.addItemToReups(item.getItemId());
                            item.setRepostStatus(AMResultItem.ItemAPIStatus.On);
                            emitter.onNext(true);
                            emitter.onComplete();
                        } else {
                            item.setRepostStatus(AMResultItem.ItemAPIStatus.Off);
                            emitter.onNext(false);
                            emitter.onComplete();
                        }
                    }
                }
                response.close();
            } catch (Exception e) {
                Timber.w(e);
                item.setRepostStatus(AMResultItem.ItemAPIStatus.Off);
                emitter.onNext(false);
                emitter.onComplete();
            }
        });
    }

    // The boolean is true if item was unreposted
    public Observable<Boolean> unrepost(AMResultItem item) {
        item.setRepostStatus(AMResultItem.ItemAPIStatus.Loading);
        return Observable.create(emitter -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "music/" + item.getItemId() + "/repost")
                        .delete()
                        .build();
                Call call = client.newCall(request);
                call.timeout().timeout(SHORT_TIMEOUT, TimeUnit.SECONDS);

                Response response = call.execute();
                if (response.code() == 204) {
                    UserData.INSTANCE.removeItemFromReups(item.getItemId());
                    item.setRepostStatus(AMResultItem.ItemAPIStatus.Off);
                    emitter.onNext(false);
                    emitter.onComplete();
                } else {
                    try (ResponseBody responseBody = response.body()) {
                        JSONObject jsonObject = new JSONObject(responseBody.string());
                        if (jsonObject.optInt("errorcode") == ErrorCodes.ALREADY_FAVORITED) {
                            UserData.INSTANCE.removeItemFromReups(item.getItemId());
                            item.setRepostStatus(AMResultItem.ItemAPIStatus.Off);
                            emitter.onNext(false);
                            emitter.onComplete();
                        } else {
                            item.setRepostStatus(AMResultItem.ItemAPIStatus.On);
                            emitter.onNext(true);
                            emitter.onComplete();
                        }
                    }
                }
                response.close();
            } catch (Exception e) {
                Timber.w(e);
                item.setRepostStatus(AMResultItem.ItemAPIStatus.On);
                emitter.onNext(true);
                emitter.onComplete();
            }
        });
    }

    public Observable<String> getStreamURLWithSession(String musicId, boolean skipSession, @NonNull String mixpanelPage, @NonNull String extraKey) {
        String deviceId;
        if (Credentials.isLogged(MainApplication.getContext())) {
            deviceId = Credentials.load(MainApplication.getContext()).getDeviceId();
        } else {
            deviceId = Credentials.generateDeviceId(MainApplication.getContext());
        }
        return getStreamURL(MainApplication.getContext(), null, null, musicId, deviceId, skipSession, mixpanelPage, extraKey);
    }

    public Observable<String> getStreamURLForAlbumWithSession(String albumId, String musicId, boolean skipSession, @NonNull String mixpanelPage, @NonNull String extraKey) {
        String deviceId;
        if (Credentials.isLogged(MainApplication.getContext())) {
            deviceId = Credentials.load(MainApplication.getContext()).getDeviceId();
        } else {
            deviceId = Credentials.generateDeviceId(MainApplication.getContext());
        }
        return getStreamURL(MainApplication.getContext(), albumId, null, musicId, deviceId, skipSession, mixpanelPage, extraKey);
    }

    public Observable<String> getStreamURLForPlaylistWithSession(String playlistId, String musicId, boolean skipSession, @NonNull String mixpanelPage, @NonNull String extraKey) {
        String deviceId;
        if (Credentials.isLogged(MainApplication.getContext())) {
            deviceId = Credentials.load(MainApplication.getContext()).getDeviceId();
        } else {
            deviceId = Credentials.generateDeviceId(MainApplication.getContext());
        }
        return getStreamURL(MainApplication.getContext(), null, playlistId, musicId, deviceId, skipSession, mixpanelPage, extraKey);
    }

    public Observable<AMResultItem> createPlaylist(String title, String genre, String desc, boolean privatePlaylist, String musicId, @Nullable String imageBase64, @Nullable String bannerImageBase64, @NonNull String mixpanelPage) {
        return Observable.create(emitter -> {

            FormBody.Builder bodyBuilder = new FormBody.Builder()
                    .add("title", (title != null) ? title : "")
                    .add("genre", (genre != null) ? genre : "")
                    .add("description", (desc != null) ? desc : "")
                    .add("private", privatePlaylist ? "yes" : "no")
                    .add("section", mixpanelPage);
            if (musicId != null) {
                bodyBuilder.add("music_id", musicId);
            }
            if (imageBase64 != null) {
                bodyBuilder.add("image", imageBase64);
            }
            if (bannerImageBase64 != null) {
                bodyBuilder.add("image_banner", bannerImageBase64);
            }

            Request request = new Request.Builder()
                    .url(baseUrl + "playlist")
                    .post(bodyBuilder.build())
                    .build();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                try (ResponseBody responseBody = response.body()) {
                    JSONObject jsonObject = new JSONObject(responseBody.string());
                    emitter.onNext(AMResultItem.fromJson(jsonObject, false, null));
                } catch (Exception e) {
                    emitter.tryOnError(e);
                }
            } else {
                emitter.tryOnError(genericThrowable);
            }
            emitter.onComplete();
            response.close();
        });
    }

    public Observable<AMResultItem> editPlaylist(String playlistId, String title, String genre, String desc, boolean privatePlaylist, String musicId, @Nullable String imageBase64, @Nullable String bannerImageBase64) {
        return Observable.create(emitter -> {

            FormBody.Builder bodyBuilder = new FormBody.Builder()
                    .add("title", (title != null) ? title : "")
                    .add("genre", genre)
                    .add("description", (desc != null) ? desc : "")
                    .add("private", privatePlaylist ? "yes" : "no");
            if (musicId != null) {
                bodyBuilder.add("music_id", musicId);
            }
            if (imageBase64 != null) {
                bodyBuilder.add("image", imageBase64);
            }
            if (bannerImageBase64 != null) {
                bodyBuilder.add("image_banner", bannerImageBase64);
            }

            Request request = new Request.Builder()
                    .url(baseUrl + "playlist/" + playlistId)
                    .put(bodyBuilder.build())
                    .build();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                try (ResponseBody responseBody = response.body()) {
                    JSONObject jsonObject = new JSONObject(responseBody.string());
                    emitter.onNext(AMResultItem.fromJson(jsonObject, false, null));
                } catch (Exception e) {
                    emitter.tryOnError(e);
                }
            } else {
                emitter.tryOnError(genericThrowable);
            }
            emitter.onComplete();
            response.close();
        });
    }

    public Observable<Boolean> deletePlaylist(String playlistId) {
        return Observable.create(emitter -> {

            Request request = new Request.Builder()
                    .url(baseUrl + "playlist/" + playlistId)
                    .delete()
                    .build();
            Response response = client.newCall(request).execute();

            if (response.code() == 204) {
                emitter.onNext(true);
            } else {
                emitter.tryOnError(genericThrowable);
            }
            emitter.onComplete();
            response.close();
        });
    }

    public Observable<AMResultItem> getPlaylistInfo(String playlistId) {
        return Observable.create(emitter -> {
            Callback callback = new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    emitter.tryOnError(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NotNull Response response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        final AMResultItem item = AMResultItem.fromJson(jsonObject.getJSONObject("results"), false, null);
                        if (item != null && !item.isTakenDown()) {
                            emitter.onNext(item);
                            emitter.onComplete();
                        } else {
                            emitter.tryOnError(new APIException(response.code()));
                        }
                    } catch (Exception e) {
                        emitter.tryOnError(new APIException(response.code(), e));
                    } finally {
                        response.close();
                    }
                }
            };

            Request request = new Request.Builder()
                    .url(baseUrl + "playlist/" + playlistId)
                    .get()
                    .build();

            Call call = client.newCall(request);
            call.enqueue(callback);

            emitter.setCancellable(call::cancel);
        });
    }

    public void favoritePlaylist(String playlistId, final @NonNull FavoriteListener listener, @NonNull String mixpanelPage) {

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getMainHandler().post(listener::onFailure);
            }

            @Override
            public void onResponse(@NonNull Call call, Response response) {
                if (response.code() == 204) {
                    getMainHandler().post(listener::onSuccess);
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        if (jsonObject.optInt("errorcode") == ErrorCodes.ALREADY_FAVORITED) {
                            getMainHandler().post(listener::onAlreadyFavorite);
                        } else {
                            getMainHandler().post(listener::onFailure);
                        }
                    } catch (Exception e) {
                        Timber.w(e);
                    }
                }
                response.close();
            }
        };

        RequestBody body = new FormBody.Builder()
                .add("section", mixpanelPage)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "playlist/" + playlistId + "/favorite")
                .put(body)
                .build();
        Call call = client.newCall(request);
        call.timeout().timeout(SHORT_TIMEOUT, TimeUnit.SECONDS);
        call.enqueue(callback);
    }

    public void unfavoritePlaylist(String playlistId, final @NonNull FavoriteListener listener) {

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getMainHandler().post(listener::onFailure);
            }

            @Override
            public void onResponse(@NonNull Call call, Response response) {
                if (response.code() == 204) {
                    getMainHandler().post(listener::onSuccess);
                }else{
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        if(jsonObject.optInt("errorcode") == ErrorCodes.ALREADY_UNFAVORITED){
                            getMainHandler().post(listener::onAlreadyFavorite);
                        }else{
                            getMainHandler().post(listener::onFailure);
                        }
                    }catch (Exception e){
                        Timber.w(e);
                    }
                }
                response.close();
            }
        };

        Request request = new Request.Builder()
                .url(baseUrl + "playlist/" + playlistId + "/favorite")
                .delete()
                .build();
        Call call = client.newCall(request);
        call.timeout().timeout(SHORT_TIMEOUT, TimeUnit.SECONDS);
        call.enqueue(callback);
    }

    @NotNull public Observable<List<String>> searchAutoSuggest(@NonNull String query) {
        return Observable.create(emitter -> {
            String encodedQuery = query;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch (Exception e) {
                Timber.w(e);
                emitter.tryOnError(e);
            }
            Request request = new Request.Builder()
                    .url(baseUrl + "search_autosuggest?q=" + encodedQuery)
                    .get()
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    emitter.tryOnError(genericThrowable);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try {
                        ResponseBody responseBody = response.body();
                        JSONObject jsonObject = new JSONObject(responseBody.string());
                        List<String> suggestions = new ArrayList<>();
                        JSONArray resultsArray = jsonObject.getJSONArray("results");
                        for (int i = 0; i < resultsArray.length(); i++) {
                            suggestions.add(resultsArray.getString(i));
                        }
                        emitter.onNext(suggestions);
                        emitter.onComplete();
                    } catch (Exception e) {
                        Timber.w(e);
                        emitter.tryOnError(e);
                    } finally {
                        response.close();
                    }
                }
            });
            emitter.setCancellable(call::cancel);
        });
    }

    public void followArtist(String artistSlug, @NonNull final FollowListener listener) {

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getMainHandler().post(listener::onFailure);
            }

            @Override
            public void onResponse(@NonNull Call call, Response response) {
                if (response.code() == 204) {
                    getMainHandler().post(listener::onSuccess);
                } else {
                    getMainHandler().post(listener::onFailure);
                }
                response.close();
            }
        };

        Request request = new Request.Builder()
                .url(baseUrl + "artist/" + artistSlug + "/follow")
                .put(new FormBody.Builder().build())
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void unfollowArtist(String artistSlug, @NonNull final FollowListener listener) {

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getMainHandler().post(listener::onFailure);
            }

            @Override
            public void onResponse(@NonNull Call call, Response response) {
                if (response.code() == 204) {
                    getMainHandler().post(listener::onSuccess);
                } else {
                    getMainHandler().post(listener::onFailure);
                }
                response.close();
            }
        };

        Request request = new Request.Builder()
                .url(baseUrl + "artist/" + artistSlug + "/follow")
                .delete(new FormBody.Builder().build())
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void trackMonetizedPlay(String musicId, @NonNull String mixpanelPage) {

        String deviceId;
        if (Credentials.isLogged(MainApplication.getContext())) {
            deviceId = Credentials.load(MainApplication.getContext()).getDeviceId();
        } else {
            deviceId = Credentials.generateDeviceId(MainApplication.getContext());
        }

        RequestBody body = new FormBody.Builder()
                .add("time", Integer.toString(ConstantsKt.SONG_MONETIZATION_SECONDS))
                .add("session", deviceId)
                .add("section", mixpanelPage)
                .build();

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, Response response) {
                response.close();
            }
        };

        Request request = new Request.Builder()
                .url(baseUrl + "music/" + musicId + "/play")
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public APIRequestData getUserNotifications(@Nullable String pagingToken, final boolean aggregated) {
        Observable<APIResponseData> observable = Observable.create(emitter -> {
            Request request = new Request.Builder()
                    .url(baseUrl + "user/native-notifications" + (!aggregated ? "?only_unseen=1" : (pagingToken != null ? "?paging_token=" + pagingToken : "")))
                    .get()
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    emitter.tryOnError(genericThrowable);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try {
                        ResponseBody responseBody = response.body();

                        List<AMNotification> notifications = new ArrayList<>();
                        String resultPpagingToken = null;

                        if (aggregated) {

                            JSONObject jsonObject = new JSONObject(responseBody.string());
                            JSONArray results = jsonObject.optJSONArray("results");
                            resultPpagingToken = jsonObject.optString("paging_token");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject jsonObj = results.optJSONObject(i);
                                AMNotification notification = AMNotification.fromJSON(jsonObj);
                                if (notification != null) {
                                    notifications.add(notification);
                                }
                            }
                            AMArtist savedArtist = AMArtist.getSavedArtist();
                            if (savedArtist != null) {
                                savedArtist.setUnseenNotificationsCount(0);
                                savedArtist.save();
                            }

                        } else {

                            JSONObject jsonObject = new JSONObject(responseBody.string()).optJSONObject("counters");
                            if (jsonObject != null) {
                                int count = jsonObject.optInt("unseen");
                                AMArtist savedArtist = AMArtist.getSavedArtist();
                                if (savedArtist != null) {
                                    savedArtist.setUnseenNotificationsCount(count);
                                    savedArtist.save();
                                }
                            }

                        }

                        if (aggregated) {
                            markNotificationsAsSeen();
                        }

                        emitter.onNext(new APIResponseData(notifications, resultPpagingToken));
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.tryOnError(e);
                    } finally {
                        response.close();
                    }
                }
            });
            emitter.setCancellable(call::cancel);
        });
        return new APIRequestData(observable, null);
    }

    private void markNotificationsAsSeen() {

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.close();
            }
        };

        FormBody.Builder bodyBuilder = new FormBody.Builder()
                .add("for_all", Boolean.toString(true));

        Request request = new Request.Builder()
                .url(baseUrl + "user/native-notifications/seen")
                .post(bodyBuilder.build())
                .build();
        client.newCall(request).enqueue(callback);
    }

    public Observable<AMResultItem> getSongInfo(String itemId) {
        String url;
        if (itemId.contains("/")) {
            url = baseUrl + "music/song/" + itemId;
        } else {
            url = baseUrl + "music/" + itemId;
        }
        return getInfo(url);
    }

    public Observable<AMResultItem> getSongInfo(AMResultItem item) {
        String url = baseUrl + "music/song/" + item.getUploaderSlug() + "/" + item.getUrlSlug();
        return getInfo(url);
    }

    public Observable<AMResultItem> getAlbumInfo(String itemId) {
        String url;
        if (itemId.contains("/")) {
            url = baseUrl + "music/album/" + itemId;
        } else {
            url = baseUrl + "music/" + itemId;
        }
        return getInfo(url);
    }

    public Observable<AMResultItem> getAlbumInfo(AMResultItem album) {
        String url = baseUrl + "music/album/" + album.getUploaderSlug() + "/" + album.getUrlSlug();
        return getInfo(url);
    }

    private Observable<AMResultItem> getInfo(String url) {

        String extraKey = Uri.parse(url).getEncodedQuery();

        return Observable.create(emitter -> {
            Callback callback = new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    emitter.tryOnError(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NotNull Response response) {
                    ResponseBody responseBody = response.body();
                    try {
                        if (response.isSuccessful()) {
                            JSONObject jsonObject = new JSONObject(responseBody.string());

                            AMResultItem item = AMResultItem.fromJson(jsonObject.optJSONObject("results"), true, extraKey);
                            if (item == null || item.isTakenDown()) {
                                emitter.tryOnError(new APIException(response.code()));
                            } else {
                                emitter.onNext(item);
                            }
                        } else {
                            emitter.tryOnError(new APIException(response.code()));
                        }
                    } catch (Exception e) {
                        Timber.w(e);
                        emitter.tryOnError(e);
                    } finally {
                        response.close();
                    }
                }
            };

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Call call = client.newCall(request);
            call.enqueue(callback);

            emitter.setCancellable(call::cancel);
        });
    }

    private Observable<String> getStreamURL(Context context,
                                            String albumId,
                                            String playlistId,
                                            String musicId,
                                            String deviceId,
                                            boolean skipSession,
                                            @NonNull String mixpanelPage,
                                            @Nullable String extraKey) {
        return Observable.create(emitter -> {
            Callback callback = new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    emitter.tryOnError(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NotNull Response response) {
                    try {
                        if (response.isSuccessful()) {
                            final String streamUrl = Utils.INSTANCE.deslash(response.body().string().replaceAll("\"", ""));
                            emitter.onNext(streamUrl);
                        } else {
                            emitter.tryOnError(new APIException(response.code()));
                        }
                    } catch (Exception e) {
                        emitter.tryOnError(e);
                    } finally {
                        response.close();
                    }
                }
            };

            FormBody.Builder bodyBuilder = new FormBody.Builder()
                    .add("section", mixpanelPage);
            if (!skipSession) {
                bodyBuilder.add("session", (deviceId != null) ? deviceId : "");
            }
            if (albumId != null) {
                bodyBuilder.add("album_id", albumId);
            }
            if (playlistId != null) {
                bodyBuilder.add("playlist_id", playlistId);
            }
            if (context != null && PremiumRepository.getInstance().isPremium() &&
                    Reachability.getInstance().getConnectedToWiFi()) {
                bodyBuilder.add("hq", "1");
            }

            String url = baseUrl + "music/" + musicId + "/play" + (TextUtils.isEmpty(extraKey) ? "" : ("?" + extraKey));

            Request request = new Request.Builder()
                    .url(url)
                    .post(bodyBuilder.build())
                    .build();

            Call call = client.newCall(request);
            call.enqueue(callback);

            emitter.setCancellable(call::cancel);
        });
    }

    private Observable<APIResponseData> getArtists(String url, @Nullable String subentityName) {
        return Observable.create(emitter -> {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    emitter.tryOnError(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try {
                        ResponseBody responseBody = response.body();
                        List<AMArtist> artists = new ArrayList<>();
                        JSONArray artistsArray;
                        String pagingToken = null;
                        if (subentityName != null) {
                            JSONObject jsonObject = new JSONObject(responseBody.string());
                            artistsArray = jsonObject.getJSONArray(subentityName);
                            pagingToken = jsonObject.optString("paging_token", null);
                        } else {
                            artistsArray = new JSONArray(responseBody.string());
                        }
                        for (int i = 0; i < artistsArray.length(); i++) {
                            AMArtist artist = AMArtist.fromJSON(false, artistsArray.optJSONObject(i));
                            artists.add(artist);
                        }
                        emitter.onNext(new APIResponseData(artists, pagingToken));
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.tryOnError(e);
                    } finally {
                        response.close();
                    }
                }
            });
            emitter.setCancellable(call::cancel);
        });
    }

    private Observable<APIResponseData> getMusicAsObservable(String url, String subentityName, boolean ignoreGeorestrictedMusic) {
        return Observable.create(emitter -> {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    emitter.tryOnError(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            ResponseBody responseBody = response.body();

                            JSONObject jsonObject = new JSONObject(responseBody.string());
                            List<Object> items = new ArrayList<>();

                            String pagingToken = jsonObject.has("paging_token") ? jsonObject.optString("paging_token") : null;

                            JSONObject verifiedArtistJson = jsonObject.optJSONObject("verified_artist");
                            AMArtist verifiedArtist = verifiedArtistJson != null ? AMArtist.fromJSON(false, verifiedArtistJson) : null;
                            if (verifiedArtist != null) {
                                verifiedArtist.setHighlightedSearchResult(true);
                                items.add(verifiedArtist);
                            }

                            JSONObject tastemakerArtistJson = jsonObject.optJSONObject("tastemaker_artist");
                            AMArtist tastemakerArtist = tastemakerArtistJson != null ? AMArtist.fromJSON(false, tastemakerArtistJson) : null;
                            if (tastemakerArtist != null) {
                                tastemakerArtist.setHighlightedSearchResult(true);
                                items.add(tastemakerArtist);
                            }

                            JSONObject authenticatedArtistJson = jsonObject.optJSONObject("authenticated_artist");
                            AMArtist authenticatedArtist = authenticatedArtistJson != null ? AMArtist.fromJSON(false, authenticatedArtistJson) : null;
                            if (authenticatedArtist != null) {
                                authenticatedArtist.setHighlightedSearchResult(true);
                                items.add(authenticatedArtist);
                            }

                            JSONObject verifiedPlaylistJson = jsonObject.optJSONObject("verified_playlist");
                            AMResultItem verifiedPlaylist = verifiedPlaylistJson != null ? AMResultItem.fromJson(verifiedPlaylistJson, true, null) : null;
                            if (verifiedPlaylist != null) {
                                verifiedPlaylist.setVerifiedSearchResult(true);
                                items.add(verifiedPlaylist);
                            }

                            JSONObject tastemakerPlaylistJson = jsonObject.optJSONObject("tastemaker_playlist");
                            AMResultItem tastemakerPlaylist = tastemakerPlaylistJson != null ? AMResultItem.fromJson(tastemakerPlaylistJson, true, null) : null;
                            if (tastemakerPlaylist != null) {
                                tastemakerPlaylist.setVerifiedSearchResult(true);
                                items.add(tastemakerPlaylist);
                            }

                            boolean related = jsonObject.optBoolean("related");

                            JSONArray resultsArray;
                            if (subentityName == null) {
                                resultsArray = jsonObject.optJSONArray("results");
                            } else {
                                resultsArray = jsonObject.optJSONObject("results").optJSONArray(subentityName);
                            }
                            if (resultsArray != null) {
                                for (int i = 0; i < resultsArray.length(); i++) {
                                    try {
                                        AMResultItem item = AMResultItem.fromJson(resultsArray.optJSONObject(i), ignoreGeorestrictedMusic, null);
                                        if (item != null) {
                                            items.add(item);
                                        }
                                    } catch (Exception e) {
                                        Timber.w(e);
                                    }
                                }
                            }
                            emitter.onNext(new APIResponseData(items, pagingToken, false, related));
                            emitter.onComplete();
                        } else {
                            emitter.tryOnError(new APIException(response.code()));
                        }
                    } catch (Exception e) {
                        emitter.tryOnError(e);
                    } finally {
                        response.close();
                    }
                }
            });
            emitter.setCancellable(call::cancel);
        });
    }

    public void checkMusicAvailability(@NonNull List<String> itemIDs, @Nullable final ArrayListener<String> listener) {

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (listener != null) {
                    getMainHandler().post(listener::onFailure);
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NotNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    JSONArray array = new JSONArray(responseBody.string());
                    final List<String> deletedItemIDs = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        deletedItemIDs.add(array.optString(i));
                    }
                    if (listener != null) {
                        getMainHandler().post(() -> listener.onSuccess(deletedItemIDs));
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        getMainHandler().post(listener::onFailure);
                    }
                }
                response.close();
            }
        };

        FormBody.Builder bodyBuilder = new FormBody.Builder().add("music_id", TextUtils.join(",", itemIDs.toArray(new String[]{})));

        Request request = new Request.Builder()
                .url(baseUrl + "music/status")
                .post(bodyBuilder.build())
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void forgotPassword(@NonNull final String email, @NonNull final ForgotPasswordListener listener) {

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getMainHandler().post(() -> listener.onFailure("", false));
            }

            @Override
            public void onResponse(@NonNull Call call, Response response) {
                if (response.isSuccessful()) {
                    getMainHandler().post(listener::onSuccess);
                } else {
                    ResponseBody responseBody = response.body();

                    String errorMessage = null;
                    boolean emailNotFound = false;
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody.string());
                        errorMessage = jsonObject.optString("message");
                        JSONObject errorsObject = jsonObject.optJSONObject("errors");
                        for (Iterator<String> keysIterator = errorsObject.keys(); keysIterator.hasNext(); ) {
                            JSONObject errorObject = errorsObject.optJSONObject(keysIterator.next());
                            for (Iterator<String> errorKeysIterator = errorObject.keys(); errorKeysIterator.hasNext(); ) {
                                String key = errorKeysIterator.next();
                                if ("keyNotFound".equals(key)) {
                                    emailNotFound = true;
                                }
                                errorMessage = errorObject.optString(key);
                                break;
                            }
                            break;
                        }
                    } catch (Exception ee) {
                        Timber.w(ee);
                    }

                    final String errorMsg = errorMessage;
                    final boolean emailNotFoundFinal = emailNotFound;
                    getMainHandler().post(() -> listener.onFailure(errorMsg, emailNotFoundFinal));
                }
                response.close();
            }
        };

        FormBody.Builder bodyBuilder = new FormBody.Builder().add("email", email);

        Request request = new Request.Builder()
                .url(baseUrl + "user/forgot-password")
                .post(bodyBuilder.build())
                .build();
        client.newCall(request).enqueue(callback);
    }
    
    public Observable<List<AMResultItem>> getHighlights(String userSlug, boolean myAccount) {
        return Observable.create(emitter -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "artist/" + userSlug + "/pinned")
                        .get()
                        .build();

                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        emitter.tryOnError(e);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        try {
                            ResponseBody responseBody = response.body();
                            List<AMResultItem> items = new ArrayList<>();
                            JSONArray resultsArray = new JSONArray(responseBody.string());
                            for (int i = 0; i < resultsArray.length(); i++) {
                                try {
                                    AMResultItem item = AMResultItem.fromJson(resultsArray.optJSONObject(i), !myAccount, null);
                                    if (item != null) {
                                        items.add(item);
                                    }
                                } catch (Exception e) {
                                    Timber.w(e);
                                }
                            }
                            if (myAccount) {
                                UserData.INSTANCE.setHighlights(items);
                            }
                            emitter.onNext(items);
                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.tryOnError(e);
                        } finally {
                            response.close();
                        }
                    }
                });
                emitter.setCancellable(call::cancel);
            } catch (Exception e) {
                Timber.w(e);
                emitter.onNext(Collections.emptyList());
                emitter.onComplete();
            }
        });
    }

    public Observable<Boolean> addHighlight(@NonNull AMResultItem music) {
        return Observable.create(emitter -> {
            try {
                String userSlug = Credentials.load(MainApplication.getContext()).getUserUrlSlug();

                JSONArray entities = new JSONArray();
                JSONObject musicJsonObject = new JSONObject();
                musicJsonObject.put("kind", music.getTypeForHighlightingAPI());
                musicJsonObject.put("id", music.getItemId());
                entities.put(musicJsonObject);

                RequestBody body = new FormBody.Builder()
                        .add("entities", entities.toString())
                        .build();

                Request request = new Request.Builder()
                        .url(baseUrl + "artist/" + userSlug + "/pinned")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    emitter.onNext(true);
                    emitter.onComplete();
                    response.close();
                    return;
                }
                response.close();
            } catch (Exception e) {
                Timber.w(e);
            }
            emitter.onNext(false);
            emitter.onComplete();
        });
    }

    public Observable<Boolean> removeHighlight(@NonNull AMResultItem music) {
        return Observable.create(emitter -> {
            try {
                String userSlug = Credentials.load(MainApplication.getContext()).getUserUrlSlug();

                JSONArray entities = new JSONArray();
                JSONObject musicJsonObject = new JSONObject();
                musicJsonObject.put("kind", music.getTypeForHighlightingAPI());
                musicJsonObject.put("id", music.getItemId());
                entities.put(musicJsonObject);

                Request request = new Request.Builder()
                        .url(baseUrl + "artist/" + userSlug + "/pinned?entities=" + URLEncoder.encode(entities.toString(), "UTF-8"))
                        .delete()
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    emitter.onNext(true);
                    emitter.onComplete();
                    response.close();
                    return;
                }
                response.close();
            } catch (Exception e) {
                Timber.w(e);
            }
            emitter.onNext(false);
            emitter.onComplete();
        });
    }

    public Observable<List<AMResultItem>> reorderHighlights(@NonNull List<AMResultItem> musicList) {
        return Observable.create(emitter -> {
            try {
                String userSlug = Credentials.load(MainApplication.getContext()).getUserUrlSlug();

                JSONArray entities = new JSONArray();
                for (AMResultItem music : musicList) {
                    JSONObject musicJsonObject = new JSONObject();
                    musicJsonObject.put("kind", music.getTypeForHighlightingAPI());
                    musicJsonObject.put("id", music.getItemId());
                    musicJsonObject.put("position", entities.length());
                    entities.put(musicJsonObject);
                }

                RequestBody body = new FormBody.Builder()
                        .add("entities", entities.toString())
                        .build();

                Request request = new Request.Builder()
                        .url(baseUrl + "artist/" + userSlug + "/pinned")
                        .put(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    List<AMResultItem> items = new ArrayList<>();
                    JSONArray resultsArray = new JSONArray(responseBody.string());
                    for (int i = 0; i < resultsArray.length(); i++) {
                        try {
                            AMResultItem item = AMResultItem.fromJson(resultsArray.optJSONObject(i), false, null);
                            if (item != null) {
                                items.add(item);
                            }
                        } catch (Exception e) {
                            Timber.w(e);
                        }
                    }
                    responseBody.close();
                    UserData.INSTANCE.setHighlights(items);
                    emitter.onNext(items);
                    emitter.onComplete();
                    response.close();
                    return;
                }
                response.close();

            } catch (Exception e) {
                Timber.w(e);
            }
            emitter.onNext(Collections.emptyList());
            emitter.onComplete();
        });
    }
}
