package org.give2peer.karma.fragment;

import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
//import android.preference.ListPreference;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import android.preference.Preference;
//import android.support.v4.preference.PreferenceFragment;

import com.github.machinarius.preferencefragment.PreferenceFragment;

import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.entity.Location;
import org.give2peer.karma.entity.Server;
import org.give2peer.karma.listener.OnForgetEntityClickListener;
import org.give2peer.karma.listener.OnLocationNameChangeListener;
import org.give2peer.karma.listener.OnLocationPostalChangeListener;
import org.give2peer.karma.listener.OnServerNameChangeListener;
import org.give2peer.karma.listener.OnServerPasswordChangeListener;
import org.give2peer.karma.listener.OnServerUrlChangeListener;
import org.give2peer.karma.listener.OnServerUsernameChangeListener;
import org.give2peer.karma.listener.OnTestServerClickListener;
import org.give2peer.karma.preference.ServerChooserPreference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * THIS FILE IS BAD. BAD, BAD, BAD. Dirty hacks, inefficient learner's code, bloat, it's got it all.
 * We are procedurally generating most of the settings, from our SQLite-persisted servers and
 * locations.
 * This is probably not the best way to do this, but it gives the app a nice feel.
 * Besides, subsonic does it, and as a (blissfully ignorant) user, I really liked it.
 *
 * ---
 *
 * Forgive me, it was like the very first thing I wrote when I first started android. Eurgh.
 *
 * What we're doing before release is to hide the activity showing this fragment.
 * To see it, long press on the copyright logos in the About activity.
 *
 * Servers
 *   [choice] Choose a server
 *   [edit] Default server
 *   [edit] Server Foo
 *   [edit] Server Bar
 *   [add]
 *
 */
public class ServerSettingsFragment extends PreferenceFragment
{
    // These list holders are simply to ensure that the garbage collector will not eat our listeners
    protected List<OnPreferenceChangeListener> notGarbageChangeListeners = new ArrayList<>();
    protected List<OnPreferenceClickListener>  notGarbageClickListeners  = new ArrayList<>();

    protected List<PreferenceScreen> serversEditScreens = new ArrayList<>();

    // To make sure that it is not garbage-collected either, because it too has listeners.
    protected ServerChooserPreference scp;

    /**
     * A internal collection of servers, rebuilt from database on each view refresh,
     * which is on creation of the fragment and adding of a server. (and maybe deletion)
     * I'm not sure why we have these anymore, as we're always refreshing the View.
     */
    //protected HashMap<Long, Server>   servers   = new HashMap<>();

