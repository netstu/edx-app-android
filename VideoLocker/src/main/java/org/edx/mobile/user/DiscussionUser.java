
package org.edx.mobile.user;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DiscussionUser implements Serializable {

    @SerializedName("profile")
    private Profile profile;

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public static class Profile implements Serializable{

        @SerializedName("image")
        private ProfileImage image;

        public ProfileImage getImage() {
            return image;
        }

        public void setImage(ProfileImage image) {
            this.image = image;
        }

    }
}
