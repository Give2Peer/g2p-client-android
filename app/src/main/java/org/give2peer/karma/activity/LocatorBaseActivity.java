package org.give2peer.karma.activity;

import android.app.ProgressDialog;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.yayandroid.locationmanager.LocationBaseActivity;
import com.yayandroid.locationmanager.LocationConfiguration;
import com.yayandroid.locationmanager.LocationManager;
import com.yayandroid.locationmanager.constants.FailType;
import com.yayandroid.locationmanager.constants.LogType;
import com.yayandroid.locationmanager.constants.ProviderType;

import org.give2peer.karma.DeviceUtils;
import org.give2peer.karma.R;
import org.give2peer.karma.event.LocationFailureEvent;
import org.give2peer.karma.event.LocationUpdateEvent;
import org.greenrobot.eventbus.EventBus;

abstract public class LocatorBaseActivity extends LocationBaseActivity
{
    protected Location location;

    /**
     * Override this in child classes.
     * @return the string descriptor of the location rationale message to display.
     */
    protected int getLocationRationale()
    {
        return R.string.dialog_default_location_rationale;
    }

    protected boolean isLocationReady() {
        return null != location;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocationManager.setLogType(DeviceUtils.isEmulator() ? LogType.GENERAL : LogType.NONE);
    }

    // Not sure whether or not we'd have to do anything in the onResume anymore
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        if ( ! isLocationReady()
//                && getLocationManager().isWaitingForLocation()
//                && ! getLocationManager().isAnyDialogShowing()) {
//            getLocation();
//        }
//    }

    @Override
    public LocationConfiguration getLocationConfiguration() {
        LocationConfiguration lc = new LocationConfiguration()
                .keepTracking(false)
                .setMinAccuracy(50.0f)
                .setWaitPeriod(ProviderType.GOOGLE_PLAY_SERVICES, 5 * 1000)
                .setWaitPeriod(ProviderType.GPS, 10 * 1000)
                .setWaitPeriod(ProviderType.NETWORK, 5 * 1000)
                .setGPSMessage(getString(R.string.dialog_gps_disabled_msg))
                .setRationalMessage(getString(getLocationRationale()));

        // Asking is flaky on older versions, on API 10 for sure.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            lc.askForGooglePlayServices(true);
        }

        // There is a bug with Google Play Services on API 10 phones that makes it crash when
        // Internet is not available. We cannot catch other applications crashes, so we skip it.
        // That bug will never ever get fixed upstream, so this piece of monkey poop will dry.
        if (! isOnline() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            lc.doNotUseGooglePlayServices(true);
        }

        return lc;
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

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        EventBus.getDefault().post(new LocationUpdateEvent(location));
    }

    @Override
    public void onLocationFailed(int failType) {
        switch (failType) {
            case FailType.PERMISSION_DENIED: {
                Log.d("G2P", "Couldn't get location, because user didn't give permission!");
                break;
            }
            case FailType.GP_SERVICES_NOT_AVAILABLE:
            case FailType.GP_SERVICES_CONNECTION_FAIL: {
                Log.d("G2P", "Couldn't get location, because Google Play Services not available!");
                break;
            }
            case FailType.NETWORK_NOT_AVAILABLE: {
                Log.d("G2P", "Couldn't get location, because network is not accessible!");
                break;
            }
            case FailType.TIMEOUT: {
                Log.d("G2P", "Couldn't get location, and timeout!");
                break;
            }
            case FailType.GP_SERVICES_SETTINGS_DENIED: {
                Log.d("G2P", "Couldn't get location, because user didn't activate providers via settingsApi!");
                break;
            }
            case FailType.GP_SERVICES_SETTINGS_DIALOG: {
                Log.d("G2P", "Couldn't display settingsApi dialog!");
                break;
            }
        }

        EventBus.getDefault().post(new LocationFailureEvent(failType));
    }

}
