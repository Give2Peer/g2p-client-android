package org.give2peer.karma.activity;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
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
import android.widget.CheckBox;
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

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.give2peer.karma.Application;
import org.give2peer.karma.FileUtils;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.R;
import org.give2peer.karma.event.LocationUpdateEvent;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.exception.QuotaException;
import org.give2peer.karma.factory.BitmapFactory;
import org.give2peer.karma.service.ExceptionHandler;
import org.give2peer.karma.task.NewItemTask;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Handles :
 * - Receiving an image from another activity's share intent
 * - (deprecated, but yet enabled back) launching the camera otherwise
 *   I deprecated this because the code around this hack is REALLY smelly.
 *   I disabled it and it was just too confusing for my alpha testers.
 *   So I enabled it back, and we'll just have to carry on until we ditch API 10 support.
 *   This can be cleaned up and un-deprecated
 * - A form to add a new item, with a nice Floating Action Button to send.
 * - Rotating the received image before sending it
 *
 * This is where the user adds new items in the database.
 * It should handle the three main moop intents :
 * - garbage is
 *   - spotted
 *   - recycled
 *   - destroyed
 * - lost&found is
 *   - spotted
 * - donation is
 *   - made
 * Unless we make multiple Activities for each, and make multiple share options.
 * That would reduce the number of actions the app requires of the user.
 */
@EActivity(R.layout.activity_new_item)
public  class      NewItemActivity
        extends    LocatorBaseActivity
        implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback
{
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final String BUNDLE_IMAGE_PATHS = "imagePaths";

    Application app;

    /**
     * Stores the image files paths, and is bundled. (may cause issues, now that I think of it)
     * We only support ONE image for now.
     */
    protected ArrayList<String> imagePaths;

    /**
     * Amount of rotation in degrees the user wants to apply to its image before sending it.
     * Increasing this value results in a clockwise rotation.
     * This will become an array when we'll have multiple images.
     */
    protected int imageRotation = 0;

    //// VIEWS /////////////////////////////////////////////////////////////////////////////////////

    @ViewById
    FloatingActionButton newItemSendButton;
    @ViewById
    ProgressBar newItemProgressBar;
    @ViewById
    ImageView newItemImageView;
    @ViewById
    EditText newItemTitleEditText;
    @ViewById
    EditText newItemDescriptionEditText;
    @ViewById
    EditText newItemLocationEditText;
    @ViewById
    RadioButton newItemGiftRadioButton;
    @ViewById
    RadioButton newItemLostRadioButton;
    @ViewById
    RadioButton newItemMoopRadioButton;

//    @ViewById
//    CheckBox newItemGiftCheckBox;

    @ViewById
    NestedScrollView newItemFormScrollView;
    @ViewById
    RelativeLayout   newItemMapWrapper;
    @ViewById
    RelativeLayout   newItemImageWrapper;


    //// LIFECYCLE LISTENERS ///////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("G2P", "Starting new item activity.");
        app = (Application) getApplication();

