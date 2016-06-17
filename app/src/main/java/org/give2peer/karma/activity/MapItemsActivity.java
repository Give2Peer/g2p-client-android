package org.give2peer.karma.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.entity.Slide;
import com.shamanland.fab.FloatingActionButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.give2peer.karma.Application;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.R;
import org.give2peer.karma.event.AuthenticationEvent;
import org.give2peer.karma.event.LocationUpdateEvent;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.response.FindItemsResponse;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
 * I know the pre-registration is done
 */
@EActivity(R.layout.activity_map_items)
public class      MapItemsActivity
       extends    LocatorBaseActivity
       implements OnMapReadyCallback
{
    Application app;

    GoogleMap googleMap;

    // Whether or not the user is currently drawing a region instead of moving around on the map.
    Boolean isDrawing = false;

    Boolean isLayoutReady = false;

    ArrayList<LatLng> drawingCoordinates = new ArrayList<>();

    /**
     * A set of displayed items on the map, to avoid duplicates when you make another request.
     * We also use this to avoid making redundant requests.
     * Use markerItemHashMap instead.
     */
    @Deprecated
    List<Item> displayedItems = new ArrayList<>();

    // Allows us to find items from their respective markers during map UI events
    HashMap<Marker, Item> markerItemHashMap = new HashMap<Marker, Item>();


    //// CONFIGURATION /////////////////////////////////////////////////////////////////////////////

    int lineColor = 0x88FF3399;
    int fillColor = 0x33FF3399;
    int fillAlpha = 0x55000000;

    /**
     * @return the string descriptor of the location rationale message to display.
     */
    protected int getLocationRationale() {
        return R.string.dialog_find_items_location_rationale;
    }


    //// VIEWS /////////////////////////////////////////////////////////////////////////////////////

    @ViewById
    TextView noInternetTextView; // time to start refactoring the "no internet" flow for activities!

    @ViewById
    ProgressBar mapItemsProgressBar;

    @ViewById
    FloatingActionButton mapItemsFloatingActionButton;

    @ViewById
    FrameLayout mapItemsDrawFrame;

    @ViewById
    Toolbar mapItemsToolbar;


    //// LIFECYCLE /////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (Application) getApplication();
        loadOnboardingIfNeeded();
    }

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

//        if (isMapReady()) {
//            if (hasMapItemMarkers()) {
//                // Sometimes the map fragment loses the zoom level, let's try to fix that.
//                Log.d("G2P", "Zooming in on items when resuming map activity.");
//                zoomOnItems(googleMap, displayedItems);
//            } else {
//                // Map is empty, maybe we just activated the GPS or Internet, so try again
//                //getLocation();
//            }
//        }

    }

    /**
     * todo: ensure this is necessary (it might very well not be)
     */
    @AfterViews
    public void readyLayout() {
        isLayoutReady = true;
    }

    Drawer drawer;

    /**
     * MAP
     * ADD
     * PROFILE
     * LEADERBOARD
     */
    @AfterViews
    public void setUpNavigationDrawer() {

        final Activity activity = this;

        setSupportActionBar(mapItemsToolbar);

        PrimaryDrawerItem mapDrawerItem = new PrimaryDrawerItem()
                .withName(R.string.menu_action_map)
                .withIcon(R.drawable.ic_map_black_36dp)
                .withIconTintingEnabled(true)
                ;

        PrimaryDrawerItem profileDrawerItem = new PrimaryDrawerItem()
                .withName(R.string.menu_action_profile)
                .withIcon(R.drawable.ic_perm_identity_black_36dp)
                .withIconTintingEnabled(true);

        SecondaryDrawerItem addDrawerItem = (SecondaryDrawerItem) new SecondaryDrawerItem()
                .withName(R.string.menu_action_add_item);

        SecondaryDrawerItem settingsDrawerItem = (SecondaryDrawerItem) new SecondaryDrawerItem()
                .withName(R.string.menu_action_settings);



        DrawerBuilder drawerBuilder = new DrawerBuilder().withActivity(this)
                .withToolbar(mapItemsToolbar)
                .addDrawerItems(
                        mapDrawerItem,
                        profileDrawerItem,
                        addDrawerItem,
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(R.string.menu_action_settings).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                            @Override
                            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                                app.launchSettings(activity);
                                return true;
                            }
                        })
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        return false; // consumed the event ?
                    }
                })
//                .inflateMenu(R.menu.drawer)
                ;



        drawer = drawerBuilder.build();


    }

