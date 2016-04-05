package org.edx.mobile.test.feature;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.webkit.WebView;

import org.edx.mobile.R;
import org.edx.mobile.test.feature.IdlingResources.ElapsedTimeIdlingResource;
import org.edx.mobile.test.feature.IdlingResources.FindCoursesIdlingResource;
import org.edx.mobile.test.feature.data.TestValues;
import org.edx.mobile.test.feature.interactor.AppInteractor;
import org.edx.mobile.view.WebViewFindCoursesActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webContent;
import static android.support.test.espresso.web.matcher.DomMatchers.hasElementWithId;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;

/**
 * Created by cleeedx on 4/5/16.
 */
public class FindCoursesFeatureTest extends FeatureTest  {

    private ElapsedTimeIdlingResource idlingResource;

    @Rule
    public ActivityTestRule<WebViewFindCoursesActivity> mActivityRule =
            new ActivityTestRule<WebViewFindCoursesActivity>(WebViewFindCoursesActivity.class, false, false) {
                @Override
                protected void afterActivityLaunched() {
                    onWebView().forceJavascriptEnabled();
                }
            };

    @Before
    public void registerIntentServiceIdlingResource() {
        idlingResource = new ElapsedTimeIdlingResource(10000);
        Espresso.registerIdlingResources(idlingResource);
    }

    @After
    public void unregisterIntentServiceIdlingResource() {
        Espresso.unregisterIdlingResources(idlingResource);
    }

    @Test
    public void afterSelectingFindCourses_something() {
        new AppInteractor()
                .launchApp()
                .observeLandingScreen()
                .navigateToLogInScreen()
                .logIn(TestValues.ACTIVE_USER_CREDENTIALS)
                .openNavigationDrawer()
                .selectFindCourses()
                .observeFindCoursesScreen();

    }

    @Test
    public void typeTextInInput_clickButton_SubmitsForm() {
        // Lazily launch the Activity with a custom start Intent per test
        mActivityRule.launchActivity(withWebFormIntent());


        onWebView().check(webContent(hasElementWithId("12")));
//        onWebView()
//                .withElement(findElement(Locator.ID, "12"));
    }

    private static Intent withWebFormIntent() {
        Intent basicFormIntent = new Intent();
        basicFormIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return basicFormIntent;
    }
}
