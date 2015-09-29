package org.give2peer.give2peer.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    TextView profileUsernameTextView;
    @ViewById
    TextView profileLevelTextView;
    @ViewById
    TextView profileExperienceProgressTextView;
    @ViewById
    TextView profileExperienceRequiredTextView;
    @ViewById
    ProgressBar profileLevelProgressBar;

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
                    // todo : better error handling here
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
                    app.toast(e.toString());
                }
            }
        }.execute();
    }

    protected void refreshUI (User user)
    {
        profileUsernameTextView.setText(user.getPrettyUsername());
        profileLevelTextView.setText(String.valueOf(user.getLevel()));
        profileExperienceProgressTextView.setText(String.valueOf(user.getExperienceProgress()));
        profileExperienceRequiredTextView.setText(String.valueOf(user.getExperienceRequired()));
        profileLevelProgressBar.setMax(user.getExperienceRequired());
        profileLevelProgressBar.setProgress(user.getExperienceProgress());
    }
}
