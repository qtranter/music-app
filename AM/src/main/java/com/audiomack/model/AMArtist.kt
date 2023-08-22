package com.audiomack.model

import android.app.Activity
import android.text.TextUtils
import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table
import com.activeandroid.query.Delete
import com.activeandroid.query.Select
import com.audiomack.MainApplication
import com.audiomack.activities.BaseActivity
import com.audiomack.data.sizes.SizesRepository
import com.audiomack.network.APIInterface
import com.audiomack.ui.slideupmenu.share.SlideUpMenuShareFragment
import com.audiomack.utils.DateUtils
import com.audiomack.utils.Utils
import com.audiomack.utils.getStringOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min
import org.json.JSONObject
import timber.log.Timber

@Table(name = "artists")
class AMArtist : Model() {

    @Column(name = "artist_id", unique = true)
    var artistId: String? = null

    @Column(name = "name")
    var name: String? = null
    set(value) {
        field = value?.take(NAME_MAX_LENGTH)
    }

    @Column(name = "image")
    private var image: String? = null

    @Column(name = "genre")
    var genre: String? = null

    @Column(name = "hometown")
    var hometown: String? = null
    set(value) {
        field = value?.take(HOMETOWN_MAX_LENGTH)
    }

    @Column(name = "bio")
    var bio: String? = null
    set(value) {
        field = value?.take(BIO_MAX_LENGTH)
    }

    @Column(name = "url")
    var url: String? = null
    set(value) {
        field = value?.take(URL_MAX_LENGTH)
    }

    @Column(name = "url_slug")
    var urlSlug: String? = null
    set(value) {
        field = value?.take(URL_MAX_LENGTH)
    }

    @Column(name = "label")
    var label: String? = null
    set(value) {
        field = value?.take(LABEL_MAX_LENGTH)
    }

    @Column(name = "twitter")
    var twitter: String? = null

    @Column(name = "facebook")
    var facebook: String? = null

    @Column(name = "instagram")
    var instagram: String? = null

    @Column(name = "verified")
    var isVerified: Boolean = false
        private set

    @Column(name = "uploadsCount")
    var uploadsCount: Int = 0
        private set

    @Column(name = "favoritesCount")
    var favoritesCount: Int = 0
        private set

    @Column(name = "playlistsCount")
    var playlistsCount: Int = 0
        private set

    @Column(name = "followingCount")
    var followingCount: Int = 0
        private set

    @Column(name = "followersCount")
    var followersCount: Int = 0
        private set

    @Column(name = "feedCount")
    var feedCount: Int = 0

    @Column(name = "unseenNotificationsCount")
    var unseenNotificationsCount: Int = 0

    @Column(name = "created")
    private var created: Date? = null

    @Column(name = "admin")
    var isAdmin: Boolean = false
        private set

    @Column(name = "banner")
    var banner: String? = null
        private set

    @Column(name = "youtube")
    var youtube: String? = null

    @Column(name = "playsCount")
    var playsCount: Long = 0
        private set

    @Column(name = "tastemaker")
    var isTastemaker: Boolean = false
        private set

    @Column(name = "reupsCount")
    var reupsCount: Int = 0
        private set

    @Column(name = "pinnedCount")
    var pinnedCount: Int = 0
        private set

    @Column(name = "canComment")
    var canComment: Boolean = false
        private set

    @Column(name = "authenticated")
    var isAuthenticated: Boolean = false
        private set

    @Column(name = "twitter_id")
    var twitterId: String? = null
        private set

    @Column(name = "facebook_id")
    var facebookId: String? = null
        private set

    @Column(name = "instagram_id")
    var instagramId: String? = null
        private set

    @Column(name = "youtube_id")
    var youtubeId: String? = null
        private set

    /** UTC date **/
    @Column(name = "birthday")
    var birthday: Date? = null

    @Column(name = "gender")
    var gender: Gender? = null

    var imageBase64: String? = null
    var bannerBase64: String? = null

