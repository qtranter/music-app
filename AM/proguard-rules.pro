# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/matteo/Development/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Some rules taken from https://gist.github.com/jemshit/767ab25a9670eb0083bafa65f8d786bb

# MoPub Proguard Config
# NOTE: You should also include the Android Proguard config found with the build tools:
# $ANDROID_HOME/tools/proguard/proguard-android.txt

# Keep public classes and methods.
# https://github.com/mopub/mopub-android-sdk/blob/master/mopub-sample/proguard.cfg
# https://twittercommunity.com/t/mopub-android-proguard-error-plz-help-proguard/68988/3
# https://tech.yandex.com/mobile-ads/doc/dg/android/quick-start/mopub-adapter-docpage/
# https://www.appodeal.com/sdk/documentation?framework=3&full=1&platform=1
-keepclassmembers class com.mopub.** { public *; }
-keep public class com.mopub.**
-keep public class android.webkit.JavascriptInterface {}
-dontwarn com.mopub.**
-keep class com.mopub.mobileads.** {*;}
-keepclassmembers class com.mopub.mobileads.CustomEventBannerAdapter {!private !public !protected *;}
# Explicitly keep any custom event classes in any package.
-keep class * extends com.mopub.mobileads.CustomEventBanner {}
-keep class * extends com.mopub.mobileads.CustomEventInterstitial {}
-keep class * extends com.mopub.nativeads.CustomEventNative {}
-keep class * extends com.mopub.nativeads.CustomEventRewardedAd {}
-keep class * extends com.mopub.nativeads.CustomEventRewardedVideo {}
-keep class * extends com.mopub.nativeads.MoPubNative {}
-keep class * extends com.mopub.mobileads.BaseHtmlWebView {}

# Common
-keepattributes JavascriptInterface
-keepattributes SourceFile,LineNumberTable

# Annotations
-keepattributes *Annotation*

# Keep methods that are accessed via reflection
-keepclassmembers class ** { @com.mopub.common.util.ReflectionTarget *; }

# Preserve GMS ads classes
-keep class com.google.android.gms.ads.** {*;}

# Viewability support
-keepclassmembers class com.integralads.avid.library.mopub.** { public *; }
-keep public class com.integralads.avid.library.mopub.**
-keepclassmembers class com.moat.analytics.mobile.** { public *; }
-keep class com.moat.analytics.mobile.**

# Moat SDK (TapJoy, Irounsource ...)
-keep class com.moat.** {*;}
-dontwarn com.moat.**

# Support for Android Advertiser ID.
-keep class com.google.android.gms.common.GooglePlayServicesUtil {*;}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {*;}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {*;}
-dontwarn com.google.android.gms.**

# Support for Google Play Services
# http://developer.android.com/google/play-services/setup.html
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Filter out warnings that refer to legacy Code.
-dontwarn org.apache.http.**
-dontwarn com.mopub.volley.toolbox.**

-keepattributes EnclosingMethod

# https://github.com/BranchMetrics/android-branch-deep-linking/commit/11a81f799296cf9d6968e05c1e03a23b720ef12a
# https://github.com/BranchMetrics/react-native-branch-deep-linking/issues/255
# Branch io
-dontwarn io.branch.**

# okhttp
-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

# comscore
# https://github.com/mparticle-integrations/mparticle-android-integration-comscore/blob/master/consumer-proguard.pro
-keep class com.comscore.** { *; }
-dontwarn com.comscore.**

# ultimate recyclerview
# https://github.com/cymcsg/UltimateRecyclerView/blob/master/UltimateRecyclerView/app/proguard-rules.pro
#-dontwarn com.squareup.haha.guava.**
#-dontwarn com.squareup.haha.perflib.**
#-dontwarn com.squareup.haha.trove.**
-dontwarn com.squareup.leakcanary.**
#-keep class com.squareup.haha.** { *; }
-keep class com.squareup.leakcanary.** { *; }


### Stetho, Stetho Realm plugin
-keep class com.facebook.stetho.** {
  *;
}
-dontwarn com.facebook.stetho.**

### facebook sdk problems related to billing
-keep class * implements com.android.vending.billing.IInAppBillingService { *; }

-keep class com.uphyca.** { *; }

### Support v7, Design
# http://stackoverflow.com/questions/29679177/cardview-shadow-not-appearing-in-lollipop-after-obfuscate-with-proguard/29698051
-keep class android.support.v7.widget.RoundRectDrawable { *; }

