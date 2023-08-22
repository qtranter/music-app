package com.audiomack.model;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import android.text.TextUtils;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.query.Update;
import com.audiomack.MainApplication;
import com.audiomack.R;
import com.audiomack.activities.BaseActivity;
import com.audiomack.data.music.local.LocalMediaRepository;
import com.audiomack.data.premium.PremiumRepository;
import com.audiomack.data.sizes.SizesRepository;
import com.audiomack.data.storage.StorageKt;
import com.audiomack.data.tracking.mixpanel.MixpanelRepository;
import com.audiomack.data.user.UserData;
import com.audiomack.download.AMMusicDownloader;
import com.audiomack.data.storage.StorageProvider;
import com.audiomack.network.API;
import com.audiomack.ui.home.HomeActivity;
import com.audiomack.ui.slideupmenu.share.SlideUpMenuShareFragment;
import com.audiomack.utils.DateUtils;
import com.audiomack.utils.ExtensionsKt;
import com.audiomack.utils.Utils;
import com.audiomack.views.AMProgressHUD;
import com.audiomack.views.AMSnackbar;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

@Table(name = "items")
public class AMResultItem extends Model {

    // ItemImagePresetPlaylist and ItemImagePresetSong should be removed sooner or later.
    // Leaving them here to allow proper image files deletion.
    public enum ItemImagePreset {
        ItemImagePresetSmall, @Deprecated ItemImagePresetPlaylist, @Deprecated ItemImagePresetSong, ItemImagePresetOriginal
    }

    public enum ItemAPIStatus {
        Off, On, Loading, Queued
    }

    public enum MusicDownloadType {
        Limited, Premium, Free
    }

    public final static int PLAYLIST_IMAGE_MAX_SIZE_PX = 1024;

    public static final String TYPE_SONG = "song";
    public static final String TYPE_ALBUM = "album";
    public static final String TYPE_ALBUM_TRACK = "album_track";
    public static final String TYPE_PLAYLIST = "playlist";
    public static final String TYPE_PLAYLIST_TRACK = "playlist_track";
    public static final String TYPE_PODCAST = "podcast";

    @Column(name = "item_id", unique = true)
    protected String itemId;
    @Column(name = "type")
    protected String type;                        // TYPE_ALBUM or TYPE_SONG or TYPE_ALBUM_TRACK or TYPE_PLAYLIST_TRACK

    @Column(name = "artist")
    protected String artist;
    @Column(name = "title")
    protected String title;
    @Column(name = "album")
    protected String album;
    @Column(name = "image")
    protected String image;
    @Column(name = "featured")
    protected String featured;
    @Column(name = "producer")
    protected String producer;
    @Column(name = "genre")
    protected String genre;
    @Column(name = "desc")
    protected String desc;

    @Column(name = "url")
    protected String url;
    @Column(name = "uploader_name")
    protected String uploaderName;
    @Column(name = "uploader_id")
    protected String uploaderId;
    @Column(name = "url_slug")
    protected String urlSlug;

    @Column(name = "uploader_slug")
    protected String uploaderSlug;

    @Deprecated // use songReleaseDate instead
    @Column(name = "uploaded")
    protected String released;

    @Column(name = "buy_url")
    protected String buyURL;

    @Deprecated
    @Column(name = "download_url")
    protected String downloadURL;               // not in use anymore, always rely on "url" (the streaming url)

    /**
     * id of the album/playlist; only for items with type equal to TYPE_ALBUM_TRACK and TYPE_PLAYLIST_TRACK
     */
    @Column(name = "parent_id", index = true)
    protected String parentId;

    @Column(name = "track_number")
    protected int trackNumber;

    protected int discNumber;

    @Deprecated
    @Column(name = "full_path")                 // the filesystem path of the downloaded file
    protected String fullPath;

    @Deprecated
    @Column(name = "download_manager_id")
    protected long downloadManagerId;             // not used anymore after transitioning away from the OS DownloadManager

    @Column(name = "video_ad")
    protected boolean videoAd;

    @Column(name = "private_playlist")
    protected boolean privatePlaylist;            // used only for playlists

    @Column(name = "created")
    protected String created;                     // used only for playlists for the moment

    @Column(name = "stream_only")
    protected boolean streamOnly;                 // used only for albums

    @Column(name = "album_track_downloaded_as_single", index = true)
    protected boolean albumTrackDownloadedAsSingle;

    @Column(name = "download_completed")
    protected boolean downloadCompleted;

    @Column(name = "original_image")
    protected String originalImage;

    @Column(name = "playlist_image")
    protected String playlistImage;

    @Column(name = "small_image")
    protected String smallImage;

    @Column(name = "song_image")
    protected String songImage;

    @Column(name = "cached")
    protected boolean cached; // For cached only songs

    @Column(name = "amp")
    protected boolean amp;

    @Column(name = "amp_duration")
    protected int ampDuration;

    @Column(name = "synced")
    protected boolean synced;

    @Column(name = "uploader_followed")
    protected boolean uploaderFollowed;

    @Column(name = "last_updated")
    protected String lastUpdated;

    @Column(name = "repost_artist_name")
    protected String repostArtistName;

    @Column(name = "repost_timestamp")
    protected long repostTimestamp;

    @Column(name = "playlist")
    protected String playlist;                     // used to show playlist name on player screen, it's set at runtime when loading a playlist on the queue

    @Column(name = "offline_toast_shown")
    protected boolean offlineToastShown;

    @Column(name = "banner")
    protected String banner;                      // only used for playlists for the moment

    /**
     * Contains a json with the MixpanelSource object content, persisted only for bookmark reasons.
     * Only use this field when restoring AMResultItem(s) from AMBokmarkedItem(s),
     * otherwise rely on [currentMixpanelSource].
     */
    @Column(name = "mixpanel_source")
    private String originalMixpanelSource;

    private MixpanelSource currentMixpanelSource;

    @Column(name = "comments")                    // comment count for object
    protected int commentCount;

    @Column(name = "duration")
    protected long duration;

    protected List<AMResultItem> tracks;          // for albums and playlist

    protected int[] volumeData;

    protected String status;

    protected String uploaderTwitter;

    protected List<AMComment> commentList;

    public Long playsCount;

    public Long favoritesCount;

    public Long repostsCount;

    public Long playlistsCount;

    private String rankDaily;

    private String rankWeekly;

    private String rankMonthly;

    private String rankAllTime;

    @Column(name = "uploader_verified")
    private boolean uploaderVerified;

    @Column(name = "uploader_tastemaker")
    private boolean uploaderTastemaker;

    @Column(name = "uploader_authenticated")
    private boolean uploaderAuthenticated;

    @Column(name = "uploader_image")
    private String uploaderImage;

    @Column(name = "uploader_followers")
    private Long uploaderFollowers;

    @Column(name = "album_release_date")
    protected long albumReleaseDate;

    @Column(name = "song_release_date")
    protected long songReleaseDate;

    @Column(name = "tags")
    private String tags;

