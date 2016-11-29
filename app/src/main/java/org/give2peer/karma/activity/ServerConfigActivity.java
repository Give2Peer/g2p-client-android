package org.give2peer.karma.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import org.give2peer.karma.fragment.ServerSettingsFragment;

/**
 * An activity for developers only (for now) that allows them to configure the server this app
 * should connect to.
 * We're hiding this from regular users because it can be confusing, and somewhat dangerous.
 * Anyhow, it's useless right now, as no-one is using this app. I suck at marketing T_T
 */
public class ServerConfigActivity extends FragmentActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new ServerSettingsFragment())
                .commit();
    }
}
