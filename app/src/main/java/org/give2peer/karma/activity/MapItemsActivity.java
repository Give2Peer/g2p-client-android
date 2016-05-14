package org.give2peer.karma.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.entity.Slide;
import com.shamanland.fab.FloatingActionButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.R;
import org.give2peer.karma.event.AuthenticationEvent;
import org.give2peer.karma.response.FindItemsResponse;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


@EActivity(R.layout.activity_map_items)
public class      MapItemsActivity
       extends    LocatorActivity
       implements OnMapReadyCallback
{
    // Allows us to find items from their respective markers during map UI events
    HashMap<Marker, Item> markerItemHashMap = new HashMap<Marker, Item>();

    GoogleMap googleMap;

    Boolean     isDrawing = false; // whether or not the user is drawing

    ArrayList<LatLng> drawingCoordinates = new ArrayList<>();

    /**
     * A set of displayed items on the map, to avoid duplicates when you make another request.
     */
    List<Item> displayedItems = new ArrayList<>();

    int lineColor = 0x88FF3399;
    int fillColor = 0x33FF3399;
    int fillAlpha = 0x55000000;

    //// VIEWS /////////////////////////////////////////////////////////////////////////////////////

    @ViewById
    TextView noInternetTextView; // time to start refactoring the "no internet" flow for activities!

    @ViewById
    ProgressBar mapItemsProgressBar;

    @ViewById
    FloatingActionButton mapItemsDrawButton;

    @ViewById
    FrameLayout mapItemsDrawFrame;


    //// LIFECYCLE /////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        loadTutorialIfNeeded();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if ( ! EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override
    public void onStop()
    {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @AfterViews
    protected void authenticate()
    {
        // onStart() is called AFTER this method, and so nobody listens to AuthenticationEvent yet,
        // so we need to register to the EventBus here and not in the onStart.
        if ( ! EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);

        // If the user is not authenticated, take care of it
        app.requireAuthentication(this);
    }

    @Subscribe
    public void loadMap(AuthenticationEvent authenticationEvent)
    {
        if (authenticationEvent.isFailure()) {
            hideLoader();
            noInternetTextView.setVisibility(View.VISIBLE);
            return;
        }

        // I never had a failure there, but better safe than sorry !
        try {
            Log.d("G2P", "Loading the map fragment...");
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapItemsFragment);
            mapFragment.getMapAsync(this);
        } catch (Exception e) {
            Log.e("G2P", "There was a problem loading the map fragment.");
            Log.e("G2P", e.getMessage());
            e.printStackTrace();

            app.toast("Failed to load the map on this device. Sorry!\nPlease report this !");
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if ( ! displayedItems.isEmpty() && null != googleMap) {
            Log.d("G2P", "Zooming in on items when resuming map activity.");
            zoomOnItems(googleMap, displayedItems);
        }
    }

    @Override
    public void onLocated(Location loc)
    {
        super.onLocated(loc);
        if (isMapReady()) executeFinderTask(new LatLng(loc.getLatitude(), loc.getLongitude()));
    }


    // OPTIONS MENU ////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean found = app.onOptionsItemSelected(item, this);
        return found || super.onOptionsItemSelected(item);
    }


    //// UI LISTENERS //////////////////////////////////////////////////////////////////////////////

    // todo: use @Click annotation
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
            app.toast(getString(R.string.toast_draw_on_map));
        }
        // 3. We already clicked the button, and should be drawing. Let's cancel !
        else {
            isDrawing = false;
        }

        updateDrawButton();
    }


    //// ACTIONS ///////////////////////////////////////////////////////////////////////////////////

    public void loadTutorialIfNeeded()
    {
        if ( ! app.isUserOnBoard()) {
            app.isUserOnBoard(true);
            loadTutorial();
        }
    }

    public void loadTutorial()
    {
        new IntroductionBuilder(this).withSlides(getOnboardingSlides(this)).introduceMyself();
    }

    protected List<Slide> getOnboardingSlides(Context ctx)
    {
        List<Slide> slides = new ArrayList<>();

        slides.add(new Slide()
                .withTitle(R.string.tutorial_slide_1_title)
                .withDescription(R.string.tutorial_slide_1_description)
                .withColorResource(R.color.tutorial_slide_1_background)
                .withImage(R.drawable.tutorial_slide_1)
        );

        slides.add(new Slide()
                .withTitle(R.string.tutorial_slide_2_title)
                .withDescription(R.string.tutorial_slide_2_description)
                .withColorResource(R.color.tutorial_slide_2_background)
                .withImage(R.drawable.tutorial_slide_2)
        );

        slides.add(new Slide()
                .withTitle(R.string.tutorial_slide_3_title)
                .withDescription(R.string.tutorial_slide_3_description)
                .withColorResource(R.color.tutorial_slide_3_background)
                .withImage(R.drawable.tutorial_slide_3)
        );

        slides.add(new Slide()
                .withTitle(R.string.tutorial_slide_4_title)
                .withDescription(R.string.tutorial_slide_4_description)
                .withColorResource(R.color.tutorial_slide_4_background)
                .withImage(R.drawable.tutorial_slide_4)
        );

        return slides;
    }

    protected void updateDrawButton()
    {
        // 1. We are currently finding items, and this button is a CANCEL button.
        if (isFinding()) {
            mapItemsDrawButton.setImageResource(R.drawable.ic_clear_white_24dp);
        }
        // 2. We are on the map, and we want to FIND
        else if (!isDrawing) {
            mapItemsDrawButton.setImageResource(R.drawable.ic_search_white_24dp);
        }
        // 3. We just clicked on it and are drawing, show CANCEL
        else {
            mapItemsDrawButton.setImageResource(R.drawable.ic_clear_white_24dp);
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

        final Activity activity = this;

        finder = new AsyncTask<Void, Void, ArrayList<Item>>()
        {
            Exception exception;

            @Override
            protected ArrayList<Item> doInBackground(Void... params)
            {
                FindItemsResponse itemsResponse = new FindItemsResponse();
                try {
                    itemsResponse = app.getRestService().findAround(where.latitude, where.longitude);
                } catch (Exception e) {
                    exception = e;
                }

                ArrayList<Item> items = new ArrayList<Item>();
                if (null != itemsResponse.getItems())
                {
                    Collections.addAll(items, itemsResponse.getItems());
                }

                ArrayList<Item> filteredItems = new ArrayList<Item>();
                filteredItems.addAll(items);

                // Remove duplicates (comparing getId)
                // This logic should probably reside in an ItemsCache or some such
                // pretty sure this does work but causes bugs
//                for (Item newItem : items) {
//                    boolean alreadyThere = false;
//                    for (Item oldItem: displayedItems) {
//                        Log.d("G2P", String.format("Comparing %d and %d", newItem.getId(), oldItem.getId()));
//                        if (newItem.getId() == oldItem.getId()) {
//                            alreadyThere = true;
//                            Log.d("G2P", "Item already fetched, and subsequently ignored.");
//                            break;
//                        }
//                    }
//                    if (!alreadyThere) filteredItems.add(newItem);
//                }

                // Remove items outside of container polygon (if specified)
                if (container != null) {
                    items = filteredItems;
                    filteredItems = new ArrayList<Item>();
                    for (Item item : items) {
                        if (pointInPolygon(item.getLatLng(), container)) {
                            filteredItems.add(item);
                        }
                    }
                }

                // Add to the cache
                displayedItems.clear();
                displayedItems.addAll(filteredItems);

                return filteredItems;
            }

            @Override
            protected void onPostExecute(ArrayList<Item> items)
            {
                super.onPostExecute(items);

                // Hide the loader, whether there was an exception or not.
                hideLoader();

                // Something went wrong with the request: probably no internet.
                if (null != exception) {
                    Log.e("G2P", "Something went wrong while finding items !");
                    exception.printStackTrace();
                    noInternetTextView.setVisibility(View.VISIBLE);
                    updateDrawButtonDelayed();
                    return;
                } else {
                    noInternetTextView.setVisibility(View.GONE);
                }

                // Now, either we found items or we didn't (TQ: maybe the app crashed :3)
                int itemsCount = items.size();
                if (itemsCount == 0) {
                    // There were no items found
                    app.toast("No items were found in this area.", Toast.LENGTH_LONG);
                } else {

                    // todo : Use a custom InfoWindowAdapter to add an image ?
                    //        but there are lots of caveats with this canvas drawing technique !

                    // Add markers to the map
                    for (int i=0; i<itemsCount; i++) {
                        Item item = items.get(i);

                        Marker m = googleMap.addMarker(
                                new MarkerOptions()
                                        .position(item.getLatLng())
                                        .title(item.getTitle())
                                        .snippet(item.getHumanUpdatedAt())
                        );

                        dropPinEffect(m, Math.round(i * 222));

                        // We also map the markers to the items for the click callback
                        markerItemHashMap.put(m, item);
                    }

                    // Zoom on items
                    zoomOnItems(googleMap, items);

                    googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                        @Override
                        public void onInfoWindowClick(Marker marker) {
                            Item item = markerItemHashMap.get(marker);
                            app.showItemPopup(activity, item);
                        }
                    });

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

    protected boolean isFinding()
    {
        return finder != null && finder.getStatus() != AsyncTask.Status.FINISHED;
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
        }

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

                                googleMap.clear();
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

    protected void zoomOnItems(GoogleMap googleMap, List<Item> items)
    {
        // Collect the LatLngs to zoom and pan the camera ideally
        LatLngBounds.Builder bc = new LatLngBounds.Builder();

        // As we don't want to zoom in to the max when all the items are at the
        // exact same position (usually when there is only one item), we also
        // include in the builder coordinates around the item, to ensure a minimal
        // level of zoom higher than the vendor's minimal level of zoom.
        // Note: our method will have artifacts around poles, but WHO CARES ?!
        double latPad = 180. / 30000;
        double lngPad = 360. / 60000;

        int itemsCount = items.size();
        for (int i=0; i<itemsCount; i++) {
            Item item = items.get(i);

            // Add coordinates to the boundaries builder
            bc.include(item.getLatLng());

            // And coordinates of our padding
            double lat = item.getLatitude();
            double lng = item.getLongitude();
            bc.include(new LatLng(lat+latPad, lng)); // north (or south)
            bc.include(new LatLng(lat-latPad, lng)); // south (or north)
            bc.include(new LatLng(lat, lng-lngPad)); // east (or west)
            bc.include(new LatLng(lat, lng+lngPad)); // west (or east)
        }

        // Pan and zoom the camera
        //CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bc.build(), 55);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 55));
    }


    private void showLoader()
    {
        mapItemsProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoader()
    {
        mapItemsProgressBar.setVisibility(View.GONE);
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
