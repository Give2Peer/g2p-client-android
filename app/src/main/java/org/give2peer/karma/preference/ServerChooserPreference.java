package org.give2peer.karma.preference;

import android.app.Activity;
import android.content.Intent;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;

import org.give2peer.karma.Application;
import org.give2peer.karma.activity.SettingsActivity;
import org.give2peer.karma.entity.Server;

import java.util.List;

/**
 * As we're using the server chooser in multiple places, we've moved the logic to this class.
 * It is used in the ServerSettingsFragment and ServerChooserFragment
 */
public class ServerChooserPreference
{
    // Makes sure GC does not eat our listener
    protected Preference.OnPreferenceChangeListener notGarbageListener;

    public ServerChooserPreference(
            final Activity activity,
            final ListPreference serversListChooser,
            final boolean provideAddNewServer)
    {
        final Application app = (Application) activity.getApplication();

        // List the server configurations
        final List<Server> servers = Server.listAll(Server.class);
        int serversCount = servers.size();

        if (serversCount == 0) {
            serversListChooser.setEnabled(false);
            return;
        }

        int plusAddNew = 0;
        if (provideAddNewServer) {
            plusAddNew = 1;
        }

        CharSequence[] entries     = new CharSequence[serversCount+plusAddNew];
        CharSequence[] entryValues = new CharSequence[serversCount+plusAddNew];

        // Servers choices
        for (int i=0; i<serversCount; i++) {
            Server config = servers.get(i);
            entries    [i] = config.getName()+"\n"+config.getUrl();
            entryValues[i] = config.getId().toString();
        }

        // "New server" convenience choice
        if (provideAddNewServer) {
            entries    [serversCount] = "Add a new server";
            entryValues[serversCount] = "-1";
        }

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
                if (-1 == id) { // "Add a new server" convenience choice that redirects to settings
                    Intent intent = new Intent(activity, SettingsActivity.class);
                    activity.startActivity(intent);
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