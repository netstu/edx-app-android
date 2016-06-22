package org.edx.mobile.view.view_holders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import org.edx.mobile.R;
import org.edx.mobile.discussion.DiscussionTextUtils;
import org.edx.mobile.discussion.IAuthorData;
import org.edx.mobile.user.ProfileImage;
import org.edx.mobile.util.Config;

public class AuthorLayoutViewHolder {

    public interface ProfileImageProvider {
        @Nullable
        ProfileImage getProfileImage();
    }

    public final ViewGroup profileRow;
    public final ImageView profileImageView;
    public final TextView authorTextView;
    public final TextView dateTextView;
    public final TextView answerTextView;

    public AuthorLayoutViewHolder(View itemView) {
        profileRow = (ViewGroup) itemView;
        profileImageView = (ImageView) itemView.findViewById(R.id.profile_image);
        authorTextView = (TextView) itemView.findViewById(R.id.discussion_author_text_view);
        dateTextView = (TextView) itemView.findViewById(R.id.discussion_date_text_view);
        answerTextView = (TextView) itemView.findViewById(R.id.discussion_responses_answer_text_view);

        final Context context = answerTextView.getContext();
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                answerTextView,
                new IconDrawable(context, FontAwesomeIcons.fa_check_square_o)
                        .sizeRes(context, R.dimen.edx_xxx_small)
                        .colorRes(context, R.color.edx_utility_success),
                null, null, null);
    }

    public void populateViewHolder(@NonNull Config config, @NonNull IAuthorData authorData,
                                   @NonNull ProfileImageProvider provider,
                                   long initialTimeStampMs,
                                   @NonNull final Runnable listener) {
        final Context context = profileImageView.getContext();
        if (provider.getProfileImage() != null) {
            ProfileImage profileImage = provider.getProfileImage();
            if (profileImage.hasImage()) {
                Glide.with(context).load(profileImage.getImageUrlMedium())
                        .into(profileImageView);
            } else {
                profileImageView.setImageResource(R.drawable.xsie);
            }
        }
        DiscussionTextUtils.setAuthorText(authorTextView, authorData);
        if (authorData.getCreatedAt() != null) {
            CharSequence relativeTime = DiscussionTextUtils.getRelativeTimeSpanString(context,
                    initialTimeStampMs, authorData.getCreatedAt().getTime());
            dateTextView.setText(relativeTime);
        } else {
            dateTextView.setVisibility(View.GONE);
        }
        if (config.isUserProfilesEnabled() && !authorData.isAuthorAnonymous()) {
            profileRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.run();
                }
            });
        }
    }
}
