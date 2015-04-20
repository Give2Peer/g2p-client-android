package org.give2peer.give2peer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.orm.SugarApp;

import org.give2peer.give2peer.entity.ServerConfiguration;

import java.io.File;
import java.io.FileInputStream;
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

    protected Location location;

    protected ServerConfiguration currentServer;

    protected RestService restService;

    public Application getInstance() { return singleton; }

    // FLOW ////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate()
    {
        super.onCreate();
        singleton = this;

        // Load the Location from preferences
        loadLocation();

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

    public void setServerConfiguration(ServerConfiguration config)
    {
        currentServer = config;
        restService = new RestService(currentServer);
    }

    public ServerConfiguration guessServerConfiguration()
    {
        ServerConfiguration serverConfiguration = null;

        // Grab the locally-stored servers in our yummy SQLite database
        List<ServerConfiguration> servers = ServerConfiguration.listAll(ServerConfiguration.class);

        // Add a default server to the database if there are no servers at all
        if (0 == servers.size()) {
            ServerConfiguration defaultServer = new ServerConfiguration();
            defaultServer.loadDefaults().save();
            servers.add(defaultServer);
        }

        // Yes, our server chooser saves the ids as strings. If you know how to save ints...
        String currentServerIdString = getPrefs().getString("current_server_id", null);

        // Loop through our server configs to find the one with our id, it's cheaper than findById()
        if (null != currentServerIdString) {
            int currentServerId = Integer.valueOf(currentServerIdString);
            for (int i=0; i<servers.size(); i++) {
                ServerConfiguration config = servers.get(i);
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

    // LOCATION ////////////////////////////////////////////////////////////////////////////////////

    protected void saveLocation()
    {
        if (null == location) return;
        SharedPreferences sharedPref = getPrefs();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("latitude",  (float) location.getLatitude());
        editor.putFloat("longitude", (float) location.getLongitude());
        editor.apply();
    }

    protected void loadLocation()
    {
        SharedPreferences sharedPref = getPrefs();

        double lat = (double) sharedPref.getFloat("latitude",  666);
        double lng = (double) sharedPref.getFloat("longitude", 666);
        if (lat == 666 || lng == 666) return;

        location = new Location("g2p");
        location.setLatitude(lat);
        location.setLongitude(lng);
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
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
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




    // LOCATION ////////////////////////////////////////////////////////////////////////////////////

    public boolean hasLocation() { return null != location; }

    public Location getLocation() { return location; }

    public void setLocation(Location location)
    {
        this.location = location;
        saveLocation();
    }


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
