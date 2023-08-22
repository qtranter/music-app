package com.audiomack.ui.home

import androidx.annotation.VisibleForTesting
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MaximizePlayerData
import com.audiomack.utils.SingleLiveEvent

class NavigationEvent<T> : SingleLiveEvent<T>()

interface NavigationActions {
    fun navigateBack()

    fun launchLogin(source: LoginSignupSource)
    fun launchQueue()
    fun launchLocalFilesSelection()
    fun launchInAppPurchase(mode: InAppPurchaseMode)
    fun launchPlayer(data: MaximizePlayerData)
    fun launchSettingsEvent()
    fun launchNotificationsEvent()
    fun launchMyLibrarySearchEvent()
    fun launchAddToPlaylist(model: AddToPlaylistModel)
}

interface NavigationEvents {
    val navigateBackEvent: NavigationEvent<Unit>

    val launchLoginEvent: NavigationEvent<LoginSignupSource>
    val launchQueueEvent: NavigationEvent<Unit>
    val launchLocalFilesSelectionEvent: NavigationEvent<Unit>
    val launchInAppPurchaseEvent: NavigationEvent<InAppPurchaseMode>
    val launchPlayerEvent: NavigationEvent<MaximizePlayerData>
    val launchSettingsEvent: NavigationEvent<Unit>
    val launchNotificationsEvent: NavigationEvent<Unit>
    val launchMyLibrarySearchEvent: NavigationEvent<Unit>
    val launchAddToPlaylistEvent: NavigationEvent<AddToPlaylistModel>
}

class NavigationManager private constructor() : NavigationActions, NavigationEvents {
    override val navigateBackEvent = NavigationEvent<Unit>()

    override val launchLoginEvent = NavigationEvent<LoginSignupSource>()
    override val launchQueueEvent = NavigationEvent<Unit>()
    override val launchInAppPurchaseEvent = NavigationEvent<InAppPurchaseMode>()
    override val launchLocalFilesSelectionEvent = NavigationEvent<Unit>()
    override val launchPlayerEvent = NavigationEvent<MaximizePlayerData>()
    override val launchSettingsEvent = NavigationEvent<Unit>()
    override val launchNotificationsEvent = NavigationEvent<Unit>()
    override val launchMyLibrarySearchEvent = NavigationEvent<Unit>()
    override val launchAddToPlaylistEvent = NavigationEvent<AddToPlaylistModel>()

    override fun navigateBack() = navigateBackEvent.call()

    override fun launchLogin(source: LoginSignupSource) = launchLoginEvent.postValue(source)
    override fun launchQueue() = launchQueueEvent.call()
    override fun launchLocalFilesSelection() = launchLocalFilesSelectionEvent.call()
    override fun launchInAppPurchase(mode: InAppPurchaseMode) =
        launchInAppPurchaseEvent.postValue(mode)
    override fun launchPlayer(data: MaximizePlayerData) = launchPlayerEvent.postValue(data)
    override fun launchSettingsEvent() = launchSettingsEvent.call()
    override fun launchNotificationsEvent() = launchNotificationsEvent.call()
    override fun launchMyLibrarySearchEvent() = launchMyLibrarySearchEvent.call()
    override fun launchAddToPlaylist(model: AddToPlaylistModel) = launchAddToPlaylistEvent.postValue(model)

    companion object {
        @Volatile
        private var sInstance: NavigationManager? = null

        fun getInstance(): NavigationManager =
            sInstance ?: NavigationManager().also { sInstance = it }

        @VisibleForTesting
        fun destroy() {
            sInstance = null
        }
    }
}
