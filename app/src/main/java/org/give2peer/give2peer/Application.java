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
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;


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
public class Application extends android.app.Application
{
    private static Application singleton;

    protected Location location;

    private static String PREFERENCES_NAME = "org.give2peer.preferences";

    String serverUrl = "http://g2p.give2peer.org";
    String username = "Goutte";
    String password = "Goutte";

    protected RestService restService;

    public Application getInstance() { return singleton; }

    // FLOW ////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate()
    {
        super.onCreate();
        singleton = this;

        loadLocation();

        restService = new RestService(serverUrl, username, password);
    }

    // PREFERENCES /////////////////////////////////////////////////////////////////////////////////

    protected void saveLocation()
    {
        if (null == location) return;
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("latitude",  (float) location.getLatitude());
        editor.putFloat("longitude", (float) location.getLongitude());
        editor.apply();
    }

    protected void loadLocation()
    {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE
        );

        double lat = (double) sharedPref.getFloat("latitude",  666);
        double lng = (double) sharedPref.getFloat("longitude", 666);
        if (lat == 666 || lng == 666) return;

        location = new Location("g2p");
        location.setLatitude(lat);
        location.setLongitude(lng);
    }

    // UTILS ///////////////////////////////////////////////////////////////////////////////////////

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
    public Bitmap getBitmapFromPath(String path) {
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

    public boolean hasLocation() { return null != location; }

    public Location getLocation() { return location; }

    public void setLocation(Location location) {
        this.location = location;
        saveLocation();
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
