package org.edx.mobile.view.my_videos;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.inject.Inject;

import org.edx.mobile.R;
import org.edx.mobile.base.BaseFragment;
import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.databinding.FragmentMyAllVideosBinding;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.edx.mobile.module.analytics.ISegment;
import org.edx.mobile.task.GetAllDownloadedVideosTask;
import org.edx.mobile.util.AppConstants;
import org.edx.mobile.view.Router;
import org.edx.mobile.view.VideoListActivity;
import org.edx.mobile.view.adapters.MyAllVideoCourseAdapter;

import java.util.List;

public class MyAllVideosFragment extends BaseFragment {

    private MyAllVideoCourseAdapter myCoursesAdaptor;
    private GetAllDownloadedVideosTask getAllDownloadedVideosTask;

    private FragmentMyAllVideosBinding binding;

    @Inject
    protected IEdxEnvironment environment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        environment.getSegment().trackScreenView(ISegment.Screens.MY_VIDEOS_ALL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return DataBindingUtil.inflate(inflater, R.layout.fragment_my_all_videos, container, false).getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        binding = DataBindingUtil.getBinding(getView());
        binding.myVideoCourseList.setEmptyView(binding.emptyListView);

        myCoursesAdaptor = new MyAllVideoCourseAdapter(getActivity(), environment) {
            @Override
            public void onItemClicked(EnrolledCoursesResponse model) {
                AppConstants.myVideosDeleteMode = false;

                Intent videoIntent = new Intent(getActivity(), VideoListActivity.class);
                videoIntent.putExtra(Router.EXTRA_ENROLLMENT, model);
                videoIntent.putExtra(Router.EXTRA_FROM_MY_VIDEOS, true);
                startActivity(videoIntent);
            }
        };

        addMyAllVideosData();
        binding.myVideoCourseList.setAdapter(myCoursesAdaptor);
        binding.myVideoCourseList.setOnItemClickListener(myCoursesAdaptor);
    }

    @Override
    public void onResume() {
        super.onResume();
        addMyAllVideosData();
        myCoursesAdaptor.notifyDataSetChanged();
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (getAllDownloadedVideosTask != null) {
            getAllDownloadedVideosTask.cancel(true);
            getAllDownloadedVideosTask = null;
        }
    }

    private void addMyAllVideosData() {
        if (myCoursesAdaptor != null) {
            myCoursesAdaptor.clear();

            if (getAllDownloadedVideosTask != null) {
                getAllDownloadedVideosTask.cancel(true);
            } else {
                getAllDownloadedVideosTask = new GetAllDownloadedVideosTask(getActivity()) {

                    @Override
                    protected void onSuccess(List<EnrolledCoursesResponse> enrolledCoursesResponses) throws Exception {
                        super.onSuccess(enrolledCoursesResponses);
                        if (enrolledCoursesResponses != null) {
                            for (EnrolledCoursesResponse m : enrolledCoursesResponses) {
                                if (m.isIs_active()) {
                                    myCoursesAdaptor.add(m);
                                }
                            }
                        }
                    }
                };
            }
            getAllDownloadedVideosTask.execute();
        }
    }
}