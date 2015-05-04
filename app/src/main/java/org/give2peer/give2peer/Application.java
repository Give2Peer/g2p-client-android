package org.give2peer.give2peer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.orm.SugarApp;

import org.give2peer.give2peer.entity.Location;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.exception.GeocodingException;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * The application is a singleton instance that is shared through all of our Activities.
 * It is created automatically when the app starts.
 * I'm not sure we're using the fact that it is a singleton right now, though.
 *
 * In the activity, grab it like this :
 * ```
 * Application app = (Application) getApplication();
 * ```
 */
public class Application extends SugarApp
{
    private static Application singleton;

    protected android.location.Location location;

    protected Server currentServer;

    protected RestService restService;

    public Application getInstance() { return singleton; }

    // FLOW ////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate()
    {
        super.onCreate();
        singleton = this;

        // Load the Location from preferences
        loadGeoLocation();

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

    // SERVERS /////////////////////////////////////////////////////////////////////////////////////

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

        // Add a default server to the database if there are no servers at all
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

    // GEO LOCATION ////////////////////////////////////////////////////////////////////////////////

    protected void saveGeoLocation()
    {
        if (null == location) return;
        SharedPreferences sharedPref = getPrefs();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("latitude",  (float) location.getLatitude());
        editor.putFloat("longitude", (float) location.getLongitude());
        editor.putLong("last_located_date", (new Date()).getTime());
        editor.apply();
    }

    protected void loadGeoLocation()
    {
        SharedPreferences sharedPref = getPrefs();

        double lat = (double) sharedPref.getFloat("latitude",  666);
        double lng = (double) sharedPref.getFloat("longitude", 999);
        if (lat == 666 || lng == 999) return; // ahem...

        location = new android.location.Location("g2p");
        location.setLatitude(lat);
        location.setLongitude(lng);
    }

    public boolean hasGeoLocation() { return null != location; }

    public android.location.Location getGeoLocation() { return location; }

    public void setGeoLocation(android.location.Location location)
    {
        this.location = location;
        saveGeoLocation();
    }

    public Date getLastLocatedDate()
    {
        SharedPreferences sharedPref = getPrefs();
        long time = sharedPref.getLong("last_located_date", 0);

        if (0 == time) return null;
        else           return new Date(time);
    }

    public String getPrettyDurationSinceLastLocatedDate()
    {
        Date then = getLastLocatedDate();
        if (null == then) return "";

        PrettyTime p = new PrettyTime();

        return p.format(then);
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
                    R.string.error_geocoding_failed,
                    location.getName(),
                    location.getPostal()
            ));
        }
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

    // UTILS ///////////////////////////////////////////////////////////////////////////////////////

    public SharedPreferences getPrefs()
    {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
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


    /**
     * Corrects the orientation of a Bitmap. Orientation, depending of the device,
     * is not correctly set in the EXIF data of the taken image when it is saved
     * into disk.
     *
     * Explanation:
     * 	Camera orientation is not working ok (as is when capturing an image) because
     *  OEMs do not adhere to the standard. So, each company does this following their
     *  own way.
     *
     * @param path	path to the file
     * @return
     */
    public Bitmap getBitmapFromPath(String path)
    {
        Bitmap bitmap = null;

        try {
            File f = new File(path);
            ExifInterface exif = new ExifInterface(f.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int rotate = 0;
            switch(orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate += 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate += 90;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate += 90;
            }

            Matrix mat = new Matrix();
            mat.postRotate(rotate);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;

            Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
            bitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);

        } catch (OutOfMemoryError e) {
            Log.e("G2P", "getBitmapFromPath() [OutOfMemory!]: " + e.getMessage(), e);
        } catch (Throwable e) {
            Log.e("G2P","getBitmapFromPath(): " + e.getMessage(), e);
        }

        return bitmap;
    }


    public RestService getRestService() { return restService; }


    // STATS ///////////////////////////////////////////////////////////////////////////////////////

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

    // HELPERS /////////////////////////////////////////////////////////////////////////////////////

    public void toast(String message) { toast(message, Toast.LENGTH_SHORT); }
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
