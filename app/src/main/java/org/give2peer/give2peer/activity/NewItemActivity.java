package org.give2peer.give2peer.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.annotation.NonNull;
//import android.support.v7.internal.widget.AdapterViewCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.entity.Location;
import org.give2peer.give2peer.task.GiveItemTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import im.delight.android.keyvaluespinner.KeyValueSpinner;

public class NewItemActivity extends Activity
{
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final String BUNDLE_IMAGE_PATHS = "imagePaths";

    protected Application app;

    protected ArrayList<String> imagePaths; // stores the images Files paths, and is saved

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);

        Log.d("G2P", "Starting new item activity.");

        // Grab the app
        app = (Application) getApplication();

        // Check if there's a camera available
        // todo: propose the gallery picker?
        if (!app.hasCameraSupport()) {
            app.toast(getString(R.string.toast_no_camera_available));
            finish();
            return;
        }

        // On some devices, the Camera activity destroys this activity, so we need to restore the
        // paths of the files we created.
        if (null != savedInstanceState) {
            imagePaths = savedInstanceState.getStringArrayList(BUNDLE_IMAGE_PATHS);
        }

        // This activity may have been destroyed by the Camera activity ; if it's the case,
        // the imagePaths is not null, as we saved it.
        if (null == imagePaths) {
            // Only initialize if it has not been restored from bundle state.
            imagePaths = new ArrayList<>();
        }

        // Directly try to grab a new image if and only if there are no files paths stored
        // Otherwise, it means that `onActivityResult` will be called.
        if (imagePaths.size() == 0) {
            try {
                addNewPicture();
            } catch (IOException ex) {
                Log.e("G2P", "Failed to add a new picture.");
                ex.printStackTrace();
                app.toast(getString(R.string.toast_new_item_file_error), Toast.LENGTH_LONG);
                finish();
            }
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        // On some devices, the Camera activity destroys this activity, so we need to save the
        // paths of the files we created.
        outState.putStringArrayList(BUNDLE_IMAGE_PATHS, imagePaths);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            if (imagePaths.size() == 0) {
                Log.e("G2P", "No image paths are prepared to receive camera output. Cancelling...");
                finish();
            }

            // Right now there's only one image per item, but when there'll be multiple images...
            String imagePath = imagePaths.get(imagePaths.size()-1);
            File pictureFile = new File(imagePath);

            Bitmap imageBitmap = null;

            if (null != intent) {
                // Unsure if this ever happens as we're providing `MediaStore.EXTRA_OUTPUT` now.
                Log.d("G2P", "REQUEST_IMAGE_CAPTURE intent data is not null.");
                Bundle extras = intent.getExtras();
                imageBitmap = (Bitmap) extras.get("data");
            } else {
                Log.d("G2P", "REQUEST_IMAGE_CAPTURE intent data is null.");
                imageBitmap = app.getBitmapFromPath(imagePath);
            }

            if (null == imageBitmap) {
                Log.e("G2P", "Add new item : the image bitmap was `null` at : " + imagePath);
                finish();
                return;
            }

            // Sometimes the camera sends back an empty bitmap, so we're trying this
            if (imageBitmap.getHeight() == 0 || imageBitmap.getWidth() == 0) {
                Log.e("G2P", "Add new item : the bitmap is empty !");
                finish();
                return;
            }

            // Put the bitmap in the View to show the user
            ((ImageView)findViewById(R.id.newItemImageView)).setImageBitmap(imageBitmap);

            // Write the bitmap to file
            FileOutputStream fOut;
            try {
                fOut = new FileOutputStream(pictureFile);
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                fOut.flush();
                fOut.close();
            } catch (Exception e) {
                Log.e("G2P", e.getMessage());
                e.printStackTrace();
                finish();
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            // If the user cancelled the capture of a picture, we GTFO.
            // It may be nice to allow a user to add a picture from a gallery instead of taking one.
            // see http://stackoverflow.com/questions/20021431/android-how-to-take-a-picture-from-camera-or-gallery
            Log.d("G2P", "User Cancelled the image capture.");
            finish();
        }
    }


    //// ACTIONS ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Launch the Camera activity to grab a picture, which will get back to `onActivityResult`.
     */
    protected void addNewPicture() throws IOException
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

    public void send()
    {
        // Update the UI
        disableSending();

        // Collect inputs from the form
        EditText titleInput    = (EditText) findViewById(R.id.newItemTitleEditText);
        EditText locationInput = (EditText) findViewById(R.id.newItemLocationEditText);

        // Grab the Location, from input or GPS. It is MANDATORY.
        String locationInputValue = locationInput.getText().toString();
        if (locationInputValue.isEmpty()) {
            android.location.Location location = app.getGeoLocation();
            if (null != location) {
                locationInputValue = String.format(
                        "%f/%f",
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
        // Remember, maybe this activity was destroyed while taking a picture
        List<File> imageFiles = new ArrayList<>();
        for (String path : imagePaths) {
            imageFiles.add(new File(path));
        }

        // Create a new Item with all that data
        Item item = new Item();
        item.setLocation(locationInputValue);
        item.setTitle(titleInput.getText().toString());
        item.setPictures(imageFiles);

        // Try to upload it, along with its image(s).
        GiveItemTask git = new GiveItemTask(app) {
            @Override
            protected void onPostExecute(Item item) {
                if (!hasException()) {
                    finish();
                    app.toast(getString(R.string.toast_new_item_uploaded, item.getTitle()));
                } else {
                    Exception e = getException();
                    String toast;
                    if (e instanceof IOException) {
                        toast = getString(R.string.toast_no_internet_available);
                    } else {
                        toast = getString(R.string.toast_new_item_upload_failed);
                    }
                    app.toast(toast, Toast.LENGTH_LONG);
                    Log.e("G2P", e.getMessage());
                    e.printStackTrace();
                    enableSending();
                }
            }
        };
        git.execute(item);
    }


    //// UI ////////////////////////////////////////////////////////////////////////////////////////

    public void onSend(View view)
    {
        send();
    }

    protected void enableSending()
    {
        Button      sendButton   = (Button)      findViewById(R.id.newItemSendButton);
        ProgressBar sendProgress = (ProgressBar) findViewById(R.id.newItemProgressBar);

        sendButton.setEnabled(true);
        sendProgress.setVisibility(View.GONE);
    }

    protected void disableSending()
    {
        Button      sendButton   = (Button)      findViewById(R.id.newItemSendButton);
        ProgressBar sendProgress = (ProgressBar) findViewById(R.id.newItemProgressBar);

        sendButton.setEnabled(false);
        sendProgress.setVisibility(View.VISIBLE);
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
