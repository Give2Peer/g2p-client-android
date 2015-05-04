package org.give2peer.give2peer.activity;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.fragment.LocationChooserFragment;
import org.give2peer.give2peer.fragment.ServerChooserFragment;


/**
 * This is the landing activity when a user starts the app.
 *
 * Callbacks : http://developer.android.com/training/basics/activity-lifecycle/starting.html
 *
 */
public class MainActivity extends ActionBarActivity
{

    protected Application app;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Grab the app
        app = (Application) getApplication();

        // Load the chooser fragments
        refreshServerChooser();
        refreshLocationChooser();

        // Useful, to run a query on the UI thread, for debugging ONLY of course
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshServerChooser();
        refreshLocationChooser();
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

        if (id == R.id.menu_action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // LISTENERS ///////////////////////////////////////////////////////////////////////////////////



    // UI LISTENERS ////////////////////////////////////////////////////////////////////////////////

    public void onListAroundMe(View view)
    {
        // Start the "list around" activity
        Intent intent = new Intent(this, ListAroundActivity.class);
        intent.putExtra("page", 0);
        startActivity(intent);
    }

    public void onGiveItem(View view)
    {
        snapshotNewItem("give");
    }

    public void onSpotItem(View view)
    {
        snapshotNewItem("spot");
    }

    /**
     * @param action MUST be either "give" or "spot".
     */
    protected void snapshotNewItem(String action)
    {
        // Start the "new item" activity
        Intent intent = new Intent(this, NewItemActivity.class);
        intent.putExtra("action", action);
        startActivity(intent);
    }

    public void onReportBug(View view)
    {
        String url = "http://www.give2peer.org";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    // UI ACTIONS //////////////////////////////////////////////////////////////////////////////////

//    public void refreshActionsView()
//    {
//        boolean enabled = null != app.getLocation();
//        findViewById(R.id.listAroundMeButton).setEnabled(enabled);
//        findViewById(R.id.giveItemButton).setEnabled(enabled);
//        findViewById(R.id.spotItemButton).setEnabled(enabled);
//    }

    public void refreshServerChooser()
    {
        // Ask the app to guess a server configuration
        app.setServerConfiguration(app.guessServerConfiguration());
        // Display the server chooser fragment
        getFragmentManager().beginTransaction()
                .replace(R.id.serverChooserFragment, new ServerChooserFragment())
                .commit();
    }

    public void refreshLocationChooser()
    {
        // Ask the app to guess a server configuration
        //app.setServerConfiguration(app.guessServerConfiguration());
        // Display the server chooser fragment
        getFragmentManager().beginTransaction()
                .replace(R.id.locationChooserFragment, new LocationChooserFragment())
                .commit();
    }


    // HELPERS /////////////////////////////////////////////////////////////////////////////////////

    protected void toast(String message) { toast(message, Toast.LENGTH_SHORT); }
    protected void toast(String message, int duration)
    {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    protected void dump(String text)
    {
        TextView dumpTextView = (TextView) findViewById(R.id.dumpTextView);
        dumpTextView.setVisibility(View.VISIBLE);
        dumpTextView.setText(text);
    }





    // ACTIONS /////////////////////////////////////////////////////////////////////////////////////


}

