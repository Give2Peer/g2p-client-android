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
import android.widget.TextView;
import android.widget.Toast;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.ItemRepository;
import org.give2peer.give2peer.OneTimeLocationListener;
import org.give2peer.give2peer.R;


/**
 * Still not sure if this should be our main logic class.
 * What happens when I change Activities ?
 *
 * Callbacks : http://developer.android.com/training/basics/activity-lifecycle/starting.html
 *
 */
public class MainActivity extends ActionBarActivity
{
    protected LocationManager lm;
    protected LocationProvider lp;
    protected Location location;

    ItemRepository ir;

    protected Application app;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Grab the app
        app = (Application) getApplication();

        // Let's grab the location manager
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        // We never know, maybe there's an available location already
        refreshLocationView();

        // TEST -- fixme: remove and async all queries
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);

        // Prepare the Item repository
        ir = app.getItemRepository();

        setContentView(R.layout.activity_main);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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

        final View listAroundMeButton = findViewById(R.id.listAroundMeButton);
        listAroundMeButton.setEnabled(false);

        // Fetch the location asynchronously
        LocationListener locationListener = new OneTimeLocationListener(lm, getLocationCriteria()) {
            @Override
            public void onLocationChanged(Location newLocation) {
                super.onLocationChanged(newLocation);
                location = newLocation;
                app.setLocation(location);
                refreshLocationView();
                detectLocationButton.setEnabled(true);
                listAroundMeButton.setEnabled(true);
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

    // UI ACTIONS //////////////////////////////////////////////////////////////////////////////////

    public void refreshLocationView()
    {
        TextView currentLocationView = (TextView) findViewById(R.id.currentLocationView);
        if (null != location) {
            double latitude  = location.getLatitude();
            double longitude = location.getLongitude();

            currentLocationView.setText(String.format("%.4f/%.4f", latitude, longitude));
        }
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

//    protected String getDummyJson()
//    {
//        return getString(R.string.dummyFindItemsResponse);
//    }


    // CONFIGURATION ///////////////////////////////////////////////////////////////////////////////

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

//    protected ArrayList<Item> findItemsAroundMe(int page)
//    {
//        ArrayList<Item> items = new ArrayList<>();
//
//        if (null != location) {
//            items = ir.findAroundPaginated(location.getLatitude(), location.getLongitude(), page);
//        }
//
//        return items;
//    }
//
//    protected ArrayList<Item> findItemsAroundMeDummy()
//    {
//        ArrayList<Item> items = new ArrayList<>();
//        // try parse the string to a JSON object
//        try {
//            JSONObject row;
//            JSONArray rows = new JSONArray(getDummyJson());
//            for (int i = 0 ; i < rows.length() ; i++) {
//                row = rows.getJSONObject(i);
//                items.add(new Item(row));
//            }
//        } catch (JSONException e) {
//            Log.e("JSON Parser", "Error parsing data : " + e.toString());
//        }
//
//        return items;
//    }
}

