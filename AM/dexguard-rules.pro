-multidex

# The following are needed for OneSignal "it looks like they have consumer rules preventing these resources from being obfuscated, but they are defined in a keep.xml resource file. DexGuard does not support reading those rules yet, but we may do so in a future version"
-keepresources anim/onesignal_fade_in
-keepresources anim/onesignal_fade_out
-keepresources drawable/ic_os_notification_fallback_white_24dp
-keepresources drawable/ic_stat_onesignal_default
-keepresources drawable/ic_onesignal_large_icon_default
-keepresources raw/onesignal_default_sound

# Tentative fix for the `Fatal Exception: java.lang.NoClassDefFoundError: Failed resolution of: Lcom/huawei/hms/api/HuaweiApiAvailability;`
# https://github.com/OneSignal/OneSignal-Android-SDK/issues/1194
# https://github.com/OneSignal/OneSignal-Android-SDK/blob/3.15.6/OneSignalSDK/onesignal/src/main/java/com/onesignal/OSUtils.java
# It appears DexGuard removes the try-catch block. This is due to DexGuard assuming Class.getName() has no side effects, such that, when its return value is not used, the call can be removed. Also, because the class is necessarily available at compile time, DexGuard assumes it is always available. When this method call is removed, all exception-throwing code is gone, such that the exception handler does not need to be there any longer.
-keep, includecode, allowshrinking, allowobfuscation class com.onesignal.** { *; }

# AppsFlyer rules pasted here because of the invalid syntax on their consumer rules
# See dexguard section in AM/build.gradle for more info
-keepresourcefiles com/appsflyer/internal/a-,com/appsflyer/internal/b-
-keepresources *
