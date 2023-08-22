package com.audiomack.usecases.favorite

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.audiomack.R
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.showFavoritedToast
import com.audiomack.utils.showOfflineAlert
import com.audiomack.views.AMSnackbar
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class FavoriteEventsHandler(
    private val fragment: Fragment,
    private val events: FavoriteEvents
) {

    private val viewLifecycleOwner: LifecycleOwner
        get() = fragment.viewLifecycleOwner

    init {
        initNavigationEventObservers()
    }

    private fun initNavigationEventObservers() {
        with(events) {
            favoriteEvent.observe(viewLifecycleOwner) {
                fragment.activity?.showFavoritedToast(it)
            }

            loginRequiredEvent.observe(viewLifecycleOwner) { source ->
                (fragment.activity as? HomeActivity)?.showLoginRequiredAlert(source)
            }

            notifyOfflineEvent.observe(viewLifecycleOwner) {
                fragment.activity?.showOfflineAlert()
            }

            errorEvent.observe(viewLifecycleOwner) {
                onError(it)
            }
        }
    }

    private fun onError(throwable: Throwable) {
        Timber.tag(TAG).e(throwable)
        with(fragment) {
            AMSnackbar.Builder(activity)
                .withTitle("")
                .withSubtitle(getString(R.string.generic_api_error))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withDuration(Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    companion object {
        private const val TAG = "FavoritesEventsHandler"
    }
}

fun Fragment.setupFavoriteHandler(
    events: FavoriteEvents
): FavoriteEventsHandler {
    return FavoriteEventsHandler(this, events)
}
