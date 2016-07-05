package org.give2peer.karma.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.rsv.widget.WebImageView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.give2peer.karma.Application;
import org.give2peer.karma.GeoUtils;
import org.give2peer.karma.R;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.event.LocationUpdateEvent;
import org.give2peer.karma.exception.CriticalException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;


/**
 * Actions :
 * - Show directions
 * - Report abuse
 * - Thank author
 * - Edit item
 * - Delete item
 */
@EActivity(R.layout.activity_view_item)
public class ViewItemActivity
     extends LocatorBaseActivity
  implements OnMapReadyCallback
{
    Application app;
    Item item;

    //// VIEWS /////////////////////////////////////////////////////////////////////////////////////

    // What would the FAB be ?
//    @ViewById
//    FloatingActionButton viewItemSendButton;

    @ViewById
    WebImageView viewItemImageView;
    @ViewById
    TextView viewItemTitleTextView;
    @ViewById
    TextView viewItemDescriptionTextView;
    @ViewById
    TextView viewItemAuthorshipTextView;

    @ViewById
    NestedScrollView viewItemFormScrollView;
    @ViewById
    RelativeLayout   viewItemMapWrapper;
    @ViewById
    RelativeLayout   viewItemImageWrapper;




    //// LIFECYCLE LISTENERS ///////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("G2P", "Starting new item activity.");
        app = (Application) getApplication();

        try {
            item = (Item) savedInstanceState.get("item");
        } catch (Exception e) {
            Log.d("G2P", "No item was passed in the saved instance bundle.");
        }

        if (null == item) {
            item = getIntent().getParcelableExtra("item");
        }

        if (null == item) {
            throw new CriticalException("No item was passed to the view item activity.");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if ( ! EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        if (EventBus.getDefault().isRegistered(this))  EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If the user is not preregistered, let's do this dudeez !
        // fixme: move auth to a base class ?
        app.requireAuthentication(this);

        // We never know, maybe the map and location are ready already ?
        updateMap();
    }

    @Subscribe
    public void updateMapWhenLocated(LocationUpdateEvent locationUpdateEvent) {
        //Location location = locationUpdateEvent.getLocation();
        updateMap();
    }

    //// AFTER VIEWS ///////////////////////////////////////////////////////////////////////////////

    /**
     * Hiding the action bar that way works on APi 10 ! \o/ \o/
     */
    @AfterViews
    public void hideActionBar() {
        ActionBar ab = getSupportActionBar();
        if (null != ab) ab.hide();
    }


    @AfterViews
    public void fillLayoutWithItem() {
        viewItemImageView.setWebImageUrl(item.getThumbnailNoSsl());
        viewItemTitleTextView.setText(item.getHumanTitle(this));
        if (item.hasDescription()) {
            viewItemDescriptionTextView.setText(item.getDescription());
            viewItemDescriptionTextView.setVisibility(View.VISIBLE);
        }
        viewItemAuthorshipTextView.setText(getString(R.string.time_ago_by_someone,
                item.getHumanUpdatedAt(), item.getAuthor().getPrettyUsername()
        ));
    }


    @AfterViews
    public void resizeCollapsingSection() {
        // We want the collapsing section to fit the whole screen height minus a fixed height.
        // We want it to work on all devices, on both orientations. hence, we set it that way.

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int pxHeight = displayMetrics.heightPixels;
        int pxWidth = displayMetrics.widthPixels;
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        //Log.i("G2P", String.format("Display Metrics : %.1fdp x %.1fdp", dpWidth, dpHeight));
        //Log.i("G2P", String.format("Display Metrics : %dpx x %dpx", pxWidth, pxHeight));
        // Nexus S portrait : Display Metrics : 360.0dp x 592.0dp
        //                    Display Metrics : 1080px x 1776px

        int newHeight = pxHeight - app.dpi2pix(108);

        viewItemMapWrapper.getLayoutParams().height = newHeight;
        viewItemImageWrapper.getLayoutParams().height = newHeight;
    }



    //// ITEM LOCATION ON MAP //////////////////////////////////////////////////////////////////////

    GoogleMap googleMap;
    Marker    itemLocationMarker;

    @AfterViews
    public void loadMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.viewItemMapFragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        if ( ! isLocationReady()) {
            Log.d("G2P", "Trying to guess the location...");
            getLocation();
        }

        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                // Make sure we can scroll on the map and not on the scrollable view
                AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.viewItemAppBarLayout);
                CoordinatorLayout.LayoutParams params =
                        (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
                AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
                behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                    @Override
                    public boolean canDrag(@NonNull AppBarLayout appBarLayout) { return false; }
                });
                // ... and also flag the map as loaded
                mapLoaded = true;
                // ... so then we also need to try to update the map, whatever.
                updateMap();
            }
        });

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);

        updateMap();
    }

    protected boolean isMapReady() {
        return null != googleMap;
    }

    boolean mapLoaded = false;

    /**
     * If we don't check this, `newLatLngBounds` will fail.
     */
    protected boolean isMapLoaded() {
        return mapLoaded;
    }

    public LatLng getLatLng() {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    protected void updateMap() {
        if ( ! isMapReady() || ! isLocationReady() || ! isMapLoaded()) {
            return;
        }

        // Even with the check above, the following must be safe to run multiple times,
        // because I think it happens in some lifecycle cases. I suck at android :|
        Log.d("G2P", "Updating the view item location map..."); // let's see !

        // Let's clear the map
        googleMap.clear();

        // and zoom on the user position and item
        ArrayList<LatLng> latlngs = new ArrayList<>();

        latlngs.add(GeoUtils.locationToLatLng(location));
        latlngs.add(item.getLatLng());

        // Zoom and pan the camera ideally around the drawn area
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                GeoUtils.getLatLngBounds(latlngs), 56
        ));

        // Let's put a marker on the map where the item is
        itemLocationMarker = googleMap.addMarker(new MarkerOptions()
                .position(item.getLatLng())
        );

        // We don't need to ask for permission again, we already did while creating the activity.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);

            googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    getLocation();
                    return false; // don't consume the event, let the camera zoom on my location
                }
            });
        }
    }


    //// ACTIONS ///////////////////////////////////////////////////////////////////////////////////




    //// UI ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Shows the picture in the collapsing app bar instead of the map.
     * Does nothing if the picture is already shown.
     */
    protected void showPicture()
    {
        viewItemMapWrapper.setVisibility(View.GONE);
        viewItemImageWrapper.setVisibility(View.VISIBLE);
    }
    /**
     * Shows the map in the collapsing app bar instead of the picture.
     * Does nothing if the map is already shown.
     */
    protected void showMap()
    {
        viewItemImageWrapper.setVisibility(View.GONE);
        viewItemMapWrapper.setVisibility(View.VISIBLE);
    }

    @Click
    public void viewItemShowPicButtonClicked() { showPicture(); }

    @Click
    public void viewItemShowMapButtonClicked() {
        if ( ! isLocationReady()) { getLocation(); }
        showMap();
    }

    //// UTILS /////////////////////////////////////////////////////////////////////////////////////

}
