package org.give2peer.give2peer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//import org.apache.http.entity.mime;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Still not sure if this should be our main logic class.
 * What happens when I change Activities ?
 *
 * Callbacks : http://developer.android.com/training/basics/activity-lifecycle/starting.html
 *
 */
public class MainActivity extends ActionBarActivity
{
    protected LocationManager lm;
    protected LocationProvider lp;
    protected Location location;

    ItemRepository ir;

    Application app;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Grab the app
        app = (Application) getApplication();

        // Let's grab the location manager
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        // We never know, maybe there's an available location already
        refreshLocationView();

        // TEST -- fixme: remove and async all queries
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Prepare the Item repository
        ir = app.getItemRepository();

        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // LISTENERS ///////////////////////////////////////////////////////////////////////////////////



    // UI LISTENERS ////////////////////////////////////////////////////////////////////////////////

    public void onListAroundMe(View view)
    {
        if (app.isOnline()) {
            // Start the "list around" activity
            Intent intent = new Intent(this, ListAroundActivity.class);
            intent.putExtra("page", 0);
            startActivity(intent);
        } else {
            toast("Internet is not available.");
        }
    }

    public void onDetectLocation(View view)
    {
        // Disable some buttons, accessed within inner class, so `final` is needed
        final View detectLocationButton = findViewById(R.id.detectLocationButton);
        detectLocationButton.setEnabled(false);

        final View listAroundMeButton = findViewById(R.id.listAroundMeButton);
        listAroundMeButton.setEnabled(false);

        // Fetch the location asynchronously
        LocationListener locationListener = new OneTimeLocationListener(lm, getLocationCriteria()) {
            @Override
            public void onLocationChanged(Location newLocation) {
                super.onLocationChanged(newLocation);
                location = newLocation;
                app.setLocation(location);
                refreshLocationView();
                detectLocationButton.setEnabled(true);
                listAroundMeButton.setEnabled(true);
                toast("Successfully updated current location.");
            }
        };
    }

    public void onGiveItemButton(View view)
    {
        snapshotNewItem("give");
    }

    public void onSpotItemButton(View view)
    {
        snapshotNewItem("spot");
    }


    static final int REQUEST_IMAGE_CAPTURE = 1;

    /**
     *
     * @param action Must be either "give" or "spot".
     */
    protected void snapshotNewItem(String action)
    {
        if (!app.hasCameraSupport()) {
            toast("No camera could be found.");
            return;
        }

        // Will probably do that in another Activity anyway

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Make sure we have an Activity that can capture images
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                String msg = "An error occurred while creating the file to store the photograph.";
                Log.e("G2P", msg);
                Log.e("G2P", ex.getMessage());
                ex.printStackTrace();
                toast(msg);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ((ImageView)findViewById(R.id.captureImageView)).setImageBitmap(imageBitmap);

            Location loc = app.getLocation();
            Item item = new Item();
            item.setLocation(String.format("%f/%f", loc.getLatitude(), loc.getLongitude()));
            item.setTitle("TEST");

            app.getItemRepository().giveItem(item);
        }
    }

//    protected void uploadImage()
//    {
//        HttpClient client = new DefaultHttpClient();
//
//        try
//        {
//
//            HttpPost post = new HttpPost(URL);
//
//            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
//            entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//
//            entityBuilder.addTextBody(USER_ID, userId);
//            entityBuilder.addTextBody(NAME, name);
//            entityBuilder.addTextBody(TYPE, type);
//            entityBuilder.addTextBody(COMMENT, comment);
//            entityBuilder.addTextBody(LATITUDE, String.valueOf(User.Latitude));
//            entityBuilder.addTextBody(LONGITUDE, String.valueOf(User.Longitude));
//
//            if(file != null)
//            {
//                entityBuilder.addBinaryBody(IMAGE, file);
//            }
//
//            HttpEntity entity = entityBuilder.build();
//
//            post.setEntity(entity);
//
//            HttpResponse response = client.execute(post);
//
//            HttpEntity httpEntity = response.getEntity();
//
//            result = EntityUtils.toString(httpEntity);
//
//            Log.v("result", result);
//        }
//        catch(Exception e)
//        {
//            e.printStackTrace();
//        } finally
//        {
//            client.getConnectionManager().shutdown();
//        }
//    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "g2p_" + timeStamp + "_plop";
        imageFileName = "g2p_fuck_plop";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        storageDir.mkdirs();
        Log.i("G2P", storageDir.getPath());
        Log.i("G2P", imageFileName);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }


    // UI ACTIONS //////////////////////////////////////////////////////////////////////////////////

    public void refreshLocationView()
    {
        TextView currentLocationView = (TextView) findViewById(R.id.currentLocationView);
        if (null != location) {
            double latitude  = location.getLatitude();
            double longitude = location.getLongitude();

            currentLocationView.setText(String.format("%.4f/%.4f", latitude, longitude));
        }
    }

    // HELPERS /////////////////////////////////////////////////////////////////////////////////////

    protected void toast(String message)
    {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    protected void dump(String text)
    {
        TextView dumpTextView = (TextView) findViewById(R.id.dumpTextView);
        dumpTextView.setVisibility(View.VISIBLE);
        dumpTextView.setText(text);
    }

//    protected String getDummyJson()
//    {
//        return getString(R.string.dummyFindItemsResponse);
//    }


    // CONFIGURATION ///////////////////////////////////////////////////////////////////////////////

    protected Criteria getLocationCriteria()
    {
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);

        return criteria;
    }

    // ACTIONS /////////////////////////////////////////////////////////////////////////////////////

//    protected ArrayList<Item> findItemsAroundMe(int page)
//    {
//        ArrayList<Item> items = new ArrayList<>();
//
//        if (null != location) {
//            items = ir.findAroundPaginated(location.getLatitude(), location.getLongitude(), page);
//        }
//
//        return items;
//    }
//
//    protected ArrayList<Item> findItemsAroundMeDummy()
//    {
//        ArrayList<Item> items = new ArrayList<>();
//        // try parse the string to a JSON object
//        try {
//            JSONObject row;
//            JSONArray rows = new JSONArray(getDummyJson());
//            for (int i = 0 ; i < rows.length() ; i++) {
//                row = rows.getJSONObject(i);
//                items.add(new Item(row));
//            }
//        } catch (JSONException e) {
//            Log.e("JSON Parser", "Error parsing data : " + e.toString());
//        }
//
//        return items;
//    }
}

