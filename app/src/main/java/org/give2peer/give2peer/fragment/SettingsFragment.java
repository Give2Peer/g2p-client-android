package org.give2peer.give2peer.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.listener.OnForgetServerClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * We are procedurally generating most of the settings, from our SQLite-persisted servers.
 * This is probably not the best way to do this, but it gives the app a nice feel.
 */
public class SettingsFragment extends PreferenceFragment {

    // These three holders are simply to ensure that the garbage collector will not destroy them
    protected Preference.OnPreferenceChangeListener updateSummaryListener;
    protected Preference.OnPreferenceChangeListener updateObfuscatedSummaryListener;
    protected List<UpdateScreenListener> updateScreenListeners = new ArrayList<>();

    public HashMap<Long, Server> servers = new HashMap<>();

    private class UpdateScreenListener implements Preference.OnPreferenceChangeListener
    {
        PreferenceScreen screen;
        UpdateScreenListener(PreferenceScreen screen)
        {
            this.screen = screen;
        }
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            preference.setSummary((String)newValue);
            screen.setTitle((String)newValue);

            return true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        // We'll need that later when we'll have static preferences
        // Android actually needs it now too, it seems, to instantiate stuff internally
        addPreferencesFromResource(R.xml.preferences);

        // Build the server's input listeners once, as there's no need to build them many times.
        // Besides, they need to be hard-referenced, or the garbage collector will eat them.
        updateSummaryListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String)newValue);

                return true;
            }
        };

        updateObfuscatedSummaryListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(new String(new char[((String)newValue).length()])
                          .replace("\0", "*"));

                return true;
            }
        };

        refreshView();
    }

    public void refreshView() {

        // We can only do this using a Reflection hack I don't like. Too bad. And, also, WTF?
        // pm.inflateFromResource((Context) getActivity(), R.xml.server_editor, null)

        // Therefore, we're going to procedurally create everything

        // List the server configurations
        Iterator<Server> serversIterator = Server.findAll(Server.class);

        // Prepare some variables
        Context context = (Context) getActivity();
        PreferenceManager pm = getPreferenceManager();

        // Freshen the screen
        getPreferenceScreen().removeAll();

        // Create the Server category
        PreferenceCategory cat = new PreferenceCategory(context);
        cat.setTitle("Servers");

        // The category MUST be added to the root node BEFORE we add screens to it
        // (something about injected deps like PreferenceManager, probably)
        // This code can possibly be improved by much, remember.
        getPreferenceScreen().addPreference(cat);

        for (;serversIterator.hasNext();) {
            Server server = serversIterator.next();

            servers.put(server.getId(), server);

            //Log.i("G2P", "Creating prefs screen for " + server.getName());

            // Create a screen for the server

            PreferenceScreen screen = pm.createPreferenceScreen(context);
            screen.setTitle(server.getName());
            screen.setIcon(R.drawable.ic_edit_black_36dp);

            UpdateScreenListener updateScreenListener = new UpdateScreenListener(screen);
            updateScreenListeners.add(updateScreenListener);

            EditTextPreference name = new EditTextPreference(context);
            name.setTitle("Name");
            name.setKey(String.format("server_%d_name", server.getId()));
            name.setSummary(server.getName());
            name.setText(server.getName());
            name.setOnPreferenceChangeListener(updateScreenListener);
            screen.addPreference(name);

            EditTextPreference uri = new EditTextPreference(context);
            uri.setTitle("Address");
            uri.setKey(String.format("server_%d_uri", server.getId()));
            uri.setSummary(server.getUrl());
            uri.setText(server.getUrl());
            uri.setOnPreferenceChangeListener(updateSummaryListener);
            screen.addPreference(uri);

            EditTextPreference username = new EditTextPreference(context);
            username.setTitle("Username");
            username.setKey(String.format("server_%d_username", server.getId()));
            username.setSummary(server.getUsername());
            username.setText(server.getUsername());
            username.setOnPreferenceChangeListener(updateSummaryListener);
            screen.addPreference(username);

            EditTextPreference password = new EditTextPreference(context);
            password.setTitle("Password");
            password.setKey(String.format("server_%d_password", server.getId()));
            password.setSummary(new String(new char[server.getPassword().length()])
                    .replace("\0", "*"));
            password.setText(server.getPassword());
            password.setOnPreferenceChangeListener(updateObfuscatedSummaryListener);
            screen.addPreference(password);

            // Forget this server
            Preference delServer = new Preference(context);
            delServer.setTitle("Forget this server");
            delServer.setIcon(R.drawable.ic_gps_fixed_black_18dp);

            delServer.setOnPreferenceClickListener(new OnForgetServerClickListener(this, server));

            screen.addPreference(delServer);

            cat.addPreference(screen);
        }

        // Add server button
        Preference addServer = new Preference(context);
        addServer.setTitle("Add a server");
        addServer.setIcon(R.drawable.ic_add_circle_outline_black_36dp);

        addServer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Server newServer = (new Server()).loadDummy();
                newServer.save();
                refreshView();
                return true;
            }
        });

        cat.addPreference(addServer);
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i("G2P", "Updating server configuration...");

        Application app = (Application) getActivity().getApplication();
        SharedPreferences prefs = app.getPrefs();

        for (Server server : servers.values()) {
            Long id = server.getId();
            server.setName(prefs.getString(String.format("server_%d_name", id),
                    server.getName()));
            server.setUrl(prefs.getString(String.format("server_%d_uri", id),
                    server.getUrl()));
            server.setUsername(prefs.getString(String.format("server_%d_username", id),
                    server.getUsername()));
            server.setPassword(prefs.getString(String.format("server_%d_password", id),
                    server.getPassword()));

            server.save();
        }

    }

}