package org.give2peer.karma.activity;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.shamanland.fab.FloatingActionButton;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.give2peer.karma.Application;
import org.give2peer.karma.response.CreateItemResponse;
import org.give2peer.karma.response.PictureItemBeforehandResponse;
import org.give2peer.karma.response.PictureItemResponse;
import org.give2peer.karma.utils.FileUtils;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.R;
import org.give2peer.karma.event.LocationUpdateEvent;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.factory.BitmapFactory;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * This is where the user adds new items in the database.
 *
 * Handles :
 * - Receiving an image from another activity's share intent.
 * - Launching the camera otherwise.
 * - Rotating the received image before sending it.
 * - A form to add a new item, with a nice Floating Action Button.
 *
 */
@EActivity(R.layout.activity_new_item)
public  class      NewItemActivity
        extends    LocatorBaseActivity
        implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener,
                   ActivityCompat.OnRequestPermissionsResultCallback
{
    static final int REQUEST_CODE_IMAGE_CAPTURE = 1;
    static final int REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION = 2;

    /**
     * Stores the image files paths, and is bundled so that it survives the Camera activity.
     * Those are paths to local files that we created ourselves,
     * and therefore are safe to rotate, resize, crop, and delete.
     * Notes:
     *     - These images are (at least, should be) all JPG images.
     *     - We only use one (1) image for now, the last one of this array.
     */
    protected ArrayList<String> imagePaths;
    static final String BUNDLE_IMAGE_PATHS = "imagePaths";

    /**
     * Amount of rotation in degrees the user wants to apply to its image before sending it.
     * Increasing this value results in a clockwise rotation.
     * This will become an array when we'll have multiple images.
     * Right now the user can set the rotation by clicking on the image.
     */
    protected int imageRotation = 0;

    /**
     * Ids of the pictures we pre-uploaded before submitting.
     */
    protected List<String> pictureIds = new ArrayList<>();


    //// VIEWS /////////////////////////////////////////////////////////////////////////////////////

    @App
    Application app;

    @ViewById
    FloatingActionButton newItemSendButton;
    @ViewById
    ProgressBar newItemProgressBar;
    @ViewById
    ImageView   newItemImageView;
    @ViewById
    EditText    newItemTitleEditText;
    @ViewById
    EditText    newItemDescriptionEditText;
    @ViewById
    EditText    newItemLocationEditText;
    @ViewById
    RadioButton newItemGiftRadioButton;
    @ViewById
    RadioButton newItemLostRadioButton;
    @ViewById
    RadioButton newItemMoopRadioButton;

    @ViewById
    NestedScrollView newItemFormScrollView;
    @ViewById
    RelativeLayout   newItemMapWrapper;
    @ViewById
    RelativeLayout   newItemImageWrapper;


    //// LIFECYCLE LISTENERS ///////////////////////////////////////////////////////////////////////

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // On some devices, the Camera activity destroys this activity, so we need to save the
        // paths of the files we created before launching the camera, so we can restore them after.
        // This happens only when this activity is launched without a shared image.
        outState.putStringArrayList(BUNDLE_IMAGE_PATHS, imagePaths);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("G2P", "Starting new item activity.");

        // For when we launched the Camera Activity FROM Karma (and are not using the share...),
        // and the Camera activity has destroyed this activity, so we need to restore the
        // paths of the files we created before we launched the Camera.
        if (null != savedInstanceState) {
            imagePaths = savedInstanceState.getStringArrayList(BUNDLE_IMAGE_PATHS);
        }

        // Only initialize the imagePaths if they have not been restored from bundle state.
        if (null == imagePaths) {
            imagePaths = new ArrayList<>();
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
        app.requireAuthentication(this);
        // We never know, maybe the map and location are ready already ?
        updateMap();
    }

    private class OnAccessPermissionsCallback {
        void onGranted() {}
        void onDenied() {
            Log.i("G2P", "Permission to access external storage denied… Why, you paranoid clod?");
            app.toasty(getString(R.string.toast_permission_read_denied));
            finish();
        }
    }

    protected OnAccessPermissionsCallback accessPermissionsCallback;

    /**
     * This is only useful for Android Marshmallow and above, as permissions like these are given
     * on-the-fly and not on app install like in older android flavors.
     */
    protected void askForAccessPermissions(OnAccessPermissionsCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            callback.onGranted();
            return;
        }

        accessPermissionsCallback = callback;

        // Let's ask for permissions if they're not already granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
        {
            Log.i("G2P", String.format(
                    "Not enough permissions ! read=%d / write=%d",
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE),
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ));

            // This is true when the user has denied the permissions once
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                final Activity activity = this;
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(getString(R.string.permissions_rw_title))
                        .setMessage(getString(R.string.permissions_rw_message))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                // beware, this is run on UI thread
                                ActivityCompat.requestPermissions(
                                        activity,
                                        new String[] {
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                        },
                                        REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION
                                );
                            }
                        })
                        .create()
                        .show();

            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION
                );
            }
        } else {
            callback.onGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (grantResults.length == 0) {
            // In some rare cases, this might happen ; consider permissions denied
            Log.d("G2P", "onRequestPermissionsResult with no permissions.");
            if (null != accessPermissionsCallback) {
                accessPermissionsCallback.onDenied();
            }
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION:
                Log.d("G2P", String.format("Grants: %d %d", grantResults[0], grantResults[1]));
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (null != accessPermissionsCallback) {
                        accessPermissionsCallback.onGranted();
                    }
                    fillThumbnail(); // todo: ensure we still need this
                } else {
                    if (null != accessPermissionsCallback) {
                        accessPermissionsCallback.onDenied();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

//    protected void onAccessPermissionsDenied() {
//        Log.i("G2P", "Permission to access external storage denied... Why, you paranoid clod ?");
//        app.toasty(getString(R.string.toast_permission_read_denied));
//        finish();
//    }

    /**
     * The Camera and Gallery activities will provide the images to this activity in this method,
     * if we use requestNewPicture().
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_CODE_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Put the bitmap in the View to show the user
            fillThumbnail();
        } else if (requestCode == REQUEST_CODE_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            // If the user cancelled the capture of a picture, we GTFO.
            // It may be nice to allow a user to add a picture from a gallery instead of taking one.
            // see http://stackoverflow.com/questions/20021431/android-how-to-take-a-picture-from-camera-or-gallery
            Log.d("G2P", "User cancelled the image capture.");
            finish();
        }
    }

    @Subscribe
    public void updateMapWhenLocated(LocationUpdateEvent locationUpdateEvent) {
        //Location location = locationUpdateEvent.getLocation();
        // Successfully located device : we hint to the user that the Location field is optional.
        // Note that this field is not shown anymore, but is still used in the "form".
        newItemLocationEditText.setHint(R.string.new_item_label_location_optional);
        // Now, we update the map (it will fetch itself the relevant location)
        updateMap();
    }


    //// AFTER VIEWS ///////////////////////////////////////////////////////////////////////////////

    /**
     * Hiding the action bar that way works on APi 10 ! \o/
     */
    @AfterViews
    public void hideActionBar() {
        ActionBar ab = getSupportActionBar();
        if (null != ab) ab.hide();
    }

    /**
     * Recover the image from the share or the camera, but ask for permissions first !
     */
    @AfterViews
    public void recoverImage() {
        // We need to do that as this is run before onResume
        askForAccessPermissions(new OnAccessPermissionsCallback() {
            @Override
            void onGranted() {
                doRecoverImage();
            }
        });
    }


    //// IMAGE RECOVERY ////////////////////////////////////////////////////////////////////////////

    /**
     * This is to recover the image from the Intent, or launch the Camera to pick one if we
     * arrived on this activity by other means (ie: the Intent is empty).
     */
    public void doRecoverImage() {
        // This activity may have been destroyed by the Camera activity.
        // If it's the case, the imagePaths is not null, as we saved it.
        // Only initialize it if it has not been restored from bundle state.
        if (null == imagePaths) {
            imagePaths = new ArrayList<>();
        }

        // Handle external images sent to this app by the "share with" feature
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (action != null && action.equals(Intent.ACTION_SEND) && type != null) {
            if (type.startsWith("image/")) {
                // Handle a single image being sent
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                Log.d("G2P", "Add new item with shared image URl `" + imageUri.toString() + "`.");
                String imagePathOfUser = getPathFromImageURI(imageUri);
                Log.d("G2P", "... which translates into path `" + imagePathOfUser + "`.");
                // Let's copy the received image to a local JPG image path created just for it.
                // That way, we can crop and resize and screw it up at will, and even delete it.
                processUserSharedImage(imagePathOfUser);
            } else {
                // The intent filter in the manifest should ensure that we never EVER throw this.
                throw new CriticalException("You shared something that is not an image. Don't.");
            }
//        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
//            if (type.startsWith("image/")) {
//                handleSendMultipleImages(intent); // Handle multiple images being sent
//            }
        } else {
            // Directly try to grab a new image if there are no files paths stored.
            // Otherwise, it means that `onActivityResult` will be called..
            if (imagePaths.size() > 0) {
                fillThumbnail();
            } else {
                try {
                    // Check if there's a camera available
                    // todo: propose the gallery picker?
                    if ( ! app.hasCameraSupport()) {
                        app.toast(getString(R.string.toast_no_camera_available));
                        finish();
                        return;
                    }
                    // Launch the camera to add a new picture
                    requestNewPicture();

                } catch (CriticalException ex) {
                    Log.e("G2P", "Failed to add a new picture.");
                    ex.printStackTrace();
                    app.toasty(getString(R.string.toast_new_item_file_error));
                    finish();
                }
            }
        }
    }

    /**
     * Copy the provided image to a local JPG image path created just for it.
     * We also update the thumbnail view afterwards (in the UI thread, of course).
     *
     * @param imagePathOfUser path to an image provided by the user that we should not alter
     */
    @Background(serial="send")
    void processUserSharedImage(String imagePathOfUser) {
        File imageFileTmp = createImageFile();
        String imagePathTmp = imageFileTmp.getAbsolutePath();
        FileUtils.convertToJpg(imagePathOfUser, imagePathTmp);
        imagePaths.add(imagePathTmp);
        fillThumbnail();
    }


    //// ITEM LOCATION ON MAP //////////////////////////////////////////////////////////////////////

    GoogleMap googleMap;
    Marker    itemLocationMarker;

    @AfterViews
    public void loadMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.newItemMapFragment);
        mapFragment.getMapAsync(this);
    }

    @AfterViews
    public void resizeCollapsingMapSection() {
        // We want the collapsing section to fit the whole screen height minus a fixed height.
        // We want it to work on all devices, on both orientations. hence, we set it that way.
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        newItemMapWrapper.getLayoutParams().height = displayMetrics.heightPixels - app.dpi2pix(108);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        if ( ! isLocationReady()) {
            Log.d("G2P", "Trying to guess the location...");
            getLocation();
        }

        // Make sure we can scroll on the map and not on the scrollable view
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.newItemAppBarLayout);
                CoordinatorLayout.LayoutParams params =
                        (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
                AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
                behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                    @Override
                    public boolean canDrag(@NonNull AppBarLayout appBarLayout) { return false; }
                });
            }
        });

        googleMap.setOnMapLongClickListener(this);

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);

        updateMap();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (null == itemLocationMarker) {
            itemLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .draggable(true)
            );

            // We NEED to set the drag listener here too, oddly
            googleMap.setOnMarkerDragListener(makeMapMarkerDragListener());
        }
    }

    protected boolean isMapReady() {
        return null != googleMap;
    }

