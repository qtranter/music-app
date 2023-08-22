package com.audiomack.ui.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.GENERAL_PREFERENCES_INCLUDE_LOCAL_FILES
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMMusicType
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.EventFilterSaved
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource.OfflineFilter
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.home.NavigationActions
import com.audiomack.ui.home.NavigationManager
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import io.reactivex.Observable
import io.reactivex.Single
import org.greenrobot.eventbus.EventBus

class FilterViewModel(
    private val originalFilter: FilterData,
    private val preferencesRepo: PreferencesDataSource = PreferencesRepository(),
    private val navigationActions: NavigationActions = NavigationManager.getInstance(),
    private val userRepo: UserDataSource = UserRepository.getInstance(),
    private val tracking: TrackingDataSource = TrackingRepository(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    private val filter = originalFilter.copy(
        fragmentClassName = originalFilter.fragmentClassName,
        title = originalFilter.title,
        sections = arrayListOf<FilterSection>().apply { addAll(originalFilter.sections) },
        selection = originalFilter.selection.copy(
            genre = originalFilter.selection.genre,
            period = originalFilter.selection.period,
            type = originalFilter.selection.type
        )
    )

    var closeEvent = SingleLiveEvent<Void>()
    var updateUIEvent = SingleLiveEvent<Void>()

    val screenTitle = filter.title

    val typeVisible = filter.sections.contains(FilterSection.Type)
    val localVisible = filter.sections.contains(FilterSection.Local)
    val typeAllSelected: Boolean get() { return filter.selection.type == AMMusicType.All }
    val typeSongsSelected: Boolean get() { return filter.selection.type == AMMusicType.Songs }
    val typeAlbumsSelected: Boolean get() { return filter.selection.type == AMMusicType.Albums }
    val typePlaylistsVisible: Boolean get() { return !filter.excludedTypes.contains(AMMusicType.Playlists) }
    val typePlaylistsSelected: Boolean get() { return filter.selection.type == AMMusicType.Playlists }

    val sortVisible = filter.sections.contains(FilterSection.Sort)
    val sortNewestSelected: Boolean get() { return filter.selection.sort == AMResultItemSort.NewestFirst }
    val sortOldestSelected: Boolean get() { return filter.selection.sort == AMResultItemSort.OldestFirst }
    val sortAZSelected: Boolean get() { return filter.selection.sort == AMResultItemSort.AToZ }

    private val _includeLocalFiles = MutableLiveData<Boolean>()
    val includeLocalFiles: LiveData<Boolean> get() = _includeLocalFiles

    private enum class PendingLoginAction { SelectLocalFiles, IncludeLocalFiles }

    private var pendingLoginAction: PendingLoginAction? = null

    init {
        loadLocalFilePreference()
        observePreferenceChanges()
        observeLoginChanges()
    }

    fun onCreate() {
        updateUIEvent.call()
    }

    fun onCloseClick() {
        closeEvent.call()
    }

    fun onApplyClick() {
        EventBus.getDefault().post(EventFilterSaved(filter))

        closeEvent.call()
    }

    fun onTypeAllClick() {
        filter.selection.type = AMMusicType.All
        updateUIEvent.call()
    }

    fun onTypeSongsClick() {
        filter.selection.type = AMMusicType.Songs
        updateUIEvent.call()
    }

    fun onTypeAlbumsClick() {
        filter.selection.type = AMMusicType.Albums
        updateUIEvent.call()
    }

    fun onTypePlaylistsClick() {
        filter.selection.type = AMMusicType.Playlists
        updateUIEvent.call()
    }

    fun onSortNewestClick() {
        filter.selection.sort = AMResultItemSort.NewestFirst
        updateUIEvent.call()
    }

    fun onSortOldestClick() {
        filter.selection.sort = AMResultItemSort.OldestFirst
        updateUIEvent.call()
    }

    fun onSortAZClick() {
        filter.selection.sort = AMResultItemSort.AToZ
        updateUIEvent.call()
    }

    fun onFilterTypeChanged(aMMusicType: AMMusicType) {
        filter.selection.type = aMMusicType
        updateUIEvent.call()
    }

    fun onSelectLocalFilesClick() {
        if (!userRepo.isLoggedIn()) {
            pendingLoginAction = PendingLoginAction.SelectLocalFiles
            navigationActions.launchLogin(OfflineFilter)
            return
        }

        navigationActions.launchLocalFilesSelection()
    }

    fun onIncludeLocalFilesToggle(checked: Boolean) {
        if (checked && !userRepo.isLoggedIn()) {
            pendingLoginAction = PendingLoginAction.IncludeLocalFiles
            navigationActions.launchLogin(OfflineFilter)
            _includeLocalFiles.postValue(false)
            return
        }

        Single.just(checked)
            .subscribeOn(schedulers.io)
            .filter { it != preferencesRepo.includeLocalFiles }
            .map { preferencesRepo.includeLocalFiles = it }
            .doAfterSuccess { tracking.trackBreadcrumb("Include local files toggle set to $checked") }
            .observeOn(schedulers.main)
            .subscribe()
            .addTo(compositeDisposable)
    }

    private fun loadLocalFilePreference() {
        Observable.just(preferencesRepo.includeLocalFiles)
            .subscribeOn(schedulers.io)
            .observeOn(schedulers.main)
            .onErrorReturnItem(false)
            .subscribe { _includeLocalFiles.postValue(it) }
            .addTo(compositeDisposable)
    }

    private fun observePreferenceChanges() {
        preferencesRepo.observeBoolean(GENERAL_PREFERENCES_INCLUDE_LOCAL_FILES)
            .distinctUntilChanged()
            .observeOn(schedulers.main)
            .subscribe { include ->
                _includeLocalFiles.postValue(include)
                if (include) showLocalFileSelectionIfNeeded()
            }
            .addTo(compositeDisposable)
    }

    private fun showLocalFileSelectionIfNeeded() {
        Single.just(preferencesRepo.localFileSelectionShown)
            .subscribeOn(schedulers.io)
            .observeOn(schedulers.main)
            .subscribe { wasShown -> if (!wasShown) onSelectLocalFilesClick() }
            .addTo(compositeDisposable)
    }

    private fun observeLoginChanges() {
        userRepo.loginEvents
            .filter { it == EventLoginState.LOGGED_IN }
            .observeOn(schedulers.main)
            .subscribe {
                when (pendingLoginAction) {
                    PendingLoginAction.SelectLocalFiles -> onSelectLocalFilesClick()
                    PendingLoginAction.IncludeLocalFiles -> onIncludeLocalFilesToggle(true)
                }
                pendingLoginAction = null
            }
            .addTo(compositeDisposable)
    }
}