    var isHighlightedSearchResult: Boolean = false

    var urlSlugDisplay: String
        get() = String.format("@%s", urlSlug)
        set(newUrlSlug) {
            var urlSlug = newUrlSlug
            if (urlSlug.startsWith("@")) {
                urlSlug = urlSlug.replaceFirst("@".toRegex(), "")
            }
            this.urlSlug = urlSlug.substring(0, min(URL_MAX_LENGTH, urlSlug.length))
        }

    val followingExtended: String
        get() = Utils.formatFullStatNumber(followingCount.toLong())

    val followingShort: String
        get() = Utils.formatShortStatNumber(followingCount.toLong())

    val followersExtended: String
        get() = Utils.formatFullStatNumber(followersCount.toLong())

    val followersShort: String
        get() = Utils.formatShortStatNumber(followersCount.toLong())

    val playsExtended: String
        get() = Utils.formatFullStatNumber(playsCount)

    val playsShort: String
        get() = Utils.formatShortStatNumber(playsCount)

    val genrePretty: String
        get() {
            val amGenre = AMGenre.fromApiValue(genre)
            return if (amGenre === AMGenre.All) AMGenre.Other.humanValue(MainApplication.context) else amGenre.humanValue(
                MainApplication.context
            )
        }

    val amGenre: AMGenre
        get() {
            return AMGenre.fromApiValue(genre)
        }

    val link: String?
        get() = "https://audiomack.com/${urlSlug ?: ""}"

    val tinyImage: String?
        get() = image?.plus("?width=${SizesRepository.tinyArtist}")

    val smallImage: String?
        get() = image?.plus("?width=${SizesRepository.smallArtist}")

    val mediumImage: String?
        get() = image?.plus("?width=${SizesRepository.mediumArtist}")

    val largeImage: String?
        get() = image?.plus("?width=${SizesRepository.largeArtist}")

    fun getCreated(): String {
        return created?.let { DateUtils.getInstance().getArtistCreatedAsString(it) } ?: ""
    }

    fun openShareSheet(activity: Activity?, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        if (activity !is BaseActivity) {
            Timber.w("Wrong context")
            return
        }
        activity.openOptionsFragment(
            SlideUpMenuShareFragment.newInstance(
                null,
                this,
                mixpanelSource,
                mixpanelButton
            )
        )
    }

