package org.give2peer.give2peer.activity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.entity.Location;
import org.give2peer.give2peer.exception.GeocodingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MapItemsActivity extends Activity implements OnMapReadyCallback
{
    Application app;

    HashMap<Marker, Item> markerItemHashMap = new HashMap<Marker, Item>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_items);

        app = (Application) getApplication();

        int availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        // SUCCESS, SERVICE_MISSING, SERVICE_UPDATING, SERVICE_VERSION_UPDATE_REQUIRED,
        // SERVICE_DISABLED, SERVICE_INVALID
        // Read more: http://developer.android.com/google/play-services/setup.html

        if (availability != ConnectionResult.SUCCESS) {
            app.toast("Google Play is unavailable or not up-to-date.");
            GoogleApiAvailability.getInstance().getErrorDialog(this, availability, 0).show();
            finish();
        }

        // todo: also check OpenGL ES 2.0 so that we can remove the requirement in the manifest

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapItemsFragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap)
    {
        Location l = app.getLocation();
        try {
            app.geocodeLocationIfNeeded(l);
        } catch (IOException | GeocodingException e) {
            e.printStackTrace();
            app.toast(e.getMessage());
            finish();
        }
        final double lat = l.getLatitude();
        final double lng = l.getLongitude();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 1));

        AsyncTask findAndLocate = new AsyncTask<Void, Void, ArrayList<Item>>() {

            Exception exception;

            @Override
            protected ArrayList<Item> doInBackground(Void... params)
            {
                ArrayList<Item> items = new ArrayList<Item>();
                try {
                    items = app.getRestService().findAround(lat, lng);
                } catch (Exception e) {
                    exception = e;
                }
                return items;
            }

            @Override
            protected void onPostExecute(ArrayList<Item> items)
            {
                super.onPostExecute(items);

                // Something went wrong with the request: warn the user and GTFO.
                if (null != exception) {
                    app.toast(String.format("Failure: %s", exception.getMessage()), Toast.LENGTH_LONG);
                    finish();
                    return;
                }

                // Remove the loading spinner
                findViewById(R.id.mapItemsProgressBar).setVisibility(View.GONE);

                // Collect the LatLngs to zoom and pan the camera ideally
                LatLngBounds.Builder bc = new LatLngBounds.Builder();

                for (Item item : items) {

                    bc.include(item.getLatLng());

                    Marker m = googleMap.addMarker(
                            new MarkerOptions()
                                    .position(item.getLatLng())
                                    .title(item.getTitle())
                                    .snippet(item.getHumanDistance())
                    );

                    // We also map the markers to the items for the click callback
                    markerItemHashMap.put(m, item);
                }

                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 55));
            }
        }.execute();

    }
}