        // For when we launched the Camera Activity FROM Karma (and are not using the share...),
        // on some devices, the Camera activity destroys this activity, so we need to restore the
        // paths of the files we created before launching the Camera.
        // This is an edge-case of an edge-case, as most users will add new items via the Camera's
        // share function...
        // We should redo the whole image handling business, to make it clearer and more robust.
        // because I can see how lines such as this one may screw up the app, possibly :
        // what happens when we have a savedinstancestate but the picture comes from the share ?
        if (null != savedInstanceState) {
            imagePaths = savedInstanceState.getStringArrayList(BUNDLE_IMAGE_PATHS);
            if (imagePaths != null) {
                Log.d("G2P", String.format(
                        "Restored image paths %s from bundle.",
                        Arrays.toString(imagePaths.toArray()))
                );
            }
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

    protected static final int REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION = 1003;

    @Override
    protected void onResume() {
        super.onResume();
        // If the user is not preregistered, let's do this dudeez !
        app.requireAuthentication(this);
        // We never know, maybe the map and location are ready already ?
        updateMap();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)) {

            Log.d("G2P", String.format("Not enough permissions ! %d %d", ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE), ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)));


            // This is true when the user has denied the permissions once
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                final Activity activity = this;
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Permissions required")
                        .setMessage("Pictures are usually stored on the external storage, so we need your permission to access them.")
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
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (grantResults.length == 0) {
            // In some rare cases, this might happen ; consider it canceled
            Log.d("G2P", "onRequestPermissionsResult with no permissions.");
            onAccessPermissionsDenied();
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION:
                Log.d("G2P", String.format("Grants: %d %d", grantResults[0], grantResults[1]));
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fillThumbnail();
                } else {
                    onAccessPermissionsDenied();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void onAccessPermissionsDenied() {
        Log.d("G2P", "Permission to access external storage denied... Why, you paranoid clod ?");
        app.toasty(getString(R.string.toast_permission_read_denied));
        finish();
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // On some devices, the Camera activity destroys this activity, so we need to save the
        // paths of the files we created.
        // This happens only when we launched the camera by ourselves, which we don't anymore.
        outState.putStringArrayList(BUNDLE_IMAGE_PATHS, imagePaths);
    }

    /**
     * The Camera and Gallery activities will provide the images to this activity in this method,
     * if we use requestNewPicture().
     * We try not to do that anymore.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Put the bitmap in the View to show the user
            fillThumbnail();
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            // If the user cancelled the capture of a picture, we GTFO.
            // It may be nice to allow a user to add a picture from a gallery instead of taking one.
            // see http://stackoverflow.com/questions/20021431/android-how-to-take-a-picture-from-camera-or-gallery
            Log.d("G2P", "User cancelled the image capture.");
            finish();
        }
    }

    @Subscribe
    public void updateMapWhenLocated(LocationUpdateEvent locationUpdateEvent) {
        Location location = locationUpdateEvent.getLocation();
        // Successfully located device : we hint to the user that the Location field is optional.
        newItemLocationEditText.setHint(R.string.new_item_label_location_optional);
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
     * This is to recover the image from the Intent, or launch the Camera to pick one if we
     * arrived on this activity by other means (ie: the Intent is empty).
     * We shouldn't use the second use case, it's not very stable and I'd like to remove it
     * to de-clutter the code. Right now we disabled any way to go there but users may inspect and
     * launch activities of their choice from the OS, if they so choose to, I believe.
     * Instead, we could warn the user and suggest to either launch the camera or return to the map.
     * Not sure when happens in the "back" history in that case.
     * I'd love a tool to inspect in real-time that information. There's probably one already.
     *
     * Note that this is in AfterViews because we're using Android Annotations.
     */
    @AfterViews
    public void recoverImage() {
        // This activity may have been destroyed by the Camera activity ; if it's the case,
        // the imagePaths is not null, as we saved it.
        if (null == imagePaths) {
            // Only initialize if it has not been restored from bundle state.
            imagePaths = new ArrayList<>();
        }

        // Handle images sent to this app by the "share" feature
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (action != null && action.equals(Intent.ACTION_SEND) && type != null) {
            if (type.startsWith("image/")) {
                // Handle a single image being sent
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                Log.d("G2P", "Add new item with shared image URl `" + imageUri.toString() + "`.");
                String imagePath = getPathFromImageURI(imageUri);
                Log.d("G2P", "... which translates into path `" + imagePath + "`.");
                imagePaths.add(imagePath);
                fillThumbnail();
            } else {
                // The intent filter in the manifest should ensure that we never EVER throw this.
                throw new CriticalException("You shared something that is not an image. Nooope.");
            }
//        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
//            if (type.startsWith("image/")) {
//                handleSendMultipleImages(intent); // Handle multiple images being sent
//            }
        } else {
            // /!\ we try not to do that anymore, it's too unreliable.
            // Directly try to grab a new image if and only if there are no files paths stored.
            // Otherwise, it means that `onActivityResult` will be called..
            if (imagePaths.size() == 0) {
                try {
                    // Check if there's a camera available
                    // todo: propose the gallery picker?
                    if (!app.hasCameraSupport()) {
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
            } else {
                fillThumbnail();
            }
        }
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

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);

        updateMap();
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

        // Let's clear the map and zoom on the user position
        googleMap.clear();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(getLatLng(), 16));

        // Let's put a marker on the map, so that the user may drag it around if they want
        itemLocationMarker = googleMap.addMarker(new MarkerOptions()
                .position(getLatLng())
                .draggable(true)
        );

        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}
            @Override
            public void onMarkerDrag(Marker marker) {}
            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng c = marker.getPosition();
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(c));
                newItemLocationEditText.setText(String.format(
                        Locale.US, "%.8f / %.8f", c.latitude, c.longitude
                ));
            }
        });

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


    //// ACTIONS ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Try to fill the thumbnail view with the first image.
     * Fail silently (kinda) if the image is invalid.
     * Ideally, we should make a "Report Bug" activity that provides the logs and stacktrace, and
     * means to share them. How about some karma points as incentive to report bugs ? ;)
     */
    protected void fillThumbnail()
    {
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
     *
     * @deprecated
     */
    @Deprecated
    protected void requestNewPicture() throws CriticalException
    {
        // Create an new image capture intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Make sure we have an Activity that can capture images
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            app.toast(getString(R.string.toast_no_camera_available));
            finish();
            return;
        }

        // Create the File where the picture should go
        File imageFile = null;
        try {
            imageFile = createImageFile();
        } catch (IOException e) {
            throw new CriticalException("Failed to create an image file.", e);
        }
        Uri imageUri = Uri.fromFile(imageFile);

        if (null != imageFile) {
            imagePaths.add(imageFile.getPath());
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            Log.d("G2P", "Starting Camera, EXTRA_OUTPUT="+imageUri+" ("+imageUri.getPath()+")");
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            // Unsure if this can even happen. If it happens, well... meh.
            throw new CriticalException("Created image file is NULL. This should NEVER happen.");
        }
    }

    /**
     * Send the new item data to the server, in an async task.
     */
    public void send()
    {
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

        // Grab the image files from the paths
        // Remember, maybe this activity was destroyed while taking a picture with the camera.
        List<File> imageFiles = new ArrayList<>();
        for (String path : imagePaths) {
            imageFiles.add(new File(path));
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
        // fixme:
          // or not : http://stackoverflow.com/questions/29411752/custom-icon-for-a-radio-button

        // In the future we'll have more than one image...
        List<Integer> pictureRotations = new ArrayList<Integer>();
        // ... but we only support one for now.
        pictureRotations.add(imageRotation);

        // Try to upload it, along with its image(s).
        (new NewItemTask(app, this, item, imageFiles, pictureRotations) {
            @Override
            protected void onPostExecute(Item item) {
                if ( ! hasException()) {
                    app.toast(getString(R.string.toast_new_item_uploaded, item.getTitle()), Toast.LENGTH_LONG);
                    // Continue to the profile
                    Intent intent = new Intent(this.activity, ProfileActivity_.class);
                    this.activity.startActivity(intent);
                    // ... but close this activity, we don't want it in the history stack.
                    finish();
                } else {
                    Exception e = getException();

                    // Log
                    String loggedMsg = e.getMessage();
                    if ( ! (null == loggedMsg || loggedMsg.isEmpty()))  {
                        Log.e("G2P", e.getMessage());
                    }
                    e.printStackTrace();

                    // Handle the exception
                    ExceptionHandler handler = new ExceptionHandler(activity){
                        @Override
                        protected void on(QuotaException exception) {
                            toast(R.string.toast_new_item_error_quota_reached);
                        }
                    };
                    handler.handleExceptionOrFail(e);

                    // And enable sending again
                    enableSending();
                }
            }
        }).execute();
    }


    //// UI ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Shows the picture in the collapsing app bar instead of the map.
     * Does nothing if the picture is already shown.
     */
    protected void showPicture()
    {
        newItemMapWrapper.setVisibility(View.GONE);
        newItemImageWrapper.setVisibility(View.VISIBLE);
    }
    /**
     * Shows the map in the collapsing app bar instead of the picture.
     * Does nothing if the map is already shown.
     */
    protected void showMap()
    {
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

    public void onSend(View view)
    {
        send();
    }

    protected void enableSending()
    {
        newItemSendButton.setEnabled(true);
        newItemProgressBar.setVisibility(View.GONE);
    }

    protected void disableSending()
    {
        newItemSendButton.setEnabled(false);
        newItemProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Rotate the image clockwise per steps of 90° when clicked.
     * It's only since API 11, so the feature will not be available to API 10 and lower.
     */
    @Click
    public void newItemImageViewClicked()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            imageRotation = (imageRotation + 90) % 360;
            newItemImageView.setPivotX(newItemImageView.getWidth()/2);
            newItemImageView.setPivotY(newItemImageView.getHeight()/2);
            newItemImageView.setRotation(imageRotation);
        }
    }


    //// UTILS /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Convert the image URI to the direct file system path of the image file.
     * @param contentUri
     * @return the system path of the file described by the Uri.
     */
    public String getPathFromImageURI(Uri contentUri)
    {
        String path = null;

        // Support for Urls of type content://com.android.providers.downloads.documents/document/4
        // todo: don't assume it's local ? But what happens when it's not ?
        path = FileUtils.getPath(this, contentUri);

        // Fallback support for Camera URLs such as content://media/external/images/media/78
        if (null == path) {
            // I'm not sure we even go through here anymore ?
            // We toast for now, but we may well throw a CriticalException here in the future
            app.toasty("If you see this message, tell us about it !\nThe code is BLUE KOALA.");

            Log.d("G2P", String.format(
                    "Used the fallback path finder method for that url : `%s`.",
                    contentUri.toString()
            ));
            String [] proj={MediaStore.Images.Media.DATA};
            Cursor cursor = managedQuery( contentUri,
                    proj,  // Which columns to return
                    null,  // WHERE clause; which rows to return (all rows)
                    null,  // WHERE clause selection arguments (none)
                    null); // Order-by clause (ascending by name)
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cursor.moveToFirst();
            path = cursor.getString(column_index);
        }

        return path;
    }

    /**
     * Ok, this is trouble. We do NOT delete the image files after sending them. We should. Yup.
     * todo: delete the image file once it is sent. NO. Remove usage of this altogether.
     *
     * @return the File that was created.
     * @throws IOException
     */
    @Deprecated
    private File createImageFile() throws IOException
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "g2p_" + timeStamp + "_";
        File dir = Environment.getExternalStorageDirectory();
        dir.mkdirs();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                dir             /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //String path = "file:" + image.getAbsolutePath();
        return image;
    }
}