    @JvmOverloads
    fun getBirthdayString(format: String = APIInterface.DATE_FORMAT) =
        birthday?.let { SimpleDateFormat(format, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(it) }

    val age: Int?
        get() = birthday?.let { DateUtils.getAge(it) }

    val yob: Int?
        get() = birthday?.let { DateUtils.getYOB(it) }

    val needsProfileCompletion: Boolean
        get() = birthday == null || gender == null

    override fun equals(other: Any?): Boolean {
        return other is AMArtist && artistId == other.artistId
    }

    override fun hashCode(): Int {
        return artistId?.hashCode() ?: super.hashCode()
    }

    enum class Gender(private val key: String) {
        MALE("male"), FEMALE("female"), NON_BINARY("non-binary");

        override fun toString() = key

        companion object {
            fun forKey(key: String) = when (key) {
                MALE.key -> MALE
                FEMALE.key -> FEMALE
                else -> NON_BINARY
            }
        }
    }

    companion object {

        private const val NAME_MAX_LENGTH = 65
        private const val LABEL_MAX_LENGTH = 65
        private const val HOMETOWN_MAX_LENGTH = 65
        const val URL_MAX_LENGTH = 80
        const val BIO_MAX_LENGTH = 900
        const val AVATAR_MAX_SIZE_PX = 1024

        @JvmStatic
        @Deprecated("Use ArtistDAO instead (both sync and rx calls available)", ReplaceWith(
            "findSync()",
            "com.audiomack.data.database.ArtistDAO"
        ))
        fun getSavedArtist(): AMArtist? {
            return Select().from(AMArtist::class.java).executeSingle()
        }

        fun isMyAccount(artist: AMArtist): Boolean {
            val myArtist = getSavedArtist()
            return myArtist?.artistId != null && TextUtils.equals(
                myArtist.artistId,
                artist.artistId
            )
        }

        @JvmStatic
        fun fromJSON(myAccount: Boolean, jsonObject: JSONObject): AMArtist {

            var artist: AMArtist?

            if (myAccount) {
                artist = getSavedArtist()
                if (artist == null) {
                    artist = AMArtist()
                }
            } else {
                artist = AMArtist()
            }

            artist.artistId = jsonObject.optString("id")
            artist.name = jsonObject.optString("name")
            artist.image = jsonObject.optString("image_base")
            artist.genre = jsonObject.optString("genre")
            artist.hometown = jsonObject.optString("hometown")
            artist.bio = jsonObject.optString("bio")
            artist.url = jsonObject.optString("url")
            artist.urlSlug = jsonObject.optString("url_slug")
            artist.label = jsonObject.optString("label")
            artist.twitter = jsonObject.optString("twitter")
            artist.facebook = jsonObject.optString("facebook")
            artist.instagram = jsonObject.optString("instagram")
            artist.isVerified = jsonObject.optString("verified") == "yes"
            artist.isTastemaker = jsonObject.optString("verified") == "tastemaker"
            artist.isAuthenticated = listOf("authenticated", "verify-pending", "verify-declined")
                    .contains(jsonObject.optString("verified"))
            artist.uploadsCount = jsonObject.optInt("upload_count_excluding_reups")
            artist.favoritesCount = (jsonObject.optJSONArray("favorite_music")?.length() ?: 0) + (jsonObject.optJSONArray("favorite_playlists")?.length() ?: 0)
            artist.playlistsCount = jsonObject.optJSONArray("playlists")?.length() ?: 0
            artist.followingCount = jsonObject.optInt("following_count")
            artist.followersCount = jsonObject.optInt("followers_count")
            artist.feedCount = jsonObject.optInt("new_feed_items")
            artist.created = Date(jsonObject.optLong("created") * 1000)
            artist.isAdmin = jsonObject.optBoolean("is_admin")
            artist.banner = jsonObject.optString("image_banner")
            artist.youtube = jsonObject.optString("youtube")
            artist.reupsCount = jsonObject.optJSONArray("reups")?.length() ?: 0
            artist.pinnedCount = jsonObject.optJSONArray("pinned")?.length() ?: 0
            artist.canComment = jsonObject.optBoolean("can_comment")
            artist.twitterId = jsonObject.getStringOrNull("twitter_id")?.takeIf { it != "false" }
            artist.facebookId = jsonObject.getStringOrNull("facebook_id")?.takeIf { it != "false" }
            artist.instagramId = jsonObject.getStringOrNull("instagram_id")?.takeIf { it != "false" }
            artist.youtubeId = jsonObject.getStringOrNull("youtube_id")?.takeIf { it != "false" }
            artist.gender = jsonObject.optString("gender")?.takeIf { it.isNotBlank() && it != "false" }?.let {
                Gender.forKey(it)
            }
            artist.birthday = jsonObject.optString("birthday")?.takeIf { it.isNotBlank() && it != "false" }?.let {
                SimpleDateFormat(APIInterface.DATE_FORMAT, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(it)
            }
            jsonObject.optJSONObject("stats")?.let { stats ->
                artist.playsCount = stats.optLong("plays-raw")
            }

            if (myAccount && !jsonObject.getStringOrNull("email").isNullOrEmpty()) {
                val credentials = Credentials.load(MainApplication.context)
                if (credentials != null) {
                    credentials.email = jsonObject.optString("email")
                    Credentials.save(credentials, MainApplication.context!!)
                }
                artist.save()
            }

            return artist
        }

        fun logout() {
            Delete().from(AMArtist::class.java).execute<AMArtist>()
        }
    }
}
