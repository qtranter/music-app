package com.audiomack.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import com.audiomack.BuildConfig;
import com.audiomack.ConstantsKt;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

import androidx.core.content.ContextCompat;

public class GeneralPreferencesHelper {

    private static GeneralPreferencesHelper instance;

    public static GeneralPreferencesHelper getInstance(Context context){
        if(instance == null){
            instance = new GeneralPreferencesHelper(context);
        }
        return instance;
    }

    private Boolean liveEnvironment;
    private Boolean excludeReups;
    private Boolean trackAds;
    private Integer playerPlaylistTooltipCount;
    private Integer playerQueueTooltipCount;
    private Integer playerScrollTooltipCount;
    private Integer playerEqTooltipCount;
    private Integer queueAddToPlaylistTooltipCount;
    private Integer miniPlayerTooltipCount;
    private Integer commentTooltipCount;
    private Integer suggestedFollowsTooltipCount;
    private Integer playlistShuffleTooltipCount;
    private Integer playlistDownloadTooltipCount;
    private Integer albumFavoriteTooltipCount;
    private Integer downloadInAppMessagingCount;
    private Integer limitedDownloadInAppMessagingCount;
    private String permissionsAnswer;
    private Long contactSupportTimestamp;
    private Long playCount;

    private SecureSharedPreferences sharedPreferences;

    private GeneralPreferencesHelper(Context context){
        super();
        sharedPreferences = getSharedPreferences(context);
        loadLiveEnvironmentStatus(context);
        loadExcludeReups(context);
        loadPlayerPlaylistTooltipCount(context);
        loadPlayerQueueTooltipCount(context);
        loadQueueAddToPlaylistTooltipCount(context);
        loadPlaylistShuffleTooltipCount(context);
        loadPlaylistDownloadTooltipCount(context);
        loadAlbumFavoriteTooltipCount(context);
        loadMiniplayerTooltipCount(context);
        loadSuggestedFollowsTooltipCount(context);
        loadDownloadInAppMessageShown(context);
        loadPermissionsAnswer(context);
    }

    private SecureSharedPreferences getSharedPreferences(Context context) {
        if(sharedPreferences == null){
            sharedPreferences = new SecureSharedPreferences(context, ConstantsKt.GENERAL_PREFERENCES, BuildConfig.AM_PREFERENCES_SECRET, true);
        }
        return sharedPreferences;
    }

    // Environment

    public boolean isLiveEnvironment(Context context){
        if(liveEnvironment == null){
            loadLiveEnvironmentStatus(context);
        }
        return liveEnvironment;
    }

