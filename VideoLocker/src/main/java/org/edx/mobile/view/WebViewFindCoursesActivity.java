package org.edx.mobile.view;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.edx.mobile.R;
import org.edx.mobile.base.FindCoursesBaseActivity;
import org.edx.mobile.module.analytics.ISegment;

import java.util.Date;

import roboguice.inject.ContentView;

@ContentView(R.layout.activity_find_courses)
public class WebViewFindCoursesActivity extends FindCoursesBaseActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // configure slider layout. This should be called only once and
        // hence is shifted to onCreate() function
        configureDrawer();

        environment.getSegment().trackScreenView(ISegment.Screens.FIND_COURSES);

        webView = (WebView) findViewById(R.id.webview);
//        webView.loadUrl(environment.getConfig().getCourseDiscoveryConfig().getCourseSearchUrl());
        webView.loadUrl("http://csb.stanford.edu/class/public/pages/sykes_webdesign/05_simple.html");
    }

    @Override
    protected void onOnline() {
        super.onOnline();
        if (!isWebViewLoaded()) {
            webView.reload();
        }
    }
}
