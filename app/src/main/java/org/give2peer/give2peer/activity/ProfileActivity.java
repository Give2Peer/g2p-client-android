package org.give2peer.give2peer.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.R;


/**
 * The profile activity.
 * It requires the user to be registered.
 * - User informations
 * - Items added by the user
 */
public class ProfileActivity extends ActionBarActivity
{
    Application app;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
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
        // todo
    }
}
