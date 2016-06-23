package org.edx.mobile.profiles;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.edx.mobile.third_party.lang.ObjectUtils;

public class UserProfileBioModel {
    @Nullable
    public final String bioText;
    @NonNull
    public final ContentType contentType;

    public UserProfileBioModel(@NonNull ContentType contentType, String bioText) {
        this.bioText = bioText;
        this.contentType = contentType;
    }

    public enum ContentType {
        EMPTY,
        NO_ABOUT_ME,
        INCOMPLETE,
        PARENTAL_CONSENT_REQUIRED,
        ABOUT_ME
    }

    @Override
    public boolean equals(Object object) {
        if(!(object instanceof UserProfileBioModel)) {
            return false;
        }
        UserProfileBioModel model = (UserProfileBioModel)object;
        return contentType.equals(model.contentType) && ObjectUtils.equals(model.bioText, bioText);
    }
}
