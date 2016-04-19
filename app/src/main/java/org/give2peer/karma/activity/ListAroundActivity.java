package org.give2peer.karma.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.entity.Location;
import org.give2peer.karma.task.FindItemsTask;


/**
 *
 *
 *
 * THIS IS NOT USED ANYMORE. But it provides snippets. Yeah, I know, git. Still... Alone noob here !
 *
 *
 *
 *
 * This fullscreen activity lists items in a grid, from closest to furthest.
 * It lists at most 32 items. (as server API returns at most 32 items)
 *
 * Ideas:
 *   - list more items when we get to the bottom
 *   - don't reload the items when the orientation changes
 */
public class ListAroundActivity extends Activity
{
    Application app;

    public FindItemsTask fit;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Grab the app
        app = (Application) getApplication();

        // Make sure we're online
        if (!app.isOnline()) {
            app.toast(getString(R.string.toast_no_internet_available));
            finish();
            return;
        }

        // Grab the Location, and make sure we have one set
        Location location = app.getLocation();
        if (null == location) {
            app.toast(getString(R.string.toast_no_location_available));
            finish();
            return;
        }
        if (!location.hasLatLng() && location.getPostal().isEmpty()) {
            app.toast(getString(R.string.toast_invalid_location));
            finish();
            return;
        }

        // Extract some parameters from the intent
        Intent in = getIntent();
        int page = in.getIntExtra("page", 0);


        // Set the content from the layout
        setContentView(R.layout.activity_list_around);

        // Launch the asynchronous Task
        fit = (FindItemsTask) new FindItemsTask(app, this, page).execute();
    }

    // LISTENERS ///////////////////////////////////////////////////////////////////////////////////


    // UI LISTENERS ////////////////////////////////////////////////////////////////////////////////


    // UI ACTIONS //////////////////////////////////////////////////////////////////////////////////


    // HELPERS /////////////////////////////////////////////////////////////////////////////////////


    // CONFIGURATION ///////////////////////////////////////////////////////////////////////////////


    // ACTIONS /////////////////////////////////////////////////////////////////////////////////////

}
