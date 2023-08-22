package com.audiomack.model

import com.audiomack.ui.filter.FilterData

class EventDownload(val itemId: String?, val completed: Boolean)
class EventFeaturedPostPulled
class EventFollowChange(val artistId: String)
class EventPlayer(val command: PlayerCommand)
class EventPlaylistDeleted(val item: AMResultItem)
class EventPlaylistEdited(val item: AMResultItem)
@Deprecated("Subscribe to PlayerPlayback.state.observable.distinctUntilChanged() instead")
class EventPlayPauseChange
class EventShowUnreadTicketsAlert
@Deprecated("Subscribe to PlayerPlayback.item.distinctUntilChanged() instead")
class EventSongChange
class EventHighlightsUpdated
class EventSearchFiltersChanged
class EventFavoriteStatusChanged(val itemId: String)
class EventRemovedDownloadFromList(val item: AMResultItem)
class EventDeletedDownload(val item: AMResultItem)
class EventCommentAdded(val comment: AMComment)
class EventCommentCountUpdated(val count: Int, val item: AMResultItem)
class EventToggleRemoveAdVisibility(val visible: Boolean)
class EventSocialEmailAdded(val email: String)
class EventShowDownloadSuccessToast(val music: AMResultItem)
class EventShowDownloadFailureToast
class EventShowAddedToOfflineInAppMessage(val premiumLimited: Boolean, val mixpanelSource: MixpanelSource, val downloadCount: Int)
class EventFavoriteDelete(val item: AMResultItem)
class EventTrackRemoved(val trackIds: List<String>)
class EventFilterSaved(val filter: FilterData)
class EventCommentIntroDismissed
class EventUploadDeleted(val item: AMResultItem)
class EventContentReported(val reportType: ReportType)
class EventDownloadsEdited
