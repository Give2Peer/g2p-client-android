package org.give2peer.karma.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.util.Log;

import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.activity.SettingsActivity;
import org.give2peer.karma.entity.Location;

import java.util.List;

/**
 * This will list all the locations stored in our local database, plus a dynamic location provided
 * by the GPS.
 * It also provides a button to refresh the location provided by the GPS.
 */
public class LocationChooserFragment extends PreferenceFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.location_chooser);
    }

    protected Preference.OnPreferenceChangeListener notGarbageListener;

    @Override
    public void onResume()
    {
        super.onResume();
        refreshView();
    }

    public void refreshView()
    {
        if (null == getActivity()) return;

        // Some services we're going to need
        final Application app = (Application) getActivity().getApplication();
        final LocationManager lm = (LocationManager) getActivity()
                .getSystemService(Context.LOCATION_SERVICE);

        // Grab the Views we're going to edit
        final ListPreference chooser = (ListPreference) getPreferenceManager()
                .findPreference("current_location_id");
        final Preference detector = (Preference) getPreferenceManager()
                .findPreference("detect_location");


        // List the locations from the database
        final List<Location> locations = Location.listAll(Location.class);
        int locationsCount = locations.size();

        int numChoicesBef = 1; // GPS-detected location choice
        int numChoicesAft = 1; // "Add location" convenience choice

        CharSequence[] entries     = new CharSequence[locationsCount+numChoicesBef+numChoicesAft];
        CharSequence[] entryValues = new CharSequence[locationsCount+numChoicesBef+numChoicesAft];

        // Add the GPS-given location
        if (app.hasGeoLocation()) {
            entries[0] = String.format("From GPS %s", app.getPrettyDurationSinceLastLocatedDate());
        } else {
            entries[0] = "GPS location unknown";
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

        // Set the name of the currently chosen location as summary
        int currentLocationId = Integer.valueOf(chooser.getValue());
        if (0 == currentLocationId) {
            if (app.hasGeoLocation()) {
                chooser.setSummary(String.format("From GPS %s", app.getPrettyDurationSinceLastLocatedDate()));
            } else {
                chooser.setSummary("GPS location unknown");
            }
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
                    chooser.setSummary(String.format("From GPS %s", app.getPrettyDurationSinceLastLocatedDate()));
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
                        app.toast("That location cannot be chosen: "+e.getMessage());
                        return false;
                    }
                }

                return true;
            }
        };
        chooser.setOnPreferenceChangeListener(notGarbageListener);


        // Detect Location button
        detector.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Disable the button
                detector.setEnabled(false);
                detector.setIcon(R.drawable.ic_my_location_grey600_36dp); // use styles instead !

                if (! lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Move to the settings
                                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Dismiss the dialog
                                    dialog.dismiss();
                                }
                            });

                    final AlertDialog alert = builder.create();
                    alert.show();

                    // Enable the button
                    detector.setEnabled(true);
                    detector.setIcon(R.drawable.ic_my_location_black_36dp); // use styles -

                    return false;
                }

                /**
                 * To minimize battery usage, and because we don't need regular updates, here's a
                 * LocationListener that will stop listening by itself when the LocationManager
                 * finds a location.
                 *
                 * This is deprecated, see http://developer.android.com/training/location/index.html
                 */
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(android.location.Location newLocation) {
                        // Remove this listener
                        lm.removeUpdates(this);

                        // Enable the button
                        detector.setEnabled(true);
                        detector.setIcon(R.drawable.ic_my_location_black_36dp); // use styles -_-

                        // Select the GPS location in the location chooser
                        chooser.setValueIndex(0);

                        // Set the location application-wise
                        app.setGeoLocation(newLocation);

                        app.toast("Successfully updated current location.");

                        refreshView();
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(String provider) {}

                    @Override
                    public void onProviderDisabled(String provider) {}
                };
                // Fetch the location asynchronously using the above listener
                lm.requestLocationUpdates(
                    lm.getBestProvider(app.getLocationCriteria(), true),
                    1000,
                    0,
                    locationListener
                );

                return false;
            }
        });
    }

}