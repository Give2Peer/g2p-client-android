package org.give2peer.give2peer.fragment;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.entity.ServerConfiguration;

import java.util.List;

public class ServerChooserFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Application app = (Application) getActivity().getApplication();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.server_chooser);

        // Grab the View we're going to edit
        final ListPreference serversListChooser = (ListPreference) getPreferenceManager()
                .findPreference("current_server_id");

        // List the server configurations
        final List<ServerConfiguration> servers = ServerConfiguration.listAll(ServerConfiguration.class);
        int serversCount = servers.size();

        Log.i("G2P", String.format("Found %d server configuration(s).", serversCount));

        if (serversCount == 0) {
            serversListChooser.setEnabled(false);
            return;
        }

        CharSequence[] entries     = new CharSequence[serversCount];
        CharSequence[] entryValues = new CharSequence[serversCount];

        for (int i=0; i<serversCount; i++) {
            ServerConfiguration config = servers.get(i);
            entries[i] = config.getName()+"\n"+config.getUrl();
            entryValues[i] = config.getId().toString();
        }

        serversListChooser.setEntries(entries);
        serversListChooser.setEntryValues(entryValues);

        // Pick a default value if none is set
        if (null == serversListChooser.getValue()) {
            Log.i("G2P", "Setting first found server as current server.");
            serversListChooser.setValueIndex(0);
        }

        // Set the name of the server as description
        int currentServerId = Integer.valueOf(serversListChooser.getValue());
        for (int i=0; i<serversCount; i++) {
            ServerConfiguration config = servers.get(i);
            if (config.getId() == currentServerId) {
                serversListChooser.setSummary(config.getName());
                break;
            }
        }

        // Make sure we tell the Application about the configuration change
        serversListChooser.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                if (preference == serversListChooser) {
                    ServerConfiguration server = ServerConfiguration.findById(
                            ServerConfiguration.class,
                            Long.valueOf((String) newValue)
                    );
                    // This will reload our rest service, internally
                    app.setServerConfiguration(server);
                }

                return false;
            }
        });
    }

}