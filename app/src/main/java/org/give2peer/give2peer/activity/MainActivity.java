package org.give2peer.give2peer.activity;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.OneTimeLocationListener;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.fragment.ServerChooserFragment;


/**
 * This is the landing activity when a user starts the app.
 *
 * Callbacks : http://developer.android.com/training/basics/activity-lifecycle/starting.html
 *
 */
public class MainActivity extends ActionBarActivity
{
    protected LocationManager lm;
    protected LocationProvider lp;

    protected Application app;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Grab the app
        app = (Application) getApplication();

        // Let's grab the location manager
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        // We never know, maybe there's an available location already
        refreshLocationView();
        refreshActionsView();

        refreshServerChooser();

        // Useful, to run a query on the UI thread, for debugging ONLY of course
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshServerChooser();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // LISTENERS ///////////////////////////////////////////////////////////////////////////////////



    // UI LISTENERS ////////////////////////////////////////////////////////////////////////////////

    public void onListAroundMe(View view)
    {
        if (app.isOnline()) {
            // Start the "list around" activity
            Intent intent = new Intent(this, ListAroundActivity.class);
            intent.putExtra("page", 0);
            startActivity(intent);
        } else {
            toast("Internet is not available.");
        }
    }

    public void onDetectLocation(View view)
    {
        // Disable some buttons, accessed within inner class, so `final` is needed
        final View detectLocationButton = findViewById(R.id.detectLocationButton);
        detectLocationButton.setEnabled(false);

        // Fetch the location asynchronously
        LocationListener locationListener = new OneTimeLocationListener(lm, getLocationCriteria()) {
            @Override
            public void onLocationChanged(Location newLocation) {
                super.onLocationChanged(newLocation);
                app.setLocation(newLocation);
                refreshLocationView();
                refreshActionsView();
                detectLocationButton.setEnabled(true);
                toast("Successfully updated current location.");
            }
        };
    }

    public void onGiveItemButton(View view)
    {
        snapshotNewItem("give");
    }

    public void onSpotItemButton(View view)
    {
        snapshotNewItem("spot");
    }


    // UI ACTIONS //////////////////////////////////////////////////////////////////////////////////

    public void refreshActionsView()
    {
        boolean enabled = null != app.getLocation();
        findViewById(R.id.listAroundMeButton).setEnabled(enabled);
        findViewById(R.id.giveItemButton).setEnabled(enabled);
        findViewById(R.id.spotItemButton).setEnabled(enabled);
    }

    public void refreshLocationView()
    {
        Location location = app.getLocation();
        TextView title = ((TextView)findViewById(R.id.currentLocationTitle));
        if (null != location) {
            double latitude  = location.getLatitude();
            double longitude = location.getLongitude();

            title.setText(getString(R.string.title_current_location));

            ((EditText) findViewById(R.id.latitudeEditText )).setText(String.valueOf(latitude ));
            ((EditText) findViewById(R.id.longitudeEditText)).setText(String.valueOf(longitude));
        } else {
            title.setText(getString(R.string.title_set_up_location));
        }
    }

    public void refreshServerChooser()
    {
        // Ask the app to guess a server configuration
        app.setServerConfiguration(app.guessServerConfiguration());
        // Display the server chooser fragment
        getFragmentManager().beginTransaction()
                .replace(R.id.serverChooserFragment, new ServerChooserFragment())
                .commit();
    }


    // HELPERS /////////////////////////////////////////////////////////////////////////////////////

    protected void toast(String message) { toast(message, Toast.LENGTH_SHORT); }
    protected void toast(String message, int duration)
    {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    protected void dump(String text)
    {
        TextView dumpTextView = (TextView) findViewById(R.id.dumpTextView);
        dumpTextView.setVisibility(View.VISIBLE);
        dumpTextView.setText(text);
    }


    // CONFIGURATION ///////////////////////////////////////////////////////////////////////////////

    /**
     * @return a criteria tailored to our needs.
     */
    protected Criteria getLocationCriteria()
    {
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);

        return criteria;
    }


    // ACTIONS /////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param action Must be either "give" or "spot".
     */
    protected void snapshotNewItem(String action)
    {
        if (!app.hasCameraSupport()) {
            toast(getString(R.string.toast_no_camera_available));
            return;
        }

        // Start the "new item" activity
        Intent intent = new Intent(this, NewItemActivity.class);
        intent.putExtra("action", action);
        startActivity(intent);
    }
}

