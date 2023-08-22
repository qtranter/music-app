package com.audiomack.ui.home

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.LifecycleOwner
import com.audiomack.R
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.ui.mylibrary.offline.local.LocalMediaSelectionFragment
import com.audiomack.ui.mylibrary.search.MyLibrarySearchFragment
import com.audiomack.ui.notifications.NotificationsContainerFragment
import com.audiomack.ui.playlist.add.AddToPlaylistsActivity
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.ui.queue.QueueFragment
import com.audiomack.ui.settings.SettingsActivity

class HomeNavigationHandler(
    private val activity: AppCompatActivity,
    private val events: NavigationEvents
) {

    private val lifecycle: LifecycleOwner
        get() = activity

    private val fm: FragmentManager
        get() = activity.supportFragmentManager

    init {
        initNavigationEventObservers()
    }

    private fun initNavigationEventObservers() {
        with(events) {
            navigateBackEvent.observe(lifecycle) {
                fm.popBackStack()
            }

            launchQueueEvent.observe(lifecycle) {
                fm.commit {
                    val fragment = QueueFragment.newInstance()
                    add(R.id.fullScreenContainer, fragment, QueueFragment.TAG)
                    addToBackStack(QueueFragment.TAG)
                }
            }

            launchInAppPurchaseEvent.observe(lifecycle) { mode ->
                InAppPurchaseActivity.show(activity, mode)
            }

            launchLocalFilesSelectionEvent.observe(lifecycle) {
                fm.commit {
                    val fragment = LocalMediaSelectionFragment.newInstance()
                    add(R.id.fullScreenContainer, fragment, LocalMediaSelectionFragment.TAG)
                    addToBackStack(LocalMediaSelectionFragment.TAG)
                }
            }

            launchLoginEvent.observe(lifecycle) { source ->
                AuthenticationActivity.show(activity, source)
            }

            launchSettingsEvent.observe(lifecycle) {
                SettingsActivity.show(activity)
            }

            launchNotificationsEvent.observe(lifecycle) {
                fm.commit {
                    val fragment = NotificationsContainerFragment.newInstance()
                    add(R.id.fullScreenContainer, fragment, NotificationsContainerFragment.TAG)
                    addToBackStack(NotificationsContainerFragment.TAG)
                }
            }

            launchMyLibrarySearchEvent.observe(lifecycle) {
                fm.commit {
                    val fragment = MyLibrarySearchFragment.newInstance()
                    add(R.id.mainContainer, fragment)
                    addToBackStack(MyLibrarySearchFragment.TAG)
                }
            }

            launchAddToPlaylistEvent.observe(lifecycle) { model ->
                AddToPlaylistsActivity.show(activity, model)
            }
        }
    }
}
