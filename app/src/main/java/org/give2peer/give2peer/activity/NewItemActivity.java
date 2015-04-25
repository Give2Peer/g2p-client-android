package org.give2peer.give2peer.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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

public class NewItemActivity extends ActionBarActivity
{
    static final int REQUEST_IMAGE_CAPTURE = 1;

    protected Application app;

    protected List<File> pictureFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);

        // Grab the app
        app = (Application) getApplication();

        if (!app.hasCameraSupport()) {
            app.toast(getString(R.string.toast_no_camera_available));
            finish();
            return;
        }

        if (!app.hasLocation()) {
            app.toast(getString(R.string.toast_no_location_available));
            finish();
            return;
        }

        // Initialize
        pictureFiles = new ArrayList<>();

        // Directly try to grab a new picture
        addNewPicture();
    }

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
        File pictureFile = null;
        try {
            pictureFile = createImageFile();
        } catch (IOException ex) {
            Log.e("G2P", ex.getMessage());
            ex.printStackTrace();
            app.toast(getString(R.string.toast_new_item_file_error));
            finish();
            return;
        }

        // Continue only if the File was successfully created
        if (pictureFile != null) {
            pictureFiles.add(pictureFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pictureFile));
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            File pictureFile = pictureFiles.get(pictureFiles.size()-1);

            Bitmap imageBitmap;
            if (null != data) {
                Bundle extras = data.getExtras();
                imageBitmap = (Bitmap) extras.get("data");
            } else {
                imageBitmap = app.getBitmapFromPath(pictureFile.getPath());
            }

            if (null == imageBitmap) {
                app.toast("WHY IS THE IMAGE `NULL`?\nWHAT DID YOU DO!?");
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
            finish();
        }
    }

    public void onSend(View view)
    {
        EditText titleInput = (EditText) findViewById(R.id.newItemTitleEditText);
        final Button      sendButton   = (Button)      findViewById(R.id.newItemSendButton);
        final ProgressBar sendProgress = (ProgressBar) findViewById(R.id.newItemProgressBar);

        sendButton.setEnabled(false);
        sendProgress.setVisibility(View.VISIBLE);

        Location location = app.getLocation();

        Item item = new Item();
        item.setLocation(location.forItem());
        item.setTitle(titleInput.getText().toString());
        item.setPictures(pictureFiles);

        GiveItemTask git = new GiveItemTask(app) {
            @Override
            protected void onPostExecute(Item item) {
                if (!hasException()) {
                    finish();
                    app.toast(getString(R.string.toast_new_item_uploaded, item.getTitle()));
                } else {
                    app.toast(String.format("Failure: %s", getException().getMessage()), Toast.LENGTH_LONG);
                    sendButton.setEnabled(true);
                    sendProgress.setVisibility(View.GONE);
                }
            }
        };
        git.execute(item);
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