//    protected boolean isLocated() {
//        return null != location;
//    }

    public LatLng getLatLng() {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    protected void updateMap() {
        if ( ! isMapReady() || ! isLocationReady()) {
            return;
        }

        // Even with the check above, the following must be safe to run multiple times,
        // because I think it happens in some lifecycle cases. I suck at android :|
        Log.d("G2P", "Updating the new item location map..."); // let's see !

        // Clear the map and zoom on the user position
        googleMap.clear();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(getLatLng(), 16));

        // Put a marker on the map, if any, so that the user may drag it around if they want
        if (null == itemLocationMarker) {
            itemLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(getLatLng())
                    .draggable(true)
            );
        }

        googleMap.setOnMarkerDragListener(makeMapMarkerDragListener());

        // We don't need to ask for permission again, we already did while creating the activity.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);

            googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    getLocation();
                    itemLocationMarker.setPosition(getLatLng());
                    return false; // don't consume the event, let the camera zoom on my location
                }
            });
        }
    }

    GoogleMap.OnMarkerDragListener makeMapMarkerDragListener() {
        return new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}
            @Override
            public void onMarkerDrag(Marker marker) {}
            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng c = marker.getPosition();
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(c));
                newItemLocationEditText.setText(String.format(
                        Locale.FRENCH, "%.8f / %.8f", c.latitude, c.longitude
                ));
            }
        };
    }


    //// ACTIONS ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Try to fill the thumbnail view with the last image.
     * Fail silently (kinda) if the image is invalid.
     * Ideally, we should make a "Report Bug" activity that provides the logs and stacktrace, and
     * means to share them. How about some karma points as incentive to report bugs ? ;)
     */
    @UiThread
    protected void fillThumbnail() {
        if (imagePaths.size() > 0) {
            String imagePath = imagePaths.get(imagePaths.size() - 1);
            try { // imagePaths may be set but the files may not exist yet
                int w = Application.THUMB_MAX_WIDTH;
                int h = Application.THUMB_MAX_HEIGHT;
                Bitmap imageBitmap = BitmapFactory.getThumbBitmap(imagePath, w, h);

                newItemImageView.setImageBitmap(imageBitmap);
            } catch (Exception e) {
                Log.e("G2P", "Failed to create a thumbnail.\n" +
                             "Permissions were denied, or\n" +
                             "Image '"+imagePath+"' probably has no bitmap data.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Launch the Camera activity to grab a picture, which will get back to `onActivityResult`.
     */
    protected void requestNewPicture() throws CriticalException {
        // Create an new image capture intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Make sure we have an Activity that can capture images
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            app.toast(getString(R.string.toast_no_camera_available));
            finish();
            return;
        }

        // Create the File where the picture should go before launching the camera
        File imageFile = createImageFile();

        Uri imageUri = Uri.fromFile(imageFile);
        Log.d("G2P", "Starting Camera, EXTRA_OUTPUT="+imageUri+" ("+imageUri.getPath()+")");

        imagePaths.add(imageFile.getPath());

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(takePictureIntent, REQUEST_CODE_IMAGE_CAPTURE);
    }

    /**
     * Send the new item data to the server, in an async task.
     */
    public void send() {
        disableSending();

        // Grab the Location, from input or GPS. It is MANDATORY.
        String locationInputValue = newItemLocationEditText.getText().toString();
        if (locationInputValue.isEmpty()) {
            if (null != itemLocationMarker) {
                LatLng latlng = itemLocationMarker.getPosition();
                locationInputValue = String.format(
                        Locale.US,
                        "%f / %f",
                        latlng.latitude,
                        latlng.longitude
                );
            } else {
                app.toast(getString(R.string.toast_no_location_available), Toast.LENGTH_LONG);
                getLocation();
                showMap();
                enableSending();
                return;
            }
        }

        // Create a new Item with all that data
        Item item = new Item();
        item.setLocation(locationInputValue);
        item.setTitle(newItemTitleEditText.getText().toString());
        item.setDescription(newItemDescriptionEditText.getText().toString());
        if (newItemGiftRadioButton.isChecked()) {
            item.setType(Item.TYPE_GIFT);
        } else if (newItemLostRadioButton.isChecked()) {
            item.setType(Item.TYPE_LOST);
        } else {
            item.setType(Item.TYPE_MOOP);
        }

        // todo Handle the quota Exception
//                    // Handle the exception
//                    ExceptionHandler handler = new ExceptionHandler(activity){
//                        @Override
//                        protected void on(QuotaException exception) {
//                            app.toast(R.string.toast_new_item_error_quota_reached);
//                        }
//                    };

        sendItemImage();
        sendItemData(item);
    }


    //// NETWORK ///////////////////////////////////////////////////////////////////////////////////

    static int MAX_PICTURES_COUNT = 4;
    String[] pictures = new String[MAX_PICTURES_COUNT]; // use List instead ?

    boolean arePicturesSent = false;

    /**
     * We cannot send the picture right at the beginning of the activity, we need to wait for the
     * user to press the "send" FAB because of the picture rotation we want to apply.
     * Also, we need the picture to keep existing as the user may recreate the activity by rotating
     * the screen. I'm sad. We need a background Service or something to keep the UI snappy.
     */
    @Background(serial="send")
    protected void sendItemImage() {
        if (arePicturesSent) return;

        arePicturesSent = true;
        PictureItemBeforehandResponse pibr;
        try {
            int i = 0;
            for (String path : imagePaths) {

                FileSystemResource fsr = new FileSystemResource(path);
                File file = new File(path);

                // Rotate
                FileUtils.rotateImageFile(file.getPath(), imageRotation);

                // Send (this takes a LONG time on poor networks)
                pibr = app.getRestClient().pictureItemBeforehand(fsr);
                if (null != pibr) {
                    pictures[i] = pibr.getPicture().getId().toString();
                }

                // Delete
                if ( ! file.delete()) {
                    Log.e("G2P", String.format("Failed to delete item image file '%s'.", file.getPath()));
                } else {
                    Log.d("G2P", String.format("Deleted item image file '%s'.", file.getPath()));
                }

                i++;
            }
        } catch (Exception e) {
            arePicturesSent = false;
            Log.e("G2P", "Failed to send image.");
            e.printStackTrace();
            failSending(e.getMessage()); // todo: L18N
        }
    }

    @Background(serial="send")
    protected void sendItemData(Item item) {
        String picture = "";
        if (pictures.length > 0) picture = pictures[0];
        CreateItemResponse cir = app.getRestClient().createItem(
                item.getLocation(),
                item.getTitle(),
                item.getDescription(),
                item.getType(),
                picture
        );
        if (null != cir) {
            doneSending(cir.getItem());
        } else {
            failSending();
        }
    }

    @UiThread
    protected void doneSending(Item item) {
        // Congratulate the user
        app.toasty(getString(R.string.toast_new_item_uploaded, item.getTitle()));
        // Continue to the profile
        Intent intent = new Intent(this, ProfileActivity_.class);
        this.startActivity(intent);
        // Close this activity, we don't want it in the history stack.
        finish();
    }

    @UiThread
    protected void failSending(String why) {
        app.toasty(why);
        failSending();
    }

    @UiThread
    protected void failSending() {
        enableSending();
    }


    //// UI ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Shows the picture in the collapsing app bar instead of the map.
     * Does nothing if the picture is already shown.
     */
    protected void showPicture() {
        newItemMapWrapper.setVisibility(View.GONE);
        newItemImageWrapper.setVisibility(View.VISIBLE);
    }

    /**
     * Shows the map in the collapsing app bar instead of the picture.
     * Does nothing if the map is already shown.
     */
    protected void showMap() {
        newItemImageWrapper.setVisibility(View.GONE);
        newItemMapWrapper.setVisibility(View.VISIBLE);
    }

    @Click
    public void newItemShowPicButtonClicked() { showPicture(); }

    @Click
    public void newItemShowMapButtonClicked() {
        if ( ! isLocationReady()) { getLocation(); }
        showMap();
    }

    /**
     * Rotate the image clockwise per steps of 90° when clicked.
     * It's only since API 11, so the feature will not be available to API 10 and lower.
     */
    @Click
    public void newItemImageViewClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            imageRotation = (imageRotation + 90) % 360;
            newItemImageView.setPivotX(newItemImageView.getWidth()/2);
            newItemImageView.setPivotY(newItemImageView.getHeight()/2);
            newItemImageView.setRotation(imageRotation);
        }
    }

    public void onSend(View view) { send(); }

    protected void enableSending() {
        newItemSendButton.setEnabled(true);
        newItemProgressBar.setVisibility(View.GONE);
    }

    protected void disableSending() {
        newItemSendButton.setEnabled(false);
        newItemProgressBar.setVisibility(View.VISIBLE);
    }


    //// UTILS /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Convert the image URI to the direct file system path of the image file.
     * @param contentUri of the image we want the path from
     * @return the system path of the file described by the Uri.
     */
    public String getPathFromImageURI(Uri contentUri) {
        String path = null;

        // Support for Urls of type content://com.android.providers.downloads.documents/document/4
        path = FileUtils.getPath(this, contentUri);

        if (null == path) { // probably because the URI ain't local
            throw new CriticalException(String.format(
                    "Unable to get the local path for the image URI '%s'.", contentUri
            ));
        }

        return path;
    }

    /**
     * @return the File that was created.
     * @throws CriticalException
     */
    private File createImageFile() throws CriticalException {
        File imageFile = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String imageFileName = "karma_" + timeStamp + "_";
            File dir = Environment.getExternalStorageDirectory();
            dir.mkdirs();
            imageFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    dir             /* directory */
            );
        } catch (IOException e) {
            throw new CriticalException("Failed to create an image file.", e);
        }

        return imageFile;
    }
}
