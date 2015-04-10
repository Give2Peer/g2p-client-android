package org.give2peer.give2peer;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;



public class ItemAdapter extends ArrayAdapter {

    private final List<Item> objects;
    private final int layoutResource;
    private final Context context;
    protected int size;

    public ItemAdapter(Context context, int resource, int size, List<Item> objects) {
        super(context, resource, objects);
        this.context = context;
        this.size = size;
        this.objects = objects;
        this.layoutResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        GridView grid = (GridView)parent;
//        int size = grid.getRequestedColumnWidth();

        ItemHolder holder = null;
        View row = convertView;

        if (null == row) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResource, parent, false);

            holder = new ItemHolder();
//        holder.imgIcon = (ImageView)row.findViewById(R.id.imgIcon);
            holder.txtTitle = (TextView)row.findViewById(R.id.itemTitleTextView);

            row.setTag(holder);
        } else {
            holder = (ItemHolder) row.getTag();
        }

        Item item = objects.get(position);
        holder.txtTitle.setText(item.getThumbnailTitle());

        row.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, size));

        return row;
    }

    static class ItemHolder
    {
        ImageView imgThumb;
        TextView  txtTitle;
    }

}