    /**
     * A internal collection of locations, rebuilt from database on each view refresh,
     * I'm not sure why we have these anymore, as we're always refreshing the View.
     */
    //protected HashMap<Long, Location> locations = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource.
        // Android actually requires this, it seems, to instantiate stuff internally, even if
        // it is empty and useless because we're procedurally generating everything.
        addPreferencesFromResource(R.xml.backoffice);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        refreshView();
    }

    public void refreshView()
    {
        refreshView(false);
    }

    public void refreshView(boolean openLastServer)
    {
        // We can only do this using a Reflection hack I don't like. Too bad. And, also, WTF?
        // pm.inflateFromResource((Context) getActivity(), R.xml.server_editor, null)

        // Therefore, we're going to procedurally create *everything*.

        // Prepare some variables
        final Application app = (Application) getActivity().getApplication();
        Context context = (Context) getActivity();
        PreferenceManager pm = getPreferenceManager();
        boolean canSetIcons = app.canSetIcons();

        //context.setTheme(R.style.Theme_AppCompat);

        // Freshen the screen
        getPreferenceScreen().removeAll();
        setPreferenceScreen(pm.createPreferenceScreen(context));

        // Remove all the (old) listeners, so that the GC can eat them
        notGarbageChangeListeners.clear();
        notGarbageClickListeners.clear();

        // Empty the memoization caches
        //servers.clear();
        //locations.clear();
        serversEditScreens.clear();


        /// DISCLAIMER /////////////////////////////////////////////////////////////////////////////

        Preference disclaimer = new Preference(context);
        disclaimer.setTitle("You found the secret backoffice !");
        disclaimer.setSummary("From here you can change the remote server credentials, which is useful for development !");
        disclaimer.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                app.toasty("With great power comes\ngreat responsibility.");
                return true;
            }
        });
        getPreferenceScreen().addPreference(disclaimer);


        /// THE SERVERS ////////////////////////////////////////////////////////////////////////////


        // Create the Server category
        PreferenceCategory cat = new PreferenceCategory(context);
        cat.setTitle(app.getString(R.string.settings_category_servers));
        cat.setEnabled(true); // test

        // The category MUST be added to the root node BEFORE we add screens to it
        // (something about injected deps like PreferenceManager, probably)
        // This code can possibly be improved by much, remember.
        getPreferenceScreen().addPreference(cat);

        // Select the server
        ListPreference serverChooser = new ListPreference(context);
        serverChooser.setTitle(app.getString(R.string.settings_server_choose));
        serverChooser.setKey("current_server_id");
        if (canSetIcons) serverChooser.setIcon(R.drawable.ic_expand_more_black_36dp);
        // The ServerChooserPreference fills the list with appropriate values
        // It would probably be best to have ServerChooserPreference extend ListPreference
        scp = new ServerChooserPreference(getActivity(), serverChooser, false);
        cat.addPreference(serverChooser);

        // List the server configurations
        List<Server> serversList = Server.listAll(Server.class);
        for (int i=0; i<serversList.size(); i++) {
            Server server = serversList.get(i);

            // Create a sub-screen for that server
            PreferenceScreen screen = pm.createPreferenceScreen(context);
            screen.setTitle(server.getName());
            if (canSetIcons) screen.setIcon(R.drawable.ic_edit_black_36dp);
            if (i == serversList.size()-1) screen.setKey("last_server_edit");

            // Store it so that we may navigate to it procedurally when adding a new one
            serversEditScreens.add(screen);

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
            name.setTitle(app.getString(R.string.settings_server_name));
            name.setKey(String.format("server_%d_name", server.getId()));
            name.setSummary(server.getName());
            name.setText(server.getName());
            name.setOnPreferenceChangeListener(nameListener);
            screen.addPreference(name);

            EditTextPreference uri = new EditTextPreference(context);
            uri.setTitle(app.getString(R.string.settings_server_address));
            uri.setKey(String.format("server_%d_uri", server.getId()));
            uri.setSummary(server.getUrl());
            uri.setText(server.getUrl());
            uri.setOnPreferenceChangeListener(urlListener);
            screen.addPreference(uri);

            EditTextPreference username = new EditTextPreference(context);
            username.setTitle(app.getString(R.string.settings_server_username));
            username.setKey(String.format("server_%d_username", server.getId()));
            username.setSummary(server.getUsername());
            username.setText(server.getUsername());
            username.setOnPreferenceChangeListener(usernameListener);
            screen.addPreference(username);

            EditTextPreference password = new EditTextPreference(context);
            password.setTitle(app.getString(R.string.settings_server_password));
            password.setKey(String.format("server_%d_password", server.getId()));
            password.setSummary(new String(new char[server.getPassword().length()])
                    .replace("\0", "*")); // hmmm. DRY this using a lib ?
            password.setText(server.getPassword());
            password.setOnPreferenceChangeListener(passwordListener);
            screen.addPreference(password);

            // Forget this server, but, not unlike love, we can never forget the first one
            if (i > 0) {
                Preference delServer = new Preference(context);
                delServer.setTitle(app.getString(R.string.settings_server_forget));
                if (canSetIcons) delServer.setIcon(R.drawable.ic_remove_circle_outline_black_36dp);
                delServer.setOnPreferenceClickListener(forgetListener);
                screen.addPreference(delServer);
            }

            // Test this server configuration
            Preference testServer = new Preference(context);
            testServer.setTitle(app.getString(R.string.settings_server_test));
            if (canSetIcons)
                testServer.setIcon(R.drawable.ic_signal_wifi_statusbar_not_connected_black_36dp);
            testServer.setOnPreferenceClickListener(new OnTestServerClickListener(this, server));
            screen.addPreference(testServer);

            cat.addPreference(screen);
        }

        // Add server button
        Preference addServer = new Preference(context);
        addServer.setTitle(app.getString(R.string.settings_server_add));
        if (canSetIcons) addServer.setIcon(R.drawable.ic_add_circle_outline_black_36dp);
        addServer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Server newServer = (new Server()).loadDummy();
                newServer.save();
                refreshView(true);
                return true;
            }
        });

        cat.addPreference(addServer);


        /// THE LOCATIONS //////////////////////////////////////////////////////////////////////////

