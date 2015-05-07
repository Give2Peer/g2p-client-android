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

import java.util.List;

public class ServerChooserFragment extends PreferenceFragment {

    // Makes sure GC does not eat our listener
    protected Preference.OnPreferenceChangeListener notGarbageListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.server_chooser);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshView();
    }

    public void refreshView() {

        final Application app = (Application) getActivity().getApplication();

        // Grab the View we're going to edit
        final ListPreference serversListChooser = (ListPreference) getPreferenceManager()
                .findPreference("current_server_id");

        // List the server configurations
        final List<Server> servers = Server.listAll(Server.class);
        int serversCount = servers.size();

        Log.i("G2P", String.format("Found %d server configuration(s).", serversCount));

        if (serversCount == 0) {
            serversListChooser.setEnabled(false);
            return;
        }

        CharSequence[] entries     = new CharSequence[serversCount+1];
        CharSequence[] entryValues = new CharSequence[serversCount+1];

        // Servers choices
        for (int i=0; i<serversCount; i++) {
            Server config = servers.get(i);
            entries    [i] = config.getName()+"\n"+config.getUrl();
            entryValues[i] = config.getId().toString();
        }

        // "New server" convenience choice
        entries    [serversCount] = "Add a new server";
        entryValues[serversCount] = "-1";

        serversListChooser.setEntries(entries);
        serversListChooser.setEntryValues(entryValues);

        // Pick a default value if none is set
        if (null == serversListChooser.getValue()) {
            Log.i("G2P", "Setting first found server as current server.");
            serversListChooser.setValueIndex(0);
        }
        if (Integer.valueOf(serversListChooser.getValue()) >= serversCount) {
            Log.i("G2P", "Setting first found server as current server.");
            serversListChooser.setValueIndex(0);
        }

        // Set the name of the server as description
        int currentServerId = Integer.valueOf(serversListChooser.getValue());
        for (int i=0; i<serversCount; i++) {
            Server config = servers.get(i);
            if (config.getId() == currentServerId) {
                serversListChooser.setSummary(config.getName());
                break;
            }
        }

        // Make sure we tell the Application about the configuration change
        notGarbageListener = new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                long id = Long.valueOf((String) newValue);
                if (-1 == id) { // "Add a new server" convenience choice: redirect to settings
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    startActivity(intent);
                    return false;
                } else {        // Selected a server, let's use that one now
                    Server server = Server.findById(Server.class, id);
                    // Update the summary of the chooser
                    serversListChooser.setSummary(server.getName());
                    // This will also reload our rest service, internally
                    app.setServerConfiguration(server);

                    return true;
                }

            }
        };
        serversListChooser.setOnPreferenceChangeListener(notGarbageListener);
    }
}