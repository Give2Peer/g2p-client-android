package org.give2peer.give2peer.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.shamanland.fab.FloatingActionButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.exception.QuotaException;
import org.give2peer.give2peer.task.NewItemTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Handles :
 * - Receiving an image from another activity's share intent, launching the camera otherwise
 * - A form to add a new item, with a nice Floating Action Button to send.
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
public class NewItemActivity extends LocatorActivity
{
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final String BUNDLE_IMAGE_PATHS = "imagePaths";

    protected ArrayList<String> imagePaths; // stores the image files paths, and is saved

    @ViewById
    FloatingActionButton newItemSendButton;
    @ViewById
    ProgressBar          newItemProgressBar;
    @ViewById
    ImageView            newItemImageView;
    @ViewById
    EditText             newItemTitleEditText;
    @ViewById
    EditText             newItemLocationEditText;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.d("G2P", "Starting new item activity.");

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
        }

    }

    @AfterViews
    public void recoverImage()
    {
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
        if (action.equals(Intent.ACTION_SEND) && type != null) {
            if (type.startsWith("image/")) {
                // Handle a single image being sent
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                String imagePath = getPathFromImageURI(imageUri);
                Log.d("G2P", "Add new item with shared image `" + imagePath + "`.");
                imagePaths.add(imagePath);
                fillThumbnail();
                //processImages();
            } else {
                // The intent filter in the manifest should ensure that we never EVER throw this.
                throw new RuntimeException("You shared something that is not an image. Nooope.");
            }
//        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
//            if (type.startsWith("image/")) {
//                handleSendMultipleImages(intent); // Handle multiple images being sent
//            }
        } else {
            // Directly try to grab a new image if and only if there are no files paths stored
            // Otherwise, it means that `onActivityResult` will be called.
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
                } catch (IOException ex) {
                    Log.e("G2P", "Failed to add a new picture.");
                    ex.printStackTrace();
                    app.toast(getString(R.string.toast_new_item_file_error), Toast.LENGTH_LONG);
                    finish();
                }
            } else {
                fillThumbnail();
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // If the user is not registered, let's forward him to the registration activity
        // todo: this is crappety crap. We should register automatically.
        app.requireAuthentication(this);
    }

    @Override
    public void onLocated(Location loc)
    {
        super.onLocated(loc); // parent saves the location Application-wise
        // Sucessfully located device : we hint to the user that the Location field is optional.
        newItemLocationEditText.setHint(R.string.new_item_label_location_optional);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        // On some devices, the Camera activity destroys this activity, so we need to save the
        // paths of the files we created.
        outState.putStringArrayList(BUNDLE_IMAGE_PATHS, imagePaths);
    }

    /**
     * The Camera and Gallery activities will provide the images to this activity in this method.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //processImages();
            // Put the bitmap in the View to show the user
            fillThumbnail();
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            // If the user cancelled the capture of a picture, we GTFO.
            // It may be nice to allow a user to add a picture from a gallery instead of taking one.
            // see http://stackoverflow.com/questions/20021431/android-how-to-take-a-picture-from-camera-or-gallery
            Log.d("G2P", "User Cancelled the image capture.");
            finish();
        }
    }

//    /**
//     * THIS IS CRAP.
//     * Besides, it MUTATES THE IMAGE AND DEGRADES ITS QUALITY !!!
//     * fixme: create a proper thumbnail to send away and then to delete, or to store in the cache
//     * @deprecated
//     */
//    protected void processImages()
//    {
//        if (imagePaths.size() == 0) {
//            String msg = getString(R.string.toast_no_image_paths);
//            Log.e("G2P", msg);
//            app.toast(msg, Toast.LENGTH_LONG);
//            finish();
//        }
//
//        // Right now there's only one image per item, but when there'll be multiple images...
//        String imagePath = imagePaths.get(imagePaths.size()-1);
//        File imageFile = new File(imagePath);
//
//        Bitmap imageBitmap = app.getBitmapFromPath(imagePath);
//
//        if (null == imageBitmap) {
//            Log.e("G2P", "Add new item : the image bitmap was `null` at : " + imagePath);
//            finish();
//            return;
//        }
//
//        // Sometimes the camera sends back an empty bitmap, so we're trying this
//        if (imageBitmap.getHeight() == 0 || imageBitmap.getWidth() == 0) {
//            Log.e("G2P", "Add new item : the bitmap is empty !");
//            finish();
//            return;
//        }
//
//        // Write the bitmap to file
//        FileOutputStream fOut;
//        try {
//            fOut = new FileOutputStream(imageFile);
//            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
//            fOut.flush();
//            fOut.close();
//        } catch (Exception e) {
//            Log.e("G2P", e.getMessage());
//            e.printStackTrace();
//            finish();
//        }
//    }

    /**
     * Convert the image URI to the direct file system path of the image file.
     * This is prety crappy too.
     * @param contentUri
     * @return the system path of the file de scribed by the Uri.
     */
    public String getPathFromImageURI(Uri contentUri)
    {
        String [] proj={MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery( contentUri,
                                      proj,  // Which columns to return
                                      null,  // WHERE clause; which rows to return (all rows)
                                      null,  // WHERE clause selection arguments (none)
                                      null); // Order-by clause (ascending by name)
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
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
                Bitmap imageBitmap = Application.getThumbBitmap(imagePath, w, h);

                newItemImageView.setImageBitmap(imageBitmap);
            } catch (Exception e) {
                Log.e("G2P", "Failed to create a thumbnail.\n" +
                             "Image '"+imagePath+"' probably has no bitmap data.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Launch the Camera activity to grab a picture, which will get back to `onActivityResult`.
     *
     * todo: enable choosing from gallery or camera ? I thought ACTION_IMAGE_CAPTURE would suffice ?
     */
    protected void requestNewPicture() throws IOException
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
        File imageFile = createImageFile();
        Uri imageUri = Uri.fromFile(imageFile);

        if (null != imageFile) {
            imagePaths.add(imageFile.getPath());
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            Log.d("G2P", "Starting Camera, EXTRA_OUTPUT="+imageUri+" ("+imageUri.getPath()+")");
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            // Unsure if this can even happen. If it happens, well... meh.
            throw new IOException("Created image file is NULL. This should NEVER happen.");
        }
    }

    /**
     * Send the new item data to the server, in a async task.
     */
    public void send()
    {
        // Update the UI
        disableSending();

        // Grab the Location, from input or GPS. It is MANDATORY.
        String locationInputValue = newItemLocationEditText.getText().toString();
        if (locationInputValue.isEmpty()) {
            android.location.Location location = app.getGeoLocation();
            if (null != location) {
                locationInputValue = String.format(
                        Locale.US,
                        "%f / %f",
                        location.getLatitude(),
                        location.getLongitude()
                );
            } else {
                app.toast(getString(R.string.toast_no_location_available), Toast.LENGTH_LONG);
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
        item.setPictures(imageFiles);

        // Try to upload it, along with its image(s).
        NewItemTask nit = new NewItemTask(app) {
            @Override
            protected void onPostExecute(Item item) {
                if (!hasException()) {
                    app.toast(getString(R.string.toast_new_item_uploaded, item.getTitle()), Toast.LENGTH_LONG);
                    // todo: here, continue to profile
                    finish();
                } else {
                    Exception e = getException();
                    String toast;
                    if (e instanceof IOException) {
                        toast = getString(R.string.toast_no_internet_available);
                    } else if (e instanceof QuotaException) {
                        toast = getString(R.string.toast_new_item_error_quota_reached);
                    } else {
                        toast = getString(R.string.toast_new_item_upload_failed);
                    }
                    app.toast(toast, Toast.LENGTH_LONG);
                    String loggedMsg = e.getMessage();
                    if ( ! (null == loggedMsg || loggedMsg.isEmpty()))  {
                        Log.e("G2P", e.getMessage());
                    }
                    e.printStackTrace();
                    enableSending();
                }
            }
        };
        nit.execute(item);
    }


    //// UI ////////////////////////////////////////////////////////////////////////////////////////

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


    //// UTILS /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Ok, this is trouble. We do NOT delete the image files after sending them. We should. Yup.
     * todo: delete the image file once it is sent.
     * Also, maybe move this utility method to the `Application`.
     *
     * @return the File that was created.
     * @throws IOException
     */
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
