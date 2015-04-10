package org.give2peer.give2peer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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
        GridView grid = (GridView)parent;
        Item item = objects.get(position);
        ItemHolder holder = null;
        View row;

        if (!item.hasThumbnailView()) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResource, parent, false);
            row.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, size));

            Log.i("ItemAdapter", "Row is null for position #"+position);

            holder = new ItemHolder();
            holder.imgThumb = (ImageView)row.findViewById(R.id.itemImageView);
            holder.txtTitle = (TextView)row.findViewById(R.id.itemTitleTextView);

            row.setTag(holder);
        } else {
            row = item.getThumbnailView();
            holder = (ItemHolder) row.getTag();
        }

        holder.txtTitle.setText(item.getThumbnailTitle());
        if (item.hasThumbnail()) {
            holder.imgThumb.setImageBitmap(item.getThumbnail());
        } else {
            item.downloadThumbnail(holder.imgThumb);
        }

        item.setThumbnailView(row);

        Log.i("ItemAdapter", "Returned row for position #"+position);

        return row;
    }

    static class ItemHolder
    {
        ImageView imgThumb;
        TextView  txtTitle;
    }

}
