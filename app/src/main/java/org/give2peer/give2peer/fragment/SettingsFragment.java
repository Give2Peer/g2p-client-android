package org.give2peer.give2peer.fragment;

import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.entity.Location;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.listener.OnForgetEntityClickListener;
import org.give2peer.give2peer.listener.OnLocationNameChangeListener;
import org.give2peer.give2peer.listener.OnLocationPostalChangeListener;
import org.give2peer.give2peer.listener.OnServerNameChangeListener;
import org.give2peer.give2peer.listener.OnServerPasswordChangeListener;
import org.give2peer.give2peer.listener.OnServerUrlChangeListener;
import org.give2peer.give2peer.listener.OnServerUsernameChangeListener;
import org.give2peer.give2peer.listener.OnTestServerClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * We are procedurally generating most of the settings, from our SQLite-persisted servers and
 * locations.
 * This is probably not the best way to do this, but it gives the app a nice feel.
 * Besides, subsonic does it, and as a (blissfully ignorant) user, I really liked it.
 *
 * Servers
 *   [edit] Server Foo
 *   [edit] Server Bar
 *   [add]
 * Addresses
 *   [edit] Home
 *   [edit] Office
 *   [edit] Somewhere else
 */
public class SettingsFragment extends PreferenceFragment {

    // These list holders are simply to ensure that the garbage collector will not eat our listeners
    protected List<OnPreferenceChangeListener> notGarbageChangeListeners = new ArrayList<>();
    protected List<OnPreferenceClickListener>  notGarbageClickListeners  = new ArrayList<>();

    /**
     * A internal collection of servers, rebuilt from database on each view refresh,
     * which is on creation of the fragment and adding of a server. (and maybe deletion)
     * I'm not sure why we have these anymore, as we're always refreshing the View.
     */
    protected HashMap<Long, Server>   servers   = new HashMap<>();

    /**
     * A internal collection of locations, rebuilt from database on each view refresh,
     * I'm not sure why we have these anymore, as we're always refreshing the View.
     */
    protected HashMap<Long, Location> locations = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        // We'll need that later when we'll have static preferences
        // Android actually needs it now too, it seems, to instantiate stuff internally
        addPreferencesFromResource(R.xml.preferences);

