package org.give2peer.give2peer;

import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;

/**
 * The application is a singleton instance that is shared through all of our Activities.
 * It is created automatically when the app starts.
 *
 * In the activity, grab it like this :
 * ```
 * Application app = (Application) getApplication();
 * ```
 *
 *
 */
public class Application extends android.app.Application
{
    private static Application singleton;

    protected Location location;

    String serverUrl = "http://g2p.give2peer.org";
    String username = "Goutte";
    String password = "Goutte";

    protected ItemRepository itemRepository;

    public Application getInstance() { return singleton; }

    @Override
    public void onCreate()
    {
        super.onCreate();
        singleton = this;

        itemRepository = new ItemRepository(serverUrl, username, password);
    }

    /**
     * This is a great hack !
     * See http://stackoverflow.com/a/27312494/265042
     *
     * @return whether internet is available or not.
     */
    public boolean canPing()
    {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
        }

        return false;
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



    public ItemRepository getItemRepository() { return itemRepository; }

    public Location getLocation() { return location; }

    public void setLocation(Location location) { this.location = location; }
}
