package org.give2peer.karma.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.mikepenz.materialdrawer.Drawer;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.entity.Slide;
import com.shamanland.fab.FloatingActionButton;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.rest.spring.annotations.RestService;
import org.androidannotations.rest.spring.api.RestErrorHandler;
import org.give2peer.karma.Application;
import org.give2peer.karma.utils.GeometryUtils;
import org.give2peer.karma.utils.LatLngUtils;
import org.give2peer.karma.adapter.ItemInfoWindowAdapter;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.R;
import org.give2peer.karma.event.AuthenticationEvent;
import org.give2peer.karma.event.LocationUpdateEvent;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.response.FindItemsResponse;
import org.give2peer.karma.service.RestClient;
import org.give2peer.karma.service.RestExceptionHandler;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.springframework.core.NestedRuntimeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


/**
 * Handles sequentially the following asynchronous tasks
 * 1. Authenticating (and pre-registering if needed)
 * 2. Loading the map
 * 3. Locating
 *
 * I don't know what happens when we launch the Onboarding (it's another activity).
 * I know the pre-registration is done.
 */
@EActivity(R.layout.activity_map_items)
public class      MapItemsActivity
       extends    LocatorBaseActivity
       implements OnMapReadyCallback, RestErrorHandler
{
    @App
    Application app;

    GoogleMap googleMap;

    // Whether or not the user is currently drawing a region instead of moving around on the map.
    Boolean isDrawing = false;

    Boolean isLayoutReady = false;

    ArrayList<LatLng> drawingCoordinates = new ArrayList<>();

    /**
     * A set of displayed items on the map, to avoid duplicates when you make another request.
     * We also use this to avoid making redundant requests.
     */
    List<Item> displayedItems = new ArrayList<>();

    // Allows us to find items from their respective markers during map UI events
    HashMap<Marker, Item> markerItemHashMap = new HashMap<Marker, Item>();


    //// CONFIGURATION /////////////////////////////////////////////////////////////////////////////

    int lineColor = 0x88FF9800;
    int fillColor = 0x55FF9800;

    /**
     * @return the string descriptor of the location rationale message to display.
     */
    protected int getLocationRationale() {
        return R.string.dialog_find_items_location_rationale;
    }


    //// VIEWS /////////////////////////////////////////////////////////////////////////////////////

    @ViewById
    TextView             noInternetTextView;
    @ViewById
    ProgressBar          mapItemsProgressBar;
    @ViewById
    FloatingActionButton mapItemsFloatingActionButton;
    @ViewById
    FrameLayout          mapItemsDrawFrame;
    @ViewById
    Toolbar              mapItemsToolbar;


    //// LIFECYCLE /////////////////////////////////////////////////////////////////////////////////

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//    }

    @Override
    public void onStart() {
        super.onStart();
        if ( ! EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        cancelFinderTask();
        if (EventBus.getDefault().isRegistered(this))  EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If the user is not preregistered, let's do dis dudeez !
        // This fires an AuthenticationEvent through the EventBus.
        // The chain of events starts here ; not very good design, this.
        app.requireAuthentication(this);

        // In case this activity was not destroyed, set up the currently selected navigation item
        setUpNavigationDrawer();
    }

    @AfterViews
    public void readyLayout() {
        isLayoutReady = true; // to make sure, before we inject the map into our views
    }

    @Subscribe
    public void onAuthenticated(AuthenticationEvent authenticationEvent) {
        if (authenticationEvent.isFailure()) {
            hideLoader();
            noInternetTextView.setVisibility(View.VISIBLE);
            return;
        }

        if ( ! isMapReady()) {
            // I never had a failure there, but better safe than sorry !
            try {
                Log.i("G2P", "Loading the map...");
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapItemsFragment);
                mapFragment.getMapAsync(this);
            } catch (Exception e) {
                // todo: we should GTFO and launch the item list instead,
                app.toasty("Failed to load the map on this device. Sorry!\nPlease report this !");
                throw new CriticalException("Failed to load the map fragment.", e);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap _googleMap) {
        if (isMapReady()) {
            Log.d("G2P", "Map is ready AGAIN !? When does this ever happen ?");
            return;
        }

        googleMap = _googleMap;

        googleMap.moveCamera(CameraUpdateFactory.zoomTo(1));

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mapItemsFloatingActionButton.setVisibility(View.GONE);
                return false;
            }
        });

        googleMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
            @Override
            public void onInfoWindowClose(Marker marker) {
                mapItemsFloatingActionButton.setVisibility(View.VISIBLE);
            }
        });

        if ( ! isLocationReady()) {
            Log.d("G2P", "Trying to guess the location...");
            getLocation();
        }

        setupRegionDrawCanvas(_googleMap);
    }

    @Subscribe
    public void findItemsAroundWhenLocatedForTheFirstTime(LocationUpdateEvent locationUpdateEvent) {
        Location location = locationUpdateEvent.getLocation();
        // I've got to refactor all these logs...
        Log.d("G2P", String.format(
                "Location found : latitude=%f, longitude=%f",
                location.getLatitude(), location.getLongitude()
        ));
        // The map should be ready but it costs almost nothing to check again.
        if (isMapReady() && ! hasMapItemMarkers() && ! isFinding()) {
            executeFinderTask(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }


    // NAVIGATION DRAWER ///////////////////////////////////////////////////////////////////////////

    Drawer navigationDrawer;

    public Drawer getNavigationDrawer() {
        return navigationDrawer;
    }

    @AfterViews
    public void setUpNavigationDrawer() {
        long drawer = Application.NAVIGATION_DRAWER_ITEM_MAP;
        if (null != navigationDrawer) {
            navigationDrawer.setSelection(drawer);
            navigationDrawer.closeDrawer();
        } else {
            navigationDrawer = app.setUpNavigationDrawer(this, mapItemsToolbar, drawer);
        }
    }

    // OPTIONS MENU ////////////////////////////////////////////////////////////////////////////////

    // We'll have map filters over here.

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_map_items, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        boolean found = app.onOptionsItemSelected(item, this);
//        return found || super.onOptionsItemSelected(item);
//    }


    //// FAB ///////////////////////////////////////////////////////////////////////////////////////

    @Click
    public void mapItemsFloatingActionButtonClicked() {
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
            hideRegion();
        }

        updateFab();
    }

    protected void updateFab() {
        // 1. We are currently finding items, and this button is a CANCEL button.
        if (isFinding()) {
            mapItemsFloatingActionButton.setImageResource(R.drawable.ic_clear_white_24dp);
        }
        // 2. We are on the map, and we want to FIND
        else if (!isDrawing) {
            mapItemsFloatingActionButton.setImageResource(R.drawable.ic_search_white_24dp);
        }
        // 3. We just clicked on it and are drawing, show CANCEL
        else {
            mapItemsFloatingActionButton.setImageResource(R.drawable.ic_clear_white_24dp);
        }
    }

    protected void updateFabDelayed() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateFab();
            }
        }, 300);
    }


    //// ONBOARDING ////////////////////////////////////////////////////////////////////////////////

    @AfterViews
    public void loadOnboardingIfNeeded() {
        //app.isUserOnBoard(false);
        if (!app.isUserOnBoard()) {
            app.isUserOnBoard(true);
            loadOnboarding();
        }
    }

    // Does startActivityForResult(INTRODUCTION_REQUEST_CODE)
    // So we may also listen to the end of it.
    public void loadOnboarding() {
        new IntroductionBuilder(this).withSlides(getOnboardingSlides(this)).introduceMyself();
    }

    protected List<Slide> getOnboardingSlides(Context ctx) {
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


    // REST SERVICE ////////////////////////////////////////////////////////////////////////////////

    @RestService
    RestClient restClient;

    @AfterInject
    void setupRestClient() {
        restClient.setRootUrl(app.getCurrentServer().getUrl());
        restClient.setRestErrorHandler(this);
    }

    @Override
    @UiThread
    public void onRestClientExceptionThrown(NestedRuntimeException e) {
        new RestExceptionHandler(app, this).handleException(e);
    }


    //// ITEMS FINDER //////////////////////////////////////////////////////////////////////////////

//    protected boolean isReadyToFindItems() {
//        return isAuthenticated() && isLocationReady() && isMapReady();
//    }

    AsyncTask finder;

    public AsyncTask executeFinderTask(final LatLng where) {
        return executeFinderTask(where, null);
    }

    public AsyncTask executeFinderTask(final LatLng where, final List<LatLng> container) {
        // We only want one finder task to be run at a time.
        // We can either cancel the new task or the running one. We arbitrarily chose the latter.
        cancelFinderTask();

        showLoader(); // We'll hide the loader in the onPostExecute listener

        final Activity activity = this; // Scope shenanigans...

        finder = new AsyncTask<Void, Void, ArrayList<Item>>() {

            FindItemsResponse itemsResponse;

            @Override
            protected ArrayList<Item> doInBackground(Void... params) {

                double maxDistance = 1000 * 1000; // 1000km should be reasonable
                // do not filter by distance if we have drawn a region
                if (null != container) maxDistance = 0;

                int skip = 0; // for pagination, as the server returns at most 64 items

                itemsResponse = restClient.findItemsAround(
                        String.valueOf(where.latitude), String.valueOf(where.longitude),
                        String.valueOf(skip), String.valueOf(maxDistance)
                );

                // Let's filter the items in the background task, for responsiveness
                ArrayList<Item> items = new ArrayList<>();
                if (null != itemsResponse && null != itemsResponse.getItems()) {
                    // Remove items outside of container polygon if specified
                    if (null != container) {
                        for (Item item : itemsResponse.getItems()) {
                            if (GeometryUtils.pointInPolygon(item.getLatLng(), container)) {
                                items.add(item);
                            } // else ignore the item
                        }
                    }
                    // ... or add them all indiscriminately
                    else {
                        Collections.addAll(items, itemsResponse.getItems());
                    }
                }

                // Add to the cache
                displayedItems.clear();
                displayedItems.addAll(items);

                return items;
            }

            @Override
            protected void onPostExecute(ArrayList<Item> items) {
                super.onPostExecute(items);

                hideLoader(); // whether there was an error or not

                // Maybe the request failed for any number of reasons
                if (null == itemsResponse) {
                    hideRegion();
                    updateFabDelayed();
                    return;
                }

                // Now, either we found items or we didn't
                int itemsCount = items.size();
                if (0 == itemsCount) {
                    app.toasty(getString(R.string.toast_no_items_found_in_area));
                } else {
                    boolean should_animate = app.getPrefs() // the user chooses, obviously
                            .getBoolean(getString(R.string.settings_pins_animated), false);

                    Marker firstMarker = null;

                    // Add markers to the map
                    for (int i = 0; i < itemsCount; i++) {
                        Item item = items.get(i);

                        Marker m = googleMap.addMarker(
                                new MarkerOptions()
                                        .position(item.getLatLng())
                                        .title(item.getHumanTitle(activity))
                                        .snippet(item.getHumanUpdatedAt())
                                        .icon(item.getMapMarkerIcon())
                                        .anchor(item.getMapMarkerU(), item.getMapMarkerV())
                        );

                        if (should_animate) {
                            dropPinEffect(m, Math.round(i * 222),
                                    item.getMapMarkerU(), item.getMapMarkerV()
                            );
                        }

                        // We also map the markers to the items for the click callback
                        markerItemHashMap.put(m, item);

                        // Store the first marker added for later usage (showing its info window)
                        if (0 == i) {
                            firstMarker = m;
                        }
                    }

                    zoomOnItems(googleMap, items); // smooth

                    /////////////
                    // Anything that needs an up-to-date markerItemHashMap needs to be set AGAIN
                    // below. Don't try to be clever and move this code to the mapReady listener.
                    // Also, we may or may not be leaking memory, the way things are implemented.

                    googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                        @Override
                        public void onInfoWindowClick(Marker marker) {
                            Item item = markerItemHashMap.get(marker);
                            app.launchViewItem(activity, item);
                        }
                    });

                    googleMap.setInfoWindowAdapter(
                            new ItemInfoWindowAdapter(activity, markerItemHashMap)
                    );

                    /////////////

                    // Show the info window of the marker when it's alone
                    // We need to do this _after_ we've set our custom info window adapter.
                    // We don't show the info window when there are multiple markers because we have
                    // no way of ensuring that the info window is not truncated by the screen's edge
                    if (null != firstMarker && 1 == itemsCount) firstMarker.showInfoWindow();

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
                updateFabDelayed();
            }

        }.execute();

        updateFab();

        return finder;
    }

    /**
     * Cancel the current subtask of finding items.
     */
    public void cancelFinderTask() {
        if (finder != null) {
            finder.cancel(true);
            finder = null;
        }
        hideLoader();
        updateFab();
    }

    protected boolean isFinding() {
        return finder != null && finder.getStatus() != AsyncTask.Status.FINISHED;
    }


    // LOADER //////////////////////////////////////////////////////////////////////////////////////

    private void showLoader() {
        mapItemsProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoader() {
        mapItemsProgressBar.setVisibility(View.GONE);
    }


    // MAP /////////////////////////////////////////////////////////////////////////////////////////

    protected boolean isMapReady() {
        return googleMap != null && isLayoutReady;
    }

    protected boolean hasMapItemMarkers() {
        return !displayedItems.isEmpty();
    }

    /**
     * Sets up the UI listeners for the region-drawing transparent canvas.
     *
     * @param googleMap A GoogleMap object that should be ready.
     */
    private void setupRegionDrawCanvas(final GoogleMap googleMap) {
        mapItemsDrawFrame.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if ( ! isDrawing) return false;

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

                                // distanceBetween snippet, interesting. Also, circles ?
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

                                LatLng centroid = LatLngUtils.getLatLngCentroid(drawingCoordinates);
                                if (centroid != null) {
                                    // We need to make a copy of our drawn path, as we may clear it
                                    // at any time.
                                    List<LatLng> container = new ArrayList<LatLng>();
                                    container.addAll(drawingCoordinates);
                                    executeFinderTask(centroid, container);
                                }

                                // Zoom and pan the camera ideally around the drawn area
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                                        LatLngUtils.getLatLngBounds(drawingCoordinates), 55
                                ));

                                break;
                        }

                        return true;
                    }
                }
        );

    }

    /**
     * Zooms and pans the `googleMap` to encompass all the `items`.
     * Note: this method will have artifacts around poles, but WHO CARES ?!
     *
     * @param googleMap to zoom.
     * @param items     that should be visible on the map.
     */
    protected void zoomOnItems(GoogleMap googleMap, List<Item> items) {
        // Collect the LatLngs to zoom and pan the camera ideally
        LatLngBounds.Builder bc = new LatLngBounds.Builder();

        // As we don't want to zoom in to the max when all the items are at the
        // exact same position (usually when there is only one item), we also
        // include in the builder coordinates around the item, to ensure a minimal
        // level of zoom higher than the vendor's minimal level of zoom.
        // This is because multiple items can have the exact same coordinates so
        // we cannot rely on their numbers alone, which would yield clearer code.
        double latPad = 180. / 30000;
        double lngPad = 360. / 60000;

        int itemsCount = items.size();
        for (int i = 0; i < itemsCount; i++) {
            Item item = items.get(i);

            // Add coordinates to the boundaries builder
            bc.include(item.getLatLng());

            // And coordinates of our padding
            double lat = item.getLatitude();
            double lng = item.getLongitude();
            bc.include(new LatLng(lat + latPad, lng)); // north (or south)
            bc.include(new LatLng(lat - latPad, lng)); // south (or north)
            bc.include(new LatLng(lat, lng - lngPad)); // east (or west)
            bc.include(new LatLng(lat, lng + lngPad)); // west (or east)
        }

        // Pan and zoom the camera
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 111));
    }

    /**
     * Animate the placement of the markers on the map with a bouncing drop effect.
     * The duration of the drop is hardcoded to 1.5s.
     *
     * @param marker to animate
     * @param delay in milliseconds of the effect (not the duration!)
     * @param u Anchor U (between 0 and 1, origin is top left)
     * @param v Anchor V (between 0 and 1, origin is top left)
     */
    private void dropPinEffect(final Marker marker, long delay, final float u, final float v) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis() + delay;
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();

        marker.setAnchor(42, 42); // effectively hides the marker

        handler.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = Math.max(
                        1 - interpolator.getInterpolation( (float) elapsed / duration ),
                        0
                );
                marker.setAnchor(u, v + 14 * t);

                if (t > 0.0) {
                    // The animation continues, post again 15ms later
                    handler.postDelayed(this, 15);
                } else {
                    // The animation has ended
                    //marker.showInfoWindow();
                }
            }
        } ,delay);
    }

    Polygon  drawnPolygon;
    Polyline drawnPolyline;
    Circle   drawnCircle;

    public void hideRegion() {
        if (drawnPolygon != null) drawnPolygon.remove();
    }

    public void drawPolygonOnMap(GoogleMap googleMap, Iterable<LatLng> polygonLatLngs) {
        if (drawnPolygon != null) drawnPolygon.remove();
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
        options.color(lineColor);
        options.width(7);

        drawnPolyline = googleMap.addPolyline(options);
    }

    public void drawCircleOnMap(GoogleMap googleMap, LatLng center, double radius) {
        if (drawnCircle != null) drawnCircle.remove();

        CircleOptions options = new CircleOptions();
        options.center(center);
        options.radius(radius);
        options.strokeColor(lineColor);
        options.fillColor(fillColor);
        options.strokeWidth(7);

        drawnCircle = googleMap.addCircle(options);
    }

}