    // Used to get access to private or unrealeased music. This key will be passed to the APIs when fetching this music or playing it.
    @Column(name = "extra_key")
    private String extraKey;

    private int playlistTracksCount;

    private boolean verifiedSearchResult;

    private boolean geoRestricted;

    private Integer newlyAddedSongs; // Used on playlists bundled notifications

    private ItemAPIStatus favoriteStatus, repostStatus, addToPlaylistStatus;
    private final BehaviorSubject<ItemAPIStatus> favoriteSubject = BehaviorSubject.create();
    private final BehaviorSubject<ItemAPIStatus> repostSubject = BehaviorSubject.create();
    private final BehaviorSubject<ItemAPIStatus> addToPlaylistSubject = BehaviorSubject.create();
    private final BehaviorSubject<Integer> commentsCountSubject = BehaviorSubject.create();

    // Contains the API value returned for the type of download
    @Column(name = "premium_download")
    protected String premiumDownload;

    @Column(name = "download_date")
    protected Date downloadDate;

    // Marks a premium-limited download as frozen, which means it's downloaded but can't be played offline because user has more than the allowed limit of premium-limited downloaded songs and is not premium.
    @Column(name = "frozen")
    private boolean frozen;

    protected boolean isLocal = false;

    public AMResultItem() {
        super();
    }

    public void copyFrom(AMResultItem item) {
        this.itemId = item.itemId;
        this.type = item.type;
        this.artist = item.artist;
        this.title = item.title;
        this.album = item.album;
        this.image = item.image;
        this.featured = item.featured;
        this.producer = item.producer;
        this.genre = item.genre;
        this.desc = item.desc;
        this.url = item.url;
        this.uploaderName = item.uploaderName;
        this.uploaderId = item.uploaderId;
        this.urlSlug = item.urlSlug;
        this.uploaderSlug = item.uploaderSlug;
        this.released = item.released;
        this.buyURL = item.buyURL;
        this.parentId = item.parentId;
        this.trackNumber = item.trackNumber;
        this.fullPath = item.fullPath;
        this.videoAd = item.videoAd;
        this.privatePlaylist = item.privatePlaylist;
        this.created = item.created;
        this.streamOnly = item.streamOnly;
        this.albumTrackDownloadedAsSingle = item.albumTrackDownloadedAsSingle;
        this.downloadCompleted = item.downloadCompleted;
        this.originalImage = item.originalImage;
        this.playlistImage = item.playlistImage;
        this.smallImage = item.smallImage;
        this.songImage = item.songImage;
        this.cached = item.cached;
        this.duration = item.duration;
        this.amp = item.amp;
        this.ampDuration = item.ampDuration;
        this.uploaderFollowed = item.uploaderFollowed;
        this.lastUpdated = item.lastUpdated;
        this.repostArtistName = item.repostArtistName;
        this.repostTimestamp = item.repostTimestamp;
        this.playlist = item.playlist;
        this.offlineToastShown = item.offlineToastShown;
        this.banner = item.banner;
        this.commentCount = item.commentCount;
        this.currentMixpanelSource = MixpanelSource.fromJSON(item.originalMixpanelSource);
        this.originalMixpanelSource = item.originalMixpanelSource;

        if (item.volumeData != null) {
            this.volumeData = new int[item.volumeData.length];
            System.arraycopy(item.volumeData, 0, this.volumeData, 0, item.volumeData.length);
        }

        this.uploaderVerified = item.uploaderVerified;
        this.uploaderTastemaker = item.uploaderTastemaker;
        this.uploaderAuthenticated = item.uploaderAuthenticated;
        this.uploaderImage = item.uploaderImage;
        this.uploaderFollowers = item.uploaderFollowers;
        this.uploaderTwitter = item.uploaderTwitter;

        this.playlistTracksCount = item.playlistTracksCount;

        this.commentList = item.commentList;

        this.albumReleaseDate = item.albumReleaseDate;
        this.songReleaseDate = item.songReleaseDate;

        this.tags = item.tags;

        this.extraKey = item.extraKey;

        this.premiumDownload = item.premiumDownload;
        this.downloadDate = item.downloadDate;
        this.frozen = item.frozen;
    }

    public AMBookmarkItem toBookmark() {
        return new AMBookmarkItem(itemId,
                type,
                artist,
                title,
                album,
                image,
                featured,
                producer,
                genre,
                desc,
                url,
                uploaderName,
                uploaderId,
                urlSlug,
                uploaderSlug,
                released,
                buyURL,
                downloadURL,
                parentId,
                trackNumber,
                discNumber,
                fullPath,
                downloadManagerId,
                videoAd,
                privatePlaylist,
                created,
                streamOnly,
                albumTrackDownloadedAsSingle,
                downloadCompleted,
                originalImage,
                playlistImage,
                smallImage,
                songImage,
                cached,
                amp,
                ampDuration,
                synced,
                uploaderFollowed,
                lastUpdated,
                repostArtistName,
                repostTimestamp,
                playlist,
                offlineToastShown,
                banner,
                originalMixpanelSource,
                commentCount,
                duration,
                uploaderVerified,
                uploaderTastemaker,
                uploaderAuthenticated,
                uploaderImage,
                uploaderFollowers,
                albumReleaseDate,
                songReleaseDate,
                tags,
                extraKey,
                premiumDownload,
                downloadDate,
                frozen,
                isLocal
                );
    }

