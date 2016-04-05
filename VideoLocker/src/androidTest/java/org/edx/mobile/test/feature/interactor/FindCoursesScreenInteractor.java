package org.edx.mobile.test.feature.interactor;

import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;

import org.edx.mobile.view.WebViewFindCoursesActivity;
import org.junit.Rule;

import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;


/**
 * Created by cleeedx on 4/5/16.
 */
public class FindCoursesScreenInteractor {

    @Rule
    public ActivityTestRule<WebViewFindCoursesActivity> mActivityRule = new ActivityTestRule<>(WebViewFindCoursesActivity.class);

    public FindCoursesScreenInteractor observeFindCoursesScreen() {
        onWebView().forceJavascriptEnabled();

        onWebView()
                .withElement(findElement(Locator.CLASS_NAME, "course-link"))
                .perform(webClick());
        onWebView()
                .withElement(findElement(Locator.CLASS_NAME, "result-count"))
                .check(webMatches(getText(), endsWith("results matching")));
        onWebView()
                .withElement(findElement(Locator.CLASS_NAME, "result-count"))
                .check(webMatches(getText(), not(containsString("0 results matching"))));
        return this;
    }
}