    public void setLiveEnvironmentStatus(Context context, boolean live){
        liveEnvironment = live;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_LIVE_ENVIRONMENT, live ? "1" : "0");
    }

    private void loadLiveEnvironmentStatus(Context context){
        String status = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_LIVE_ENVIRONMENT);
        liveEnvironment = status == null || status.equals("1");
    }


    // Exclude reups

    public boolean isExcludeReups(Context context){
        if(excludeReups == null){
            loadExcludeReups(context);
        }
        return excludeReups;
    }

    public void setExcludeReups(Context context, boolean excl){
        excludeReups = excl;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_EXCLUDE_REUPS, excl ? "1" : "0");
    }

    private void loadExcludeReups(Context context){
        String excl = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_EXCLUDE_REUPS);
        excludeReups = excl != null && excl.equals("1");
    }


    // Track ads

    public boolean isTrackAds(Context context){
        if(trackAds == null){
            loadTrackAds(context);
        }
        return trackAds;
    }

    public void setTrackAds(Context context, boolean track){
        trackAds = track;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_TRACK_ADS, track ? "1" : "0");
    }

    private void loadTrackAds(Context context){
        String status = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_TRACK_ADS);
        trackAds = "1".equals(status);
    }


    // Player playlist

    public boolean needToShowPlayerPlaylistTooltip(Context context){
        if(playerPlaylistTooltipCount == null){
            loadPlayerPlaylistTooltipCount(context);
        }
        return playerPlaylistTooltipCount < 1;
    }

    public void setPlayerPlaylistTooltipShown(Context context){
        if(playerPlaylistTooltipCount == null){
            loadPlayerPlaylistTooltipCount(context);
        }
        playerPlaylistTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_PLAYER_PLAYLIST_TOOLTIP_SHOWN_COUNT, Integer.toString(playerPlaylistTooltipCount));
    }

    private void loadPlayerPlaylistTooltipCount(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_PLAYER_PLAYLIST_TOOLTIP_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        playerPlaylistTooltipCount = Integer.parseInt(countString);
    }



    // Player queue

    public boolean needToShowPlayerQueueTooltip(Context context){
        if(playerQueueTooltipCount == null){
            loadPlayerQueueTooltipCount(context);
        }
        return playerQueueTooltipCount < 1;
    }

    public void setPlayerQueueTooltipShown(Context context){
        if(playerQueueTooltipCount == null){
            loadPlayerQueueTooltipCount(context);
        }
        playerQueueTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_PLAYER_QUEUE_TOOLTIP_SHOWN_COUNT, Integer.toString(playerQueueTooltipCount));
    }

    private void loadPlayerQueueTooltipCount(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_PLAYER_QUEUE_TOOLTIP_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        playerQueueTooltipCount = Integer.parseInt(countString);
    }


    // Player eq

    public boolean needToShowPlayerEqTooltip(Context context) {
        if (playerEqTooltipCount == null) {
            loadPlayerEqTooltipCount(context);
        }
        return playerEqTooltipCount < 1;
    }

    public void setPlayerEqTooltipShown(Context context) {
        if (playerEqTooltipCount == null) {
            loadPlayerEqTooltipCount(context);
        }
        playerEqTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_PLAYER_EQ_TOOLTIP_SHOWN_COUNT, Integer.toString(playerEqTooltipCount));
    }

    private void loadPlayerEqTooltipCount(Context context) {
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_PLAYER_EQ_TOOLTIP_SHOWN_COUNT);
        if (countString == null || countString.length() == 0) {
            countString = "0";
        }
        playerEqTooltipCount = Integer.parseInt(countString);
    }


    // Player scroll

    public boolean needToShowPlayerScrollTooltip(Context context) {
        if (playerScrollTooltipCount == null) {
            loadPlayerScrollTooltipCount(context);
        }
        return playerScrollTooltipCount < 1;
    }

    public void setPlayerScrollTooltipShown(Context context) {
        if (playerScrollTooltipCount == null) {
            loadPlayerScrollTooltipCount(context);
        }
        playerScrollTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_PLAYER_SCROLL_TOOLTIP_SHOWN_COUNT, Integer.toString(playerScrollTooltipCount));
    }

    private void loadPlayerScrollTooltipCount(Context context) {
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_PLAYER_SCROLL_TOOLTIP_SHOWN_COUNT);
        if (countString == null || countString.length() == 0) {
            countString = "0";
        }
        playerScrollTooltipCount = Integer.parseInt(countString);
    }



    // Queue add to playlist

    public boolean needToShowQueueAddToPlaylistTooltip(Context context){
        if(queueAddToPlaylistTooltipCount == null){
            loadQueueAddToPlaylistTooltipCount(context);
        }
        return queueAddToPlaylistTooltipCount < 1;
    }

    public void setQueueAddToPlaylistTooltipShown(Context context){
        if(queueAddToPlaylistTooltipCount == null){
            loadQueueAddToPlaylistTooltipCount(context);
        }
        queueAddToPlaylistTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_QUEUE_ADD_TO_PLAYLIST_TOOLTIP_SHOWN_COUNT, Integer.toString(queueAddToPlaylistTooltipCount));
    }

    private void loadQueueAddToPlaylistTooltipCount(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_QUEUE_ADD_TO_PLAYLIST_TOOLTIP_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        queueAddToPlaylistTooltipCount = Integer.parseInt(countString);
    }



    // Playlist shuffle

    public boolean needToShowPlaylistShuffleTooltip(Context context){
        if(playlistShuffleTooltipCount == null){
            loadPlaylistShuffleTooltipCount(context);
        }
        return playlistShuffleTooltipCount < 1;
    }

    public void setPlaylistShuffleTooltipShown(Context context){
        if(playlistShuffleTooltipCount == null){
            loadPlaylistShuffleTooltipCount(context);
        }
        playlistShuffleTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_PLAYLIST_SHUFFLE_TOOLTIP_SHOWN_COUNT, Integer.toString(playlistShuffleTooltipCount));
    }

    private void loadPlaylistShuffleTooltipCount(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_PLAYLIST_SHUFFLE_TOOLTIP_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        playlistShuffleTooltipCount = Integer.parseInt(countString);
    }


    // Playlist download

    public boolean needToShowPlaylistDownloadTooltip(Context context){
        if(playlistDownloadTooltipCount == null){
            loadPlaylistDownloadTooltipCount(context);
        }
        return playlistDownloadTooltipCount < 1;
    }

    public void setPlaylistDownloadTooltipShown(Context context){
        if(playlistDownloadTooltipCount == null){
            loadPlaylistDownloadTooltipCount(context);
        }
        playlistDownloadTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_PLAYLIST_DOWNLOAD_TOOLTIP_SHOWN_COUNT, Integer.toString(playlistDownloadTooltipCount));
    }

    private void loadPlaylistDownloadTooltipCount(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_PLAYLIST_DOWNLOAD_TOOLTIP_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        playlistDownloadTooltipCount = Integer.parseInt(countString);
    }


    // Album favorite

    public boolean needToShowAlbumFavoriteTooltip(Context context){
        if(albumFavoriteTooltipCount == null){
            loadAlbumFavoriteTooltipCount(context);
        }
        return albumFavoriteTooltipCount < 1;
    }

    public void setAlbumFavoriteTooltipShown(Context context){
        if(albumFavoriteTooltipCount == null){
            loadAlbumFavoriteTooltipCount(context);
        }
        albumFavoriteTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_ALBUM_FAVORITE_TOOLTIP_SHOWN_COUNT, Integer.toString(albumFavoriteTooltipCount));
    }

    private void loadAlbumFavoriteTooltipCount(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_ALBUM_FAVORITE_TOOLTIP_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        albumFavoriteTooltipCount = Integer.parseInt(countString);
    }


    // Miniplayer

    public boolean needToShowMiniplayerTooltip(Context context){
        if(miniPlayerTooltipCount == null){
            loadMiniplayerTooltipCount(context);
        }
        return miniPlayerTooltipCount < 1;
    }

    public void setMiniplayerTooltipShown(Context context){
        if(miniPlayerTooltipCount == null){
            loadMiniplayerTooltipCount(context);
        }
        miniPlayerTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_MINIPLAYER_TOOLTIP_SHOWN_COUNT, Integer.toString(miniPlayerTooltipCount));
    }

    private void loadMiniplayerTooltipCount(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_MINIPLAYER_TOOLTIP_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        miniPlayerTooltipCount = Integer.parseInt(countString);
    }



    // Suggested follows

    public boolean needToShowSuggestedFollowsTooltip(Context context){
        if(suggestedFollowsTooltipCount == null){
            loadSuggestedFollowsTooltipCount(context);
        }
        return suggestedFollowsTooltipCount < 1;
    }

    public void setSuggestedFollowsTooltipShown(Context context){
        if(suggestedFollowsTooltipCount == null){
            loadSuggestedFollowsTooltipCount(context);
        }
        suggestedFollowsTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_SUGGESTEDFOLLOWS_TOOLTIP_SHOWN_COUNT, Integer.toString(suggestedFollowsTooltipCount));
    }

    private void loadSuggestedFollowsTooltipCount(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_SUGGESTEDFOLLOWS_TOOLTIP_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        suggestedFollowsTooltipCount = Integer.parseInt(countString);
    }




    // Download in app message

    public boolean needToShowDownloadInAppMessage(Context context){
        if(downloadInAppMessagingCount == null){
            loadDownloadInAppMessageShown(context);
        }
        return downloadInAppMessagingCount < 1;
    }

    public void setDownloadInAppMessageShown(Context context){
        if(downloadInAppMessagingCount == null){
            loadDownloadInAppMessageShown(context);
        }
        downloadInAppMessagingCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_DOWNLOAD_INAPPMESSAGE_SHOWN_COUNT, Integer.toString(downloadInAppMessagingCount));
    }

    private void loadDownloadInAppMessageShown(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_DOWNLOAD_INAPPMESSAGE_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        downloadInAppMessagingCount = Integer.parseInt(countString);
    }



    // Limited download in app message

    public boolean needToShowLimitedDownloadInAppMessage(Context context){
        if(limitedDownloadInAppMessagingCount == null){
            loadLimitedDownloadInAppMessageShown(context);
        }
        return limitedDownloadInAppMessagingCount < 1;
    }

    public void setLimitedDownloadInAppMessageShown(Context context){
        if(limitedDownloadInAppMessagingCount == null){
            loadLimitedDownloadInAppMessageShown(context);
        }
        limitedDownloadInAppMessagingCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_LIMITED_DOWNLOAD_INAPPMESSAGE_SHOWN_COUNT, Integer.toString(limitedDownloadInAppMessagingCount));
    }

    private void loadLimitedDownloadInAppMessageShown(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_LIMITED_DOWNLOAD_INAPPMESSAGE_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        limitedDownloadInAppMessagingCount = Integer.parseInt(countString);
    }




    // Permissions

    public boolean needToShowPermissions(Activity context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // No runtime permissions
            return false;
        }

        String locationPermission = Manifest.permission.ACCESS_COARSE_LOCATION;
        if(ContextCompat.checkSelfPermission(context, locationPermission) == PackageManager.PERMISSION_GRANTED || context.shouldShowRequestPermissionRationale(locationPermission)){
            // Permissions already granted
            return false;
        }

        return permissionsAnswer == null;
    }

    private void loadPermissionsAnswer(Context context){
        permissionsAnswer = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_PERMISSIONS_ANSWER);
    }

    public void setPermissionsAnswer(Context context, String answer){
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_PERMISSIONS_ANSWER, answer);
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_PERMISSIONS_ANSWER_DATE, Long.toString(new Date().getTime()));
    }


    // Comments

    public boolean needToShowCommentTooltip(Context context){
        if(commentTooltipCount == null){
            loadCommentTooltipCount(context);
        }
        return commentTooltipCount < 1;
    }

    public void setCommentTooltipShown(Context context){
        if(commentTooltipCount == null){
            loadCommentTooltipCount(context);
        }
        commentTooltipCount++;
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_COMMENT_TOOLTIP_SHOWN_COUNT, Integer.toString(commentTooltipCount));
    }

    private void loadCommentTooltipCount(Context context){
        String countString = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_COMMENT_TOOLTIP_SHOWN_COUNT);
        if(countString == null || countString.length() == 0){
            countString = "0";
        }
        commentTooltipCount = Integer.parseInt(countString);
    }

    // Contact Support

    public boolean needToShowContactTooltip(Context context){

        loadContactTooltipTimestamp(context);

        Long nowTime = new Date().getTime();
        Long savedTime = contactSupportTimestamp;
        Long intervalTime = nowTime - savedTime;
        Long requiredIntervalTime = (long) (10 * 60 * 1000);

        return requiredIntervalTime >= intervalTime ;
    }

    public void setContactTooltipShown(Context context){
        long nowTime = new Date().getTime();
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_CONTACT_SUPPORT_TIMESTAMP, Long.toString(nowTime));
    }

    private void loadContactTooltipTimestamp(Context context){
        String timestamp = getSharedPreferences(context).getString(ConstantsKt.GENERAL_PREFERENCES_CONTACT_SUPPORT_TIMESTAMP);
        if(timestamp == null || timestamp.length() == 0){
            timestamp = "0";
        }
        contactSupportTimestamp = Long.parseLong(timestamp);
    }

    // Play count

    @NotNull
    public Long getPlayCount(@NotNull Context context) {
        if (playCount == null) {
            loadPlayCount(context);
        }
        return playCount;
    }

    public void incrementPlayCount(@NotNull Context context) {
        getSharedPreferences(context).put(ConstantsKt.GENERAL_PREFERENCES_PLAY_COUNT, Long.toString(++playCount));
    }

    private void loadPlayCount(Context context) {
        SecureSharedPreferences preferences = getSharedPreferences(context);
        String savedCount = preferences.getString(ConstantsKt.GENERAL_PREFERENCES_PLAY_COUNT);
        if (TextUtils.isEmpty(savedCount)) {
            savedCount = "0";
        }
        playCount = Long.parseLong(savedCount);
    }
}