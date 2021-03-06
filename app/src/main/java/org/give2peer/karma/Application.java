package org.give2peer.karma;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.orm.SugarApp;
import com.shamanland.fab.FloatingActionButton;

import net.danlew.android.joda.JodaTimeAndroid;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.rest.spring.api.RestErrorHandler;
import org.give2peer.karma.activity.AboutActivity_;
import org.give2peer.karma.activity.LoginActivity_;
import org.give2peer.karma.activity.MapItemsActivity_;
import org.give2peer.karma.activity.NewItemActivity_;
import org.give2peer.karma.activity.ProfileActivity_;
import org.give2peer.karma.activity.ServerConfigActivity;
import org.give2peer.karma.activity.SettingsActivity_;
import org.give2peer.karma.activity.ViewItemActivity_;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.entity.Location;
import org.give2peer.karma.entity.Server;
import org.give2peer.karma.event.AuthenticationEvent;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.exception.GeocodingException;
import org.give2peer.karma.exception.NoInternetException;
import org.give2peer.karma.listener.GoogleApiClientListener;
import org.give2peer.karma.response.RegistrationResponse;
import org.give2peer.karma.response.Stats;
import org.give2peer.karma.service.RestClient;
import org.give2peer.karma.service.RestExceptionHandler;
import org.give2peer.karma.service.RestService;
import org.greenrobot.eventbus.EventBus;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.core.NestedRuntimeException;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pl.polidea.webimageview.WebImageView;


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
@EApplication
public class Application extends SugarApp implements RestErrorHandler {
    public static String REPORT_BUG_URL = "https://github.com/Give2Peer/g2p-client-android/issues";
    public static int THUMB_MAX_WIDTH  = 512;
    public static int THUMB_MAX_HEIGHT = 512;

    protected android.location.Location location;

    protected Server currentServer;

    protected RestService restService;

    protected boolean isFirstTime = false;

    // FLOW ////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate()
    {
        super.onCreate();

        // A debug message helping me understand when the Application is created
        Log.d("G2P", "Application onCreate");

        // Otherwise, we get a `Resource not found: "org/joda/time/tz/data/ZoneInfoMap"`.
        JodaTimeAndroid.init(this);

        // Load the Location from preferences
        //loadGeoLocation();

        // Increment the tally of launches and fire appropriate methods, like `onFirstTime()`
        incrementLaunchesTally();

        // Figure out the configured (or default) server configuration, and load it.
        // This also loads the REST service with the found configuration.
        setServerConfiguration(guessServerConfiguration());

        // We don't even need this I guess ?
        //PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setupRestClient();
    }

    /**
     * Mah, we'll use that again later, tutorials and such.
     */
    protected void onFirstTime()
    {
        isFirstTime = true;
    }

    /**
     * Note: not really reliable for pre-registration.
     * Note: may be reset by the user, very probably.
     * => probably only good for tutorials.
     *
     * @return whether or not it is the very first time this application is ran.
     */
    public boolean isFirstTime()
    {
        return isFirstTime;
    }

    // I18N ////////////////////////////////////////////////////////////////////////////////////////

    public static Locale getLocale() {
        return Locale.getDefault();
    }

    // USER ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Ripple effect : creates a (incomplete) server in the database if none exists, and then in
     *                 that case obviously returns false.
     * @return whether the current user is (pre)registered or not at all. (no username or password)
     */
    public boolean isUserRegistered() {
        Server server = getCurrentServer();
        return null != server && server.isComplete();
    }

    /**
     * @return the username of the current user, or an empty string.
     */
    public String getUsername() {
        Server server = getCurrentServer();
        if (null != server) {
            return server.getUsername();
        } else {
            return "";
        }
    }

    /**
     * @return the username of the current user, or an empty string.
     */
    public String getPassword() {
        Server server = getCurrentServer();
        if (null != server) {
            return server.getPassword();
        } else {
            return "";
        }
    }

//    @Background
//    protected void doRegistration() {
//
//        getRestClient().preregister();
//    }