//    @AfterViews
//    public void authenticate()
//    {
//        // onStart() is sometimes called AFTER this method, and so nobody listens to
//        // AuthenticationEvent yet, so we need to register to the EventBus here too.
//        if ( ! EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
//
//        // If the user is not authenticated, take care of it
//        app.requireAuthentication(this);
//    }
//
//    @AfterViews
//    public void requestGpsEnabled()
//    {
//        super.requestGpsEnabled(this);
//    }

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


    // OPTIONS MENU ////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean found = app.onOptionsItemSelected(item, this);
        return found || super.onOptionsItemSelected(item);
    }


    //// UI LISTENERS //////////////////////////////////////////////////////////////////////////////

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

        updateDrawButton();
    }


    //// ONBOARDING ////////////////////////////////////////////////////////////////////////////////

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

    //// ACTIONS ///////////////////////////////////////////////////////////////////////////////////

    protected void updateDrawButton() {
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

    protected void updateDrawButtonDelayed() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDrawButton();
            }
        }, 300);
    }


//    protected boolean isReadyToFindItems() {
//        return isAuthenticated() && isLocationReady() && isMapReady();
//    }

    AsyncTask finder;

    public AsyncTask executeFinderTask(final LatLng where) {
        return executeFinderTask(where, null);
    }

    public AsyncTask executeFinderTask(final LatLng where, final List<LatLng> container) {
        cancelFinderTask();
        showLoader();

        final Activity activity = this;

        finder = new AsyncTask<Void, Void, ArrayList<Item>>() {
            Exception exception;

            @Override
            protected ArrayList<Item> doInBackground(Void... params) {
                FindItemsResponse itemsResponse = new FindItemsResponse();
                try {
                    itemsResponse = app.getRestService().findAround(where.latitude, where.longitude);
                } catch (Exception e) {
                    exception = e;
                }

                ArrayList<Item> items = new ArrayList<Item>();
                if (null != itemsResponse.getItems()) {
                    Collections.addAll(items, itemsResponse.getItems());
                }

                ArrayList<Item> filteredItems = new ArrayList<Item>();
                filteredItems.addAll(items);

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
            protected void onPostExecute(ArrayList<Item> items) {
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

                    // idea : Use a custom InfoWindowAdapter to add an image ?
                    //        but there are lots of caveats with this canvas drawing technique !

                    // Add markers to the map
                    for (int i = 0; i < itemsCount; i++) {
                        Item item = items.get(i);

                        String title = item.getTitle();
                        // When the title is empty the marker does not show the info window at all.
                        // We want it to show up, so let's provide an alternative title !
                        if (title.isEmpty()) {
                            title = "MOOP";
                        }
                        String snippet = item.getHumanUpdatedAt();

                        Marker m = googleMap.addMarker(
                                new MarkerOptions()
                                        .position(item.getLatLng())
                                        .title(title)
                                        .snippet(snippet)
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
    public void cancelFinderTask() {
        if (finder != null) {
            finder.cancel(true);
            finder = null;
        }
        hideLoader();
        updateDrawButton();
    }

    protected boolean isFinding() {
        return finder != null && finder.getStatus() != AsyncTask.Status.FINISHED;
    }

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

    /**
     * Zooms and pans the `googleMap` to encompass all the `items`.
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
        // Note: our method will have artifacts around poles, but WHO CARES ?!
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
        //CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bc.build(), 55);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 55));
    }


    private void showLoader() {
        mapItemsProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoader() {
        mapItemsProgressBar.setVisibility(View.GONE);
    }

    private void dropPinEffect(final Marker marker, long delay) {
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
                        1 - interpolator.getInterpolation( (float) elapsed / duration ),
                        0
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
        options.color((lineColor & 0x00FFFFFF) + fillAlpha);
        options.width(7);

        drawnPolyline = googleMap.addPolyline(options);
    }

    public void drawCircleOnMap(GoogleMap googleMap, LatLng center, double radius) {
        if (drawnCircle != null) drawnCircle.remove();

        CircleOptions options = new CircleOptions();
        options.center(center);
        options.radius(radius);
        options.strokeColor(lineColor);
        options.fillColor((lineColor & 0x00FFFFFF) + fillAlpha);
        options.strokeWidth(7);

        drawnCircle = googleMap.addCircle(options);
    }

    protected LatLngBounds getLatLngBounds(List<LatLng> latLngs) {
        LatLngBounds.Builder bc = new LatLngBounds.Builder();
        for (LatLng latLng : latLngs) { bc.include(latLng); }

        return bc.build();
    }

    protected LatLng getLatLngCentroid(List<LatLng> latLngs) {
        if (latLngs.size() == 0) return null;

        return getLatLngBounds(latLngs).getCenter();
    }

    //// MATH UTILS (SHOULD BE MOVED ELSEWHERE) ////////////////////////////////////////////////////

    /**
     * Ray casting algorithm, see http://rosettacode.org/wiki/Ray-casting_algorithm
     *
     * @param point
     * @param polygon The list of the vertices of the polygon, sequential and looping.
     * @return whether the point is inside the polygon
     */
    public boolean pointInPolygon(LatLng point, List<LatLng> polygon) {
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
     * 1) to the left of the segment and
     * 2) not above nor below the segment.
     *
     * @param point
     * @param a
     * @param b
     */
    public boolean rayCrossesSegment(LatLng point, LatLng a, LatLng b) {
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
