package org.give2peer.karma.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.listener.GoogleApiClientListener;

/**
 * The purpose of this class is to be extended, and it provides the logic for Location detection,
 * through the Google Play API, to any Activity that extends it.
 * I'm not sure this is the correct way of doing this (and I strongly suspect that it is not).
 * But it was accessible to a beginner, so here it is.
 */
@Deprecated
abstract public class LocatorActivity
        extends android.support.v7.app.AppCompatActivity
        implements GoogleApiClientListener,
                   ActivityCompat.OnRequestPermissionsResultCallback
{
    Application app;

    GoogleApiClient googleLocator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (Application) getApplication();

        GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
        int availability = gaa.isGooglePlayServicesAvailable(this);

        // SUCCESS, SERVICE_MISSING, SERVICE_UPDATING, SERVICE_VERSION_UPDATE_REQUIRED,
        // SERVICE_DISABLED, SERVICE_INVALID
        // Read more: http://developer.android.com/google/play-services/setup.html

        isResolvingLocatingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        if (availability != ConnectionResult.SUCCESS) {
            if (gaa.isUserResolvableError(availability)) {
                app.toast("Google Play is unavailable or not up-to-date.");
            } else {
                app.toast("Google Play is unavailable or not up-to-date, and it is not resolvable.");
            }
            gaa.getErrorDialog(this, availability, 0).show();
        }

        Log.d("G2P", "Building Google API Client.");
        googleLocator = app.buildGoogleLocator(this, this);
    }

    //// GEO LOCATION //////////////////////////////////////////////////////////////////////////////

    // Request code to use when launching the resolution activity
    protected static final int REQUEST_RESOLVE_ERROR = 1001;
    protected static final int REQUEST_CODE_ASK_PERMISSIONS = 1002;
    // Unique tag for the error dialog fragment
    protected static final String DIALOG_ERROR = "dialog_error";
    // To save the state of the error resolving in case the screen is rotated for example
    protected static final String STATE_RESOLVING_ERROR = "resolving_error";
    // Whether the locator activity is already resolving an error or not
    protected boolean isResolvingLocatingError = false;

    @Override
    protected void onStart()
    {
        super.onStart();
        if ( ! isResolvingLocatingError) {
            googleLocator.connect();
        }
    }

    @Override
    protected void onStop()
    {
        googleLocator.disconnect();
        super.onStop();
    }

    /**
     * Override this in child classes
     * @param location The Location fetched from google api services
     */
    public void onLocated(Location location) {
        app.setGeoLocation(location);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        locate();
    }

    /**
     * Fires the `onLocated` method with the found location.
     */
    public void locate()
    {
        if (null != googleLocator && googleLocator.isConnected()) {
            requestGpsEnabled(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if ( ! ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    final Activity activity = this;
                    new AlertDialog.Builder(this)
                            .setTitle("Permissions required")
                            .setMessage("You need to allow access to the location.")
                            .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {
                                    // beware, this is run on UI thread
                                    ActivityCompat.requestPermissions(activity,
                                            new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                            REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            })
                            .create()
                            .show();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_ASK_PERMISSIONS);
                }

            } else {
                // We got all he right permissions, let's locate !
                Location location = LocationServices.FusedLocationApi.getLastLocation(googleLocator);
                if (location == null) {
                    // If you get this on the emulator, try opening Google Maps first.
                    Log.e("G2P", "Failed to retrieve the last known location.");
                } else {
                    this.onLocated(location);
                }
            }
        } else {
            // I've never seen that happen yet.
            Log.d("G2P", "Tried to locate but Google Locator API is not yet available.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (grantResults.length == 0) {
            // In some rare cases, this might happen ; consider it canceled
            Log.d("G2P", "onRequestPermissionsResult with no permissions.");
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    locate();
                } else {
                    // Permission Denied
                    app.toasty("Permission to locate denied.\nYou are on your own.");
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onConnectionSuspended(int reason)
    {
        // `reason` can have the following values
        // GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST
        // GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED
        Log.i("G2P", "Connection to Google API suspended.");
        // I don't know what else to do here. Ideas ?
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.e("G2P", "Connection to Google API services failed.");
        // If we're not already attempting to resolve an error...
        if ( !isResolvingLocatingError) {
            if (connectionResult.hasResolution()) {
                try {
                    isResolvingLocatingError = true;
                    connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    googleLocator.connect();
                }
            } else {
                // Show dialog using GooglePlayServicesUtil.getErrorDialog()
                showGoogleApiErrorDialog(connectionResult.getErrorCode());
                isResolvingLocatingError = true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            isResolvingLocatingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!googleLocator.isConnecting() && !googleLocator.isConnected()) {
                    googleLocator.connect();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, isResolvingLocatingError);
    }


    //// GOOGLE API ERROR DIALOG ///////////////////////////////////////////////////////////////////

    /* Creates a dialog for an error message */
    private void showGoogleApiErrorDialog(int errorCode)
    {
        // Create a fragment for the error dialog
        GoogleApiErrorDialogFragment dialogFragment = new GoogleApiErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), DIALOG_ERROR);
    }

    /* Called from GoogleApiErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed()
    {
        isResolvingLocatingError = false;
    }

    /* A fragment to display an error dialog */
    public static class GoogleApiErrorDialogFragment extends DialogFragment
    {
        public GoogleApiErrorDialogFragment() {}

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR
            );
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((LocatorActivity)getActivity()).onDialogDismissed();
        }
    }




    //// GPS DISABLED ERROR DIALOG /////////////////////////////////////////////////////////////////

    /**
     * Show a dialog suggesting to enable the GPS when it is not.
     * It will redirect the user to its system location settings.
     * Note: context should probably be an Activity, not an Application.
     */
    public void requestGpsEnabled(final Context context)
    {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if ( ! locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.dialog_gps_disabled_msg)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_gps_disabled_oui, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            context.startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.dialog_gps_disabled_non, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

}
