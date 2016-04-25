package org.give2peer.karma;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.orm.SugarApp;

import net.danlew.android.joda.JodaTimeAndroid;

import org.give2peer.karma.activity.LoginActivity_;
import org.give2peer.karma.entity.Location;
import org.give2peer.karma.entity.Server;
import org.give2peer.karma.exception.GeocodingException;
import org.give2peer.karma.listener.GoogleApiClientListener;
import org.give2peer.karma.service.RestService;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * The application is a singleton instance that is shared through all of our Activities.
 * It is created automatically when the app starts.
 * I'm not sure we're using the fact that it is a singleton right now, though. Maybe the OS does ?
 *
 * Anyhow, it extends SugarDb, but that's not mandatory, we could just as well extend something else
 * if need be. This is just ... well... even more sugar.
 *
 * In an activity, grab the Application like this :
 * ```
 * Application app = (Application) getApplication();
 * ```
 * or, with Android Annotations, ever-so-simply define and annotate the `app` property :
 * ```
 * * @App
 * * Application app;
 * ```
 */
public class Application extends SugarApp
{
    public static String REPORT_BUG_URL = "https://github.com/Give2Peer/g2p-client-android/issues";
    public static int THUMB_MAX_WIDTH  = 512;
    public static int THUMB_MAX_HEIGHT = 512;

    private static Application singleton;

    protected android.location.Location location;

    protected Server currentServer;

    protected RestService restService;

    // Unsure if this is even used somewhere...
    public Application getInstance() { return singleton; }

    // FLOW ////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate()
    {
        super.onCreate();

        singleton = this;

        // A debug message helping me understand when the Application is created
        Log.d("G2P", "G2P Application onCreate");

        // Otherwise, get get a `Resource not found: "org/joda/time/tz/data/ZoneInfoMap"`.
        JodaTimeAndroid.init(this);

        // Load the Location from preferences
        //loadGeoLocation();

        // Increment the tally of launches and fire appropriate methods, like `onFirstTime()`
        incrementLaunchesTally();

        // Figure out the configured (or default) server configuration, and load it.
        // This also loads the REST service with the found configuration.
        setServerConfiguration(guessServerConfiguration());
    }

    /**
     * Mah, we'll use that again later, tutorials and such.
     */
    protected void onFirstTime()
    {
        // nothing is cool
    }

    // USER ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * FIXME: this is horseshit
     * @return whether the current user is registered or not
     */
    public boolean isUserRegistered()
    {
        Server server = getCurrentServer();
        return null != server && !server.getUsername().equals(Server.DEFAULT_USERNAME);
    }


    public void requireAuthentication(final Activity activity)
    {
        requireAuthentication(activity, null);
    }

    public void requireAuthentication(final Activity activity, @Nullable String message)
    {
        if (!isUserRegistered()) {
            requestLogin(activity, message);
        }
    }