    public AMResultItem copyFrom(AMBookmarkItem bookmark) {
        this.itemId = bookmark.getItemId();
        this.type = bookmark.getType();
        this.artist = bookmark.getArtist();
        this.title = bookmark.getTitle();
        this.album = bookmark.getAlbum();
        this.image = bookmark.getImage();
        this.featured = bookmark.getFeatured();
        this.producer = bookmark.getProducer();
        this.genre = bookmark.getGenre();
        this.desc = bookmark.getDesc();
        this.url = bookmark.getUrl();
        this.uploaderName = bookmark.getUploaderName();
        this.uploaderId = bookmark.getUploaderId();
        this.urlSlug = bookmark.getUrlSlug();
        this.uploaderSlug = bookmark.getUploaderSlug();
        this.released = bookmark.getReleased();
        this.buyURL = bookmark.getBuyURL();
        this.downloadURL = bookmark.getDownloadURL();
        this.parentId = bookmark.getParentId();
        this.trackNumber = bookmark.getTrackNumber();
        this.discNumber = bookmark.getDiscNumber();
        this.fullPath = bookmark.getFullPath();
        this.downloadManagerId = bookmark.getDownloadManagerId();
        this.videoAd = bookmark.getVideoAd();
        this.privatePlaylist = bookmark.getPrivatePlaylist();
        this.created = bookmark.getCreated();
        this.streamOnly = bookmark.getStreamOnly();
        this.albumTrackDownloadedAsSingle = bookmark.getAlbumTrackDownloadedAsSingle();
        this.downloadCompleted = bookmark.getDownloadCompleted();
        this.originalImage = bookmark.getOriginalImage();
        this.playlistImage = bookmark.getPlaylistImage();
        this.smallImage = bookmark.getSmallImage();
        this.songImage = bookmark.getSongImage();
        this.cached = bookmark.getCached();
        this.duration = bookmark.getDuration();
        this.amp = bookmark.getAmp();
        this.ampDuration = bookmark.getAmpDuration();
        this.uploaderFollowed = bookmark.getUploaderFollowed();
        this.lastUpdated = bookmark.getLastUpdated();
        this.repostArtistName = bookmark.getRepostArtistName();
        this.repostTimestamp = bookmark.getRepostTimestamp();
        this.playlist = bookmark.getPlaylist();
        this.banner = bookmark.getBanner();
        this.offlineToastShown = bookmark.getOfflineToastShown();
        this.commentCount = bookmark.getCommentCount();
        this.currentMixpanelSource = MixpanelSource.fromJSON(bookmark.getOriginalMixpanelSource());
        this.originalMixpanelSource = bookmark.getOriginalMixpanelSource();
        this.uploaderVerified = bookmark.getUploaderVerified();
        this.uploaderTastemaker = bookmark.getUploaderTastemaker();
        this.uploaderAuthenticated = bookmark.getUploaderAuthenticated();
        this.uploaderImage = bookmark.getUploaderImage();
        this.uploaderFollowers = bookmark.getUploaderFollowers();
        this.albumReleaseDate = bookmark.getAlbumReleaseDate();
        this.songReleaseDate = bookmark.getSongReleaseDate();
        this.tags = bookmark.getTags();
        this.extraKey = bookmark.getExtraKey();
        this.premiumDownload = bookmark.getPremiumDownload();
        this.downloadDate = bookmark.getDownloadDate();
        this.frozen = bookmark.getFrozen();
        this.isLocal = bookmark.isLocal();
        return this;
    }

    @Nullable
    public static AMResultItem fromBundledPlaylistNotificationJson(JSONObject jsonObj, boolean ignoreGeorestrictedMusic) {

        boolean geoRestricted = jsonObj.optBoolean("geo_restricted");

        if (geoRestricted && ignoreGeorestrictedMusic) {
            return null;
        }

        AMResultItem playlist = new AMResultItem();

        playlist.geoRestricted = geoRestricted;

        playlist.itemId = jsonObj.optString("playlist_id");
        playlist.urlSlug = jsonObj.optString("playlist_url_slug");
        playlist.title = jsonObj.optString("playlist_name");
        playlist.uploaderId = jsonObj.optString("artist_id");
        playlist.uploaderSlug = jsonObj.optString("artist_url_slug");
        playlist.uploaderName = jsonObj.optString("artist_name");
        playlist.newlyAddedSongs = jsonObj.optInt("count");
        playlist.type = TYPE_PLAYLIST;
        playlist.smallImage = jsonObj.optString("playlist_image_url");
        playlist.originalImage = playlist.smallImage;

        return playlist;
    }

