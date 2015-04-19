package org.give2peer.give2peer.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.give2peer.give2peer.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}