package org.give2peer.give2peer.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
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
import org.give2peer.give2peer.listener.GoogleApiClientListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class      MapItemsActivity
       extends    LocatorActivity
       implements OnMapReadyCallback
{
    // Allows us to find items from their respective markers during map UI events
    HashMap<Marker, Item> markerItemHashMap = new HashMap<Marker, Item>();

    GoogleMap googleMap;

    FrameLayout mapItemsDrawFrame;
    Boolean     isDrawing = false; // whether or not the user is drawing

    ArrayList<LatLng> drawingCoordinates = new ArrayList<>();

    HashSet<Item> displayedItems = new HashSet<>();

    int lineColor = 0xFFFF3399;
    int fillColor = 0x33FF3399;
    int fillAlpha = 0x55000000;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_items);

        // I never had a failure there, but better safe than sorry !
        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapItemsFragment);
            mapFragment.getMapAsync(this);
        } catch (Exception e) {
            Log.e("G2P", "There was a problem loading the map fragment.");
            Log.e("G2P", e.getMessage());
            e.printStackTrace();

            app.toast("Failed to load the map on this device. Sorry!");

            // todo: show a help view, and/or the "report a bug" button.
        }
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

        if (id == R.id.menu_action_settings) {
            launchSettings();
            return true;
        }
        if (id == R.id.menu_action_add_item) {
            launchNewItem();
            return true;
        }
        if (id == R.id.menu_action_report_bug) {
            launchBugReport();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocated(Location loc)
    {
        super.onLocated(loc);
        if (isMapReady()) executeFinderTask(new LatLng(loc.getLatitude(), loc.getLongitude()));
    }

    //// ACTIONS ///////////////////////////////////////////////////////////////////////////////////

    public void launchBugReport()
    {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(Application.REPORT_BUG_URL));
        startActivity(i);
    }

    public void launchNewItem()
    {
        Intent intent = new Intent(this, NewItemActivity.class);
        startActivity(intent);
    }

    public void launchSettings()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    protected boolean isFinding()
    {
        return finder != null && finder.getStatus() != AsyncTask.Status.FINISHED;
    }

    public void onDrawButton(View button)
    {
        // 1. We are currently finding items, and this button is a CANCEL button.
        if (isFinding()) {
            cancelFinderTask();
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

    protected void updateDrawButtonDelayed()
    {
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

    AsyncTask finder;

    public AsyncTask executeFinderTask(final LatLng where)
    {
        return executeFinderTask(where, null);
    }

    public AsyncTask executeFinderTask(final LatLng where, final List<LatLng> container)
    {
        cancelFinderTask();
        showLoader();

        finder = new AsyncTask<Void, Void, ArrayList<Item>>()
        {
            Exception exception;

            @Override
            protected ArrayList<Item> doInBackground(Void... params)
            {
                ArrayList<Item> items = new ArrayList<Item>();
                try {
                    items = app.getRestService().findAround(where.latitude, where.longitude);
                } catch (Exception e) {
                    exception = e;
                }

                // Remove duplicates (comparing getId)
                // This logic should probably reside in an ItemsCache or some such
                ArrayList<Item> newItems = new ArrayList<Item>();
                for (Item newItem : items) {
                    boolean alreadyThere = false;
                    for (Item oldItem: displayedItems) {
                        if (newItem.getId() == oldItem.getId()) {
                            alreadyThere = true;
                            Log.d("G2P", "Item already fetched, and subsenquently ignored.");
                            break;
                        }
                    }
                    if (!alreadyThere) newItems.add(newItem);
                }

                // Remove items outside of container polygon (if specified)
                if (container != null) {
                    items = newItems;
                    newItems = new ArrayList<Item>();
                    for (Item item : items) {
                        if (pointInPolygon(item.getLatLng(), container)) {
                            newItems.add(item);
                        }
                    }
                }

                // Add to the cache
                displayedItems.addAll(newItems);

                return newItems;
            }

            @Override
            protected void onPostExecute(ArrayList<Item> items)
            {
                super.onPostExecute(items);

                // Hide the loader, whether there was an exception or not
                hideLoader();

                // Disable the Cancel button, it's too late to cancel now
                findViewById(R.id.mapItemsDrawButton).setEnabled(false);

                // Something went wrong with the request: probably no internet
                if (null != exception) {
                    exception.printStackTrace();
                    findViewById(R.id.noInternetTextView).setVisibility(View.VISIBLE);
                    updateDrawButtonDelayed();
                    return;
                } else {
                    findViewById(R.id.noInternetTextView).setVisibility(View.GONE);
                }

                int itemsCount = items.size();

                if (itemsCount == 0) {
                    // There were no items found
                    app.toast("No items were found in this area.", Toast.LENGTH_LONG);
                } else {
                    // Collect the LatLngs to zoom and pan the camera ideally
                    LatLngBounds.Builder bc = new LatLngBounds.Builder();

                    for (int i=0; i<itemsCount; i++) {
                        Item item = items.get(i);

                        bc.include(item.getLatLng());

                        Marker m = googleMap.addMarker(
                                new MarkerOptions()
                                        .position(item.getLatLng())
                                        .title(item.getTitle())
                                        .snippet(item.getHumanDistance())
                        );

                        dropPinEffect(m, Math.round(i * 222));

                        // We also map the markers to the items for the click callback
                        markerItemHashMap.put(m, item);
                    }

                    // Pan and zoom the camera
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 55));

                    // Hide the drawn region, and draw a circle instead
                    // This is a cheap solution to the problem of performance, as users may draw BIG regions
                    // Items are sorted by distance, so we are grabbing the last one
//                    double radius = items.get(itemsCount-1).getDistance();
//                    hideRegion();
//                    drawCircleOnMap(googleMap, where, radius);
                }



                // Hmmmm... This is pretty bad.
                // We need to be sure that this task's status is FINISHED before we update the UI.
                // We could remove this hack by using a isFinished bool that we update ourselves
                // but this solution also has non-trivial issues when running concurrent tasks.
                // We can only implement it if we make sure that only one task may run at a time.
                updateDrawButtonDelayed();
            }

        }.execute();

        updateDrawButton();

        return finder;
    }

    /**
     * Cancel the current subtask of finding items.
     */
    public void cancelFinderTask()
    {
        if (finder != null) finder.cancel(true);
        finder = null;
    }

    protected boolean isMapReady()
    {
        return googleMap != null;
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

//                                LatLng center = drawingCoordinates.get(0);
//                                float[] results = new float[3];
//                                android.location.Location.distanceBetween(
//                                        center.latitude, center.longitude,
//                                        latLng.latitude, latLng.longitude,
//                                        results);
//                                float distance = results[0];
//                                drawCircleOnMap(googleMap, center, distance);

                                break;

                            case MotionEvent.ACTION_DOWN: // the finger touches the screen
                                drawingCoordinates.clear();
                                drawingCoordinates.add(latLng);
                                drawPolylineOnMap(googleMap, drawingCoordinates);
                                break;

                            case MotionEvent.ACTION_UP:   // the finger leaves the screen
                                drawPolygonOnMap(googleMap, drawingCoordinates);

                                isDrawing = false;

                                LatLng centroid = getLatLngCentroid(drawingCoordinates);
                                if (centroid != null) {
                                    // We need to make a copy of our drawn path, as we may clear it
                                    // at any time. (we're even clearing it right below)
                                    List<LatLng> container = new ArrayList<LatLng>();
                                    container.addAll(drawingCoordinates);
                                    executeFinderTask(centroid, container);
                                }

                                // Zoom and pan the camera ideally around the drawn area
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                                        getLatLngBounds(drawingCoordinates), 55
                                ));

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
                    // The animation continues, post again 15ms later
                    handler.postDelayed(this, 15);
                } else {
                    // The animation has ended
                    //marker.showInfoWindow();
                }
            }
        }
        ,delay);
    }
    
    Polygon  drawnPolygon;
    Polyline drawnPolyline;
    Circle   drawnCircle;

    public void hideRegion()
    {
        if (drawnPolygon != null)  drawnPolygon.remove();
    }

    public void drawPolygonOnMap(GoogleMap googleMap, Iterable<LatLng> polygonLatLngs)
    {
        if (drawnPolygon != null)  drawnPolygon.remove();
        if (drawnPolyline != null) drawnPolyline.remove();

        PolygonOptions options = new PolygonOptions();
        options.addAll(polygonLatLngs);
        options.strokeColor(lineColor);
        options.strokeWidth(7);
        options.fillColor(fillColor);

        drawnPolygon = googleMap.addPolygon(options);
    }

    public void drawPolylineOnMap(GoogleMap googleMap, Iterable<LatLng> lineLatLngs)
    {
        if (drawnPolyline != null) drawnPolyline.remove();

        PolylineOptions options = new PolylineOptions();
        options.addAll(lineLatLngs);
        options.color((lineColor & 0x00FFFFFF) + fillAlpha);
        options.width(7);

        drawnPolyline = googleMap.addPolyline(options);
    }

    public void drawCircleOnMap(GoogleMap googleMap, LatLng center, double radius)
    {
        if (drawnCircle != null) drawnCircle.remove();

        CircleOptions options = new CircleOptions();
        options.center(center);
        options.radius(radius);
        options.strokeColor(lineColor);
        options.fillColor((lineColor & 0x00FFFFFF) + fillAlpha);
        options.strokeWidth(7);

        drawnCircle = googleMap.addCircle(options);
    }

    protected LatLngBounds getLatLngBounds(List<LatLng> latLngs)
    {
        LatLngBounds.Builder bc = new LatLngBounds.Builder();
        for (LatLng latLng : latLngs) { bc.include(latLng); }

        return bc.build();
    }

    protected LatLng getLatLngCentroid(List<LatLng> latLngs)
    {
        if (latLngs.size() == 0) return null;

        return getLatLngBounds(latLngs).getCenter();
    }

    //// MATH UTILS (SHOULD BE MOVED ELSEWHERE) ////////////////////////////////////////////////////

    /**
     * Ray casting alogrithm, see http://rosettacode.org/wiki/Ray-casting_algorithm
     *
     * @param point
     * @param polygon The list of the vertices of the polygon, sequential and looping.
     * @return whether the point is inside the polygon
     */
    public boolean pointInPolygon(LatLng point, List<LatLng> polygon)
    {
        int crossings = 0;
        int verticesCount = polygon.size();

        // For each edge
        for (int i = 0; i < verticesCount; i++) {
            int j = (i + 1) % verticesCount;
            LatLng a = polygon.get(i);
            LatLng b = polygon.get(j);
            if (rayCrossesSegment(point, a, b)) crossings++;
        }

        // Odd number of crossings?
        return crossings % 2 == 1;
    }

    /**
     * Ray Casting algorithm checks, for each segment AB,
     * Returns true if the point is
     *   1) to the left of the segment and
     *   2) not above nor below the segment.
     *
     * @param point
     * @param a
     * @param b
     */
    public boolean rayCrossesSegment(LatLng point, LatLng a, LatLng b)
    {
        double px = point.longitude,
               py = point.latitude,
               ax = a.longitude,
               ay = a.latitude,
               bx = b.longitude,
               by = b.latitude;
        if (ay > by) {
            ax = b.longitude;
            ay = b.latitude;
            bx = a.longitude;
            by = a.latitude;
        }
        // Alter longitude to cater for 180 degree crossings
        if (px < 0 || ax < 0 || bx < 0) { px += 360; ax += 360; bx += 360; }
        // If the point has the same latitude as a or b, increase slightly py
        if (py == ay || py == by) py += 0.00000001;
        // If the point is above, below or to the right of the segment, it returns false
        if ((py > by || py < ay) || (px > Math.max(ax, bx))) { return false; }
        // If the point is not above, below or to the right and is to the left, return true
        else if (px < Math.min(ax, bx))                      { return true;  }
        // When the two above conditions are not met, compare the slopes of segment AB and AP
        // to see if the point P is to the left of segment AB or not.
        else {
            double slopeAB = (ax != bx) ? ((by - ay) / (bx - ax)) : Double.POSITIVE_INFINITY;
            double slopeAP = (ax != px) ? ((py - ay) / (px - ax)) : Double.POSITIVE_INFINITY;
            return slopeAP >= slopeAB;
        }
    }
}