    @Nullable
    public static AMResultItem fromJson(JSONObject jsonObj, boolean ignoreGeorestrictedMusic, @Nullable String extraKey) {

        boolean geoRestricted = jsonObj.optBoolean("geo_restricted");

        if (geoRestricted && ignoreGeorestrictedMusic) {
            return null;
        }

        AMResultItem item = new AMResultItem();

        item.geoRestricted = geoRestricted;
        item.extraKey = extraKey;

        item.itemId = jsonObj.optString("id");

        item.image = jsonObj.optString("image");

        String imageBase = jsonObj.optString("image_base");
        if(!TextUtils.isEmpty(imageBase)){
            item.originalImage = imageBase + "?width=" + SizesRepository.INSTANCE.getLargeMusic();
            item.smallImage = imageBase + "?width=" + SizesRepository.INSTANCE.getSmallMusic();
        }else{
            item.originalImage = item.image;
            item.smallImage = item.image;
        }

        item.title = jsonObj.optString("title");
        item.type = jsonObj.optString("type");
        item.url = jsonObj.optString("streaming_url");

        if(item.isPlaylist()){
            JSONObject artistObj = jsonObj.optJSONObject("artist");
            item.artist = artistObj.optString("name");
            item.uploaderName = item.artist;
            item.uploaderId = artistObj.optString("id");
            item.uploaderSlug = artistObj.optString("url_slug");
            item.uploaderVerified = artistObj.optString("verified").equals("yes");
            item.uploaderTastemaker = artistObj.optString("verified").equals("tastemaker");
            item.uploaderAuthenticated = Arrays.asList("authenticated", "verify-pending", "verify-declined")
                    .contains(artistObj.optString("verified"));
            item.uploaderFollowed = artistObj.optString("follow").equals("yes");
            item.uploaderTwitter = artistObj.optString("twitter");
            item.uploaderFollowers = artistObj.optLong("followers_count");
            item.uploaderImage = artistObj.optString("image");
        }else {
            item.artist = jsonObj.optString("artist");

            JSONObject uploaderObj = jsonObj.optJSONObject("uploader");
            item.uploaderName = uploaderObj.optString("name");
            item.uploaderId = uploaderObj.optString("id");
            item.uploaderSlug = uploaderObj.optString("url_slug");
            item.uploaderVerified = uploaderObj.optString("verified").equals("yes");
            item.uploaderTastemaker = uploaderObj.optString("verified").equals("tastemaker");
            item.uploaderAuthenticated = Arrays.asList("authenticated", "verify-pending", "verify-declined")
                    .contains(uploaderObj.optString("verified"));
            item.uploaderFollowed = uploaderObj.optString("follow").equals("yes");
            item.uploaderTwitter = uploaderObj.optString("twitter");
            item.uploaderFollowers = uploaderObj.optLong("followers_count");
            item.uploaderImage = uploaderObj.optString("image");
        }

        item.desc = jsonObj.optString("description");
        item.urlSlug = jsonObj.optString("url_slug");
        item.featured = jsonObj.optString("featuring")
                .replaceAll("[,]", "$0 ")
                .replaceAll("\\s+", " ");
        item.producer = jsonObj.optString("producer");
        item.genre = jsonObj.optString("genre");
        item.buyURL = jsonObj.optString("buy_link");
        item.videoAd = jsonObj.optString("video_ad").equals("yes");
        item.streamOnly = jsonObj.optString("stream_only").equals("yes");
        item.amp = jsonObj.optBoolean("amp");
        item.ampDuration = jsonObj.optInt("amp_duration");

        long releaseDate = jsonObj.optLong("original_release_date") != 0 ? jsonObj.optLong("original_release_date") : jsonObj.optLong("released");
        item.released = DateUtils.getItemDateAsString(releaseDate*1000);
        item.songReleaseDate = releaseDate*1000;

        item.premiumDownload = jsonObj.optString("premium_download");

        // process playlist items and other stuff if playlist
        if (item.isPlaylist()) {
            item.privatePlaylist = jsonObj.optString("private").equals("yes");
            item.created = DateUtils.getItemDateAsString(jsonObj.optLong("created")*1000);
            item.lastUpdated = DateUtils.getItemDateAsString(jsonObj.optLong("updated")*1000);
            item.playlistTracksCount = jsonObj.optInt("track_count");

            JSONArray playlistItemsArray = jsonObj.optJSONArray("tracks");
            if(playlistItemsArray != null){
                List<AMResultItem> playlistItems = new ArrayList<>();
                for(int i=0; i<playlistItemsArray.length(); i++){
                    JSONObject playlistItemObj = playlistItemsArray.optJSONObject(i);
                    if(playlistItemObj != null){
                        AMResultItem playlistItem = AMResultItem.fromJson(playlistItemObj, ignoreGeorestrictedMusic, null);
                        if (playlistItem != null) {
                            playlistItem.type = TYPE_PLAYLIST_TRACK;
                            playlistItem.parentId = item.itemId;
                            playlistItem.playlist = item.title;
                            playlistItem.trackNumber = i + 1;
                            playlistItems.add(playlistItem);
                        }
                    }
                }
                if(playlistItems.size() > 0){
                    item.tracks = playlistItems;
                }
            }
        }

        // process tracks if album
        if (item.isAlbum()) {
            JSONArray tracksArray = jsonObj.optJSONArray("tracks");
            if(tracksArray != null){
                List<AMResultItem> tracks = new ArrayList<>();
                int trackNumber = 0;
                for(int i=0; i<tracksArray.length(); i++){
                    JSONObject trackObj = tracksArray.optJSONObject(i);

                    if(trackObj != null) {
                        AMResultItem track = new AMResultItem();

                        track.title = trackObj.optString("title");
                        track.url = trackObj.optString("streaming_url");
                        track.itemId = trackObj.optString("song_id");
                        track.featured = trackObj.optString("featuring");
                        track.type = TYPE_ALBUM_TRACK;
                        track.parentId = item.itemId;
                        track.album = item.title;
                        track.artist = !TextUtils.isEmpty(trackObj.optString("artist")) ? trackObj.optString("artist") : item.artist;
                        track.genre = !TextUtils.isEmpty(trackObj.optString("genre")) ? trackObj.optString("genre") : item.genre;
                        track.image = item.image;
                        track.originalImage = item.originalImage;
                        track.smallImage = item.smallImage;
                        track.uploaderName = item.uploaderName;
                        track.uploaderId = item.uploaderId;
                        track.uploaderSlug = item.uploaderSlug;
                        track.urlSlug = trackObj.optString("url_slug");
                        track.trackNumber = ++trackNumber;
                        track.amp = trackObj.optBoolean("amp");
                        track.ampDuration = trackObj.optInt("amp_duration");
                        track.uploaderFollowed = item.uploaderFollowed;
                        track.uploaderTwitter = item.uploaderTwitter;
                        track.albumReleaseDate = item.songReleaseDate;
                        long trackReleaseDate = trackObj.optLong("original_release_date") != 0 ? trackObj.optLong("original_release_date") : trackObj.optLong("released");
                        track.songReleaseDate = trackReleaseDate*1000;

                        track.duration = trackObj.optLong("duration");
                        if(trackObj.has("volume_data")){
                            try {
                                JSONArray volumesArray = new JSONArray(trackObj.optString("volume_data"));
                                track.volumeData = new int[volumesArray.length()];
                                for (int k = 0; k < volumesArray.length(); k++) {
                                    track.volumeData[k] = volumesArray.getInt(k);
                                }
                            }catch (Exception e){
                                // Songs that do not have volume_data are given a random array
                            }
                        }
                        track.geoRestricted = trackObj.optBoolean("geo_restricted");
                        track.extraKey = extraKey;

                        track.premiumDownload = !TextUtils.isEmpty(trackObj.optString("premium_download")) ? trackObj.optString("premium_download") : item.premiumDownload;

                        tracks.add(track);
                    }
                }
                if(tracks.size() > 0){
                    item.tracks = tracks;
                }
            }
        }

        // process stats if included
        JSONObject statsObj = jsonObj.optJSONObject("stats");
        if(statsObj != null){
            item.playsCount = statsObj.optLong("plays-raw");
            item.favoritesCount = statsObj.optLong("favorites-raw");
            item.repostsCount = statsObj.optLong("reposts-raw");
            item.playlistsCount = statsObj.optLong("playlists-raw");
            item.commentCount = statsObj.optInt("comments");
            JSONObject rankObj = statsObj.optJSONObject("rankings");
            if(rankObj != null) {
                item.rankDaily = rankObj.optString("daily");
                item.rankWeekly = rankObj.optString("weekly");
                item.rankMonthly = rankObj.optString("monthly");
                item.rankAllTime = rankObj.optString("total");
            }
        }

        if(jsonObj.has("volume_data")){
            try {
                JSONArray volumesArray = new JSONArray(jsonObj.optString("volume_data"));
                item.volumeData = new int[volumesArray.length()];
                for (int i = 0; i < volumesArray.length(); i++) {
                    item.volumeData[i] = volumesArray.getInt(i);
                }
            }catch (Exception e){
                // Songs that do not have volume_data are given a random array
            }
        }

        item.duration = jsonObj.optLong("duration");
        item.repostTimestamp = jsonObj.optLong("repost_ts");
        JSONObject repostArtistObj = jsonObj.optJSONObject("repost_artist");
        if(repostArtistObj != null){
            item.repostArtistName = repostArtistObj.optString("name", null);
        }else{
            item.repostArtistName = jsonObj.optString("repost", null);
        }

        item.status = jsonObj.optString("status");

        item.banner = jsonObj.optString("image_banner", null);

        item.tags = jsonObj.optString("tagdisplay");

        return item;
    }

    @Deprecated // Use MusicDAO instead
    public static AMResultItem findById(String itemId) {
        if (TextUtils.isEmpty(itemId)) {
            return null;
        }
        return new Select().from(AMResultItem.class).where("item_id = ?", itemId).executeSingle();
    }

    @Deprecated // Use MusicDAO instead
    public static AMResultItem findDownloadedById(String itemId) {
        return new Select().from(AMResultItem.class).where("item_id = ? AND (cached = ? OR cached IS NULL)", itemId, false).executeSingle();
    }

    // TODO move to MusicDAO
    public static @NotNull List<String> getAllItemsIds() {
        List<AMResultItem> items = new Select("ID", "item_id").from(AMResultItem.class).where("(type = ? OR type = ? OR type = ? OR album_track_downloaded_as_single = ?) AND (cached = ? OR cached IS NULL)", TYPE_ALBUM, TYPE_SONG, TYPE_PLAYLIST_TRACK, true, false).execute();
        List<String> itemsIDs = new ArrayList<>();
        for (AMResultItem item : items) {
            if (!TextUtils.isEmpty(item.itemId)) {
                itemsIDs.add(item.itemId);
            }
        }
        return itemsIDs;
    }

    public static boolean isAlbumFullyDownloaded(String albumId) {
        return isAlbumFullyDownloaded(albumId, null);
    }

