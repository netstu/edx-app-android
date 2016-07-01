package org.edx.mobile.event;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.edx.mobile.R;
import org.edx.mobile.third_party.lang.BooleanUtils;
import org.edx.mobile.third_party.lang.ObjectUtils;
import org.edx.mobile.third_party.versioning.ArtifactVersion;

import java.util.Date;

import de.greenrobot.event.EventBus;

/**
 * An event signifying that a new version of the app is available on the app stores.
 */
public class NewVersionAvailableEvent implements Comparable<NewVersionAvailableEvent> {
    /**
     * Post an instance of NewVersionAvailableEvent on the event bus, based on the provided
     * properties, if this hasn't been posted before. The sticky events will be queried for an
     * existing report, and a new one will only be posted if it has more urgent information than the
     * previous one. The events are posted and retained as sticky events in order to have a
     * conveniently and semantically accessible session-based singleton of it to compare against,
     * but this has the implication that they can't be removed from the event bus after consumption
     * by the subscribers. To address this restriction, this class defined methods to mark instances
     * as having being consumed, which can be used by subscribers for this purpose.
     *
     * If all the parameters are null or false, then it wouldn't be a valid event, and nothing would
     * be posted on the event bus.
     *
     * @param newVersion        The version number of the latest release of the app.
     * @param lastSupportedDate The last date on which the current version of the app will be
     *                          supported.
     * @param isUnsupported     Whether the current version is unsupported. This is based on whether
     *                          we're getting HTTP 426 errors, and thus can't be inferred from the
     *                          last supported date (the two properties may not be consistent with
     *                          each other due to wrong local clock time or an inconsistency in the
     *                          server configurations).
     */
    public static void post(@Nullable final ArtifactVersion newVersion,
                            @Nullable final Date lastSupportedDate,
                            final boolean isUnsupported) {
        final NewVersionAvailableEvent event;
        try {
            event = new NewVersionAvailableEvent(newVersion, lastSupportedDate, isUnsupported);
        } catch (IllegalArgumentException e) {
            return;
        }
        final EventBus eventBus = EventBus.getDefault();
        final NewVersionAvailableEvent postedEvent =
                eventBus.getStickyEvent(NewVersionAvailableEvent.class);
        if (postedEvent == null || event.compareTo(postedEvent) > 0) {
            eventBus.postSticky(event);
        }
    }

    @Nullable
    private final ArtifactVersion newVersion;
    @Nullable
    private final Date lastSupportedDate;
    private final boolean isUnsupported;

    private boolean isConsumed;

    /**
     * Construct a new instance of NewVersionAvailableEvent. Any individual parameter can be null or
     * false, but at last one needs to be non-null or true in order for the event to be valid. The
     * constructor is private because the class is only supposed to be initialized from the
     * {@link #post(ArtifactVersion, Date, boolean)} method.
     *
     * @param newVersion        The version number of the latest release of the app.
     * @param lastSupportedDate The last date on which the current version of the app will be
     *                          supported.
     * @param isUnsupported     Whether the current version is unsupported. This is based on whether
     *                          we're getting HTTP 426 errors, and thus can't be inferred from the
     *                          last supported date (the two properties may not be consistent with
     *                          each other due to wrong local clock time or an inconsistency in the
     *                          server configurations).
     * @throws IllegalArgumentException if all of the parameters are {@code null} or {@code false}.
     */
    private NewVersionAvailableEvent(@Nullable final ArtifactVersion newVersion,
                                    @Nullable final Date lastSupportedDate,
                                    final boolean isUnsupported) {
            throws IllegalArgumentException {
        if (!isUnsupported && lastSupportedDate == null && newVersion == null) {
            throw new IllegalStateException("At least one parameter needs to be non-null or true");
        }
        this.newVersion = newVersion;
        // Date is not immutable, so make a defensive copy of it.
        this.lastSupportedDate = lastSupportedDate == null ?
                null : (Date) lastSupportedDate.clone();
        this.isUnsupported = isUnsupported;
    }

    /**
     * @return The version number of the latest release of the app, or {@code null} if not
     *         available.
     */
    @Nullable
    public ArtifactVersion getNewVersion() {
        return newVersion;
    }

    /**
     * @return The last date on which the current version of the app will be supported, or
     *         {@code null} if not available.
     */
    @Nullable
    public Date getLastSupportedDate() {
        return lastSupportedDate;
    }

    /**
     * Returns whether the current version is unsupported. This is based on whether we're getting
     * HTTP 426 errors, and thus can't be inferred from the last supported date (the two properties
     * may not be consistent with each other due to wrong local clock time or an inconsistency in
     * the server configurations).
     *
     * @return Whether the current version is unsupported.
     */
    public boolean isUnsupported() {
        return isUnsupported;
    }

    /**
     * Resolve the notification string, and return it.
     *
     * @param context A Context to resolve the string
     * @return The notification string.
     */
    @NonNull
    public CharSequence getNotificationString(@NonNull final Context context) {
        @StringRes
        final int notificationStringRes;
        if (isUnsupported) {
            notificationStringRes = R.string.app_version_unsupported;
        } else if (lastSupportedDate == null) {
            notificationStringRes = R.string.app_version_outdated;
        } else {
            // Deadline date is available, but won't be displayed for now.
            notificationStringRes = R.string.app_version_deprecated;
        }
        return context.getText(notificationStringRes);
    }

    /**
     * Mark the event as consumed by the subscribers.
     */
    public void markAsConsumed() {
        isConsumed = true;
    }

    /**
     * @return Whether the event has been consumed by the subscribers.
     */
    public boolean isConsumed() {
        return isConsumed;
    }

    /**
     * Compare this to another instance to determine their priority. Events reporting the current
     * app version as unsupported have the highest priority, followed by deprecation events, which
     * are prioritised according to the closeness of the last supported date they report, followed
     * by new version availability events, which are prioritized according to the reported new
     * version number.
     *
     * @param another the object to compare to this instance.
     * @return a negative integer if this instance has lesser priority than {@code another};
     *         a positive integer if this instance has greater priority than {@code another};
     *         0 if this instance has the same priority as {@code another}.
     * @throws ClassCastException if {@code another} cannot be converted into
     *         something comparable to {@code this} instance.
     */
    @Override
    public int compareTo(@NonNull final NewVersionAvailableEvent another) {
        int result = BooleanUtils.compare(isUnsupported, another.isUnsupported);
        if (result == 0) {
            /* Reverse the comparator here, since the closer the date is, the higher the priority.
             * Since we're reversing the result, the comparator is instructed to count a null value
             * in the last supported date (non-deprecated new version availability event) as of
             * higher priority than a non-null value (deprecation event).
             */
            result = 0 - ObjectUtils.compare(lastSupportedDate, another.lastSupportedDate, true);
            if (result == 0) {
                result = ObjectUtils.compare(newVersion, another.newVersion);
            }
        }
        return result;
    }
}