    public void requireAuthentication(final Activity activity) {
        if (isUserRegistered()) {
            EventBus.getDefault().post(new AuthenticationEvent(true));
        } else {

            final Server config = getCurrentServer();

            new AsyncTask<Void, Void, RegistrationResponse>() {
                private final ProgressDialog dialog = new ProgressDialog(activity);
                Exception exception;

                @Override
                protected RegistrationResponse doInBackground(Void... nope) {
                    RegistrationResponse response = getRestClient().preregister();

                    try {
                        config.setUsername(response.getUser().getUsername());
                        if ( ! response.getPassword().isEmpty()) {
                            config.setPassword(response.getPassword());
                        }
                        config.save();
                        setServerConfiguration(config);
                    } catch (Exception e) {
                        exception = e;
                    }

                    return response;
                }

                protected void onPreExecute() {
                    this.dialog.setCancelable(false);
                    this.dialog.setMessage(getString(R.string.dialog_preregistration));
                    this.dialog.show();
                }

                protected void onPostExecute(final RegistrationResponse response) {
                    if (this.dialog.isShowing()) {
                        this.dialog.dismiss();
                    }
                    if (null != response) {
                        Log.d("G2P", "Pre-registered successfully.");
                        toasty(String.format(
                                getString(R.string.toast_preregistration_welcome),
                                response.getUser().getPrettyUsername()
                        ));
                        EventBus.getDefault().post(new AuthenticationEvent(true));
                    } else if (null != exception) {
                        Log.d("G2P", "Failed to pre-register.");
                        exception.printStackTrace();
                        EventBus.getDefault().post(new AuthenticationEvent(false));
                        throw new CriticalException(exception); // while in beta...
                    } else {
                        // An error happened but the error handler probably handled it.
                        EventBus.getDefault().post(new AuthenticationEvent(false));
                    }
                }
            }.execute();

        }
    }


    // REST CLIENT /////////////////////////////////////////////////////////////////////////////////

    @org.androidannotations.rest.spring.annotations.RestService
    RestClient restClient;

    public RestClient getRestClient() {
        return restClient;
    }

    void setupRestClient() {
        restClient.setRootUrl(getCurrentServer().getUrl());
        restClient.setRestErrorHandler(this);
    }

    @Override
    @UiThread
    public void onRestClientExceptionThrown(NestedRuntimeException e) {
        new RestExceptionHandler(this, this.getApplicationContext()).handleException(e);
    }


    // ACTIONS /////////////////////////////////////////////////////////////////////////////////////

