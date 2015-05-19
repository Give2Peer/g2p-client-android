package org.give2peer.give2peer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.activity.SettingsActivity;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.preference.ServerChooserPreference;

import java.util.List;

public class ServerChooserFragment extends PreferenceFragment
{
    protected ServerChooserPreference scp;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Load the preferences layout from an XML resource
        addPreferencesFromResource(R.xml.server_chooser);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refreshView();
    }

    public void refreshView()
    {
        // Grab the View we're going to edit
        final ListPreference serversListChooser = (ListPreference) getPreferenceManager()
                .findPreference("current_server_id");

        // The ServerChooserPreference fills the list with appropriate values
        scp = new ServerChooserPreference(getActivity(), serversListChooser, true);
    }
}