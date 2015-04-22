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
import org.give2peer.give2peer.entity.Location;

import java.util.List;

/**
 * This will list all the locations stored in our local database, plus a dynamic location provided
 * by the GPS.
 */
public class LocationChooserFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.location_chooser);
    }

    protected Preference.OnPreferenceChangeListener notGarbageListener;

    @Override
    public void onResume() {
        super.onResume();

        final Application app = (Application) getActivity().getApplication();

        // Grab the View we're going to edit
        final ListPreference chooser = (ListPreference) getPreferenceManager()
                .findPreference("current_location_id");


        // List the locations from the database
        final List<Location> locations = Location.listAll(Location.class);
        int locationsCount = locations.size();

        int numChoicesBef = 1; // GPS-detected location choice
        int numChoicesAft = 1; // "Add location" convenience choice

        CharSequence[] entries     = new CharSequence[locationsCount+numChoicesBef+numChoicesAft];
        CharSequence[] entryValues = new CharSequence[locationsCount+numChoicesBef+numChoicesAft];

        // Add the GPS-given location
        if (app.hasLocation()) {
            android.location.Location l = app.getLocation();
            entries[0] = String.format("From GPS: %.3f/%.3f", l.getLatitude(), l.getLongitude());
        } else {
            entries[0] = "From GPS: unknown";
        }
        entryValues[0] = "0";

        // Add the locations from the database as choices
        for (int i=0; i<locationsCount; i++) {
            Location location = locations.get(i);
            entries    [numChoicesBef+i] = location.getName();
            entryValues[numChoicesBef+i] = location.getId().toString();
        }

        // Add the convenience "Add location" choice
        entries    [locationsCount+numChoicesBef+numChoicesAft-1] = "Add a new location";
        entryValues[locationsCount+numChoicesBef+numChoicesAft-1] = "-1";

        chooser.setEntries(entries);
        chooser.setEntryValues(entryValues);

        // Pick a default value if none is set
        if (null == chooser.getValue()) {
            Log.d("G2P", "No location: setting first found location as current location.");
            chooser.setValueIndex(0);
        }
        if (Integer.valueOf(chooser.getValue()) >= locationsCount+numChoicesBef) {
            Log.d("G2P", "Wrong location: setting first found location as current location.");
            chooser.setValueIndex(0);
        }

        // Set the name of the location as summary
        int currentLocationId = Integer.valueOf(chooser.getValue());
        if (0 == currentLocationId) {
            chooser.setSummary("From GPS");
        } else {
            for (int i=0; i<locationsCount; i++) {
                Location location = locations.get(i);
                if (location.getId() == currentLocationId) {
                    chooser.setSummary(location.getName());
                    break;
                }
            }
        }

        // Make sure we tell the Application about the configuration change
        notGarbageListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Long id = Long.valueOf((String) newValue);

                if (0 == id) {         // We chose the GPS-detected location
                    chooser.setSummary("From GPS");
                } else if (-1 == id) { // We choose to add a new location
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    startActivity(intent);
                    return false;
                } else {               // We want a location
                    try {
                        Location location = Location.findById(Location.class, id);
                        // Update the summary of the chooser
                        chooser.setSummary(location.getName());
                    } catch (Exception e) {
                        app.toast("That location cannot be chosen.");
                        return false;
                    }
                }

                return true;
            }
        };
        chooser.setOnPreferenceChangeListener(notGarbageListener);
    }
}