package com.audiomack.network;

import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import com.audiomack.data.premium.PremiumRepository;
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository;
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource;
import com.audiomack.data.tracking.mixpanel.MixpanelRepository;
import com.audiomack.data.user.UserDataSource;
import com.audiomack.data.user.UserRepository;
import com.audiomack.onesignal.OneSignalDataSource;
import com.audiomack.onesignal.OneSignalRepository;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;
import com.mopub.mobileads.MopubAdapterNameManager;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.embrace.android.embracesdk.Embrace;
import timber.log.Timber;

public class AnalyticsHelper {

    private static final String TAG = "AnalyticsHelper";

    public interface IdentityListener {
        void onTrackIdentity();
    }

    private static AnalyticsHelper instance;
    private final MixpanelDataSource mixpanelRepository = new MixpanelRepository();
    private final OneSignalDataSource oneSignalRepository = OneSignalRepository.getInstance();

    public static AnalyticsHelper getInstance(){
        if(instance == null){
            instance = new AnalyticsHelper();
        }
        return instance;
    }

    private FirebaseAnalytics firebaseAnalytics;
    private AppEventsLogger facebookEventsLogger;
    private Embrace embrace;
    private final String REGEX = "[^a-zA-Z0-9_]";

    private IdentityListener identityListener;

    public void init(@NonNull Application context){
        this.firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        if(FacebookSdk.isInitialized()) {
            this.facebookEventsLogger = AppEventsLogger.newLogger(context);
        }
        embrace = Embrace.getInstance();
    }

    public void trackScreen(String screen){

        if(!TextUtils.isEmpty(screen)) {
            screen = screen.replaceAll(REGEX, "");
        }
        Timber.tag(AnalyticsHelper.class.getSimpleName()).d("Screen: " + screen);
        firebaseAnalytics.logEvent(screen, null);
        trackBreadcrumb(screen);
    }

    public void trackEvent(String category, @Nullable String action, @Nullable String label){

        if(!TextUtils.isEmpty(category)){
            category = category.replaceAll(REGEX, "");
        }
        Bundle params = new Bundle();
        params.putString("Action", (action!=null)?action:"");
        params.putString("Label", (label!=null)?label:"");
        Timber.tag(AnalyticsHelper.class.getSimpleName()).d("Event: " + category + " - Params: " + params);
        firebaseAnalytics.logEvent(category, params);
    }

    public void trackEventOnAllProviders(@NonNull String eventName, @Nullable HashMap<String, String> parameters){
        trackEventOnFirebase(eventName, parameters);
        trackEventOnFacebook(eventName, parameters);
    }

    public void trackEventOnFirebase(@NonNull String eventName, @Nullable HashMap<String, String> parameters){
        Timber.tag(AnalyticsHelper.class.getSimpleName()).d("trackEventOnFirebase > Event: " + eventName + " - Params: " + parameters);

        Bundle params = new Bundle();
        if(parameters != null) {
            for(String key : parameters.keySet()){
                params.putString(key, parameters.get(key) != null ? parameters.get(key) : "");
            }
        }

        firebaseAnalytics.logEvent(eventName, params);
    }

    public void trackEventOnFacebook(@NonNull String eventName, @Nullable HashMap<String, String> parameters){
        Timber.tag(AnalyticsHelper.class.getSimpleName()).d("trackEventOnFacebook > Event: " + eventName + " - Params: " + parameters);

        Bundle params = new Bundle();
        if(parameters != null) {
            for(String key : parameters.keySet()){
                params.putString(key, parameters.get(key) != null ? parameters.get(key) : "");
            }
        }

        if(facebookEventsLogger != null) {
            facebookEventsLogger.logEvent(eventName, params);
        }
    }

    public void setIdentityListener(IdentityListener identityListener) {
        this.identityListener = identityListener;
    }