    private static boolean isAlbumFullyDownloaded(String albumId, @Nullable AMResultItem album) {
        if (TextUtils.isEmpty(albumId)) {
            return false;
        }
        List<AMResultItem> tracks = new Select("ID", "download_completed").from(AMResultItem.class).where("parent_id = ?", albumId).execute();
        if (tracks.size() == 0 || (album != null && album.tracks != null && tracks.size() < album.tracks.size())) {
            return false;
        }
        for (AMResultItem track : tracks) {
            if (!track.downloadCompleted) {
                return false;
            }
        }
        return true;
    }

    public static boolean isPlaylistFullyDownloaded(String playlistId) {
        AMResultItem dbItem = AMResultItem.findById(playlistId);
        if (dbItem == null) {
            return false;
        }
        List<AMPlaylistTracks> references = AMPlaylistTracks.tracksForPlaylist(playlistId);
        if (references.size() == 0) {
            return false;
        }
        List<String> trackIds = new ArrayList<>();
        for (AMPlaylistTracks reference : references) {
            trackIds.add(reference.getTrackId());
        }
        int downloadCompletedCount = new Select("count(download_completed)").from(AMResultItem.class).where("download_completed = 1 AND item_id IN (" + TextUtils.join(",", trackIds) + ")").count();
        return trackIds.size() == downloadCompletedCount;
    }

    @Deprecated // remove from here, access [MusicDownloader] instead
    public boolean isDownloadInProgress() {
        return AMMusicDownloader.getInstance().isMusicBeingDownloaded(this);
    }

    @Deprecated // remove from here, access [MusicDownloader] instead
    public boolean isDownloadQueued() {
        return AMMusicDownloader.getInstance().isMusicWaitingForDownload(this);
    }

    public static void markMusicAsSynced(@NonNull String musicId) {
        try {
            if (!musicId.contains(",")) {
                new Update(AMResultItem.class).set("synced = ?", 1).where("item_id = ?", musicId).execute();
            } else {
                new Update(AMResultItem.class).set("synced = ?", 1).execute();
            }
        } catch (SQLiteException e) {
            // noop
        }
    }

    public boolean isDownloaded() {
        return findById(itemId) != null;
    }

    public boolean isDownloadedAndNotCached() {
        AMResultItem dbItem = findById(itemId);
        return dbItem != null && !dbItem.cached;
    }

    @WorkerThread
    public boolean isDownloadFrozen() {
        if (getDownloadType() == MusicDownloadType.Free) {
            return false;
        }
        if (PremiumRepository.getInstance().isPremium()) {
            return false;
        }
        AMResultItem dbItem = findById(itemId);
        if (dbItem != null) {
            frozen = dbItem.frozen;
        }
        return frozen;
    }

    @WorkerThread
    public boolean isPremiumOnlyDownloadFrozen() {
        if (getDownloadType() != MusicDownloadType.Premium) {
            return false;
        }
        if (PremiumRepository.getInstance().isPremium()) {
            return false;
        }
        AMResultItem dbItem = findById(itemId);
        return dbItem != null;
    }

    public boolean isSong() { return (TYPE_SONG.equals(type)); }

    public boolean isAlbum() { return (TYPE_ALBUM.equals(type)); }

    public boolean isAlbumTrack() { return (TYPE_ALBUM_TRACK.equals(type)); }

    public boolean isPlaylist() { return (TYPE_PLAYLIST.equals(type)); }

    public boolean isPlaylistTrack() { return (TYPE_PLAYLIST_TRACK.equals(type)); }

    public boolean isPodcast() { return (TYPE_PODCAST.equals(genre)); }

    public AMGenre getAMGenre() {
        return AMGenre.fromApiValue(genre);
    }

    public String getTrackIDs() {
        List<String> ids = new ArrayList<>();
        if (tracks != null) {
            for (AMResultItem track : tracks) {
                ids.add(track.getItemId());
            }
        }
        return TextUtils.join(",", ids);
    }

    public boolean isUploadedByMyself(Context context){
        try {
            if (Credentials.isLogged(context)) {
                Credentials credentials = Credentials.load(context);
                if (credentials != null) {
                    return uploaderSlug != null && uploaderSlug.equals(credentials.getUserUrlSlug());
                }
            }
        } catch (Exception e) {
            Timber.w(e);
        }
        return false;
    }

    @Nullable
    public String getImageURLWithPreset(ItemImagePreset preset) {
        String url;
        switch (preset) {
            case ItemImagePresetSmall:
                url = smallImage;
                break;
            case ItemImagePresetSong:
                url = songImage;
                break;
            case ItemImagePresetPlaylist:
                url = playlistImage;
                break;
            case ItemImagePresetOriginal:
                url = originalImage;
                break;
            default:
                url = image;
                break;
        }
        if (TextUtils.isEmpty(url)) {
            url = !TextUtils.isEmpty(image) ? image : (!TextUtils.isEmpty(songImage) ? songImage : (!TextUtils.isEmpty(playlistImage) ? playlistImage : originalImage));
        }
        return url;
    }

    public boolean isTakenDown() {
        return "suspended".equals(status) || "takedown".equals(status) || "unplayable".equals(status);
    }

    public void updatePlaylist(AMResultItem playlist) {
        this.title = playlist.title;
        this.image = playlist.image;
        this.lastUpdated = playlist.lastUpdated;
        this.originalImage = playlist.originalImage;
        this.playlistImage = playlist.playlistImage;
        this.smallImage = playlist.smallImage;
        this.songImage = playlist.songImage;
        this.banner = playlist.banner;
        AMMusicDownloader.getInstance().cacheImages(this);
        this.save();
    }

    public long getDuration() {
        return duration;
    }

    public String getItemId() {
        return itemId;
    }

    public String getType() {
        return type;
    }

    public String getTypeForHighlightingAPI() {
        if (TYPE_PLAYLIST_TRACK.equals(type) || TYPE_ALBUM_TRACK.equals(type)) {
            return TYPE_SONG;
        }
        return type;
    }

    @NonNull
    public String getTypeForMusicApi() {
        if (isPlaylist()) {
            return TYPE_PLAYLIST;
        } else if (isAlbum()) {
            return TYPE_ALBUM;
        }
        return TYPE_SONG;
    }

    /** Only use for Cast **/
    @NonNull
    public String getTypeForCastApi() {
        if (isPlaylistTrack()) {
            return TYPE_PLAYLIST;
        } else if (isAlbumTrack()) {
            return TYPE_ALBUM;
        }
        return TYPE_SONG;
    }

