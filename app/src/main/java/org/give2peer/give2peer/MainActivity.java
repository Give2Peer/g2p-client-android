package org.give2peer.give2peer;

import android.content.Context;
import android.graphics.Color;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    // BUTTONS /////////////////////////////////////////////////////////////////////////////////////

    public void onSynchronize(View view) {
        ArrayList<Item> items = new ArrayList<Item>();
        Item item;
        for (int i = 0 ; i < 42 ; i++) {
            item = new Item();
            item.setTitle("Item "+i);
        }

        GridView gridview = (GridView) findViewById(R.id.itemsGridView);

        gridview.setBackgroundColor(Color.CYAN);


        items = findItemsAroundMe();


        toast("Synchronizing...");
        dump(items.get(0).title);
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

    protected String getDummyJson()
    {
        return getString(R.string.dummyFindItemsResponse);
    }

    protected ArrayList<Item> findItemsAroundMe()
    {
        ArrayList<Item> items = new ArrayList<>();
        // try parse the string to a JSON object
        try {
            JSONObject row;
            JSONArray rows = new JSONArray(getDummyJson());
            for (int i = 0 ; i < rows.length() ; i++) {
                row = rows.getJSONObject(i);
                items.add(new Item(row));
            }
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data : " + e.toString());
        }
        return items;
    }
}