        refreshView();
    }

    public void refreshView() {

        // We can only do this using a Reflection hack I don't like. Too bad. And, also, WTF?
        // pm.inflateFromResource((Context) getActivity(), R.xml.server_editor, null)

        // Therefore, we're going to procedurally create *everything*.

        // Prepare some variables
        final Application app = (Application) getActivity().getApplication();
        Context context = (Context) getActivity();
        PreferenceManager pm = getPreferenceManager();

        // Freshen the screen
        getPreferenceScreen().removeAll();

        // Remove all the (old) listeners, so that the GC can eat them
        notGarbageChangeListeners.clear();
        notGarbageClickListeners.clear();

        // Empty the memoization caches
        servers.clear();
        locations.clear();


        /// THE SERVERS ////////////////////////////////////////////////////////////////////////////

        // List the server configurations
        Iterator<Server> serversIterator = Server.findAll(Server.class);

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

            // Create a sub-screen for that server
            PreferenceScreen screen = pm.createPreferenceScreen(context);
            screen.setTitle(server.getName());
            screen.setIcon(R.drawable.ic_edit_black_36dp);

            // Our change or click listeners, that will update the database and the UI
            OnPreferenceChangeListener nameListener     = new OnServerNameChangeListener(server, screen);
            OnPreferenceChangeListener urlListener      = new OnServerUrlChangeListener(server);
            OnPreferenceChangeListener usernameListener = new OnServerUsernameChangeListener(server);
            OnPreferenceChangeListener passwordListener = new OnServerPasswordChangeListener(server);
            OnPreferenceClickListener  forgetListener   = new OnForgetEntityClickListener(this, server);
            // We hard-reference them so that the garbage collector does not destroy them
            notGarbageChangeListeners.add(nameListener);
            notGarbageChangeListeners.add(urlListener);
            notGarbageChangeListeners.add(usernameListener);
            notGarbageChangeListeners.add(passwordListener);
            notGarbageClickListeners.add(forgetListener);

            EditTextPreference name = new EditTextPreference(context);
            name.setTitle("Name");
            name.setKey(String.format("server_%d_name", server.getId()));
            name.setSummary(server.getName());
            name.setText(server.getName());
            name.setOnPreferenceChangeListener(nameListener);
            screen.addPreference(name);

            EditTextPreference uri = new EditTextPreference(context);
            uri.setTitle("Address");
            uri.setKey(String.format("server_%d_uri", server.getId()));
            uri.setSummary(server.getUrl());
            uri.setText(server.getUrl());
            uri.setOnPreferenceChangeListener(urlListener);
            screen.addPreference(uri);

            EditTextPreference username = new EditTextPreference(context);
            username.setTitle("Username");
            username.setKey(String.format("server_%d_username", server.getId()));
            username.setSummary(server.getUsername());
            username.setText(server.getUsername());
            username.setOnPreferenceChangeListener(usernameListener);
            screen.addPreference(username);

            EditTextPreference password = new EditTextPreference(context);
            password.setTitle("Password");
            password.setKey(String.format("server_%d_password", server.getId()));
            password.setSummary(new String(new char[server.getPassword().length()])
                    .replace("\0", "*")); // hmmm. DRY this using a lib ?
            password.setText(server.getPassword());
            password.setOnPreferenceChangeListener(passwordListener);
            screen.addPreference(password);

            // Forget this server
            Preference delServer = new Preference(context);
            delServer.setTitle("Forget this server");
            delServer.setIcon(R.drawable.ic_remove_circle_outline_black_36dp);
            delServer.setOnPreferenceClickListener(forgetListener);
            screen.addPreference(delServer);

            // Test this server configuration
            Preference testServer = new Preference(context);
            testServer.setTitle("Test the server");
            testServer.setIcon(R.drawable.ic_signal_wifi_statusbar_not_connected_black_36dp);
            testServer.setOnPreferenceClickListener(new OnTestServerClickListener(this, server));
            screen.addPreference(testServer);

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


        /// THE LOCATIONS //////////////////////////////////////////////////////////////////////////

        // List the server configurations
        Iterator<Location> locationsIterator = Location.findAll(Location.class);

        // Create the Server category
        PreferenceCategory locationsCategory = new PreferenceCategory(context);
        locationsCategory.setTitle("Locations");

        // The category MUST be added to the root node BEFORE we add screens to it
        // (something about injected deps like PreferenceManager, probably)
        // This code can possibly be improved by much, remember.
        getPreferenceScreen().addPreference(locationsCategory);

        for (;locationsIterator.hasNext();) {
            Location location = locationsIterator.next();

            locations.put(location.getId(), location);

            // Create a sub-screen for that location

            PreferenceScreen screen = pm.createPreferenceScreen(context);
            screen.setTitle(location.getName());
            screen.setIcon(R.drawable.ic_edit_black_36dp);

            // Our change and click listeners, that will update the database and the UI
            OnPreferenceChangeListener nameListener   = new OnLocationNameChangeListener(location, screen);
            OnPreferenceChangeListener postalListener = new OnLocationPostalChangeListener(location);
            OnPreferenceClickListener  forgetListener = new OnForgetEntityClickListener(this, location);
            // We hard-reference them so that the garbage collector does not destroy them
            notGarbageChangeListeners.add(nameListener);
            notGarbageChangeListeners.add(postalListener);
            notGarbageClickListeners.add(forgetListener);

            EditTextPreference name = new EditTextPreference(context);
            name.setTitle("Name");
            name.setKey(String.format("location_%d_name", location.getId()));
            name.setSummary(location.getName());
            name.setText(location.getName());
            name.setOnPreferenceChangeListener(nameListener);
            screen.addPreference(name);

            EditTextPreference postal = new EditTextPreference(context);
            postal.setTitle("Address");
            postal.setKey(String.format("location_%d_postal", location.getId()));
            postal.setSummary(location.getPostal());
            postal.setText(location.getPostal());
            postal.setOnPreferenceChangeListener(postalListener);
            screen.addPreference(postal);

            // Forget this location
            Preference delLocation = new Preference(context);
            delLocation.setTitle("Forget this location");
            delLocation.setIcon(R.drawable.ic_remove_circle_outline_black_36dp);
            delLocation.setOnPreferenceClickListener(forgetListener);
            screen.addPreference(delLocation);

            locationsCategory.addPreference(screen);
        }

        // Add a location
        Preference addLocation = new Preference(context);
        addLocation.setTitle("Add a location");
        addLocation.setIcon(R.drawable.ic_add_circle_outline_black_36dp);
        addLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Location newLocation = (new Location()).loadDummy();
                newLocation.save();
                refreshView();
                return true;
            }
        });

        locationsCategory.addPreference(addLocation);


    }

//    @Override
//    public void onPause() {
//        super.onPause();
//
//        Log.i("G2P", "Updating server configuration...");
//
//        Application app = (Application) getActivity().getApplication();
//        SharedPreferences prefs = app.getPrefs();
//
//        for (Server server : servers.values()) {
//            Long id = server.getId();
//            server.setName(prefs.getString(String.format("server_%d_name", id),
//                    server.getName()));
//            server.setUrl(prefs.getString(String.format("server_%d_uri", id),
//                    server.getUrl()));
//            server.setUsername(prefs.getString(String.format("server_%d_username", id),
//                    server.getUsername()));
//            server.setPassword(prefs.getString(String.format("server_%d_password", id),
//                    server.getPassword()));
//
//            server.save();
//        }
//
//    }

}