    public void launchBugReport(Activity activity) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(Application.REPORT_BUG_URL));
        activity.startActivity(i);
    }

    public void launchNewItem(Activity activity) {
        Intent intent = new Intent(this, NewItemActivity_.class);
        activity.startActivity(intent);
    }

    public void launchViewItem(Activity activity, Item item) {
        Intent intent = new Intent(this, ViewItemActivity_.class);
        intent.putExtra("item", item);
        activity.startActivity(intent);
    }

    public void launchLogin(Activity activity) {
        Intent intent = new Intent(this, LoginActivity_.class);
        activity.startActivity(intent);
    }

    public void launchMap(Activity activity)
    {
        launchActivity(activity, MapItemsActivity_.class);
    }

    public void launchProfile(Activity activity) {
        launchActivity(activity, ProfileActivity_.class);
    }

    public void launchSettings(Activity activity) {
        launchActivity(activity, SettingsActivity_.class);
    }

    public void launchServerConfig(Activity activity) {
        launchActivity(activity, ServerConfigActivity.class);
    }

    public void launchAbout(Activity activity)
    {
        launchActivity(activity, AboutActivity_.class);
    }

    /**
     * Launch activity described by its `activityClass`, from provided `activity`.
     * Make sure we don't re-create a new activity if we already have one running.
     *
     * @param activity Our current activity, mostly used as Context
     * @param activityClass The activity we want to launch.
     */
    public void launchActivity(Activity activity, Class<?> activityClass ) {
        Intent intent = new Intent(activity, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }

    /**
     * Open the provided url in the user's preferred web browser.
     */
    public void openBrowser(final Activity activity, String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            toasty(getString(R.string.toast_no_browser_available));
            e.printStackTrace();
        }
    }


    // SERVERS /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Should never fail.
     * Ideally, run this in an async task as there may be SQL requests made.
     * @return the currently used Server configuration
     */
    public Server getCurrentServer() {
        if (null == currentServer) {
            currentServer = guessServerConfiguration();
        }
        return currentServer;
    }

    public void setServerConfiguration(Server config) {
        currentServer = config;
        restService = new RestService(currentServer);
    }

    public boolean hasServerConfiguration() {
        // Grab the locally-stored servers in our yummy SQLite database
        List<Server> servers = Server.listAll(Server.class);

        return 0 < servers.size();
    }

    /**
     * We're checking the presence of a username and password.
     * If either is empty, the server configuration is not complete !
     *
     * @param config the server configuration to check
     * @return whether the server configuration is complete
     */
    public boolean isServerConfigurationComplete(Server config)
    {
        return config.isComplete();
    }


    public Server guessServerConfiguration() {
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

//    public void requestLogin(final Activity activity, @Nullable String message)
//    {
//        if (null == message) {
//            message = "To continue, you need to be logged in. Do so now?";
//        }
//        new AlertDialog.Builder(activity)
//            .setTitle("Authentication needed")
//            .setMessage(message)
//            .setCancelable(false)
//            .setPositiveButton(
//                    android.R.string.yes, new DialogInterface.OnClickListener()
//                    {
//                        public void onClick(DialogInterface dialog, int which)
//                        {
//                            // Go to the login activity
//                            Intent intent = new Intent(activity, LoginActivity_.class);
//                            activity.startActivity(intent);
//                        }
//                    }
//            )
//            .setNegativeButton(
//                    android.R.string.no, new DialogInterface.OnClickListener()
//                    {
//                        public void onClick(DialogInterface dialog, int which)
//                        {
//                            // GTFO, then
//                            activity.finish();
//                        }
//                    }
//            )
//            .setIcon(android.R.drawable.ic_dialog_alert)
//            .show();
//    }


    // STALENESS MARKERS ///////////////////////////////////////////////////////////////////////////

    protected Map<String, Boolean> stalenessMarkers = new HashMap<>();

    public boolean isStale(String what) {
        if (stalenessMarkers.containsKey(what)) {
            return stalenessMarkers.get(what);
        } else {
            return false;
        }
    }

    public void setStale(String what) {
        stalenessMarkers.put(what, true);
    }

    public void setFresh(String what) {
        stalenessMarkers.remove(what);
    }


    // ONBOARDING //////////////////////////////////////////////////////////////////////////////////

    public boolean isUserOnBoard() {
        return getPrefs().getBoolean("is_on_board", false);
    }

    public void isUserOnBoard(boolean onBoard) {
        getPrefs().edit().putBoolean("is_on_board", onBoard).apply();
    }


    // PROPER LOCATION /////////////////////////////////////////////////////////////////////////////

    /**
     * Code smell here...
     * Either move all of location logic out of LocatorActivity or move this to LocatorActivity ?
     */
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

    public boolean hasLocation() {
        return null != getLocation();
    }

    public Location getLocation() {
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

    /**
     * Converts dpi to pixels, because Android's API is stupid. See `PopupWindow`.
     * Seriously, Google ! I suck, but you shouldn't !
     */
    public int dpi2pix(int dpi)
    {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,  dpi,
                getResources().getDisplayMetrics()
        );
    }


    // SERVICES ////////////////////////////////////////////////////////////////////////////////////

    /**
     * The REST service handles all the HTTP nitty-gritty, and provides named methods for each API.
     */
    @Deprecated
    public RestService getOldRestService() { return restService; }


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

    Toast toast;

    public void toast(int stringResId)  { toast(getString(stringResId), Toast.LENGTH_SHORT); }
    public void toasty(int stringResId) { toast(getString(stringResId), Toast.LENGTH_LONG);  }
    public void toast(String message)   { toast(message, Toast.LENGTH_SHORT);                }
    public void toasty(String message)  { toast(message, Toast.LENGTH_LONG);                 }

    public void toast(String message, int duration) {
        Context context = getApplicationContext();
        toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    Snackbar snack;

    /**
     * Snackbars are better suited for errors messages than toasts.
     * Falls back on a toast if the currently focused view cannot be found for any reason.
     */
    public void snack(Activity activity, String msg)
    {
        View view = activity.getCurrentFocus();
        if (null == view) {
            Log.e("G2P", "activity.getCurrentFocus() is null !");
            toast(msg);
        } else {
            snack = Snackbar.make(activity.getCurrentFocus(), msg, Snackbar.LENGTH_INDEFINITE);
            snack.show();
        }
    }

    public void showItemPopup(Activity activity, final Item item)
    {
        final PopupWindow pw;

        try {
            // We need to get the instance of the LayoutInflater, use the context of this activity
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // Inflate the view from a predefined XML layout
            View layout = inflater.inflate(
                    R.layout.popup_item,
                    (ViewGroup) activity.findViewById(R.id.popupItemRoot)
            );
            // Create a 300px width and 485px height PopupWindow
            // It's BAD to set the dimensions like that ! Wow ! No !
            // pw = new PopupWindow(layout); // but ... nope,
            int widthDpi = 200;
            int heightDpi = 324; // ~= width * 1.618

            Resources r = getResources();
            int widthPix = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDpi, r.getDisplayMetrics());
            int heightPix = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDpi, r.getDisplayMetrics());

            Log.d("G2P", String.format("Popup %dx%d", widthPix, heightPix));

            pw = new PopupWindow(layout, widthPix, heightPix, true);
            pw.setFocusable(true);
            // pw.setAnimationStyle(R.anim.abc_popup_enter); // good try, does nothing

            // We have a transparent background by default ?
            // It's fine, we set a background to the layouts
//            Drawable d = new ColorDrawable(Color.DKGRAY);
//            d.setAlpha(222);
//            pw.setBackgroundDrawable(d);

            // Hook clicking anywhere to the popup window dismissal
            RelativeLayout root = (RelativeLayout) layout.findViewById(R.id.popupItemRoot);
            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pw.dismiss();
                }
            });

            // Show the thank you button
            // todo: but only if you're level 2 ?
            FloatingActionButton tyb = (FloatingActionButton) layout.findViewById(R.id.popupItemThankButton);
            if (null != item.getAuthor() && ! isCurrentUserAuthorOf(item)) {
                tyb.setVisibility(View.VISIBLE);
            } else {
                tyb.setVisibility(View.GONE);
            }
            tyb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String message;
                    if (null != item.getAuthor()) {
                        message = String.format(
                                "In the future, you will be able to thank %s.",
                                item.getAuthor().getPrettyUsername()
                        );
                    } else {
                        message = "In the future, you will be able to thank people.";
                    }
                    toast(message);
                }
            });

            // Set an image if there's one
            WebImageView image = (WebImageView) layout.findViewById(R.id.popupItemImageView);
            if ( ! item.getThumbnail().isEmpty()) {
                image.setImageURL(item.getThumbnailNoSsl());
//            } else {
                // Nah, this screws up the FAB's relative position
                // image.setVisibility(View.GONE);
            }

            // Set a title if there's one
            TextView title = (TextView) layout.findViewById(R.id.popupItemTitleText);
            if (item.getTitle().isEmpty()) {
                title.setVisibility(View.GONE);
            } else {
                title.setText(item.getTitle());
            }

            // Set an authorship if there's one
            TextView by = (TextView) layout.findViewById(R.id.popupItemByText);
            if (null == item.getAuthor()) {
                by.setVisibility(View.GONE);
            } else {
                by.setText(String.format("by %s", item.getAuthor().getPrettyUsername()));
            }

            // Display the popup in the center
            pw.showAtLocation(layout, Gravity.CENTER, 0, 0);



