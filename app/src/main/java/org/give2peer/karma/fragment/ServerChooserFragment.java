package org.give2peer.karma.fragment;

import android.os.Bundle;
import android.preference.ListPreference;
//import android.preference.PreferenceFragment;
//import android.support.v4.
//import android.support.v4.app.FragmentManager;
import android.preference.PreferenceFragment;

import org.give2peer.karma.R;
import org.give2peer.karma.preference.ServerChooserPreference;

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