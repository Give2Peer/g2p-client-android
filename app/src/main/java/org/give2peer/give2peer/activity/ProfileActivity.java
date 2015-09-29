package org.give2peer.give2peer.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.entity.User;


/**
 * The profile activity.
 * It requires the user to be registered.
 * - User informations
 * - Items added by the user
 */
@EActivity(R.layout.activity_profile)
public class ProfileActivity extends ActionBarActivity
{
    Application app;

    @ViewById
    TextView       profileUsernameTextView;
    @ViewById
    TextView       profileLevelTextView;
    @ViewById
    TextView       profileExperienceProgressTextView;
    @ViewById
    TextView       profileExperienceRequiredTextView;
    @ViewById
    ProgressBar    profileLevelProgressBar;
    @ViewById
    LinearLayout   profileContentLayout;
    @ViewById
    RelativeLayout profileLoadingLayout;
    @ViewById
    ProgressBar    profileLoadingProgressBar;
    @ViewById
    Button         profileRetryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        app = (Application) getApplication();

        Log.d("G2P", "Starting profile activity.");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d("G2P", "Resuming profile activity.");

        // If the user is not authenticated, forward him to the login activity.
        app.requireAuthentication(this);

        // Fill up the profile page
        synchronize();
    }

    protected void synchronize()
    {
        final Application app = this.app;
        new AsyncTask<Void, Void, Void>()
        {
            User me;
            Exception e;

            @Override
            protected Void doInBackground(Void... nope)
            {
                try {
                    me = app.getRestService().getProfile();
                } catch (Exception oops) {
                    e = oops;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void nope)
            {
                super.onPostExecute(nope);

                if (null != me) {
                    refreshUI(me);
                } else {
                    // todo : better error handling here, depending on Exception type
                    app.toast(e.toString());
                    profileRetryButton.setVisibility(View.VISIBLE);
                    profileLoadingProgressBar.setVisibility(View.GONE);
                }
            }
        }.execute();
    }

    @Click
    void profileRetryButton()
    {
        profileRetryButton.setVisibility(View.GONE);
        profileLoadingProgressBar.setVisibility(View.VISIBLE);
        synchronize();
    }

    protected void refreshUI (User user)
    {
        profileUsernameTextView.setText(user.getPrettyUsername());
        profileLevelTextView.setText(String.valueOf(user.getLevel()));
        profileExperienceProgressTextView.setText(String.valueOf(user.getExperienceProgress()));
        profileExperienceRequiredTextView.setText(String.valueOf(user.getExperienceRequired()));
        profileLevelProgressBar.setMax(user.getExperienceRequired());
        profileLevelProgressBar.setProgress(user.getExperienceProgress());

        profileLoadingLayout.setVisibility(View.GONE);
        profileContentLayout.setVisibility(View.VISIBLE);
    }
}
