package org.give2peer.give2peer.activity;

import android.app.Activity;
import android.os.Bundle;

import org.give2peer.give2peer.fragment.SettingsFragment;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
