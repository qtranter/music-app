package com.audiomack.fragments;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.audiomack.MainApplication;
import com.audiomack.R;
import com.audiomack.adapters.DataRecyclerViewAdapter;
import com.audiomack.data.ads.AdProvidersHelper;
import com.audiomack.data.ads.AdsDataSource;
import com.audiomack.data.featured.FeaturedSpotRepository;
import com.audiomack.data.premium.PremiumDataSource;
import com.audiomack.data.premium.PremiumRepository;
import com.audiomack.data.reachability.Reachability;
import com.audiomack.data.tracking.mixpanel.MixpanelConstantsKt;
import com.audiomack.model.AMArtist;
import com.audiomack.model.AMFeaturedSpot;
import com.audiomack.model.AMFooterRow;
import com.audiomack.model.AMNotification;
import com.audiomack.model.AMResultItem;
import com.audiomack.model.APIRequestData;
import com.audiomack.model.Action;
import com.audiomack.model.BenchmarkModel;
import com.audiomack.model.CellType;
import com.audiomack.model.EventDownload;
import com.audiomack.model.EventSongChange;
import com.audiomack.model.MaximizePlayerData;
import com.audiomack.model.MixpanelSource;
import com.audiomack.model.NextPageData;
import com.audiomack.model.SubscriptionNotification;
import com.audiomack.ui.artist.ArtistFragment;
import com.audiomack.ui.browse.BrowseFragment;
import com.audiomack.ui.browse.DataTrendingFragment;
import com.audiomack.ui.data.DataViewModel;
import com.audiomack.ui.home.HomeActivity;
import com.audiomack.ui.mylibrary.MyLibraryFragment;
import com.audiomack.ui.mylibrary.offline.local.menu.SlideUpMenuLocalMediaFragment;
import com.audiomack.ui.mylibrary.search.MyLibrarySearchFragment;
import com.audiomack.ui.player.maxi.morefromartist.PlayerMoreFromArtistFragment;
import com.audiomack.ui.premium.InAppPurchaseActivity;
import com.audiomack.ui.search.SearchFragment;
import com.audiomack.ui.search.results.DataSearchMusicFragment;
import com.audiomack.ui.settings.OptionsMenuFragment;
import com.audiomack.ui.slideupmenu.music.SlideUpMenuMusicFragment;
import com.audiomack.ui.tooltip.TooltipCorner;
import com.audiomack.ui.tooltip.TooltipFragment;
import com.audiomack.utils.ExtensionsKt;
import com.audiomack.views.AMProgressHUD;
import com.audiomack.views.AMSnackbar;
import com.audiomack.views.ProgressLogoView;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public abstract class DataFragment extends TrackedFragment implements DataRecyclerViewAdapter.RecyclerViewListener {

    private ProgressLogoView animationView;
    private View noConnectionPlaceholderView;
    private View placeholderView;
    private TextView noConnectionPlaceholderTextView;
    private ImageView noConnectionPlaceholderImageView;
    private Button noConnectionPlaceholderButton;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View headerView;
    private TextView upsellView;

    protected DataRecyclerViewAdapter recyclerViewAdapter;

    private String currentUrl;
    protected int currentPage;
    private boolean firstDownloadPerformed;
    protected boolean downloading;
    protected String pagingToken;

    protected String genre;
    protected String category;
    protected @Nullable
    String query;
    protected boolean showRepostInfo;

    private boolean viewCreated;
    private boolean isVisibleToUserCalledOnce;

    private AMLayoutListener layoutListener;

    private AdsDataSource adsDataSource = AdProvidersHelper.INSTANCE;
    private PremiumDataSource premiumDataSource = PremiumRepository.getInstance();

    protected DataViewModel viewModel;

    protected Disposable requestDataDisposable;

    public DataFragment() {
    }

    public DataFragment(@NotNull String logTag) {
        super(logTag);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_data, container, false);
            animationView = view.findViewById(R.id.animationView);
            noConnectionPlaceholderView = view.findViewById(R.id.noConnectionPlaceholderView);
            noConnectionPlaceholderTextView = noConnectionPlaceholderView.findViewById(R.id.tvMessage);
            noConnectionPlaceholderImageView = noConnectionPlaceholderView.findViewById(R.id.imageView);
            noConnectionPlaceholderButton = noConnectionPlaceholderView.findViewById(R.id.cta);
            recyclerView = view.findViewById(R.id.recyclerView);
            swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
            return view;
        } catch (Exception e) {
            Timber.w(e);
            return null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DataViewModel.class);

        initViewModelObservers();

        placeholderView = placeholderCustomView();
        ((FrameLayout) view).addView(placeholderView, ((FrameLayout) view).indexOfChild(noConnectionPlaceholderView) + 1);
        placeholderView.setVisibility(View.GONE);
        noConnectionPlaceholderView.setVisibility(View.GONE);

        View customHeaderView = recyclerViewHeader();
        LinearLayout headerContainer = new LinearLayout(getContext());
        headerContainer.setOrientation(LinearLayout.VERTICAL);
        headerContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        headerView = headerContainer;
        if (customHeaderView != null || canShowUpsellView()) {
            if (canShowUpsellView()) {
                upsellView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.header_upsell, headerContainer, false);
                upsellView.setOnClickListener(v -> {
                    viewModel.onUpsellClicked();
                });
                headerContainer.addView(upsellView);
                updateUpsellView();
            }
            if (customHeaderView != null) {
                customHeaderView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
                headerContainer.addView(customHeaderView);
            }
        } else {
            View emptyView = new View(getContext());
            emptyView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            headerContainer.addView(emptyView);
        }

        recyclerViewAdapter = new DataRecyclerViewAdapter(
                recyclerView,
                getCellType(),
                this,
                this instanceof DataDownloadsFragment,
                getParentFragment() instanceof SearchFragment,
                showRepostInfo,
                !(this instanceof DataUploadsFragment) && !(getParentFragment() instanceof MyLibrarySearchFragment),
                headerView,
                footerLayoutResId()
        );

        recyclerView.setLayoutManager(getLayoutManager());
        getLayoutManager().setItemPrefetchEnabled(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(recyclerViewAdapter);
        calculateBottomSectionHeight();

        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(swipeRefreshLayout.getContext(), R.color.orange));
        swipeRefreshLayout.setOnRefreshListener(() -> triggerPullToRefresh(true));
        swipeRefreshLayout.setHapticFeedbackEnabled(true);

        layoutListener = new AMLayoutListener(this);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView view, int newState) {
                super.onScrollStateChanged(view, newState);
                hideKeyboard();
            }
        });

        viewCreated = true;
    }

    private void initViewModelObservers() {
        viewModel.getFollowStatus().observe(getViewLifecycleOwner(), followed -> recyclerViewAdapter.notifyDataSetChanged());

        viewModel.getNotifyFollowToastEvent().observe(getViewLifecycleOwner(), notify -> ExtensionsKt.showFollowedToast(DataFragment.this, notify));

        viewModel.getOfflineAlertEvent().observe(getViewLifecycleOwner(), notify -> ExtensionsKt.showOfflineAlert(DataFragment.this));

        viewModel.getLoggedOutAlertEvent().observe(getViewLifecycleOwner(), loginSignupSource -> ExtensionsKt.showLoggedOutAlert(DataFragment.this, loginSignupSource));

        viewModel.getShowHUDEvent().observe(getViewLifecycleOwner(), mode -> AMProgressHUD.show(getActivity(), mode));

        viewModel.getShowPremiumEvent().observe(getViewLifecycleOwner(), mode -> InAppPurchaseActivity.show(getActivity(), mode));

        viewModel.getOpenURLEvent().observe(getViewLifecycleOwner(), url ->
            ExtensionsKt.openUrlExcludingAudiomack(getContext(), url)
        );

        viewModel.getShowConfirmDownloadDeletionEvent().observe(getViewLifecycleOwner(), music -> ExtensionsKt.confirmDownloadDeletion(this, music, null));

        viewModel.getShowConfirmPlaylistDownloadDeletionEvent().observe(getViewLifecycleOwner(), music -> ExtensionsKt.confirmPlaylistDownloadDeletion(this, music));

        viewModel.getShowFailedPlaylistDownloadEvent().observe(getViewLifecycleOwner(), aVoid -> ExtensionsKt.showFailedPlaylistDownload(this));

        viewModel.getShowConfirmPlaylistSyncEvent().observe(getViewLifecycleOwner(), result -> ExtensionsKt.confirmPlaylistSync(this, result.getSecond(), () -> viewModel.onPlaylistSyncConfirmed(result.getFirst(), getMixpanelSource(), MixpanelConstantsKt.MixpanelButtonList)));

        viewModel.getPremiumStateChangedEvent().observe(getViewLifecycleOwner(), aVoid -> updateUpsellView());
        
        viewModel.getShowPremiumDownloadEvent().observe(getViewLifecycleOwner(), model -> ((HomeActivity) getActivity()).requestPremiumDownloads(model));

        viewModel.getShowUnlockedToastEvent().observe(getViewLifecycleOwner(), musicName -> ExtensionsKt.showDownloadUnlockedToast(this, musicName));

        viewModel.getPromptNotificationPermissionEvent().observe(getViewLifecycleOwner(), redirect -> ExtensionsKt.askFollowNotificationPermissions(this, redirect));

        viewModel.getShowFollowBtn().observe(getViewLifecycleOwner(), show -> recyclerViewAdapter.showFollowBtn(show));
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (!isVisibleToUserCalledOnce) {
            showGeneralTooltip();
        }

        updateRecyclerView();

        if (viewCreated) {
            adjustInsets();
        }

        if (isVisibleToUser) {
            isVisibleToUserCalledOnce = true;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        if (recyclerView != null) {
            recyclerView.clearOnScrollListeners();
            if (layoutListener != null) {
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
            }
        }
        if (requestDataDisposable != null) {
            requestDataDisposable.dispose();
        }
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    protected void triggerPullToRefresh(boolean manuallyTriggered) {
        currentPage = 0;
        firstDownloadPerformed = false;
        updatePlaceholder();
        downloadItems(true);
        if (manuallyTriggered) {
            swipeRefreshLayout.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private void updateRecyclerView() {
        if (isAdded() && getUserVisibleHint() && recyclerViewAdapter != null && getCellType() != CellType.NOTIFICATION && getCellType() != CellType.ACCOUNT) {
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private void showGeneralTooltip() {
        if (getUserVisibleHint() && getParentFragment() instanceof BrowseFragment && adsDataSource.isFreshInstall() && !MainApplication.isFreshInstallTooltipShown()) {
            new Handler().postDelayed(() -> {
                try {
                    if (isAdded() && HomeActivity.getInstance().getTooltipFragmentReference() == null) {
                        final Point targetPoint = new Point(getView().getWidth() / 2, ((BaseTabHostFragment) getParentFragment()).getTabLayout().getHeight() + headerView.getHeight() / 2);
                        TooltipFragment tooltipFragment = TooltipFragment.newInstance(getString(R.string.tooltip_toast), R.drawable.tooltip_toast, TooltipCorner.BOTTOMRIGHT, new ArrayList<Point>() {{
                            add(targetPoint);
                        }}, () -> MainApplication.setFreshInstallTooltipShown(true));
                        ((HomeActivity) getActivity()).openTooltipFragment(tooltipFragment);
                    }
                } catch (Exception e) {
                    Timber.w(e);
                }
            }, 1000);
        }
    }

    private void calculateBottomSectionHeight() {
        if (recyclerView != null && recyclerViewAdapter != null) {

            // Eurhistic: do not compute height if there are several items (>= 20). This saves some computations
            int bottomSectionHeight;
            if ((getParentFragment() instanceof MyLibraryFragment || getParentFragment() instanceof ArtistFragment) && recyclerViewAdapter.getItemCount() < 20) {
                int collapsedHeight = 0;
                if (getParentFragment() instanceof MyLibraryFragment) {
                    collapsedHeight = ((MyLibraryFragment) getParentFragment()).getCollapsedHeaderHeight();
                } else if (getParentFragment() instanceof ArtistFragment) {
                    collapsedHeight = ((ArtistFragment) getParentFragment()).getCollapsedHeaderHeight();
                }
                int target = recyclerView.getHeight() - collapsedHeight;

                int rowCount = recyclerViewAdapter.getItemCount() + (recyclerViewAdapter.isLoadingMore() ? -2 : -1);
                int cellHeight;
                int additionalContentHeight = 0;

                CellType cellType = getCellType();
                if(cellType == CellType.PLAYLIST_GRID){
                    cellHeight = recyclerView.getWidth() / 2 + ExtensionsKt.convertDpToPixel(recyclerView.getContext(), 44);
                    rowCount = (int)Math.ceil((double)rowCount/(double)2);
                }else if(cellType == CellType.MUSIC_BROWSE_SMALL){
                    cellHeight = ExtensionsKt.convertDpToPixel(recyclerView.getContext(), 100);
                }else if(cellType == CellType.NOTIFICATION){
                    cellHeight = ExtensionsKt.convertDpToPixel(recyclerView.getContext(), 80);
                }else if(cellType == CellType.ACCOUNT){
                    cellHeight = ExtensionsKt.convertDpToPixel(recyclerView.getContext(), 100);
                }else if(cellType == CellType.MUSIC_TINY){
                    cellHeight = ExtensionsKt.convertDpToPixel(recyclerView.getContext(), 60);
                }else{
                    cellHeight = ExtensionsKt.convertDpToPixel(recyclerView.getContext(), 70);
                }
                if(recyclerViewAdapter.getItems().contains(AMFooterRow.INSTANCE)){
                    additionalContentHeight += ExtensionsKt.convertDpToPixel(recyclerView.getContext(), 91);
                    rowCount--;
                }
                if (headerView != null) {
                    additionalContentHeight += headerView.getHeight();
                }
                if(cellType == CellType.MUSIC_BROWSE_SMALL && recyclerViewAdapter.getItemCount() > 0){
                    additionalContentHeight -= ExtensionsKt.convertDpToPixel(recyclerView.getContext(), 8);
                }
                bottomSectionHeight = Math.max(1, target - rowCount * cellHeight - additionalContentHeight);
            } else {
                bottomSectionHeight = 1;
            }
            recyclerViewAdapter.setBottomSectionHeight(bottomSectionHeight);
        }
    }

    public void adjustInsets() {

        int topPadding = additionalTopPadding();
        if (getParentFragment() instanceof BaseTabHostFragment) {
            topPadding += ((BaseTabHostFragment) getParentFragment()).getTopLayoutHeight();
        }

        if(topPadding > 0) {
            swipeRefreshLayout.setProgressViewOffset(false, 0, topPadding + ExtensionsKt.convertDpToPixel(swipeRefreshLayout.getContext(), 8));
        }

        int headerPadding = additionalHeaderPadding();

        recyclerView.setPadding(0, topPadding, 0, (adsDataSource.getAdsVisible() ? getResources().getDimensionPixelSize(R.dimen.ad_height) : 0));
        
        ((FrameLayout.LayoutParams)noConnectionPlaceholderView.getLayoutParams()).topMargin = topPadding/2;

        ((FrameLayout.LayoutParams)placeholderView.getLayoutParams()).topMargin = topPadding + headerPadding;
        
        ((FrameLayout.LayoutParams)animationView.getLayoutParams()).topMargin = headerPadding;

        adjustScroll();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventSongChange eventSongChange) {
        updateRecyclerView();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventDownload eventDownload) {
        if (recyclerViewAdapter != null) {
            List<Integer> indices = recyclerViewAdapter.indicesOfItemId(eventDownload.getItemId());
            if (indices.size() > 0) {
                for (Integer index : indices) {
                    recyclerViewAdapter.notifyItemChanged(index);
                }
            }
        }
    }

    private void updateUpsellView() {
        if (upsellView != null) {
            upsellView.setVisibility((adsDataSource.getAdsVisible() || premiumDataSource.getSubscriptionNotification() != SubscriptionNotification.None) ? View.VISIBLE : View.GONE);
            if (premiumDataSource.getSubscriptionNotification() == SubscriptionNotification.BillingIssueWhileSubscribed) {
                upsellView.setText(R.string.billing_issue_subscribed);
            } else if (premiumDataSource.getSubscriptionNotification() == SubscriptionNotification.BillingIssueWhileUnsubscribed) {
                upsellView.setText(R.string.billing_issue_unsubscribed);
            } else {
                upsellView.setText(R.string.positive_upsell_message);
            }
        }
    }

    public void changeCategory(String categoryKey) {
        this.category = categoryKey;
        changedSettings();
    }

    public void changedQuery(String query) {
        this.query = query;
        changedSettings();
    }

    public void changedSettings() {
        try {
            if (recyclerViewAdapter == null) return;
            recyclerViewAdapter.setShowRepostInfo(showRepostInfo);
            recyclerViewAdapter.disableLoadMore();
            recyclerViewAdapter.clear(true);
            calculateBottomSectionHeight();
            currentPage = 0;
            firstDownloadPerformed = false;
            updatePlaceholder();
            downloadItems(true);
        } catch (Exception e) {
            Timber.w(e);
        }
    }

    protected void clearAndShowLoader() {
        recyclerViewAdapter.clear(true);
        showLoader();
    }

    private void showLoader() {
        if (isAdded()) {
            if (recyclerViewAdapter == null || recyclerViewAdapter.getRealItemsCount() == 0) {
                animationView.show();
            }
            noConnectionPlaceholderView.setVisibility(View.GONE);
            placeholderView.setVisibility(View.GONE);
        }
    }

    protected void hideLoader(boolean downloadSuccessful){
        if(isAdded()) {
            animationView.hide();

            boolean noDataPlaceholderVisible = (downloadSuccessful && (recyclerViewAdapter == null || recyclerViewAdapter.getRealItemsCount() == 0));
            boolean noConnectionPlaceholderVisible = (!downloadSuccessful && (recyclerViewAdapter == null || recyclerViewAdapter.getRealItemsCount() == 0));
            updatePlaceholder();
            noConnectionPlaceholderView.setVisibility(noConnectionPlaceholderVisible ? View.VISIBLE : View.GONE);
            placeholderView.setVisibility(noDataPlaceholderVisible ? View.VISIBLE : View.GONE);

            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    private void downloadItems(final boolean overwrite) {

        if (requestDataDisposable != null) {
            requestDataDisposable.dispose();
        }

        downloading = true;

        showLoader();

        APIRequestData requestData = apiCallObservable();
        if (requestData != null) {
            currentUrl = requestData.getUrl();
            requestDataDisposable =
                    requestData
                            .getObservable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(data -> {

                                if (Boolean.TRUE == data.getIgnore()) {
                                    downloading = false;
                                    downloadItems(false);
                                    return;
                                }

                                if (DataFragment.this instanceof DataSearchMusicFragment) {
                                    if (getParentFragment() instanceof SearchFragment && currentPage == 0) {
                                        ((SearchFragment) getParentFragment()).onSearchCompleted(Boolean.TRUE == data.getRelated());
                                    }
                                    ((DataSearchMusicFragment) DataFragment.this).toggleRelatedSearch(Boolean.TRUE == data.getRelated());
                                }

                                downloading = false;

                                String oldPagingToken = DataFragment.this.pagingToken;
                                DataFragment.this.pagingToken = data.getPagingToken();

                                try {
                                    if (recyclerViewAdapter.getRealItemsCount() == 0 || overwrite) {
                                        recyclerViewAdapter.clear(false);
                                    }
                                    recyclerViewAdapter.addBottom(data.getObjects());

                                    if (DataFragment.this instanceof DataTrendingFragment) {
                                        injectFeaturedPosts();
                                    } else if (DataFragment.this instanceof DataDownloadsFragment && !((DataDownloadsFragment) DataFragment.this).isRestoreDownloads()) {
                                        injectFooter();
                                    } else if (DataFragment.this instanceof PlayerMoreFromArtistFragment) {
                                        injectFooter();
                                    }

                                    calculateBottomSectionHeight();

                                } catch (Exception e) {
                                    Timber.w(e);
                                }

                                hideLoader(true);

                                if (DataFragment.this instanceof PlayerMoreFromArtistFragment) {
                                    recyclerViewAdapter.disableLoadMore();
                                    return;
                                }

                                if (data.getObjects().size() == 0 || (pagingToken != null && TextUtils.equals(oldPagingToken, pagingToken))) {
                                    if (currentPage == 0) {
                                        recyclerViewAdapter.disableLoadMore();
                                    } else {
                                        recyclerViewAdapter.disableLoadMoreAfterReachingLastPage(currentPage);
                                    }
                                } else {
                                    enableLoadMoreAfterFirstDownload();
                                }
                            }, throwable -> {
                                downloading = false;
                                hideLoader(false);
                                recyclerViewAdapter.hideLoadMore(true);
                                enableLoadMoreAfterFirstDownload();
                                try {
                                    if (!Reachability.getInstance().getNetworkAvailable()) {
                                        new AMSnackbar.Builder(getActivity())
                                                .withTitle(getString(R.string.download_results_no_connection))
                                                .withSubtitle(getString(R.string.please_try_request_later))
                                                .withDrawable(R.drawable.ic_snackbar_connection)
                                                .withDuration(Snackbar.LENGTH_SHORT)
                                                .show();
                                    }
                                } catch (Exception e) {
                                    Timber.w(e);
                                }
                            });
        }
    }

    protected void injectFeaturedPosts() {
        Timber.tag("featured-spots").d("Injecting featured spot...");
        if(FeaturedSpotRepository.getInstance().getFeaturedSpot() != null && recyclerViewAdapter.getRealItemsCount() > 4){
            List items = recyclerViewAdapter.getItems();
            for (int i = 0; i < items.size(); i++) {
                Object obj = items.get(i);
                if(obj instanceof AMFeaturedSpot){
                    if(obj.equals(FeaturedSpotRepository.getInstance().getFeaturedSpot())){
                        Timber.tag("featured-spots").d("...already there, ignoring");
                        return;
                    } else {
                        Timber.tag("featured-spots").d("...already there but need to replace it with the new one");
                        recyclerViewAdapter.replaceItem(4, FeaturedSpotRepository.getInstance().getFeaturedSpot());
                        return;
                    }
                }
            }
            Timber.tag("featured-spots").d("...adding it for the first time");
            recyclerViewAdapter.insertItem(4, FeaturedSpotRepository.getInstance().getFeaturedSpot());
        }else{
            Timber.tag("featured-spots").d("...not available yet or recyclerview is empty");
        }
    }

    private void injectFooter() {
        if (recyclerViewAdapter.getRealItemsCount() > 0) {
            recyclerViewAdapter.addBottom(new ArrayList() {{
                add(AMFooterRow.INSTANCE);
            }});
        }
    }

    private void enableLoadMoreAfterFirstDownload() {
        if (!firstDownloadPerformed && recyclerViewAdapter != null && recyclerViewAdapter.getRealItemsCount() > 0 && !(this instanceof DataDownloadsFragment && !((DataDownloadsFragment) this).isRestoreDownloads())) {
            firstDownloadPerformed = true;
            recyclerViewAdapter.enableLoadMore();
        }
    }

    public void shufflePlay() {
        if (recyclerViewAdapter != null && recyclerViewAdapter.getRealItemsCount() > 0) {
            AMResultItem music = recyclerViewAdapter.pickRandomMusic();
            if (music != null) {
                openMusicShuffled(music);
            }
        }
    }

    private void updatePlaceholder() {

        // No connectivity placeholder
        noConnectionPlaceholderTextView.setText(R.string.noconnection_placeholder);
        noConnectionPlaceholderButton.setText(R.string.noconnection_highlighted_placeholder);
        noConnectionPlaceholderImageView.setImageResource(R.drawable.ic_empty_offline);
        noConnectionPlaceholderView.setOnClickListener(v -> didTapOnNoConnectionPlaceholderText());
        noConnectionPlaceholderButton.setOnClickListener(v -> didTapOnNoConnectionPlaceholderText());
        noConnectionPlaceholderImageView.setOnClickListener(v -> didTapOnNoConnectionPlaceholderText());

        // No results placeholder
        configurePlaceholderView(placeholderView);
    }

    private void hideKeyboard() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            View currentFocus = activity.getCurrentFocus();
            if (inputManager != null && currentFocus != null) {
                inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                currentFocus.clearFocus();
            }
        }
    }

    // album only passed to show an album track directly (e.g. for shuffling)
    protected void openMusic(final AMResultItem item, final AMResultItem album,
                             @Nullable List<AMResultItem> musicToBePrepended) {
        openMusic(item, album, musicToBePrepended, false);
    }

    private void openMusicShuffled(final AMResultItem item) {
        openMusic(item, null, null, true);
    }

    // album only passed to show an album track directly (e.g. for shuffling)
    protected void openMusic(final AMResultItem item, final AMResultItem album,
                             @Nullable List<AMResultItem> musicToBePrepended, boolean shuffle) {

        if (HomeActivity.getInstance() == null) {
            return;
        }

        if (item.isGeoRestricted()) {
            HomeActivity.getInstance().homeViewModel.onGeorestrictedMusicClicked(() -> didRemoveGeorestrictedItem(item));
            return;
        }

        if (item.isPlaylist()) {
            HomeActivity.getInstance().requestPlaylist(item.getItemId(), getMixpanelSource(), false);
            return;
        }

        MixpanelSource mixpanelSource = getMixpanelSource();
        mixpanelSource.setShuffled(shuffle);

        boolean scrollToTop = this instanceof PlayerMoreFromArtistFragment;

        boolean offlineMode = this instanceof DataDownloadsFragment && !((DataDownloadsFragment) this).isRestoreDownloads();
        NextPageData nextPageData = getNextPageData(mixpanelSource, offlineMode);

        if (item.isAlbum()) {
            if (item.isLocal()) {
                HomeActivity.getInstance().openAlbum(item, mixpanelSource, false);
            } else {
                HomeActivity.getInstance().requestAlbum(item.getItemId(), mixpanelSource, false);
            }
            return;
        }

        List<Object> items = new ArrayList<>();
        if (musicToBePrepended != null) {
            items.addAll(musicToBePrepended);
        }
        List<Object> recyclerViewItems = recyclerViewAdapter.getItems();
        items.addAll(recyclerViewItems);
        List<AMResultItem> playlist = new ArrayList<>(items.size());
        for (Object obj : items) {
            if (obj instanceof AMFeaturedSpot && ((AMFeaturedSpot) obj).getItem() != null) {
                playlist.add(((AMFeaturedSpot) obj).getItem());
            } else if (obj instanceof AMResultItem) {
                playlist.add((AMResultItem) obj);
            } else if (obj instanceof AMNotification && ((AMNotification) obj).getTarget() != null) {
                playlist.add(((AMNotification) obj).getTarget());
            } else if (obj instanceof AMNotification && ((AMNotification) obj).getObject() instanceof AMResultItem) {
                playlist.add((AMResultItem) ((AMNotification) obj).getObject());
            }
        }

        if (shuffle && nextPageData != null) {
            HomeActivity.getInstance().requestShuffled(nextPageData, playlist);
            return;
        }

        MaximizePlayerData data;
        if (album != null) {
            data = new MaximizePlayerData(item, album, playlist, nextPageData, offlineMode, false, null, mixpanelSource, shuffle, scrollToTop, false, false, true);
        } else {
            data = new MaximizePlayerData(item, null, playlist, nextPageData, offlineMode, false, null, mixpanelSource, shuffle, scrollToTop, false, false, true);
        }
        hideKeyboard();
        ((HomeActivity)getActivity()).homeViewModel.onMaximizePlayerRequested(data);
    }

    @Nullable
    private NextPageData getNextPageData(MixpanelSource mixpanelSource, Boolean offlineScreen) {
        if (currentUrl != null) {
            return new NextPageData(currentUrl, currentPage, mixpanelSource, offlineScreen);
        }
        return null;
    }

    /* ******************** Customizations ******************** */

    protected APIRequestData apiCallObservable() {
        return null;
    }

    protected CellType getCellType() {
        return CellType.UNDEFINED;
    }

    protected RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(recyclerView.getContext(), LinearLayoutManager.VERTICAL, false);
    }

    protected @NonNull
    View placeholderCustomView() {
        return new View(getContext());
    }

    protected void configurePlaceholderView(@NonNull View placeholderView) {
    }

    private void didTapOnNoConnectionPlaceholderText() {
        changedSettings();
    }

    protected View recyclerViewHeader() {
        return null;
    }

    protected @LayoutRes
    Integer footerLayoutResId() {
        return 0;
    }

    protected boolean canShowUpsellView() {
        return false;
    }

    protected int additionalTopPadding() {
        return 0;
    }

    protected int additionalHeaderPadding() {
        return 0;
    }

    protected void didRemoveGeorestrictedItem(@NonNull AMResultItem item) {}

    protected void onPremiumStatusChanged() {
        updateUpsellView();
    }

    protected MixpanelSource getMixpanelSource() {
        throw new RuntimeException("getMixpanelSource() not implemented, please fix this!");
    }

    /* ********** V2ListViewAdapterListener methods ********** */

    @Override
    public void onClickTwoDots(final AMResultItem item) {

        if (item.isGeoRestricted()) {
            HomeActivity.getInstance().homeViewModel.onGeorestrictedMusicClicked(() -> didRemoveGeorestrictedItem(item));
            return;
        }

        if (DataFragment.this instanceof DataDownloadsFragment) {
            boolean completed = (item.isAlbum() && AMResultItem.isAlbumFullyDownloaded(item.getItemId())) || (!item.isAlbum() && item.isDownloadCompleted());
            boolean inProgress = !completed && item.isDownloadInProgress();
            boolean queued = item.isDownloadQueued();
            boolean failed = item.isDownloadedAndNotCached() && !inProgress && !queued && !completed;
            if (failed) {
                showFaileDownloadMenu(item);
                return;
            }
        }

        if (item.isLocal()) {
            long id = Long.parseLong(item.getItemId());
            SlideUpMenuLocalMediaFragment fragment = SlideUpMenuLocalMediaFragment.newInstance(id);
            ((HomeActivity) getActivity()).openOptionsFragment(fragment);
            return;
        }

        boolean restoreDownloadsMode = this instanceof DataDownloadsFragment && ((DataDownloadsFragment) this).isRestoreDownloads();
        hideKeyboard();
        ((HomeActivity) getActivity()).openOptionsFragment(SlideUpMenuMusicFragment.newInstance(item, getMixpanelSource(), restoreDownloadsMode, false, null));
    }

    private void showFaileDownloadMenu(AMResultItem item) {
        final List<Action> actions = new ArrayList<>();
        actions.add(new Action(getString(R.string.options_retry_download), () -> {
            if (isAdded()) {
                ((HomeActivity) getActivity()).popFragment();
                viewModel.onDownloadTapped(item, getMixpanelSource(), MixpanelConstantsKt.MixpanelButtonKebabMenu);
            }
        }));
        actions.add(new Action(getString(R.string.options_delete_download), () -> {
            if (isAdded()) {
                ((HomeActivity) getActivity()).popFragment();
                int index = recyclerViewAdapter.indexOfItemId(item.getItemId());
                if (DataFragment.this instanceof DataDownloadsFragment) {
                    recyclerViewAdapter.removeItem(item);
                    hideLoader(true);
                }
                item.deepDelete();
                if (!(DataFragment.this instanceof DataDownloadsFragment) && index != -1) {
                    recyclerViewAdapter.notifyItemChanged(index);
                }
            }
        }));
        hideKeyboard();
        ((HomeActivity) getActivity()).openOptionsFragment(OptionsMenuFragment.newInstance(actions));
    }

    @Override
    public void onClickFollow(AMArtist artist) {
        viewModel.onFollowTapped(null, artist, getMixpanelSource(), MixpanelConstantsKt.MixpanelButtonList);
    }

    @Override
    public void onClickDownload(AMResultItem item) {
        if (!item.isDownloadInProgress()) {
            viewModel.onDownloadTapped(item, getMixpanelSource(), MixpanelConstantsKt.MixpanelButtonList);
        }
    }

    @Override
    public void onStartLoadMore() {
        currentPage++;
        Timber.tag(DataFragment.class.getSimpleName()).d("Loading page: " + currentPage + "");
        downloadItems(false);
    }

    @Override
    public void onScrollTo(int verticalOffset) {
        if (getUserVisibleHint() && getParentFragment() instanceof ArtistFragment) {
            ((BaseTabHostFragment) getParentFragment()).didScrollTo(verticalOffset);
        }
    }

    @Override
    public void onClickItem(Object obj) {
        if (obj != null) {

            Object object = obj;
            if (obj instanceof AMFeaturedSpot) {
                AMFeaturedSpot featuredSpot = (AMFeaturedSpot) obj;
                if (featuredSpot.getItem() != null) {
                    object = featuredSpot.getItem();
                } else if (featuredSpot.getArtist() != null) {
                    object = featuredSpot.getArtist();
                }
            }
            if (object instanceof AMResultItem) {
                openMusic((AMResultItem) object, null, null);
            } else if (object instanceof AMArtist) {
                AMArtist artist = (AMArtist) object;
                if (HomeActivity.getInstance() != null) {
                    HomeActivity.getInstance().homeViewModel.onArtistScreenRequested(artist.getUrlSlug(), null, false);
                }
            }
        }
    }

    @Override
    public void onClickNotificationArtist(@NotNull String artistSlug, @NotNull AMNotification.NotificationType type) {
        try {
            viewModel.onBellNotificationClicked(type);
            getActivity().onBackPressed();
            HomeActivity.getInstance().homeViewModel.onArtistScreenRequested(artistSlug, null, false);
        } catch (Exception e) {
            Timber.w(e);
        }
    }

    @Override
    public void onClickNotificationMusic(@NotNull AMResultItem item, boolean comment, @NotNull AMNotification.NotificationType type) {
        viewModel.onBellNotificationClicked(type);
        getActivity().onBackPressed();
        if(comment){
            HomeActivity.getInstance().openComments(item, null, null);
        }else {
            openMusic(item, null, null);
        }
    }

    @Override
    public void onClickFooter() {}

    @Override
    public void onClickNotificationBenchmark(@NotNull AMResultItem item, @NotNull BenchmarkModel benchmark, @NotNull AMNotification.NotificationType type) {
        viewModel.onBellNotificationClicked(type);
        if (getActivity() != null) {
            getActivity().onBackPressed();
            if (HomeActivity.getInstance() != null) {
                HomeActivity.getInstance().homeViewModel.onBenchmarkRequested(item.getItemId(), item.getType(), benchmark, getMixpanelSource(), MixpanelConstantsKt.MixpanelButtonList);
            }
        }
    }

    @Override
    public void onClickNotificationCommentUpvote(@NotNull AMResultItem music, @NotNull AMNotification.UpvoteCommentNotificationData data, @NotNull AMNotification.NotificationType type) {
        viewModel.onBellNotificationClicked(type);
        if (getActivity() != null) {
            getActivity().onBackPressed();
            if (HomeActivity.getInstance() != null) {
                HomeActivity.getInstance().homeViewModel.onCommentsRequested(music.getItemId(), music.getTypeForMusicApi(), data.getUuid(), data.getThreadId());
            }
        }
    }

    @Override
    public void onClickNotificationBundledPlaylists(@NotNull List<? extends AMResultItem> playlists, @NotNull AMNotification.NotificationType type) {
        viewModel.onBellNotificationClicked(type);
    }

    public void adjustScroll() {
        if (recyclerViewAdapter != null && (getParentFragment() instanceof MyLibraryFragment || getParentFragment() instanceof ArtistFragment)) {
            int currentHeight = 0, expandedHeight = 0, collapsedHeight = 0;
            if (getParentFragment() instanceof MyLibraryFragment) {
                currentHeight = ((MyLibraryFragment) getParentFragment()).getCurrentHeaderHeight();
                expandedHeight = ((MyLibraryFragment) getParentFragment()).getExpandedHeaderHeight();
                collapsedHeight = ((MyLibraryFragment) getParentFragment()).getCollapsedHeaderHeight();
            } else if (getParentFragment() instanceof ArtistFragment) {
                currentHeight = ((ArtistFragment) getParentFragment()).getCurrentHeaderHeight();
                expandedHeight = ((ArtistFragment) getParentFragment()).getExpandedHeaderHeight();
                collapsedHeight = ((ArtistFragment) getParentFragment()).getCollapsedHeaderHeight();
            }
            int offset = recyclerViewAdapter.getOffsetCounter();
            if (currentHeight > collapsedHeight || offset < (expandedHeight - collapsedHeight)) {
                int correction = expandedHeight - currentHeight - offset;
                recyclerView.scrollBy(0, correction);
            }
        }
    }

    static class AMLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {

        WeakReference<DataFragment> fragmentWeakReference;

        AMLayoutListener(DataFragment fragment) {
            this.fragmentWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void onGlobalLayout() {
            DataFragment fragment = fragmentWeakReference != null ? fragmentWeakReference.get() : null;
            if (fragment != null && fragment.isAdded() && fragment.getView() != null) {
                fragment.recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                fragment.layoutListener = null;
                fragment.adjustInsets();
                fragment.changedSettings();
            }
        }
    }
}
