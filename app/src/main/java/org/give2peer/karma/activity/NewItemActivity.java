package org.give2peer.karma.activity;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.shamanland.fab.FloatingActionButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.give2peer.karma.Application;
import org.give2peer.karma.FileUtils;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.R;
import org.give2peer.karma.exception.QuotaException;
import org.give2peer.karma.factory.BitmapFactory;
import org.give2peer.karma.task.NewItemTask;

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
                Log.d("G2P", "Add new item with shared image URl `" + imageUri.toString() + "`.");
                String imagePath = getPathFromImageURI(imageUri);
                Log.d("G2P", "... which translates into path `" + imagePath + "`.");
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
        // If the user is not preregistered, let's do this dudez !
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
            Log.d("G2P", "User cancelled the image capture.");
            finish();
        }
    }

    /**
     * Convert the image URI to the direct file system path of the image file.
     * This is prety crappy too.
     * @param contentUri
     * @return the system path of the file described by the Uri.
     */
    public String getPathFromImageURI(Uri contentUri)
    {
        String path = null;

        // Support for Urls of type content://com.android.providers.downloads.documents/document/4
        // todo: don't assume it's local ?
        path = FileUtils.getPath(this, contentUri);


        // Fallback support for Camera URLs such as content://media/external/images/media/78
        if (null == path) {
            Log.d("G2P", "We tried to use the fallback path finder method for that image.");
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
//        item.setPictures(imageFiles);

        // Try to upload it, along with its image(s).
        (new NewItemTask(app, this, item, imageFiles) {
            @Override
            protected void onPostExecute(Item item) {
                if (!hasException()) {
                    app.toast(getString(R.string.toast_new_item_uploaded, item.getTitle()), Toast.LENGTH_LONG);
                    // Continue to the profile
                    Intent intent = new Intent(this.activity, ProfileActivity_.class);
                    this.activity.startActivity(intent);
                    // ... but close this activity, we don't want it in the history stack.
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
        }).execute();
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
