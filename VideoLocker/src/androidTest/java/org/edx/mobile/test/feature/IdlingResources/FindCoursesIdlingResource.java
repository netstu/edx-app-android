package org.edx.mobile.test.feature.IdlingResources;

import android.app.ActivityManager;
import android.content.Context;
import android.support.test.espresso.IdlingResource;
import android.util.Log;

import org.edx.mobile.view.WebViewFindCoursesActivity;

/**
 * Created by cleeedx on 4/5/16.
 */

public class FindCoursesIdlingResource implements IdlingResource {

    private static final String TAG = "MyActivity";

    private ResourceCallback resourceCallback;
    private WebViewFindCoursesActivity webViewFindCoursesActivity;

    private final Context context;

    public FindCoursesIdlingResource(Context context) {
        this.context = context;
        webViewFindCoursesActivity = (WebViewFindCoursesActivity) context;
    }

//    public FindCoursesIdlingResource(WebViewFindCoursesActivity activity){
//        webViewFindCoursesActivity = activity;
//    }

    @Override
    public String getName() {
        return FindCoursesIdlingResource.class.getName();
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = isFindCoursesWebViewLoaded();
        if (idle && resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }

    private boolean isFindCoursesWebViewLoaded() {
        if (webViewFindCoursesActivity.isWebViewLoaded() ) {
            return true;
        } else {
            Log.v(TAG, "false");
            return false;
        }
    }
}