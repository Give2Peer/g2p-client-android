package org.give2peer.karma.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
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
abstract public class LocatorActivity
        extends ActionBarActivity
        implements GoogleApiClientListener {
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

        isResolvingError = savedInstanceState != null
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
    // Unique tag for the error dialog fragment
    protected static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    protected boolean isResolvingError = false;
    // To save the state of the error resolving in case the screen is rotated for example
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    @Override
    protected void onStart() {
        super.onStart();
        if (!isResolvingError) {
            googleLocator.connect();
        }
    }

    @Override
    protected void onStop() {
        googleLocator.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("G2P", "Connection to Google Location API established.");
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleLocator);
        if (lastLocation == null) {
            Log.e("G2P", "Failed to retrieve the last known location.");
        } else {
            app.setGeoLocation(lastLocation);
            this.onLocated(lastLocation);
        }
    }

    /**
     * Override this in child classes
     * @param location The Location fetched from google api services
     */
    public void onLocated(Location location) {
        app.setGeoLocation(location);
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i("G2P", "Connection to Google API suspended.");
        // I don't know what else to do here. Ideas ?
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.e("G2P", "Connection to Google API services failed.");
        // If we're not already attempting to resolve an error...
        if ( ! isResolvingError) {
            if (connectionResult.hasResolution()) {
                try {
                    isResolvingError = true;
                    connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    googleLocator.connect();
                }
            } else {
                // Show dialog using GooglePlayServicesUtil.getErrorDialog()
                showErrorDialog(connectionResult.getErrorCode());
                isResolvingError = true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            isResolvingError = false;
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
        outState.putBoolean(STATE_RESOLVING_ERROR, isResolvingError);
    }


    //// GOOGLE API ERROR DIALOG ///////////////////////////////////////////////////////////////////

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode)
    {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), DIALOG_ERROR);
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed()
    {
        isResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment
    {
        public ErrorDialogFragment() {}

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


    //// GPS DISABLED ERROR DIALOG ///////////////////////////////////////////////////////////////////////

    public void requestGpsEnabled()
    {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_gps_disabled_msg)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_gps_disabled_oui, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
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
