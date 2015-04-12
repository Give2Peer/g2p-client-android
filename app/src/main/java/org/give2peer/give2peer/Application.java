package org.give2peer.give2peer;

import android.location.Location;
import android.util.Log;

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

    @Override
    public void onCreate()
    {
        Log.i("Application", "onCreate"); // make sure our singleton works todo remove
        super.onCreate();
        singleton = this;

        itemRepository = new ItemRepository(serverUrl, username, password);
    }

    public Application getInstance() { return singleton; }

    public ItemRepository getItemRepository() { return itemRepository; }

    public Location getLocation() { return location; }

    public void setLocation(Location location) { this.location = location; }
}
