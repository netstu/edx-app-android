package org.edx.mobile.view.my_videos;

import android.content.Context;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.google.inject.Inject;

import org.edx.mobile.R;
import org.edx.mobile.base.BaseFragment;
import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.databinding.FragmentVideoListWithPlayerContainerBinding;
import org.edx.mobile.interfaces.NetworkSubject;
import org.edx.mobile.interfaces.SectionItemInterface;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.api.ProfileModel;
import org.edx.mobile.model.api.TranscriptModel;
import org.edx.mobile.model.api.VideoResponseModel;
import org.edx.mobile.model.db.DownloadEntry;
import org.edx.mobile.module.analytics.ISegment;
import org.edx.mobile.module.db.DataCallback;
import org.edx.mobile.module.prefs.PrefManager;
import org.edx.mobile.player.IPlayerEventCallback;
import org.edx.mobile.player.PlayerFragment;
import org.edx.mobile.task.GetRecentDownloadedVideosTask;
import org.edx.mobile.util.AppConstants;
import org.edx.mobile.util.ResourceUtil;
import org.edx.mobile.util.UiUtil;
import org.edx.mobile.view.adapters.MyRecentVideoAdapter;
import org.edx.mobile.view.dialog.DeleteVideoDialogFragment;
import org.edx.mobile.view.dialog.IDialogCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyRecentVideosFragment extends BaseFragment implements IPlayerEventCallback {

    private DeleteVideoDialogFragment deleteDialogFragment;
    private CheckBox deleteCheckBox;
    private MenuItem deleteCheckBoxMenuItem;
    private CompoundButton.OnCheckedChangeListener deleteCheckBoxChangeListener;
    private GetRecentDownloadedVideosTask getRecentDownloadedVideosTask;
    private DownloadEntry videoModel;
    private int playingVideoIndex = -1;
    private MyRecentVideoAdapter adapter;
    private PlayerFragment playerFragment;
    private FragmentVideoListWithPlayerContainerBinding binding;
    private final Logger logger = new Logger(getClass().getName());

    @Inject
    private IEdxEnvironment environment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(!isLandscape());
        environment.getSegment().trackScreenView(ISegment.Screens.MY_VIDEOS_RECENT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return DataBindingUtil.inflate(inflater, R.layout.fragment_video_list_with_player_container, container, false).getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            restore(savedInstanceState);
        }

        binding = DataBindingUtil.getBinding(getView());

        binding.myRecentVideosPlayerContainer.setVisibility(playerFragment == null ? View.GONE : View.VISIBLE);

        if (binding.listVideo != null) {
            adapter = new MyRecentVideoAdapter(getActivity(), environment) {

                @Override
                protected void onItemClick(SectionItemInterface model, int position) {
                    showPlayer();
                    // initialize index for this model
                    playingVideoIndex = position;
                    videoModel = (DownloadEntry) model;
                    playVideoModel();
                    notifyAdapter();
                }

                @Override
                public void onSelectItem() {
                    int selectedItemsCount = adapter.getSelectedVideoItemsCount();
                    int totalVideos = adapter.getTotalVideoItemsCount();
                    binding.deleteVideoButtons.deleteBtn.setEnabled(selectedItemsCount > 0);
                    setCheckBoxChecked(selectedItemsCount == totalVideos);
                }
            };
            if (videoModel != null) {
                adapter.setVideoId(videoModel.videoId);
            }
            adapter.setSelectedPosition(playingVideoIndex);
            binding.listVideo.setEmptyView(binding.emptyListView);
            binding.listVideo.setAdapter(adapter);

            showDeletePanel(getView());

            binding.listVideo.setOnItemClickListener(adapter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        showDeletePanel(getView());
        setCheckBoxVisible(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        addToRecentAdapter(getView());
    }

    @Override
    public void onStop() {
        super.onStop();

        if (getRecentDownloadedVideosTask != null) {
            getRecentDownloadedVideosTask.cancel(true);
            getRecentDownloadedVideosTask = null;
        }

        hideConfirmDeleteDialog();
        AppConstants.myVideosDeleteMode = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (playerFragment != null) {
            ((NetworkSubject) getActivity()).unregisterNetworkObserver(playerFragment);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (playerFragment != null) {
            playerFragment.setUserVisibleHint(isVisibleToUser);
            if (isVisibleToUser) {
                playerFragment.unlockOrientation();
            } else {
                playerFragment.lockOrientation();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.video_list, menu);
        deleteCheckBoxMenuItem = menu.findItem(R.id.delete_checkbox);
        deleteCheckBoxMenuItem.setVisible(AppConstants.myVideosDeleteMode);
        deleteCheckBox = (CheckBox) deleteCheckBoxMenuItem.getActionView()
                .findViewById(R.id.select_checkbox);
        deleteCheckBox.setChecked(adapter.getSelectedVideoItemsCount() ==
                adapter.getTotalVideoItemsCount());
        if (deleteCheckBoxChangeListener == null) {
            deleteCheckBoxChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        adapter.selectAll();
                    } else {
                        adapter.unselectAll();
                    }
                    notifyAdapter();
                    binding.deleteVideoButtons.deleteBtn.setEnabled(isChecked);
                }
            };
        }
        deleteCheckBox.setOnCheckedChangeListener(deleteCheckBoxChangeListener);
    }

    private void setCheckBoxChecked(boolean checked) {
        // Temporarily remove the listener so that this isn't handled
        // as a user action to uncheck all items.
        deleteCheckBox.setOnCheckedChangeListener(null);
        deleteCheckBox.setChecked(checked);
        deleteCheckBox.setOnCheckedChangeListener(deleteCheckBoxChangeListener);
    }

    private void setCheckBoxVisible(boolean visible) {
        AppConstants.myVideosDeleteMode = visible;
        if (deleteCheckBoxMenuItem != null) {
            deleteCheckBoxMenuItem.setVisible(visible);
            setCheckBoxChecked(false);
        }
    }

    private void addToRecentAdapter(final View view) {
        if (adapter == null) {
            return;
        }
        logger.debug("reloading adapter...");
        final String selectedId = adapter.getVideoId();


        if (getRecentDownloadedVideosTask != null) {
            getRecentDownloadedVideosTask.cancel(true);
        }
        else {
            getRecentDownloadedVideosTask = new GetRecentDownloadedVideosTask(getActivity()) {
                @Override
                protected void onSuccess(List<SectionItemInterface> list) throws Exception {
                    super.onSuccess(list);
                    if (list != null && !list.isEmpty()) {
                        adapter.clear();
                        adapter.addAll(list);
                        logger.debug("reload done");
                    }

                    if (adapter.getCount() <= 0) {
                        hideDeletePanel(view);
                    }
                    binding.listVideo.setOnItemClickListener(adapter);
                    if (selectedId != null) {
                        adapter.setVideoId(selectedId);
                    }
                    notifyAdapter();
                    // Refresh the previous and next buttons visibility on the video
                    // player if a video is playing, based on the new data set.
                    if (playerFragment != null) {
                        playerFragment.setNextPreviousListeners(getNextListener(), getPreviousListener());
                    }
                }
            };
        }

        getRecentDownloadedVideosTask.execute();
    }

    private void playVideoModel() {
        if (playerFragment == null) {
            return;
        }

        if (playerFragment.isPlaying() && videoModel.getVideoId().equals(
                playerFragment.getPlayingVideo().getVideoId())) {
            logger.debug("this video is already being played, skipping play event");
            return;
        }

        Context context = getContext();
        String prefName = PrefManager.getPrefNameForLastAccessedBy(
                getProfile().username, videoModel.eid);
        PrefManager prefManager = new PrefManager(context, prefName);
        VideoResponseModel vrm;
        try {
            vrm = environment.getServiceManager().getVideoById(videoModel.eid, videoModel.videoId);
        } catch (Exception e) {
            logger.error(e);
            return;
        }
        prefManager.putLastAccessedSubsection(vrm.getSection().getId(), false);

        // reload this model
        environment.getStorage().reloadDownloadEntry(videoModel);

        logger.debug("Resumed= " + playerFragment.isResumed());

        TranscriptModel transcript = null;
        try {
            transcript = environment.getServiceManager().getTranscriptsOfVideo(videoModel.eid, videoModel.videoId);
        } catch (Exception e) {
            logger.error(e);
        }

        String filepath = null;
        // check if file available on local
        if (videoModel.filepath != null && videoModel.filepath.length()>0) {
            if (videoModel.isDownloaded()) {
                File f = new File(videoModel.filepath);
                if (f.exists()) {
                    // play from local
                    filepath = videoModel.filepath;
                    logger.debug("Playing from local file");
                }
            }
        } else {
            DownloadEntry de = (DownloadEntry) environment.getDatabase()
                    .getIVideoModelByVideoUrl(videoModel.url, null);
            if (de != null && de.filepath != null) {
                File f = new File(de.filepath);
                if (f.exists()) {
                    // play from local
                    filepath = de.filepath;
                    logger.debug("Playing from local file for " +
                            "another Download Entry");
                }
            }
        }

        if (filepath == null || filepath.length() <= 0) {
            // not available on local, so play online
            logger.warn("Local file path not available");
            filepath = videoModel.getBestEncodingUrl(context);
        }

        playerFragment.play(filepath, videoModel.lastPlayedOffset,
                videoModel.getTitle(), transcript, videoModel);

        adapter.setVideoId(this.videoModel.videoId);
    }

    private void showPlayer() {
        if (playerFragment != null) {
            return;
        }

        hideDeletePanel(getView());

        if (binding.myRecentVideosPlayerContainer.getVisibility() != View.VISIBLE) {
            binding.myRecentVideosPlayerContainer.setVisibility(View.VISIBLE);
        }

        // add and display player fragment
        playerFragment = new PlayerFragment();
        // set callback for player events
        playerFragment.setCallback(this);
        playerFragment.setNextPreviousListeners(getNextListener(), getPreviousListener());
        FragmentManager childManager = getChildFragmentManager();
        childManager.beginTransaction().add(R.id.my_recent_videos_player_container, playerFragment).commit();
        // the fragment needs to be added immediately in order to be playable
        childManager.executePendingTransactions();
        ((NetworkSubject) getActivity()).registerNetworkObserver(playerFragment);
        playerFragment.unlockOrientation();
    }


    private void hideDeletePanel(View view) {
        // hide delete button panel at bottom
        binding.deleteButtonPanel.setVisibility(View.GONE);

        // hide checkbox in action bar
        setCheckBoxVisible(false);

        // hide checkboxes in list
        notifyAdapter();
    }

    private void showDeletePanel(View view) {
        if (playerFragment != null) {
            return;
        }

        binding.deleteButtonPanel.setVisibility(View.VISIBLE);

        if (AppConstants.myVideosDeleteMode) {
            binding.deleteVideoButtons.deleteBtn.setVisibility(View.VISIBLE);
            binding.deleteVideoButtons.cancelBtn.setVisibility(View.VISIBLE);
            binding.deleteVideoButtons.editBtn.setVisibility(View.GONE);
        } else {
            binding.deleteVideoButtons.deleteBtn.setVisibility(View.GONE);
            binding.deleteVideoButtons.cancelBtn.setVisibility(View.GONE);
            binding.deleteVideoButtons.editBtn.setVisibility(View.VISIBLE);
        }

        binding.deleteVideoButtons.deleteBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<SectionItemInterface> list = adapter
                        .getSelectedItems();
                if (list != null && list.size() > 0) {
                    showConfirmDeleteDialog(list.size());
                }
            }
        });

        binding.deleteVideoButtons.cancelBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.deleteVideoButtons.editBtn.setVisibility(View.VISIBLE);
                binding.listVideo.setOnItemClickListener(adapter);
                AppConstants.myVideosDeleteMode = false;
                setCheckBoxVisible(false);
                adapter.unselectAll();
                notifyAdapter();
                binding.deleteVideoButtons.deleteBtn.setVisibility(View.GONE);
                binding.deleteVideoButtons.cancelBtn.setVisibility(View.GONE);
            }
        });

        binding.deleteVideoButtons.editBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                binding.deleteVideoButtons.editBtn.setVisibility(View.GONE);
                AppConstants.myVideosDeleteMode = true;
                notifyAdapter();
                binding.listVideo.setOnItemClickListener(null);
                binding.deleteVideoButtons.deleteBtn.setEnabled(false);
                binding.deleteVideoButtons.deleteBtn.setVisibility(View.VISIBLE);
                binding.deleteVideoButtons.cancelBtn.setVisibility(View.VISIBLE);
                setCheckBoxVisible(true);
            }
        });
    }

    protected void showConfirmDeleteDialog(int itemCount) {
        Map<String, String> dialogMap = new HashMap<>();
        dialogMap.put("title", getString(R.string.delete_dialog_title_help));
        dialogMap.put("message_1", getResources().getQuantityString(R.plurals.delete_video_dialog_msg, itemCount));
        dialogMap.put("yes_button", getString(R.string.label_delete));
        dialogMap.put("no_button",  getString(R.string.label_cancel));
        deleteDialogFragment = DeleteVideoDialogFragment.newInstance(dialogMap,
                new IDialogCallback() {
            @Override
            public void onPositiveClicked() {
                onConfirmDelete();
                deleteDialogFragment.dismiss();
            }

            @Override
            public void onNegativeClicked() {
                deleteDialogFragment.dismiss();
            }
        });
        deleteDialogFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        deleteDialogFragment.show(getFragmentManager(), "dialog");
        deleteDialogFragment.setCancelable(false);
    }

    protected void hideConfirmDeleteDialog() {
        if (deleteDialogFragment != null) {
            deleteDialogFragment.dismissAllowingStateLoss();
        }
    }

    //Deleting Downloaded videos on getting confirmation
    private void onConfirmDelete() {
        int deletedVideoCount = 0;
        ArrayList<SectionItemInterface> list = adapter.getSelectedItems();
        if (list != null) {
            for (SectionItemInterface section : list) {
                if (section.isDownload()) {
                    DownloadEntry de = (DownloadEntry) section;
                    environment.getStorage().removeDownload(de);
                    deletedVideoCount++;
                }
            }
        }
        addToRecentAdapter(getView());
        notifyAdapter();
        binding.listVideo.setOnItemClickListener(adapter);
        setCheckBoxVisible(false);
        if (deletedVideoCount > 0) {
            UiUtil.showMessage(getView(), ResourceUtil.getFormattedStringForQuantity(getResources(),
                    R.plurals.deleted_video, "video_count", deletedVideoCount).toString());
        }
        binding.deleteVideoButtons.deleteBtn.setVisibility(View.GONE);
        binding.deleteVideoButtons.editBtn.setVisibility(View.VISIBLE);
        binding.deleteVideoButtons.cancelBtn.setVisibility(View.GONE);
    }


    @Override
    public void onError() {}

    @Override
    public void onPlaybackStarted() {
        environment.getStorage().markVideoPlaying(videoModel, watchedStateCallback);
        notifyAdapter();
    }

    @Override
    public void saveCurrentPlaybackPosition(int offset) {
        DownloadEntry v = videoModel;
        if (v != null) {
            // mark this as partially watches, as playing has started
            environment.getDatabase().updateVideoLastPlayedOffset(v.videoId, offset,
                    setCurrentPositionCallback);
        }
        notifyAdapter();
    }

    @Override
    public void onPlaybackComplete() {
        DownloadEntry v = videoModel;
        if (v != null) {
            if (v.watched == DownloadEntry.WatchedState.PARTIALLY_WATCHED) {
                videoModel.watched = DownloadEntry.WatchedState.WATCHED;
                // mark this as partially watches, as playing has started
                environment.getDatabase().updateVideoWatchedState(v.videoId, DownloadEntry.WatchedState.WATCHED,
                        watchedStateCallback);
            }
        }
        notifyAdapter();
    }

    private void notifyAdapter() {
        if (adapter == null) {
            return;
        }
        adapter.setSelectedPosition(playingVideoIndex);
        adapter.notifyDataSetChanged();
    }

    /**
     * @return  true if current orientation is LANDSCAPE, false otherwise.
     *
     */
    protected boolean isLandscape() {
        return (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        logger.debug("In onSaveInstance State");
        outState.putInt("playingVideoIndex", playingVideoIndex);
        outState.putSerializable("model", videoModel);
        super.onSaveInstanceState(outState);
    }

    /**
     * Container tab will call this method to restore the saved data
     */
    protected void restore(Bundle savedInstanceState) {
        playingVideoIndex = savedInstanceState.getInt("playingVideoIndex", -1);
        videoModel = (DownloadEntry) savedInstanceState.getSerializable("model");
        playerFragment = (PlayerFragment) getChildFragmentManager()
                .findFragmentById(R.id.my_recent_videos_player_container);
        if (binding.myRecentVideosPlayerContainer != null) {
            playerFragment.setCallback(this);
            ((NetworkSubject) getActivity()).registerNetworkObserver(playerFragment);
        }
    }

    //Play the nextVideo if user selects next from Dialog
    private void playNext() {
        playingVideoIndex++;
        // check next playable video entry
        while (playingVideoIndex < adapter.getCount()) {
            SectionItemInterface i = adapter.getItem(playingVideoIndex);
            if (i != null && i instanceof DownloadEntry) {
                videoModel = (DownloadEntry) i;
                adapter.setSelectedPosition(playingVideoIndex);
                adapter.setVideoId(videoModel.videoId);
                playVideoModel();
                break;
            }
            // try next
            playingVideoIndex++;
        }
    }

    //Check if next video is available in the Video List
    private boolean hasNextVideo(int index) {
        if (index == -1) {
            index = playingVideoIndex;
        }
        for (int i = index + 1; i < adapter.getCount(); i++) {
            SectionItemInterface d = adapter.getItem(i);
            if (d != null && d instanceof DownloadEntry) {
                return true;
            }
        }
        return false;
    }

    private void playPrevious() {
        playingVideoIndex--;
        // check next playable video entry
        while (playingVideoIndex >= 0) {
            SectionItemInterface i = adapter.getItem(playingVideoIndex);
            if (i != null && i instanceof DownloadEntry) {
                videoModel = (DownloadEntry) i;
                adapter.setSelectedPosition(playingVideoIndex);
                adapter.setVideoId(videoModel.videoId);
                playVideoModel();
                break;
            }
            // try next
            playingVideoIndex--;
        }
    }

    private boolean hasPreviousVideo(int playingIndex) {
        for (int i = playingIndex - 1; i >= 0; i--) {
            SectionItemInterface d = adapter.getItem(i);
            if (d != null && d instanceof DownloadEntry) {
                return true;
            }
        }
        return false;
    }

    private View.OnClickListener getNextListener(){
        if (hasNextVideo(playingVideoIndex)) {
            return new NextClickListener();
        }
        return null;
    }

    private View.OnClickListener getPreviousListener(){
        if (hasPreviousVideo(playingVideoIndex)) {
            return new PreviousClickListener();
        }
        return null;
    }

    private class NextClickListener implements OnClickListener{
        @Override
        public void onClick(View v) {
            playNext();
        }
    }

    private class PreviousClickListener implements OnClickListener{
        @Override
        public void onClick(View v) {
            playPrevious();
        }
    }

    private DataCallback<Integer> watchedStateCallback = new DataCallback<Integer>() {
        @Override
        public void onResult(Integer result) {
            logger.debug("Watched State Updated");
        }

        @Override
        public void onFail(Exception ex) {
            logger.error(ex);
        }
    };

    private DataCallback<Integer> setCurrentPositionCallback = new DataCallback<Integer>() {
        @Override
        public void onResult(Integer result) {
            logger.debug("Current Playback Position Updated");
        }

        @Override
        public void onFail(Exception ex) {
            logger.error(ex);
        }
    };

    protected ProfileModel getProfile() {
        PrefManager prefManager = new PrefManager(getActivity(), PrefManager.Pref.LOGIN);
        return prefManager.getCurrentUserProfile();
    }
}
