package org.give2peer.give2peer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class ListAroundActivity extends Activity
{
    ItemRepository ir;

    Application app;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_around);

        Intent in = getIntent();

        int page = in.getIntExtra("page", 0);

        toast("List Around onCreate with offset "+page);

        // Grab the app
        app = (Application) getApplication();
        ir = app.getItemRepository();

        double latitude  = app.getLocation().getLatitude();
        double longitude = app.getLocation().getLongitude();

        // This is a hack for API v8 to get the column width in order to have square item thumbs
        // This will probably cause headaches in landscape mode, but hey, one thing at a time
        int nbColumns = 2; // getting this procedurally requires a higher API too
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int size = dm.widthPixels / nbColumns;

        // Grab the contents asynchronously todo
        ArrayList<Item> items = ir.findAroundPaginated(latitude, longitude, page);

        // Fill the gridView with our items
        GridView itemsGridView = (GridView) findViewById(R.id.itemsGridView);
        itemsGridView.setAdapter(new ItemAdapter(this, R.layout.grid_item, size, items));

        // Display a "Please wait" message
        toast("All done !");

    }

//    private class FindItemsTask extends AsyncTask<String, Void, String> {
//        @Override
//        protected String doInBackground(String... urls) {
//            String response = "";
//            try {
//                items = findItemsAroundMe(0);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return response;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            textView.setText(result);
//        }
//    }

//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu)
//    {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item)
//    {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    // LISTENERS ///////////////////////////////////////////////////////////////////////////////////


    // UI LISTENERS ////////////////////////////////////////////////////////////////////////////////


    // UI ACTIONS //////////////////////////////////////////////////////////////////////////////////


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

    // CONFIGURATION ///////////////////////////////////////////////////////////////////////////////


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
}

