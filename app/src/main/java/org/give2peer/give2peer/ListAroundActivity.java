package org.give2peer.give2peer;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class ListAroundActivity extends ActionBarActivity
{
    ItemRepository ir;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        toast("List Around onCreate");

        setContentView(R.layout.activity_list_around);
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

