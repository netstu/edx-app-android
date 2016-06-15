package org.edx.mobile.http;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.edx.mobile.BuildConfig;
import org.edx.mobile.event.NewVersionAvailableEvent;
import org.edx.mobile.third_party.versioning.ArtifactVersion;
import org.edx.mobile.third_party.versioning.DefaultArtifactVersion;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * An OkHttp interceptor that checks for information about app
 * updates in the response headers, and broadcasts them on the event
 * bus if found.
 */
public class NewVersionBroadcastInterceptor implements Interceptor {
    /**
     * Header field name for the latest version number of the app
     * that is available in the app stores.
     */
    private static final String HEADER_APP_LATEST_VERSION =
            "EDX-APP-LATEST-VERSION";
    /**
     * Header field name for the last date up to which the API used
     * by the current version of the app will be supported and
     * maintained.
     */
    private static final String HEADER_APP_VERSION_LAST_SUPPORTED_DATE =
            "EDX-APP-VERSION-LAST-SUPPORTED-DATE";

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Response response = chain.proceed(chain.request());

        final String appLatestVersionString = response.header(HEADER_APP_LATEST_VERSION);
        final ArtifactVersion appLatestVersion = appLatestVersionString == null ?
                null : new DefaultArtifactVersion(appLatestVersionString);

        final String lastSupportedDateString =
                response.header(HEADER_APP_VERSION_LAST_SUPPORTED_DATE);
        Date lastSupportedDate = null;
        if (lastSupportedDateString != null) {
            /* We're using Apache Commons Lang's FastDateFormat here, because it
             * has improved support for ISO 8601, including UTC offsets and the
             * "Z" notation.
             * TODO: Implement this all over the app instead of the existing
             * SimpleDateFormat.
             */
            try {
                lastSupportedDate = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT
                        .parse(lastSupportedDateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // TODO: Create a utility class that defines all HTTP errors as constants.
        final boolean isUnsupported = response.code() == 426;

        // If any of these properties is available and valid, then broadcast the
        // event with the information we have.
        if (isUnsupported || lastSupportedDate != null || appLatestVersion != null &&
                appLatestVersion.compareTo(
                        new DefaultArtifactVersion(BuildConfig.VERSION_NAME)) > 0) {
            NewVersionAvailableEvent.post(appLatestVersion, lastSupportedDate, isUnsupported);
        }

        return response;
    }
}
