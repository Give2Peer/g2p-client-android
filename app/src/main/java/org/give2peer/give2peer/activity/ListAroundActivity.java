package org.give2peer.give2peer.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.task.FindItemsTask;


/**
 * This fullscreen activity lists items in a grid, from closest to furthest.
 * It lists at most 32 items. (as server API returns at most 32 items)
 *
 * Ideas:
 *   - list more items when we get to the bottom
 */
public class ListAroundActivity extends Activity
{
    Application app;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Extract some parameters from the intent
        Intent in = getIntent();
        int page = in.getIntExtra("page", 0);

        // Grab the app
        app = (Application) getApplication();

        // Make sure we have a location
        if (null == app.getGeoLocation()) {
            app.toast(getString(R.string.toast_please_set_up_location));
            finish();
            return;
        }

        setContentView(R.layout.activity_list_around);

        // Launch the Task
        FindItemsTask fit = (FindItemsTask) new FindItemsTask(app, this, page).execute();

        // Display a "Please wait" message
        app.toast(getString(R.string.toast_connecting_please_wait), Toast.LENGTH_LONG);
    }

    // LISTENERS ///////////////////////////////////////////////////////////////////////////////////


    // UI LISTENERS ////////////////////////////////////////////////////////////////////////////////


    // UI ACTIONS //////////////////////////////////////////////////////////////////////////////////


    // HELPERS /////////////////////////////////////////////////////////////////////////////////////


    // CONFIGURATION ///////////////////////////////////////////////////////////////////////////////


    // ACTIONS /////////////////////////////////////////////////////////////////////////////////////

}

