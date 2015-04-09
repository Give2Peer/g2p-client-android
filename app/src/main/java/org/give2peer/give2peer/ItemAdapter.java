package org.give2peer.give2peer;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;

import java.util.List;

public class ItemAdapter extends ArrayAdapter {

    public ItemAdapter(Context context, int resource, int textViewResourceId, List<Item> objects) {
        super(context, resource, textViewResourceId, objects);
    }

//    @Override
//    public View getView() {
//
//    }

}
