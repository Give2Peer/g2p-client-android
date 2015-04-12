package org.give2peer.give2peer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
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

        FindItemsTask fit = (FindItemsTask) new FindItemsTask(this, page).execute();

        // Display a "Please wait" message
//        toast("All done !");

    }

    private class FindItemsTask extends AsyncTask<Void, Void, ArrayList<Item>> {

        int page;
        Context context;

        public FindItemsTask(Context context, int page)
        {
            super();
            this.page = page;
            this.context = context;
        }

        @Override
        protected ArrayList<Item> doInBackground(Void... nope) {
            ArrayList<Item> items = new ArrayList<Item>();
            try {
                double latitude  = app.getLocation().getLatitude();
                double longitude = app.getLocation().getLongitude();
                items = ir.findAroundPaginated(latitude, longitude, page);
            } catch (Exception e) {
                Log.e(this.getClass().toString(), e.getMessage());
                e.printStackTrace();
            }
            return items;
        }

        @Override
        protected void onPostExecute(ArrayList<Item> result)
        {
            // This is a hack for API v8 to get the column width in order to have square item thumbs
            // This will probably cause headaches in landscape mode, but hey, one thing at a time
            int nbColumns = 2; // getting this procedurally requires a higher API too
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int size = dm.widthPixels / nbColumns;

            // Remove the Loading...
            TextView itemsLoadingTextView = (TextView) findViewById(R.id.itemsLoadingTextView);
            itemsLoadingTextView.setVisibility(View.GONE);
            // Fill the gridView with our items
            GridView itemsGridView = (GridView) findViewById(R.id.itemsGridView);
            itemsGridView.setAdapter(new ItemAdapter(context, R.layout.grid_item, size, result));
            itemsGridView.setVisibility(View.VISIBLE);
        }
    }

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

