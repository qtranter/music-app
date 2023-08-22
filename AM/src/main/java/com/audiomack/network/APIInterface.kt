package com.audiomack.network

import com.audiomack.model.AMArtist
import com.audiomack.model.AMComment
import com.audiomack.model.AMCommentVote
import com.audiomack.model.AMCommentsResponse
import com.audiomack.model.AMFeaturedSpot
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMVoteStatus
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.Credentials
import com.audiomack.model.NotificationPreferenceTypeValue
import com.audiomack.model.ReportContentReason
import com.audiomack.model.ReportContentType
import com.audiomack.model.ReportType
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.lang.Exception
import java.util.Date

interface APIInterface {
    interface AuthenticationInterface {
        fun loginWithEmailPassword(username: String, password: String): Single<Credentials>

        fun loginWithFacebook(fbId: String, fbToken: String, socialEmail: String?): Single<Credentials>

        fun loginWithGoogle(googleToken: String, socialEmail: String?): Single<Credentials>

        fun loginWithTwitter(twitterToken: String, twitterSecret: String, socialEmail: String?): Single<Credentials>

        fun loginWithAppleId(appleIdToken: String, socialEmail: String?): Single<Credentials>

        fun forgotPassword(email: String, listener: API.ForgotPasswordListener)

        fun signup(
            username: String,
            email: String,
            password: String,
            advertisingId: String?,
            birthday: Date,
            gender: AMArtist.Gender,
            listener: API.SignupListener
        )

        fun checkEmailExistence(email: String?, slug: String?): Observable<Boolean>

        fun logout(): Completable

        fun changePassword(oldPassword: String, newPassword: String): Completable

        fun verifyForgotPasswordToken(token: String): Completable

        fun resetPassword(token: String, newPassword: String): Completable
    }

    interface SocialLinkInterface {
        fun linkSocial(providerData: LoginProviderData): Completable
    }

    interface UserInterface {
        val userData: Observable<AMArtist>

        fun completeProfile(name: String, birthday: Date, gender: AMArtist.Gender): Completable

        fun editUserAccountInfo(artist: AMArtist): Single<AMArtist>

        fun editUserUrlSlug(artist: AMArtist): Single<AMArtist>
    }

    interface FeaturedSpotsInterface {
        fun getFeaturedMusic(): Single<List<AMFeaturedSpot>>
    }

    interface CommentsInterface {
        fun getSingleComments(kind: String, id: String, uuid: String, threadId: String?): Single<AMCommentsResponse>
        fun getComments(kind: String, id: String, limit: String, offset: String, sort: String): Single<AMCommentsResponse>
        fun postComment(content: String, kind: String, id: String, thread: String?): Single<AMComment>
        fun reportComment(kind: String, id: String, uuid: String, thread: String?): Single<Boolean>
        fun deleteComment(kind: String, id: String, uuid: String, thread: String?): Single<Boolean>
        fun getVoteStatus(kind: String, id: String): Single<ArrayList<AMVoteStatus>>
        fun voteComment(comment: AMComment, isUpVote: Boolean?, kind: String, id: String): Single<AMCommentVote>
    }

    interface DownloadsInterface {
        fun addDownload(musicId: String, mixpanelPage: String)
    }

    interface ReportInterface {
        fun reportBlock(reportType: ReportType, contentId: String, contentType: ReportContentType, reason: ReportContentReason): Completable
    }

    interface SearchInterface {
        fun searchAutoSuggest(query: String): Observable<List<String>>
        fun getRecommendations(): Single<List<AMResultItem>>
    }

    interface SettingsInterface {
        fun updateEnvironment()
    }

    interface EmailVerificationInterface {
        fun runEmailVerification(hash: String): Completable
    }

    interface NotificationSettingsInterface {
        fun getNotificationPreferences(): Single<List<NotificationPreferenceTypeValue>>
        fun setNotificationPreference(typeValue: NotificationPreferenceTypeValue): Single<Boolean>
    }

    interface AccountsInterface {
        fun getSuggestedFollows(page: Int): Observable<APIResponseData>
        fun getArtistsRecommendations(): Observable<APIResponseData>
    }

    interface FeedInterface {
        fun getMyFeed(
            page: Int,
            excludeReups: Boolean,
            ignoreGeorestrictedMusic: Boolean
        ): APIRequestData
    }

    companion object {
        const val DATE_FORMAT: String = "yyyy-MM-dd"
    }
}

class APIDetailedException(val title: String?, message: String?) : Exception("$message") {
    val verboseDescription: String get() = listOfNotNull(title, message).joinToString("\n")
}