    public void trackIdentity(){
        UserDataSource userDataSource = UserRepository.getInstance();

        if(userDataSource.isLoggedIn()) {

            mixpanelRepository.trackIdentity(userDataSource, PremiumRepository.getInstance());
            new AppsFlyerRepository().trackIdentity(userDataSource);

            String userId = userDataSource.getUserId();
            String artistId = userDataSource.getArtistId();

            if (userId != null) {
                FirebaseCrashlytics.getInstance().setUserId(userId);
            }

            firebaseAnalytics.setUserProperty("UserID", userId);

            oneSignalRepository.setExternalUserId(artistId != null ? artistId : "");

            embrace.setUserIdentifier(userId);
            embrace.setUsername(userDataSource.getUserSlug());
            embrace.setUserEmail(userDataSource.getEmail());

            if (userDataSource.isTester()) {
                embrace.setUserPersona("tester");
            } else if (userDataSource.isAdmin()) {
                embrace.setUserPersona("admin");
            } else if (userDataSource.isContentCreator()) {
                embrace.setUserPersona("content_creator");
            }

            identityListener.onTrackIdentity();
        }
    }

    public void trackException(Throwable throwable) {
        Timber.e(throwable);
        FirebaseCrashlytics.getInstance().recordException(throwable);
        embrace.logError(throwable);
    }

    public void trackBreadcrumb(String message) {
        Timber.tag(TAG).i(message);
        embrace.logBreadcrumb(message);
    }

    public void trackLogin() {
        embrace.setUserPersona("logged_in");
        embrace.logBreadcrumb("User logged in");
    }

    public void trackLogout() {
        oneSignalRepository.removeExternalUserId();
        mixpanelRepository.trackLogout();
        embrace.clearUserPersona("logged_in");
        embrace.logBreadcrumb("User logged out");
    }

    public void trackSignup() {
        embrace.clearUserPersona("new_user");
        embrace.logBreadcrumb("User signed up");
    }

    public void log(@NonNull String msg, @Nullable Map<String, Object> props) {
        FirebaseCrashlytics.getInstance().recordException(new Exception(msg));
        embrace.logInfo(msg, props);
    }

    public void trackFirstSession() {
        firebaseAnalytics.setUserProperty("first_session_date", Long.toString(new Date().getTime() / 1000L));
    }

    public static class TrackingBannerAdListener implements MoPubView.BannerAdListener {

        private final AnalyticsHelper analytics;
        private MopubAdapterNameManager mopubAdapterNameManager;

        public TrackingBannerAdListener() {
            analytics = AnalyticsHelper.getInstance();
            mopubAdapterNameManager = MopubAdapterNameManager.INSTANCE;
        }

        public TrackingBannerAdListener(AnalyticsHelper analytics) {
            this.analytics = analytics;
            mopubAdapterNameManager = MopubAdapterNameManager.INSTANCE;
        }

        public TrackingBannerAdListener(AnalyticsHelper analytics, MopubAdapterNameManager mopubAdapterNameManager) {
            this.analytics = analytics;
            mopubAdapterNameManager = MopubAdapterNameManager.INSTANCE;
        }

        @Override
        public void onBannerLoaded(MoPubView moPubView) {
            String breadCrumb = buildMessage("onBannerLoaded", moPubView);
            analytics.trackBreadcrumb(breadCrumb);
        }

        @Override
        public void onBannerFailed(MoPubView moPubView, MoPubErrorCode moPubErrorCode) {
            String breadCrumb = buildMessage("onBannerFailed", moPubView);
            analytics.trackBreadcrumb(breadCrumb);
        }

        @Override
        public void onBannerClicked(MoPubView moPubView) {
            String breadCrumb = buildMessage("onBannerClicked", moPubView);
            analytics.trackBreadcrumb(breadCrumb);
        }

        @Override
        public void onBannerExpanded(MoPubView moPubView) {
            String breadCrumb = buildMessage("onBannerExpanded", moPubView);
            analytics.trackBreadcrumb(breadCrumb);
        }

        @Override
        public void onBannerCollapsed(MoPubView moPubView) {
            String breadCrumb = buildMessage("onBannerCollapsed", moPubView);
            analytics.trackBreadcrumb(breadCrumb);
        }

