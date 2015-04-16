package org.give2peer.give2peer.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;
import org.give2peer.give2peer.ItemAdapter;
import org.give2peer.give2peer.ItemRepository;
import org.give2peer.give2peer.R;

import java.util.ArrayList;


/**
 * This fullscreen activity lists items in a grid
 */
public class ListAroundActivity extends Activity
{
    Application app;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_around);

        // Extract some parameters from the intent
        Intent in = getIntent();
        int page = in.getIntExtra("page", 0);

        // Grab the app
        app = (Application) getApplication();

        // Launch the Task
        FindItemsTask fit = (FindItemsTask) new FindItemsTask(this, page).execute();

        // Display a "Please wait" message
        Toast.makeText(getApplicationContext(),
                getString(R.string.toast_connecting_please_wait), Toast.LENGTH_LONG).show();
    }

    private class FindItemsTask extends AsyncTask<Void, Void, ArrayList<Item>>
    {

        int page;
        Context context;

        public FindItemsTask(Context context, int page)
        {
            super();
            this.page = page;
            this.context = context;
        }

        @Override
        protected ArrayList<Item> doInBackground(Void... nope)
        {
            ArrayList<Item> items = new ArrayList<Item>();
            try {
                double latitude  = app.getLocation().getLatitude();
                double longitude = app.getLocation().getLongitude();
                items = app.getItemRepository().findAroundPaginated(latitude, longitude, page);
            } catch (Exception e) {
                Log.e(this.getClass().toString(), e.getMessage());
                e.printStackTrace();
            }
            return items;
        }

        @Override
        protected void onPostExecute(ArrayList<Item> items)
        {
            // This is a hack for API v8 to get the column width in order to have square item thumbs
            // This will probably cause headaches in landscape mode, but hey, one thing at a time
            int nbColumns = 2; // getting this procedurally requires a higher API too
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int size = dm.widthPixels / nbColumns;

            // Remove the Loading...
            View itemsLoadingSpinner = findViewById(R.id.itemsLoadingSpinner);
            itemsLoadingSpinner.setVisibility(View.GONE);
            // Fill the gridView with our items
            GridView itemsGridView = (GridView) findViewById(R.id.itemsGridView);
            itemsGridView.setAdapter(new ItemAdapter(context, R.layout.grid_item, size, items));
            itemsGridView.setVisibility(View.VISIBLE);
        }
    }


    // LISTENERS ///////////////////////////////////////////////////////////////////////////////////


    // UI LISTENERS ////////////////////////////////////////////////////////////////////////////////


    // UI ACTIONS //////////////////////////////////////////////////////////////////////////////////


    // HELPERS /////////////////////////////////////////////////////////////////////////////////////


    // CONFIGURATION ///////////////////////////////////////////////////////////////////////////////


    // ACTIONS /////////////////////////////////////////////////////////////////////////////////////

}