-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

-dontwarn android.support.**
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

### Picasso
-dontwarn com.squareup.okhttp.**

### OkHttp3
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

### Exoplayer2
-keepnames class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.** # Consider removing this line after upgrading to > 2.11.0

### Ultimate recycler view
-dontwarn com.marshalchen.ultimaterecyclerview.animators.BaseItemAnimator

# https://gist.github.com/xuhaibahmad/596adb8bd73c7681a6d0a1b4ad96ba67
# Green robot eventbus
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# activeandroid
-keep class com.activeandroid.** { *; }
-keep class com.activeandroid.**.** { *; }
-keep class * extends com.activeandroid.Model
-keep class * extends com.activeandroid.serializer.TypeSerializer

-keepattributes Column
-keepattributes Table
-keepclasseswithmembers class * { @com.activeandroid.annotation.Column <fields>; }

### Crashlytics
-keep public class * extends java.lang.Exception

# CropIwa
-dontwarn com.steelkiwi.cropiwa.shape.CropIwaOvalShape

# Okio
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Gson
-keepattributes Signature
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Google Android Advertising ID
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.ads.identifier.**

# Zendesk
# https://developer.zendesk.com/embeddables/docs/android-support-sdk/known_issues#nullpointerexception-after-shrinking-with-proguard
-keep class zendesk.support.request.ViewCellAttachmentMenuItem { *; }

# Mixpanel
# https://help.mixpanel.com/hc/en-us/articles/115004602543-Compile-Android-With-Proguard
-dontwarn com.mixpanel.**

# Google Analytics 3.0 specific rules ##
-keep class com.google.analytics.** { *; }
-keep class com.google.android.gms.analytics.** { *; }

# AppsFlyer
# https://support.appsflyer.com/hc/en-us/articles/207032126-AppsFlyer-SDK-Integration-Android
-dontwarn com.android.installreferrer
-dontwarn com.appsflyer.FirebaseInstanceIdListener
-dontwarn com.appsflyer.y

# TapJoy
-keep class com.tapjoy.** { *; }
-dontwarn com.tapjoy.**

# Coroutines
# https://github.com/Kotlin/kotlinx.coroutines/blob/master/ui/kotlinx-coroutines-android/example-app/app/proguard-rules.pro
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.flow.**
-dontwarn java.lang.instrument.**

# RevenueCat
# https://docs.revenuecat.com/docs/android
-keep class com.revenuecat.purchases.** { *; }

# Verizon
-keepclassmembers class com.verizon.ads** {
  public *;
}
-keep class com.verizon.ads**

-dontwarn com.audiomack.ui.home.**

# Amazon Publisher Services
-keep class com.amazon.device.ads.** { *; }

# AdColony
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn com.adcolony.sdk.ADCLogEvent

# Irounsource
-keepclassmembers class com.ironsource.sdk.controller.IronSourceWebView$JSInterface {
    public *;
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keep class com.ironsource.adapters.** { *;
}
-dontwarn com.ironsource.mediationsdk.**
-dontwarn com.ironsource.adapters.**

# Protobuf (needed after adding AdsWizz)
-keep public class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.protobuf.**

# Material components
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }

# Huawei Safety Detect
# https://developer.huawei.com/consumer/en/hms/huawei-safetydetectkit/
# https://github.com/HMS-Core/hms-safetydetect-demo-android
-keep class com.hianalytics.android.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}

# Unity Ads
# https://github.com/Unity-Technologies/unity-ads-android/blob/master/unity-ads/proguard-rules.pro
-keep class com.unity3d.ads.** {
   *;
}
-keep class com.unity3d.services.** {
   *;
}
-dontwarn com.google.ar.core.**

# Retrofit
# https://github.com/square/retrofit/blob/master/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod
# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**
# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit
# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
# More rules found in third parties
-dontwarn retrofit2.Platform$Java8

# Moshi
-keepnames @kotlin.Metadata class com.audiomack.network.retrofitModel.**
-keep class com.audiomack.network.retrofitModel.** { *; }
-keepclassmembers class com.audiomack.network.retrofitModel.** { *; }

# Embrace
-keepresources string/emb_*

# MediaRouter
-keep class androidx.mediarouter.media.** { *; }
-keepclassmembers class androidx.mediarouter.media.** { *; }