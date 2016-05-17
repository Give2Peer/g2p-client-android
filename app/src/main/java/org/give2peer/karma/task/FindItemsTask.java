package org.give2peer.karma.task;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import org.give2peer.karma.Application;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.ItemAdapter;
import org.give2peer.karma.R;
import org.give2peer.karma.entity.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * An async task to find items around a provided location, by asking the server.
 * Not sure it should be in its own file...
 */
public class FindItemsTask extends AsyncTask<Void, Void, ArrayList<Item>>
{
    int page;
    Application app;
    Activity activity;

    Exception exception;

    public FindItemsTask(Application app, Activity activity, int page)
    {
        super();
        this.app = app;
        this.activity = activity;
        this.page = page;
    }

    @Override
    protected ArrayList<Item> doInBackground(Void... nope)
    {
        ArrayList<Item> items = new ArrayList<Item>();
        try {
            Location l = app.getLocation();
            app.geocodeLocationIfNeeded(l);
            double latitude  = l.getLatitude();
            double longitude = l.getLongitude();
            Collections.addAll(items, app.getRestService().findAroundPaginated(latitude, longitude, page).getItems());
//            items = app.getRestService().findAroundPaginated(latitude, longitude, page).getItems();
        } catch (Exception e) {
            exception = e;
        }
        return items;
    }

    @Override
    protected void onPostExecute(ArrayList<Item> items)
    {
        // Maybe our activity is dead ?
        if (null == activity) return;

        // Our server URI is probably wrong, GTFO.
        if (null != exception) {
            app.toast(String.format("Failure: %s", exception.getMessage()), Toast.LENGTH_LONG);
            activity.finish();
            return;
        }

        // This is a hack for API v8 to get the column width in order to have square item thumbs
        // This will probably cause headaches in landscape mode, but hey, one thing at a time
        int nbColumns = 2; // getting this procedurally requires a higher API too
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int size = dm.widthPixels / nbColumns;

        // Remove the Loading...
        View itemsLoadingSpinner = activity.findViewById(R.id.itemsLoadingSpinner);
        View itemsLoadingText    = activity.findViewById(R.id.itemsLoadingText);
        itemsLoadingSpinner.setVisibility(View.GONE);
        itemsLoadingText   .setVisibility(View.GONE);

        // Fill the gridView with our items (we commenter out code because it was annoying)
        GridView itemsGridView = (GridView) activity.findViewById(R.id.itemsGridView);
//        itemsGridView.setAdapter(new ItemAdapter(activity, R.layout.grid_item, size, items));
        itemsGridView.setVisibility(View.VISIBLE);
    }

}
