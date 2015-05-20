package org.give2peer.give2peer.activity;

import android.app.Activity;

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;
import org.give2peer.give2peer.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MapItemsActivity extends Activity implements OnMapReadyCallback
{
    Application app;

    // Allows us to find items from their respective markers during UI events
    HashMap<Marker, Item> markerItemHashMap = new HashMap<Marker, Item>();


    FrameLayout mapItemsDrawFrame;
    Boolean     isDrawing = false; // whether the user is drawing or not

    ArrayList<LatLng> drawingCoordinates = new ArrayList<>();

    HashSet<Item> displayedItems = new HashSet<>();

    int lineColor = 0xFFFF3399;
    int fillColor = 0x33FF3399;


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

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapItemsFragment);
        mapFragment.getMapAsync(this);

    }

    protected boolean isFinding()
    {
        return finder != null && finder.getStatus() != AsyncTask.Status.FINISHED;
    }

    public void onDrawButton(View button)
    {
        // 1. We are currently finding items, and this button is a CANCEL button.
        if (isFinding()) {
            finder.cancel(true);
            finder = null;
            hideLoader();
            hideRegion();
        }
        // 2. We are on the map, and we want to FIND
        else if (!isDrawing) {
            isDrawing = true;
        }
        else {
            app.toast("How did you even manage to do that? Report a bug!");
        }

        updateDrawButton();
    }


    protected void updateDrawButton()
    {
        Button mapItemsDrawButton = (Button) findViewById(R.id.mapItemsDrawButton);

        // 1. We are currently finding items, and this button is a CANCEL button.
        if (isFinding()) {
            mapItemsDrawButton.setText("Cancel");
            mapItemsDrawButton.setEnabled(true);
        }
        // 2. We are on the map, and we want to FIND
        else if (!isDrawing) {
            mapItemsDrawButton.setText("Find");
            mapItemsDrawButton.setEnabled(true);
        }
        // 3. We are already drawing, so we hint DRAW
        else {
            mapItemsDrawButton.setText("Draw!");
            mapItemsDrawButton.setEnabled(false);
        }
    }

    GoogleMap googleMap;
    AsyncTask finder;


    public AsyncTask executeFinderTask(final LatLng where)
    {
        showLoader();
        finder = new AsyncTask<Void, Void, ArrayList<Item>>()
        {
            Exception exception;

            @Override
            protected ArrayList<Item> doInBackground(Void... params)
            {
                ArrayList<Item> items = new ArrayList<Item>();
                try {
                    //app.geocodeLocationIfNeeded(l);
                    items = app.getRestService().findAround(where.latitude, where.longitude);
                } catch (Exception e) {
                    exception = e;
                }

                // fixme: remove duplicates (comparing getId should probably work)
                // will probably not work ---> it does NOT
                //if (items.removeAll(displayedItems)) Log.w("G2P", "HEEEEEEY IT WORKS !");

                displayedItems.addAll(items);

                return items;
            }

            @Override
            protected void onPostExecute(ArrayList<Item> items)
            {
                super.onPostExecute(items);

                // Hide the loader, whether there was an exception or not
                hideLoader();

                // Something went wrong with the request: warn the user and GTFO.
                if (null != exception) {
                    app.toast(String.format("Failure: %s", exception.getMessage()), Toast.LENGTH_LONG);
                    finish();
                    return;
                }

                // Collect the LatLngs to zoom and pan the camera ideally
                LatLngBounds.Builder bc = new LatLngBounds.Builder();

                int itemsCount = items.size();

                for (int i=0; i<itemsCount; i++) {
                    Item item = items.get(i);

                    bc.include(item.getLatLng());

                    Marker m = googleMap.addMarker(
                            new MarkerOptions()
                                    .position(item.getLatLng())
                                    .title(item.getTitle())
                                    .snippet(item.getHumanDistance())
                    );

                    dropPinEffect(m, Math.round(i*222 + 222 * 0.618 * 0.618 * Math.random()));

                    // We also map the markers to the items for the click callback
                    markerItemHashMap.put(m, item);
                }

                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 55));


                // Hmmmm... This is pretty bad.
                // We need to be sure that this task's status is FINISHED before we update the UI.
                // We could remove this hack by using a isFinished bool that we update ourselves
                // but this solution also has non-trivial issues when running concurrent tasks.
                // We can only implement it if we make sure that only one task may run at a time.
                findViewById(R.id.mapItemsDrawButton).setEnabled(false);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        updateDrawButton();
                    }
                }, 300);
            }

        }.execute();

        updateDrawButton();

        return finder;
    }

    @Override
    public void onMapReady(GoogleMap _googleMap)
    {
        googleMap = _googleMap;

        googleMap.moveCamera(CameraUpdateFactory.zoomTo(1));

        if (app.hasGeoLocation()) {
            executeFinderTask(app.getGeoLocationLatLng());
        } else {
            // We cannot find any geo location handy
            // Let's warn the user that he needs to draw a region
            hideLoader();
            app.toast("Please draw the approximate region where you want to find (lost) items.");
        }

        mapItemsDrawFrame  = (FrameLayout) findViewById(R.id.mapItemsDrawFrame);
        mapItemsDrawFrame.setOnTouchListener(
                new View.OnTouchListener()
                {
                    @Override
                    public boolean onTouch(View v, MotionEvent event)
                    {
                        if (!isDrawing) return false;

                        int x = Math.round(event.getX());
                        int y = Math.round(event.getY());

                        Point point = new Point(x, y);

                        Projection projection = googleMap.getProjection();
                        LatLng latLng = projection.fromScreenLocation(point);

                        int eventAction = event.getAction();
                        switch (eventAction) {
                            case MotionEvent.ACTION_MOVE: // the finger moves on the screen
                                drawingCoordinates.add(latLng);
                                drawPolylineOnMap(googleMap, drawingCoordinates);
                                break;

                            case MotionEvent.ACTION_DOWN: // the finger touches the screen
                                drawingCoordinates.add(latLng);
                                drawPolylineOnMap(googleMap, drawingCoordinates);
                                break;

                            case MotionEvent.ACTION_UP:   // the finger leaves the screen
                                drawPolygonOnMap(googleMap, drawingCoordinates);

                                isDrawing = false;
                                LatLng centroid = getCentroidLatLng(drawingCoordinates);
                                if (centroid != null) {
                                    executeFinderTask(centroid);
                                }
                                drawingCoordinates.clear();

                                break;
                        }

                        return true;
                    }
                }
        );

    }

    private void showLoader()
    {
        findViewById(R.id.mapItemsProgressBar).setVisibility(View.VISIBLE);
    }

    private void hideLoader()
    {
        findViewById(R.id.mapItemsProgressBar).setVisibility(View.GONE);
    }

    private void dropPinEffect(final Marker marker, long delay)
    {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis() + delay;
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();

        marker.setAnchor(42, 42);

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = Math.max(
                        1 - interpolator.getInterpolation(
                                (float) elapsed
                                        / duration
                        ), 0
                );
                marker.setAnchor(0.5f, 1.0f + 14 * t);

                if (t > 0.0) {
                    // Post again 15ms later.
                    handler.postDelayed(this, 15);
                } else {
                    //marker.showInfoWindow();
                }
            }
        }
        ,delay);
    }
    
    Polygon  drawnPolygon;
    Polyline drawnPolyline;

    public void hideRegion()
    {
        if (drawnPolygon != null)  drawnPolygon.remove();
    }

    public void drawPolygonOnMap(GoogleMap googleMap, Iterable<LatLng> polygonLatLngs) {

        if (drawnPolygon != null)  drawnPolygon.remove();
        if (drawnPolyline != null) drawnPolyline.remove();

        PolygonOptions options = new PolygonOptions();
        options.addAll(polygonLatLngs);
        options.strokeColor(lineColor);
        options.strokeWidth(7);
        options.fillColor(fillColor);

        drawnPolygon = googleMap.addPolygon(options);
    }

    public void drawPolylineOnMap(GoogleMap googleMap, Iterable<LatLng> lineLatLngs) {

        if (drawnPolyline != null) drawnPolyline.remove();

        PolylineOptions options = new PolylineOptions();
        options.addAll(lineLatLngs);
        options.color((lineColor & 0x00FFFFFF) + 0x77000000); // set alpha to 77
        options.width(7);

        drawnPolyline = googleMap.addPolyline(options);
    }

    protected LatLng getCentroidLatLng(List<LatLng> latLngs)
    {
        if (latLngs.size() == 0) return null;

        LatLngBounds.Builder bc = new LatLngBounds.Builder();
        for (LatLng ll : latLngs) {
            bc.include(ll);
        }

        return bc.build().getCenter();
    }
}