    public void requestLogin(final Activity activity, @Nullable String message)
    {
        if (null == message) {
            message = "To continue, you need to be logged in. Do so now?";
        }
        new AlertDialog.Builder(activity)
            .setTitle("Authentication needed")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(
                    android.R.string.yes, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // Go to the login activity
                            Intent intent = new Intent(activity, LoginActivity_.class);
                            activity.startActivity(intent);
                        }
                    }
            )
            .setNegativeButton(
                    android.R.string.no, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // GTFO, then
                            activity.finish();
                        }
                    }
            )
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    // SERVERS /////////////////////////////////////////////////////////////////////////////////////

    public Server getCurrentServer() { return currentServer; }

    public void setServerConfiguration(Server config)
    {
        currentServer = config;
        restService = new RestService(currentServer);
    }

    public Server guessServerConfiguration()
    {
        Server serverConfiguration = null;

        // Grab the locally-stored servers in our yummy SQLite database
        List<Server> servers = Server.listAll(Server.class);

        // Add the G2P default server to that database if there are no servers at all
        if (0 == servers.size()) {
            Server defaultServer = new Server();
            defaultServer.loadDefaults().save();
            servers.add(defaultServer);
        }

        // Yes, our server chooser saves the ids as strings. If you know how to save ints...
        String currentServerIdString = getPrefs().getString("current_server_id", null);

        // Loop through our server configs to find the one with our id, it's cheaper than findById()
        if (null != currentServerIdString) {
            int currentServerId = Integer.valueOf(currentServerIdString);
            for (int i=0; i<servers.size(); i++) {
                Server config = servers.get(i);
                if (config.getId() == currentServerId) {
                    serverConfiguration = config;
                    break;
                }
            }
        }

        // It's either the first time and we have not picked a server yet, or we deleted it.
        if (null == serverConfiguration) {
            serverConfiguration = servers.get(0); // the first one, the only one, the default G2P
        }

        return serverConfiguration;
    }

    // PROPER LOCATION /////////////////////////////////////////////////////////////////////////////

    public synchronized GoogleApiClient buildGoogleLocator(
            Context context,
            GoogleApiClientListener listener
    ) {
        return new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(listener)
                .addOnConnectionFailedListener(listener)
                .addApi(LocationServices.API)
                .build();
    }


    // GEO LOCATION ////////////////////////////////////////////////////////////////////////////////

    protected static String PREF_GEO_LAT = "geo_latitude";
    protected static String PREF_GEO_LNG = "geo_longitude";
    // Time in milliseconds since the last geo location update. Defaults to EPOCH.
    protected static String PREF_GEO_TIME = "geo_last_located";
    // Zero and negative values can be legit lat/lng, and `null` is not accepted,
    // therefore this absurdly big number means no lat or lng.
    protected static float  PREF_GEO_NONE = 666999;

    protected void saveGeoLocation()
    {
        if (null == location) return;
        SharedPreferences sharedPref = getPrefs();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(PREF_GEO_LAT, (float) location.getLatitude());
        editor.putFloat(PREF_GEO_LNG, (float) location.getLongitude());
        editor.putLong(PREF_GEO_TIME, (new Date()).getTime());
        editor.apply();
    }

    protected void loadGeoLocation()
    {
        SharedPreferences sharedPref = getPrefs();

        double lat = (double) sharedPref.getFloat(PREF_GEO_LAT, PREF_GEO_NONE);
        double lng = (double) sharedPref.getFloat(PREF_GEO_LNG, PREF_GEO_NONE);
        if (lat == PREF_GEO_NONE || lng == PREF_GEO_NONE) return;

        location = new android.location.Location("g2p");
        location.setLatitude(lat);
        location.setLongitude(lng);
    }

    public boolean hasGeoLocation() { return null != location; }

    public android.location.Location getGeoLocation() { return location; }

    public void setGeoLocation(android.location.Location location)
    {
        this.location = location;
        //saveGeoLocation();
    }

    public LatLng getGeoLocationLatLng()
    {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public Date getLastLocatedDate()
    {
        SharedPreferences sharedPref = getPrefs();
        long time = sharedPref.getLong(PREF_GEO_TIME, 0);

        if (0 == time) return null;
        else           return new Date(time);
    }

    public String getPrettyDurationSinceLastLocatedDate()
    {
        Date then = getLastLocatedDate();
        if (null == then) return "";

        return (new PrettyTime()).format(then);
    }

    // LOCATION ////////////////////////////////////////////////////////////////////////////////////

    public boolean hasLocation()
    {
        return null != getLocation();
    }

    public Location getLocation()
    {
        Long id = Long.valueOf(getPrefs().getString("current_location_id", "0"));
        if (0 == id) {
            if (hasGeoLocation()) {
                android.location.Location geo = getGeoLocation();
                Location locFromGeo = new Location();
                locFromGeo.setLatitude(geo.getLatitude());
                locFromGeo.setLongitude(geo.getLongitude());
                return locFromGeo;
            } else {
                return null;
            }
        } else {
            return Location.findById(Location.class, id);
        }
    }

    /**
     * This MUST be called in async threads only, as it is a long operation.
     */
    public void geocodeLocationIfNeeded(Location location)
    throws IOException, GeocodingException
    {
        // No need to geolocate locations from GPS
        if (location.hasLatLng() && location.getPostal().isEmpty()) return;

        Geocoder geocoder = new Geocoder(getApplicationContext());
        List<Address> addresses = geocoder.getFromLocationName(location.getPostal(), 1);

        if (addresses == null || addresses.size() == 0) {
            throw new GeocodingException(getString(
                    R.string.error_geocoding_failed, location.getName(), location.getPostal()
            ));
        }
        // We grab the first one, usually the best one, and discard the others.
        Address address = addresses.get(0);
        location.setLatitude(address.getLatitude());
        location.setLongitude(address.getLongitude());
        location.save();
    }

    // CONFIGURATION ///////////////////////////////////////////////////////////////////////////////

    /**
     * @return a criteria tailored to our needs.
     */
    public Criteria getLocationCriteria()
    {
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        return criteria;
    }

    /**
     * @return whether this device supports icons the the preferences.
     */
    public boolean canSetIcons()
    {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
    }

    /**
     * Use our ACCESS_NETWORK_STATE to guess if Internet is available.
     *
     * People say that this may return false negatives in some edge cases.
     * It's best to also provide a way for the user to try to connect anyway, which is why we also
     * have a `/ping` API on the server, that simply answers with "pong" and a 200 HTTP status.
     * Note : that ping API requires authentication to the server, whereas this method does not.
     *
     * @return whether internet is available or not.
     */
    public boolean isOnline()
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * @return whether we have camera support or not.
     */
    public boolean hasCameraSupport()
    {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    // UTILS ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Preferences are Android's application configuration registry, which can be deleted by the
     * user at any time.
     */
    public SharedPreferences getPrefs()
    {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }


    // SERVICES ////////////////////////////////////////////////////////////////////////////////////

    /**
     * The REST service handles all the HTTP nitty-gritty, and provides named methods for each API.
     */
    public RestService getRestService() { return restService; }


    // STATS ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Called on creation of this application, every time.
     * This will be useful for tutorials and other events in the future.
     */
    protected void incrementLaunchesTally()
    {
        int count = getPrefs().getInt("launches_tally", 0) + 1;
        switch (count)
        {
            case 1:
                onFirstTime();
                break;
            default:
        }
        getPrefs().edit().putInt("launches_tally", count).apply();
    }

    // UI //////////////////////////////////////////////////////////////////////////////////////////

    public void toast(String message) { toast(message, Toast.LENGTH_SHORT); }
    public void toasty(String message) { toast(message, Toast.LENGTH_LONG); } // toastLong ?
    public void toast(String message, int duration)
    {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    /**
     * This is a great hack !
     * See http://stackoverflow.com/a/27312494/265042
     *
     * @return whether internet is available or not.
     */
//    public boolean canPing()
//    {
//        Runtime runtime = Runtime.getRuntime();
//        try {
//            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
//            int     exitValue = ipProcess.waitFor();
//            return (exitValue == 0);
//        } catch (IOException|InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        return false;
//    }
}
