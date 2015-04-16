package org.give2peer.give2peer;

import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;

import java.lang.reflect.Type;

//import retrofit.RequestInterceptor;
//import retrofit.RestAdapter;
//import retrofit.converter.ConversionException;
//import retrofit.converter.Converter;
//import retrofit.mime.TypedInput;
//import retrofit.mime.TypedOutput;

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

//    protected RestService restService;

    protected ItemRepository itemRepository;

    public Application getInstance() { return singleton; }

    @Override
    public void onCreate()
    {
        super.onCreate();
        singleton = this;

        itemRepository = new ItemRepository(serverUrl, username, password);

//        restService = buildRestService(serverUrl, username, password);

    }

//    public RestService buildRestService(String serverUrl, String username, String password)
//    {
//        // fixme: WTF? `:` character becomes forbidden in the username ?
//        final String credentials = username + ":" + password;
//
//        RestAdapter restAdapter = new RestAdapter.Builder()
//            .setEndpoint(serverUrl)
//            .setLogLevel(RestAdapter.LogLevel.FULL)
//            .setConverter(new Converter() {
//                @Override
//                public Object fromBody(TypedInput body, Type type) throws ConversionException {
//                    return body;
//                }
//                @Override
//                public TypedOutput toBody(Object object) {
//                    return null;
//                }
//            })
//            .setRequestInterceptor(new RequestInterceptor() {
//                @Override
//                public void intercept(RequestFacade request) {
//                    String base64 = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
//                    request.addHeader("Accept", "application/json");
//                    request.addHeader("Authorization", "Basic " + base64);
//                }
//            })
//            .build();
//
//        restService = restAdapter.create(RestService.class);
//
//        return restService;
//    }

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

    /**
     * @return whether we have camera support or not.
     */
    public boolean hasCameraSupport()
    {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }


//    public RestService getRestService() { return restService; }

    public ItemRepository getItemRepository() { return itemRepository; }

    public Location getLocation() { return location; }

    public void setLocation(Location location) { this.location = location; }
}