//        // List the server configurations
//        Iterator<Location> locationsIterator = Location.findAll(Location.class);
//
//        // Create the Server category
//        PreferenceCategory locationsCategory = new PreferenceCategory(context);
//        locationsCategory.setTitle(app.getString(R.string.settings_category_locations));
//
//        // The category MUST be added to the root node BEFORE we add screens to it
//        // (something about injected deps like PreferenceManager, probably)
//        // This code can possibly be improved by much, remember.
//        getPreferenceScreen().addPreference(locationsCategory);
//
//        for (;locationsIterator.hasNext();) {
//            Location location = locationsIterator.next();
//
//            //locations.put(location.getId(), location);
//
//            // Create a sub-screen for that location
//
//            PreferenceScreen screen = pm.createPreferenceScreen(context);
//            screen.setTitle(location.getName());
//            if (canSetIcons) screen.setIcon(R.drawable.ic_edit_black_36dp);
//
//            // Our change and click listeners, that will update the database and the UI
//            OnPreferenceChangeListener nameListener   = new OnLocationNameChangeListener(location, screen);
//            OnPreferenceChangeListener postalListener = new OnLocationPostalChangeListener(location);
//            OnPreferenceClickListener  forgetListener = new OnForgetEntityClickListener(this, location);
//            // We hard-reference them so that the garbage collector does not destroy them
//            notGarbageChangeListeners.add(nameListener);
//            notGarbageChangeListeners.add(postalListener);
//            notGarbageClickListeners.add(forgetListener);
//
//            EditTextPreference name = new EditTextPreference(context);
//            name.setTitle(app.getString(R.string.settings_location_name));
//            name.setKey(String.format("location_%d_name", location.getId()));
//            name.setSummary(location.getName());
//            name.setText(location.getName());
//            name.setOnPreferenceChangeListener(nameListener);
//            screen.addPreference(name);
//
//            EditTextPreference postal = new EditTextPreference(context);
//            postal.setTitle(app.getString(R.string.settings_location_address));
//            postal.setKey(String.format("location_%d_postal", location.getId()));
//            postal.setSummary(location.getPostal());
//            postal.setText(location.getPostal());
//            postal.setOnPreferenceChangeListener(postalListener);
//            screen.addPreference(postal);
//
//            // Forget this location
//            Preference delLocation = new Preference(context);
//            delLocation.setTitle(app.getString(R.string.settings_location_forget));
//            if (canSetIcons) delLocation.setIcon(R.drawable.ic_remove_circle_outline_black_36dp);
//            delLocation.setOnPreferenceClickListener(forgetListener);
//            screen.addPreference(delLocation);
//
//            locationsCategory.addPreference(screen);
//        }
//
//        // Add a location
//        Preference addLocation = new Preference(context);
//        addLocation.setTitle(app.getString(R.string.settings_location_add));
//        if (canSetIcons) addLocation.setIcon(R.drawable.ic_add_circle_outline_black_36dp);
//        addLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                Location newLocation = (new Location()).loadDummy();
//                newLocation.save();
//                refreshView();
//                return true;
//            }
//        });
//
//        locationsCategory.addPreference(addLocation);

        // Open the appropriate preference screen
        // This is harder than it seems, and probably harder than it should be.
        // We cannot extend PreferenceScreen (it is declared final)
        // We cannot call showDialog(), because it is private (whyyyyy?)
        // We cannot use getDialog().show() because the dialog is not instantiated...
        //serversEditScreens.get(serversEditScreens.size()-1).getDialog().show();
        // We cannot call performClick() (but... but... why?)
        //serversEditScreens.get(serversEditScreens.size()-1).performClick()
        // Therefore, we're simulating a click
//        if (openLastServer) {
//            int pos = findPreference("last_server_edit").getOrder();
//            Log.i("G2P", "Position of last server :"+pos);
//
//            ListAdapter listAdapter = getPreferenceScreen().getRootAdapter();
////            EditTextPreference editPreference = (EditTextPreference) findPreference("set_password_preference");
//
//            Preference editPreference = findPreference("last_server_edit");
//
//            int itemsCount = listAdapter.getCount();
//            for (int itemNumber = 0; itemNumber < itemsCount; itemNumber++) {
//                if (listAdapter.getItem(itemNumber).equals(editPreference)) {
//                    getPreferenceScreen().onItemClick(null, null, itemNumber, 0);
//                    break;
//                }
//            }
//
//            //getPreferenceScreen().onItemClick(null, null, pos, 0);
//        }
    }


    // Hack to set the bg color, 'cause of android bug. Nothing we can do about the title bg color?
    // https://code.google.com/p/android/issues/detail?id=4611
    // What a hack ! -- This is to make sure the background of second-level subscreens
    // is of the appropriate color on API 10. Otherwise, we cannot see a thing.
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
    {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference != null)
            if (preference instanceof PreferenceScreen)
                if (((PreferenceScreen) preference).getDialog() != null) {
                    ((PreferenceScreen) preference).getDialog().getWindow().getDecorView()
                            .setBackgroundColor(
                                    getResources().getColor(R.color.background_material_dark)
                            );
                    // We also unset the title because it is useless AND its theme is buggy
                    // Nope, nothing works. :( And setting the Title to null does not remove the bar
                    //((PreferenceScreen) preference).getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                }
        return false;
    }
}