package org.give2peer.give2peer.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

public class NewItemActivity extends Activity
{
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final String BUNDLE_IMAGE_PATHS = "imagePaths";

    protected Application app;

    protected List<File> imageFiles; // deprecated?
    protected List<Uri> imageUris; // redundant with above, trying this
    protected ArrayList<String> imagePaths; // redundant with above, trying this

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);

        // Grab the app
        app = (Application) getApplication();

        Log.d("G2P", "Starting new item activity.");

        if (!app.hasCameraSupport()) {
            app.toast(getString(R.string.toast_no_camera_available));
            finish();
            return;
        }

        // Initialize
        imageFiles = new ArrayList<>();
        imageUris = new ArrayList<>();

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
            addNewPicture();
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

    /**
     * This is called AFTER onCreate, not good
     */
//    @Override
//    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
//    {
//        super.onRestoreInstanceState(savedInstanceState);
//        // On some devices, the Camera activity destroys this activity, so we need to restore the
//        // paths of the files we created.
//        imagePaths = savedInstanceState.getStringArrayList(BUNDLE_IMAGE_PATHS);
//    }

    protected void addNewPicture()
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
        } catch (IOException ex) {
            Log.e("G2P", ex.getMessage());
            ex.printStackTrace();
            app.toast(getString(R.string.toast_new_item_file_error));
            finish();
            return;
        }

        Uri imageUri = Uri.fromFile(imageFile);
        // Try another approach
//        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                                                   new ContentValues());

        if (null != imageFile) {
            imagePaths.add(imageFile.getPath());
            imageUris.add(imageUri);
            imageFiles.add(imageFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            Log.d("G2P", "Starting Camera, EXTRA_OUTPUT="+imageUri+" ("+imageUri.getPath()+")");
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Log.e("G2P", "Created image file is NULL. This should NEVER happen.");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

//            File pictureFile = imageFiles.get(imageFiles.size()-1);
//            Uri imageUri = imageUris.get(imageUris.size()-1);

            if (imagePaths.size() == 0) {
                Log.e("G2P", "No image paths are prepared to receive camera output. Cancelling...");
                finish();
            }

            String imagePath = imagePaths.get(imagePaths.size()-1);
            File pictureFile = new File(imagePath);

            Bitmap imageBitmap = null;

            if (null != intent) {
                // Unsure if this ever happens as we're providing `MediaStore.EXTRA_OUTPUT`.
                Log.d("G2P", "REQUEST_IMAGE_CAPTURE intent data is not null.");
                Bundle extras = intent.getExtras();
                imageBitmap = (Bitmap) extras.get("data");
            } else {
                Log.d("G2P", "REQUEST_IMAGE_CAPTURE intent data is null.");

                imageBitmap = app.getBitmapFromPath(imagePath);
                //imageBitmap = app.getBitmapFromPath(imageUri.getPath());

//                try {
////                    imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
////                                                                    Uri.fromFile(pictureFile));
//
//                    // Image saved to a generated MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//                    String[] projection = {
//                            MediaStore.MediaColumns._ID,
//                            MediaStore.Images.ImageColumns.ORIENTATION,
//                            MediaStore.Images.Media.DATA
//                    };
//                    Cursor c = getContentResolver().query(imageUri, projection, null, null, null);
//                    c.moveToFirst(); // the cursor will be closed by the activity (someone said)
//                    String photoFileName = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
//                    imageBitmap = BitmapFactory.decodeFile(photoFileName);
//
//                    Log.d("G2P", "Photo file name: "+photoFileName);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }



            }

            if (null == imageBitmap) {
                Log.e("G2P", "Add new item : the image bitmap was `null` at : "+imagePath);
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

    public void onSend(View view)
    {
        disableSending();

        // Collect inputs from the form
        EditText titleInput = (EditText) findViewById(R.id.newItemTitleEditText);

        Location location = app.getLocation();
        if (null == location) {
            app.toast("No location selected.", Toast.LENGTH_LONG);
            enableSending();
            return;
        }

        Item item = new Item();
        item.setLocation(location.forItem());
        item.setTitle(titleInput.getText().toString());
        item.setPictures(imageFiles);

        GiveItemTask git = new GiveItemTask(app) {
            @Override
            protected void onPostExecute(Item item) {
                if (!hasException()) {
                    finish();
                    app.toast(getString(R.string.toast_new_item_uploaded, item.getTitle()));
                } else {
                    app.toast(String.format("Failure: %s", getException().getMessage()), Toast.LENGTH_LONG);
                    enableSending();
                }
            }
        };
        git.execute(item);
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

    // UTILS ///////////////////////////////////////////////////////////////////////////////////////

}
