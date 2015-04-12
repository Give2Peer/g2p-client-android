package org.give2peer.give2peer;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Still not sure if this should be our main logic class.
 * What happens when I change Activities ?
 */
public class MainActivity extends ActionBarActivity
{
    protected LocationManager lm;
    protected LocationProvider lp;
    protected Location location;

    ItemRepository ir;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Let's grab the location manager
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        // We never know, maybe there's an available location already
        refreshLocationView();

        // Prepare the Item repository
        String username = "Goutte";
        String password = "Goutte";
        String serverUrl = "http://g2p.give2peer.org";
        ir = new ItemRepository(serverUrl, username, password);

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

    public void onSynchronize(View view)
    {
        // Find the items around me
        ArrayList<Item> items = findItemsAroundMe(0);

        // This is a hack for API v8 to get the column width in order to have square item thumbs
        int nbColumns = 2; // getting this procedurally requires a higher API too
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int size = dm.widthPixels / nbColumns;

        // Fill the gridView with our items
        GridView itemsGridView = (GridView) findViewById(R.id.itemsGridView);
        itemsGridView.setAdapter(new ItemAdapter(this, R.layout.grid_item, size, items));

        // Warn the user
        if (items.isEmpty()) {
            toast("No items could be found.");
        }
//        dump(items.get(0).title);

    }

    public void onUpdateLocation(View view)
    {
        // Disable the button, accessed within inner class, so `final` is needed
        final View button = findViewById(R.id.updateLocationButton);
        button.setEnabled(false);

        // Fetch the location asynchronously
        LocationListener locationListener = new OneTimeLocationListener(lm, getLocationCriteria()) {
            @Override
            public void onLocationChanged(Location newLocation) {
                super.onLocationChanged(newLocation);
                location = newLocation;
                refreshLocationView();
                button.setEnabled(true);
                toast("Successfully updated current location");
            }
        };
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

    protected void toast(String message)
    {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    protected void dump(String text)
    {
        TextView dumpTextView = (TextView) findViewById(R.id.dumpTextView);
        dumpTextView.setVisibility(View.VISIBLE);
        dumpTextView.setText(text);
    }

    protected String getDummyJson()
    {
        return getString(R.string.dummyFindItemsResponse);
    }


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

    protected ArrayList<Item> findItemsAroundMe(int page)
    {
        ArrayList<Item> items = new ArrayList<>();

        if (null != location) {
            items = ir.findAroundPaginated(location.getLatitude(), location.getLongitude(), page);
        }

        return items;
    }

    protected ArrayList<Item> findItemsAroundMeDummy()
    {
        ArrayList<Item> items = new ArrayList<>();
        // try parse the string to a JSON object
        try {
            JSONObject row;
            JSONArray rows = new JSONArray(getDummyJson());
            for (int i = 0 ; i < rows.length() ; i++) {
                row = rows.getJSONObject(i);
                items.add(new Item(row));
            }
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data : " + e.toString());
        }

        return items;
    }
}

