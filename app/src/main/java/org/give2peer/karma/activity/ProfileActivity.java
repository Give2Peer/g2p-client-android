package org.give2peer.karma.activity;

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
import org.apache.http.conn.HttpHostConnectException;
import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.entity.PrivateProfile;
import org.give2peer.karma.entity.User;

import java.net.UnknownHostException;


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
        Log.d("G2P", "Starting profile activity.");

        super.onCreate(savedInstanceState);
        app = (Application) getApplication();
    }

    @Override
    protected void onResume()
    {
        Log.d("G2P", "Resuming profile activity.");

        super.onResume();

        // If the user is not authenticated, forward him to the login activity.
        app.requireAuthentication(this);

        // Fill up the profile page
        synchronize();
    }

    @Click
    void profileRetryButton()
    {
        profileRetryButton.setVisibility(View.GONE);
        profileLoadingProgressBar.setVisibility(View.VISIBLE);
        synchronize();
    }

    protected void refreshUI (PrivateProfile profile)
    {
        //Log.d("G2P", "Refreshed !");

        // Clear the space
        profileLoadingLayout.setVisibility(View.GONE);
        profileRetryButton.setVisibility(View.GONE);

        // User
        User user = profile.user;
        profileUsernameTextView.setText(user.getPrettyUsername());
        profileLevelTextView.setText(String.valueOf(user.getLevel()));
        profileExperienceProgressTextView.setText(String.valueOf(user.getKarmaProgress()));
        profileExperienceRequiredTextView.setText(String.valueOf(user.getKarmaRequired()));
        profileLevelProgressBar.setMax(user.getKarmaRequired());
        profileLevelProgressBar.setProgress(user.getKarmaProgress());

        // Items authored
        Log.d("G2P", "ITEMS");
        for (org.give2peer.karma.entity.Item item : profile.items) {
            Log.d("G2P", "Item : "+item.getTitle()+" - "+item.getLocation()+" - "+item.getCreatedAt());
        }
        Log.d("G2P", "END ITEMS");




        // Show the content
        profileContentLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Download the profile data from the server and trigger a refresh of the UI.
     * This is really too verbose.
     */
    protected void synchronize()
    {
        final Application app = this.app;
        new AsyncTask<Void, Void, Void>()
        {
            PrivateProfile profile;
            Exception e;

            @Override
            protected Void doInBackground(Void... nope)
            {
                try {
                    profile = app.getRestService().getProfile();
                } catch (Exception oops) {
                    e = oops;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void nope)
            {
                super.onPostExecute(nope);

                if (null != profile) {
                    refreshUI(profile);
                } else {
                    String msg = e.toString();
                    if (e instanceof HttpHostConnectException ||
                            e instanceof UnknownHostException) {
                        msg = getString(R.string.toast_no_internet_available);
                    }
                    app.toasty(msg);
                    profileRetryButton.setVisibility(View.VISIBLE);
                    profileLoadingProgressBar.setVisibility(View.GONE);
                }
            }
        }.execute();
    }
}