//            Button cancelButton = (Button) layout.findViewById(R.id.end_data_send_button);
//            makeBlack(cancelButton);

//            cancelButton.setOnClickListener(cancel_button_click_listener);

        } catch (Exception e) {
            // we're not even giving a shit anymore
            e.printStackTrace();
        }

    }

    public boolean isCurrentUserAuthorOf(Item item)
    {
        return getCurrentServer().getUsername().equals(item.getAuthor().getUsername());
    }

    // NAVIGATION DRAWER ///////////////////////////////////////////////////////////////////////////

    public static long NAVIGATION_DRAWER_ITEM_MAP     = 2;
    public static long NAVIGATION_DRAWER_ITEM_PROFILE = 3;
    public static long NAVIGATION_DRAWER_ITEM_ABOUT   = 4;

    /**
     * Set up the navigation drawer. It's a complex setup that we don't want to repeat in each and
     * every activity, hence its presence in the Application.
     *
     * @param activity
     * @param toolbar the Toolbar to replace the default Appbar
     * @param selectedDrawerItem One of NAVIGATION_DRAWER_ITEM_XXXXX, provide -1 to select nothing.
     */
    public Drawer setUpNavigationDrawer(
            final AppCompatActivity activity, Toolbar toolbar, long selectedDrawerItem
    ) {

        activity.setSupportActionBar(toolbar);

        PrimaryDrawerItem mapDrawerItem = new PrimaryDrawerItem()
                .withName(R.string.menu_action_map)
                .withIcon(R.drawable.ic_map_black_36dp)
                .withIconTintingEnabled(true)
//                .withSelectable(false) // Nope, we want the color to change.
                .withIdentifier(NAVIGATION_DRAWER_ITEM_MAP)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        launchMap(activity);
                        return true;
                    }
                })
                ;

        PrimaryDrawerItem profileDrawerItem = new PrimaryDrawerItem()
                .withName(R.string.menu_action_profile)
                .withIcon(R.drawable.ic_perm_identity_black_36dp)
                .withIconTintingEnabled(true)
                .withIdentifier(NAVIGATION_DRAWER_ITEM_PROFILE)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        launchProfile(activity);
                        return true;
                    }
                })
                ;

        PrimaryDrawerItem addDrawerItem = new PrimaryDrawerItem()
                .withName(R.string.menu_action_add_item)
                .withIcon(R.drawable.ic_camera_alt_black_36dp)
                .withIconTintingEnabled(true)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        launchNewItem(activity);
                        return true;
                    }
                })
                ;

        PrimaryDrawerItem settingsDrawerItem = new PrimaryDrawerItem()
                .withName(R.string.menu_action_settings)
                .withIcon(R.drawable.ic_settings_black_36dp)
                .withIconTintingEnabled(true)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        launchSettings(activity);
                        return true;
                    }
                })
                ;

        PrimaryDrawerItem aboutDrawerItem = new PrimaryDrawerItem()
                .withName(R.string.menu_action_about)
                .withIcon(R.drawable.ic_karma_black_36dp)
                .withIconTintingEnabled(true)
                .withIdentifier(NAVIGATION_DRAWER_ITEM_ABOUT)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        launchAbout(activity);
                        return true;
                    }
                })
                ;

        DrawerBuilder drawerBuilder = new DrawerBuilder().withActivity(activity)
                .withToolbar(toolbar)
                .addDrawerItems(
                        mapDrawerItem,
                        profileDrawerItem,
                        addDrawerItem,
                        new DividerDrawerItem(),
                        settingsDrawerItem,
                        aboutDrawerItem
                )
                .withSelectedItem(selectedDrawerItem)
                ;

        return drawerBuilder.build();
    }

//    /**
//     * Does nothing if the navigation drawer is not ready yet.
//     * @param selectedDrawerItem One of NAVIGATION_DRAWER_ITEM_XXXXX
//     */
//    public void selectNavigationDrawerItem(long selectedDrawerItem) {
//        if (null != navigationDrawer) {
//            navigationDrawer.setSelection(selectedDrawerItem);
//        }
//    }


    // INTENTS /////////////////////////////////////////////////////////////////////////////////////




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