    @Nullable
    public Calendar getReleaseDate() {
        if (songReleaseDate == 0) return null;
        Date date = new Date(songReleaseDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    @Nullable
    public String getFormattedReleaseDate() {
        if (songReleaseDate == 0) return null;
        Date date = new Date(songReleaseDate);
        return SimpleDateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(date);
    }

    @Nullable
    public String getArtist() {
        return artist;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getAlbum() {
        return album;
    }

    public String getUrl() {
        return url;
    }

    public String getUrlSlug() {
        return urlSlug;
    }

    @Nullable
    public String getUploaderSlug() {
        return uploaderSlug;
    }

    public void setUploaderSlug(String uploaderSlug) {
        this.uploaderSlug = uploaderSlug;
    }

    public boolean isFavorited() {
        return UserData.INSTANCE.isItemFavorited(this);
    }

    public boolean isReposted() {
        return UserData.INSTANCE.isItemReuped(itemId);
    }

    @Nullable
    public List<AMResultItem> getTracks() {
        return tracks;
    }

    @Nullable
    public List<AMResultItem> getTracksWithoutRestricted() {
        List<AMResultItem> allTracks = getTracks();
        if (allTracks == null) {
            return null;
        } else {
            List<AMResultItem> result = new ArrayList<>();
            for (AMResultItem track : allTracks) {
                if (!track.isGeoRestricted()) {
                    result.add(track);
                }
            }
            return result;
        }
    }

    public void setTracks(List<AMResultItem> tracks) {
        this.tracks = tracks;
    }

    public void setTracksAndRemoveRestricted(List<AMResultItem> tracks) {
        List<AMResultItem> nonRestrictedTracks = new ArrayList<>();
        if (tracks != null) {
            for (AMResultItem track : tracks) {
                if (!track.isGeoRestricted()) {
                    nonRestrictedTracks.add(track);
                }
            }
        }
        this.tracks = nonRestrictedTracks;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public int getDiscNumber() {
        return discNumber;
    }

    @Deprecated
    public String getFullPath() {
        return fullPath;
    }

    @Deprecated
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }


    public String getCreated() {
        return created;
    }

    @Nullable
    public String getLastUpdated() {
        return lastUpdated;
    }

    public boolean isPrivatePlaylist() {
        return privatePlaylist;
    }

    @Nullable
    public String getGenre() {
        return genre;
    }

    @Nullable
    public String getFeatured() {
        return featured;
    }

    @Nullable
    public String getProducer() {
        return producer;
    }

    @Nullable
    public String getDesc() {
        return desc;
    }

    @Nullable
    public String getUploaderName() {
        return uploaderName;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    @Nullable
    public String getRankDaily() {
        return rankDaily;
    }

    @Nullable
    public String getRankWeekly() {
        return rankWeekly;
    }

    @Nullable
    public String getRankMonthly() {
        return rankMonthly;
    }

    @Nullable
    public String getRankAllTime() {
        return rankAllTime;
    }

    @Nullable
    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    @Nullable
    public String getReleased() {
        return released;
    }

    public boolean isUploaderVerified() {
        return uploaderVerified;
    }

    public boolean isUploaderTastemaker() {
        return uploaderTastemaker;
    }

    public boolean isUploaderAuthenticated() {
        return uploaderAuthenticated;
    }

    public String getUploaderTinyImage() {
        if(!TextUtils.isEmpty(uploaderImage)){
            return uploaderImage + "?width=" + SizesRepository.INSTANCE.getTinyArtist();
        }
        return uploaderImage;
    }

    public String getUploaderLargeImage() {
        if(!TextUtils.isEmpty(uploaderImage)){
            return uploaderImage + "?width=" + SizesRepository.INSTANCE.getLargeArtist();
        }
        return uploaderImage;
    }

    public boolean isAlbumTrackDownloadedAsSingle() {
        return albumTrackDownloadedAsSingle;
    }

    public void setAlbumTrackDownloadedAsSingle(boolean albumTrackDownloadedAsSingle) {
        this.albumTrackDownloadedAsSingle = albumTrackDownloadedAsSingle;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public int[] getVolumeData() {
        return volumeData;
    }

    public int getPlaylistTracksCount() {
        return playlistTracksCount;
    }

    public void setPlaylistTracksCount(int playlistTracksCount) {
        this.playlistTracksCount = playlistTracksCount;
    }

    @NonNull
    public String getPremiumDownloadRawString() {
        return premiumDownload == null || TextUtils.isEmpty(premiumDownload) || premiumDownload.equals("false") ? "no" : premiumDownload;
    }

    @Nullable
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getRepostArtistName() {
        return repostArtistName;
    }

    public String getPlaylist() {
        return playlist;
    }

    public void setPlaylist(String playlist) {
        this.playlist = playlist;
    }

    public boolean isOfflineToastShown() {
        return offlineToastShown;
    }

    public void setOfflineToastShown(boolean offlineToastShown) {
        this.offlineToastShown = offlineToastShown;
    }

    public String getOriginalImage() {
        return originalImage;
    }

    public void setOriginalImage(String originalImage) {
        this.originalImage = originalImage;
    }

    public String getSmallImage() {
        return smallImage;
    }

    public void setSmallImage(String smallImage) {
        this.smallImage = smallImage;
    }

    @Nullable
    public String getUploaderTwitter() {
        return uploaderTwitter;
    }

    public boolean isVerifiedSearchResult() {
        return verifiedSearchResult;
    }

    public void setVerifiedSearchResult(boolean verifiedSearchResult) {
        this.verifiedSearchResult = verifiedSearchResult;
    }

    @NonNull
    public ItemAPIStatus getFavoriteStatus() {
        if (favoriteStatus == null) {
            favoriteStatus = UserData.INSTANCE.isItemFavorited(this) ? AMResultItem.ItemAPIStatus.On : AMResultItem.ItemAPIStatus.Off;
        }
        return favoriteStatus;
    }

    public BehaviorSubject<ItemAPIStatus> getFavoriteSubject() {
        return favoriteSubject;
    }

    public void setFavoriteStatus(ItemAPIStatus favoriteStatus) {
        boolean valueChanged = favoriteStatus != this.favoriteStatus;
        this.favoriteStatus = favoriteStatus;
        if (valueChanged) {
            this.favoriteSubject.onNext(favoriteStatus);
        }
    }

    public BehaviorSubject<ItemAPIStatus> getRepostSubject() {
        return repostSubject;
    }

    public void setRepostStatus(ItemAPIStatus repostStatus) {
        this.repostStatus = repostStatus;
        this.repostSubject.onNext(repostStatus);
    }

    public ItemAPIStatus getAddToPlaylistStatus() {
        return addToPlaylistStatus;
    }

    public BehaviorSubject<ItemAPIStatus> getAddToPlaylistSubject() {
        return addToPlaylistSubject;
    }

    public void setAddToPlaylistStatus(ItemAPIStatus addToPlaylistStatus) {
        this.addToPlaylistStatus = addToPlaylistStatus;
        this.addToPlaylistSubject.onNext(addToPlaylistStatus);
    }

    public BehaviorSubject<Integer> getCommentsCountSubject() {
        return commentsCountSubject;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
        this.commentsCountSubject.onNext(commentCount);
    }

    public MusicDownloadType getDownloadType() {
        String rawString = getPremiumDownloadRawString();
        if ("premium-limited".equals(rawString)) {
            return MusicDownloadType.Limited;
        } else if ("premium-only".equals(rawString)) {
            return MusicDownloadType.Premium;
        }
        return MusicDownloadType.Free;
    }

    public boolean isDownloadCompleted(boolean refresh) {
        if (refresh || getId() == null) {
            AMResultItem updatedItem = findById(itemId);
            if (updatedItem != null) {
                downloadCompleted = updatedItem.downloadCompleted;
                return downloadCompleted;
            } else {
                downloadCompleted = false;
            }
        }
        return downloadCompleted;
    }

    public boolean isDownloadCompleted() {
        return isDownloadCompleted(true);
    }

    /**
     * @return true if a song or an album with all its tracks or a playlist tracklist are successfully downloaded
     */
    public boolean isDownloadCompletedIndependentlyFromType() {
        if (isAlbum()) {
            return AMResultItem.isAlbumFullyDownloaded(itemId, this) && isDownloadCompleted();
        } else if (isPlaylist()) {
            return AMResultItem.isPlaylistFullyDownloaded(itemId);
        } else {
            return isDownloadCompleted();
        }
    }

    public void setDownloadCompleted(boolean downloadCompleted) {
        this.downloadCompleted = downloadCompleted;
    }

    public Date getDownloadDate() {
        return downloadDate;
    }

    public void setDownloadDate(Date downloadDate) {
        this.downloadDate = downloadDate;
    }

    @Nullable
    public String getBanner() {
        return banner;
    }

    public long getSongReleaseDate() {
        return songReleaseDate;
    }

    public long getAlbumReleaseDate() {
        return albumReleaseDate;
    }

    public boolean isGeoRestricted() {
        return geoRestricted;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public String getExtraKey() {
        return extraKey;
    }

    public @Nullable Integer getNewlyAddedSongs() {
        return newlyAddedSongs;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof AMResultItem) && (itemId.equals(((AMResultItem) obj).itemId));
    }

    @WorkerThread
    public void loadTracks() {
        if (isLocal) {
            AMResultItem album = LocalMediaRepository.getInstance().getAlbum(Long.parseLong(itemId)).blockingGet();
            this.tracks = album.tracks;
            return;
        }

        if (getId() == null) return;

        if(isAlbum()){
            this.tracks = new Select().from(AMResultItem.class).where("parent_id = ?", this.itemId).orderBy("track_number ASC").execute();
        } else if (isPlaylist()) {
            List<AMPlaylistTracks> references = AMPlaylistTracks.tracksForPlaylist(this.itemId);
            List<AMResultItem> tracks = new ArrayList<>();
            for (int i = 0; i < references.size(); i++) {
                AMPlaylistTracks reference = references.get(i);
                AMResultItem track = AMResultItem.findById(reference.getTrackId());
                if (track != null) {
                    track.trackNumber = i + 1; // trackNumber was not always saved to db
                    tracks.add(track);
                }
            }
            this.tracks = tracks;
        }
    }

    public @Nullable MixpanelSource getMixpanelSource() {
        return currentMixpanelSource != null ? currentMixpanelSource : MixpanelSource.fromJSON(originalMixpanelSource);
    }

    public void setMixpanelSource(MixpanelSource mixpanelSource) {
        this.currentMixpanelSource = mixpanelSource;
        this.originalMixpanelSource = mixpanelSource.toJSON();
    }

    public void deepDelete() {
        // TODO move out of this class

        if (isAlbum() || isPlaylist()) {
            loadTracks();
            if (tracks != null) {
                for (AMResultItem track : tracks) {
                    track.deepDelete();
                }
            }
        }

        if (isPlaylist()) {
            AMPlaylistTracks.deletePlaylist(itemId);
        }

        downloadCompleted = false;

        AMResultItem dbItem = AMResultItem.findById(itemId);
        if (dbItem != null) {
            File file = StorageKt.getFile(StorageProvider.getInstance(), dbItem);

            if (file != null && file.exists()) {
                File parentDir = file.getParentFile();
                file.delete();
                if (isAlbumTrack() && parentDir != null && parentDir.isDirectory()) {
                    String[] parentDirList = parentDir.list();
                    if (parentDirList != null && parentDirList.length == 0) {
                        parentDir.delete();
                    }
                }
            }
        }

        // Remove cached artworks
        try {
            String[] urls = new String[]{
                    getImageURLWithPreset(ItemImagePreset.ItemImagePresetSmall),
                    getImageURLWithPreset(ItemImagePreset.ItemImagePresetSong),
                    getImageURLWithPreset(ItemImagePreset.ItemImagePresetPlaylist),
                    getImageURLWithPreset(ItemImagePreset.ItemImagePresetOriginal),
                    banner
            };
            for (String url : urls) {
                if (!TextUtils.isEmpty(url)) {
                    File artFile = Utils.INSTANCE.remoteUrlToArtworkFile(MainApplication.getContext(), url);
                    if (artFile != null) artFile.delete();
                }
            }
        } catch (Exception e) {
            Timber.w(e);
        }

        if (getId() != null) {
            delete();
        }
        new Delete().from(AMResultItem.class).where("item_id = ?", this.itemId).execute();
    }

    public void sanityCheck() {
        // TODO move out of this class

        if (isAlbum()) {

            loadTracks();
            for (AMResultItem track : tracks) {
                track.sanityCheck();
            }
            loadTracks();
            if (tracks == null || tracks.size() == 0) {
                deepDelete();
            }

        } else if (isPlaylist()) {

            loadTracks();
            for (AMResultItem track : tracks) {
                track.sanityCheck();
            }

        } else {
            if (StorageKt.isFileValid(StorageProvider.getInstance(), this)) {

                if (!downloadCompleted) {
                    downloadCompleted = true;

                    AMResultItem dbItem = findDownloadedById(itemId);
                    if (dbItem != null) {
                        dbItem.downloadCompleted = true;
                        dbItem.save();
                    }
                }
            }
        }
    }

    private void refreshData(AMResultItem newItem, AMResultItem parentAlbum) {
        url = newItem.url;
        if (parentAlbum != null) {
            uploaderSlug = parentAlbum.uploaderSlug;
            urlSlug = parentAlbum.urlSlug;
        }
    }

    public @NonNull Observable<AMResultItem> refreshInfo() {
        Observable<AMResultItem> itemInfo;

        if (isAlbumTrack()) {
            itemInfo = API.getInstance().getAlbumInfo(parentId);
        } else if (isAlbum()) {
            itemInfo = API.getInstance().getAlbumInfo(itemId);
        } else if (isPlaylist()) {
            itemInfo = API.getInstance().getPlaylistInfo(itemId);
        } else {
            itemInfo = API.getInstance().getSongInfo(itemId);
        }

        return itemInfo.map(item -> {
            if (item != null) {
                if (item.getTracks() != null) {
                    for (AMResultItem track : item.getTracks()) {
                        if (track.getItemId().equals(itemId)) {
                            refreshData(track, item);
                            return track;
                        } else if (isAlbum()) {
                            refreshData(track, item);
                        }
                    }
                    if (isAlbum() || isPlaylist()) {
                        return AMResultItem.this;
                    }
                } else {
                    refreshData(item, null);
                    return item;
                }
            }
            return item;
        });
    }

    @Deprecated // "Use [AddMusicToQueueUseCase] instead"
    public void playNext(@Nullable Activity context, @NonNull MixpanelSource mixpanelSource,
                         @NonNull String mixpanelButton, @NonNull CompositeDisposable disposables) {
        addToQueue(context, false, mixpanelSource, disposables);
        new MixpanelRepository().trackQueue(this, QueueType.PlayNext, mixpanelSource, mixpanelButton);
    }

    @Deprecated // "Use [AddMusicToQueueUseCase] instead"
    public void playLater(@Nullable Activity context, @NonNull MixpanelSource mixpanelSource,
                          @NonNull String mixpanelButton, @NonNull CompositeDisposable disposables) {
        addToQueue(context, true, mixpanelSource, disposables);
        new MixpanelRepository().trackQueue(this, QueueType.AddToQueue, mixpanelSource, mixpanelButton);
    }

    private void addToQueue(@Nullable Activity context, boolean atTheEnd, @NonNull MixpanelSource mixpanelSource,
                            @NonNull CompositeDisposable disposables) {

        try {
            if (isPlaylist()) {
                if (isDownloaded()) {
                    loadTracks();
                    HomeActivity.getInstance().addSongs(tracks, atTheEnd, mixpanelSource);
                    ExtensionsKt.showAddedToQueueToast(HomeActivity.getInstance());
                }else {
                    AMProgressHUD.showWithStatus(context);

                    disposables.add(API.getInstance().getPlaylistInfo(itemId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(item -> {
                                try {
                                    AMProgressHUD.dismiss();
                                    for (AMResultItem track : item.getTracks()) {
                                        track.playlist = item.title;
                                    }
                                    HomeActivity.getInstance().addSongs(item.getTracksWithoutRestricted(), atTheEnd, mixpanelSource);
                                    ExtensionsKt.showAddedToQueueToast(HomeActivity.getInstance());
                                } catch (Exception e) {
                                    Timber.w(e);
                                }
                            }, throwable -> {
                                try {
                                    new AMSnackbar.Builder(HomeActivity.getInstance())
                                            .withTitle(context.getString(R.string.queue_unable_to_add_playlist))
                                            .withDrawable(R.drawable.ic_snackbar_error)
                                            .withSecondary(R.drawable.ic_snackbar_queue_grey)
                                            .show();
                                } catch (Exception e) {
                                    Timber.w(e);
                                }
                            })
                    );
                }
            } else if (isAlbum()) {
                if (tracks == null || tracks.size() == 0) {
                    loadTracks();
                }
                HomeActivity.getInstance().addSongs(getTracksWithoutRestricted(), atTheEnd, mixpanelSource);
                ExtensionsKt.showAddedToQueueToast(HomeActivity.getInstance());
            }else{
                HomeActivity.getInstance().addSongs(Collections.singletonList(this), atTheEnd, mixpanelSource);
                ExtensionsKt.showAddedToQueueToast(HomeActivity.getInstance());
            }
        } catch (Exception e) {
            Timber.w(e);
        }
    }

    public @Nullable String getLink(){
        if (isAlbum()) {
            return "https://audiomack.com/" + uploaderSlug + "/album/" + urlSlug;
        } else if (isPlaylist()) {
            return "https://audiomack.com/" + uploaderSlug + "/playlist/" + urlSlug;
        } else if (isSong() || isPlaylistTrack() || isAlbumTrack()) {
            return "https://audiomack.com/" + uploaderSlug + "/song/" + urlSlug;
        }
        return null;
    }

    public void openShareSheet(@Nullable Activity context, @NonNull MixpanelSource mixpanelSource, @NonNull String mixpanelButton) {
        if (!(context instanceof BaseActivity)) {
            Timber.w("Wrong context");
            return;
        }
        ((BaseActivity) context).openOptionsFragment(SlideUpMenuShareFragment.newInstance(this, null, mixpanelSource, mixpanelButton));
    }

    public void persistCommentCount(int commentCount) {
        AMResultItem item = AMResultItem.findById(getItemId());
        if (item != null) {
            item.setCommentCount(commentCount);
            item.save();
        }
    }

    public boolean hasStats() {
        return playsCount != null;
    }

    public @NonNull String getPlaysShort() {
        return Utils.INSTANCE.formatShortStatNumber(playsCount);
    }

    public @NonNull String getPlaysExtended() {
        return Utils.INSTANCE.formatFullStatNumber(playsCount);
    }

    public @NonNull String getFavoritesShort() {
        return Utils.INSTANCE.formatShortStatNumber(favoritesCount);
    }

    public @NonNull String getRepostsShort() {
        return Utils.INSTANCE.formatShortStatNumber(repostsCount);
    }

    public @NonNull String getPlaylistsShort() {
        return Utils.INSTANCE.formatShortStatNumber(playlistsCount);
    }

    public @NonNull String getUploaderFollowersExtended() {
        return Utils.INSTANCE.formatFullStatNumber(uploaderFollowers);
    }

    public @NonNull String getCommentsShort() {
        return Utils.INSTANCE.formatShortStatNumber((long) commentCount);
    }

    public boolean hasUploaderFollowers() {
        return uploaderFollowers != null;
    }

    public void updateSongWithFreshData(@NotNull AMResultItem freshItem) {
        uploaderVerified = freshItem.uploaderVerified;
        uploaderTastemaker = freshItem.uploaderTastemaker;
        uploaderImage = freshItem.uploaderImage;
        uploaderFollowers = freshItem.uploaderFollowers;
        uploaderName = freshItem.uploaderName;
        uploaderAuthenticated = freshItem.uploaderAuthenticated;
        tags = freshItem.tags;
    }

    @NonNull
    public String[] getTags() {
        return !TextUtils.isEmpty(tags) ? TextUtils.split(tags, ",") : new String[]{};
    }

    @NotNull
    @Override
    public String toString() {
        switch (type) {
            case TYPE_ALBUM: {
                return "{"
                        + "\"itemId\":\"" + itemId + "\""
                        + ", \"type\":\"" + type + "\""
                        + ", \"title\":\"" + title + "\""
                        + ", \"tracks\":\"" + tracks + "\""
                        + ", \"isLocal\":\"" + isLocal + "\""
                        + "}";
            }
            case TYPE_ALBUM_TRACK: {
                return "{"
                        + "\"itemId\":\"" + itemId + "\""
                        + ", \"type\":\"" + type + "\""
                        + ", \"parentId\":\"" + parentId + "\""
                        + ", \"title\":\"" + title + "\""
                        + ", \"trackNumber\":\"" + trackNumber + "\""
                        + ", \"isLocal\":\"" + isLocal + "\""
                        + "}";
            }
        }
        return "{"
                + "\"itemId\":\"" + itemId + "\""
                + ", \"type\":\"" + type + "\""
                + ", \"title\":\"" + title + "\""
                + ", \"isLocal\":\"" + isLocal + "\""
                + "}";
    }
}
