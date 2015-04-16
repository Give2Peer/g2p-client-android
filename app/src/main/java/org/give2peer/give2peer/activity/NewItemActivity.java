package org.give2peer.give2peer.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;
import org.give2peer.give2peer.R;
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
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the picture should go
            File pictureFile = null;
            try {
                pictureFile = createImageFile();
            } catch (IOException ex) {
                String msg = getString(R.string.toast_new_item_file_error);
                Log.e("G2P", ex.getMessage());
                ex.printStackTrace();
                toast(msg);
            }
            // Continue only if the File was successfully created
            if (pictureFile != null) {
                pictureFiles.add(pictureFile);
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pictureFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            // GTFO
            toast(getString(R.string.toast_no_camera_available));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            if (null == imageBitmap) {
                toast("MOTHERFUCKER WHY IS THE IMAGE NULL ?");
            }

            // Put the bitmap in the View to show the user
            ((ImageView)findViewById(R.id.newItemImageView)).setImageBitmap(imageBitmap);
            // Write the bitmap to file
            File pictureFile = pictureFiles.get(pictureFiles.size()-1);
            FileOutputStream fOut;
            try {
                fOut = new FileOutputStream(pictureFile);
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                fOut.flush();
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void onSend(View view)
    {
        EditText titleInput = (EditText) findViewById(R.id.newItemTitleEditText);
        Button   sendButton = (Button)   findViewById(R.id.newItemSendButton);
        ProgressBar sendProgress = (ProgressBar) findViewById(R.id.newItemProgressBar);

        sendButton.setEnabled(false);
        sendProgress.setVisibility(View.VISIBLE);

        Location loc = app.getLocation();
        Item item = new Item();
        item.setLocation(String.format("%f/%f", loc.getLatitude(), loc.getLongitude()));
        item.setTitle(titleInput.getText().toString());
        item.setPictures(pictureFiles);

        GiveItemTask git = new GiveItemTask(app) {
            protected void onPostExecute(Item item) {
                finish();
                toast(String.format(getString(R.string.toast_new_item_uploaded), item.getTitle()));
            }
        };
        git.execute(item);

        toast(getString(R.string.toast_new_item_uploading));
    }



    private File createImageFile() throws IOException
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "g2p_" + timeStamp + "_item";
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
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

    protected void toast(String message)
    {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }
}