        private String buildMessage(String callbackName, MoPubView moPubView) {
            StringBuilder builder = new StringBuilder(callbackName);

            String adapterName = mopubAdapterNameManager.getAdapterNameFromBanner(moPubView);
            if (TextUtils.isEmpty(adapterName)) {
                builder.append(" - ");
                builder.append(adapterName);
            }
            return builder.toString();
        }
    }

    public static class TrackingInterstitialAdListener implements MoPubInterstitial.InterstitialAdListener {

        private final AnalyticsHelper analytics;
        private final boolean soundOn;
        private MopubAdapterNameManager mopubAdapterNameManager;

        public TrackingInterstitialAdListener() {
            soundOn = false;
            analytics = AnalyticsHelper.getInstance();
            mopubAdapterNameManager = MopubAdapterNameManager.INSTANCE;
        }

        public TrackingInterstitialAdListener(boolean soundOn) {
            this.soundOn = soundOn;
            analytics = AnalyticsHelper.getInstance();
            mopubAdapterNameManager = MopubAdapterNameManager.INSTANCE;
        }

        public TrackingInterstitialAdListener(boolean soundOn, AnalyticsHelper analytics) {
            this.soundOn = soundOn;
            this.analytics = analytics;
            mopubAdapterNameManager = MopubAdapterNameManager.INSTANCE;
        }

        @Override
        public void onInterstitialLoaded(MoPubInterstitial moPubInterstitial) {
            String breadCrumb = buildMessage("onInterstitialLoaded", null, moPubInterstitial);
            analytics.trackBreadcrumb(breadCrumb);
        }

        @Override
        public void onInterstitialFailed(MoPubInterstitial moPubInterstitial, MoPubErrorCode moPubErrorCode) {
            String breadCrumb = buildMessage("onInterstitialFailed", moPubErrorCode, moPubInterstitial);
            analytics.trackBreadcrumb(breadCrumb);
        }

        @Override
        public void onInterstitialShown(MoPubInterstitial moPubInterstitial) {
            String breadCrumb = buildMessage("onInterstitialShown", null, moPubInterstitial);
            analytics.trackBreadcrumb(breadCrumb);
        }

        @Override
        public void onInterstitialClicked(MoPubInterstitial moPubInterstitial) {
            String breadCrumb = buildMessage("onInterstitialClicked", null, moPubInterstitial);
            analytics.trackBreadcrumb(breadCrumb);
        }

        @Override
        public void onInterstitialDismissed(MoPubInterstitial moPubInterstitial) {
            String breadCrumb = buildMessage("onInterstitialDismissed", null, moPubInterstitial);
            analytics.trackBreadcrumb(breadCrumb);
        }

        private String buildMessage(@NonNull String callbackName, @Nullable MoPubErrorCode moPubErrorCode, @NonNull MoPubInterstitial moPubInterstitial) {
            StringBuilder builder = new StringBuilder(callbackName);

            if (soundOn) {
                builder.append(" - ");
                builder.append("soundOn");
            }

            String adapterName = mopubAdapterNameManager.getAdapterNameFromInterstitial(moPubInterstitial);

            if (adapterName != null) {
                builder.append(" - ");
                builder.append(adapterName);
            }

            if (moPubErrorCode != null) {
                builder.append(" - ");
                builder.append(moPubErrorCode.toString());
            }

            return builder.toString();
        }
    }

    public static class TrackingNativeNetworkListener implements MoPubNative.MoPubNativeNetworkListener {

        private MopubAdapterNameManager mopubAdapterNameManager;

        public TrackingNativeNetworkListener() {
            mopubAdapterNameManager = MopubAdapterNameManager.INSTANCE;
        }

        @Override
        public void onNativeLoad(NativeAd nativeAd) {
            String adapterName = mopubAdapterNameManager.getAdapterNameFromNative(nativeAd);
            AnalyticsHelper.getInstance().trackBreadcrumb("onNativeLoad - " + adapterName);
        }

        @Override
        public void onNativeFail(NativeErrorCode nativeErrorCode) {
            AnalyticsHelper.getInstance().trackBreadcrumb("onNativeFail - code " + nativeErrorCode.getIntCode());
        }
    }
}
