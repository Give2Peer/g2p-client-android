package org.give2peer.give2peer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import pl.polidea.webimageview.WebImageView;


/**
 * This adapter transforms items into their own thumbnail view.
 * Each thumbnail can be clicked on. Right now, doing so opens a third-party mapping activity.
 *
 * The `WebImageView` handles the local caching of the thumbnails, so we do not have to worry about
 * exceeding bandwidth usage. We hope there are no memory leaks in the lib we use.
 */
public class ItemAdapter extends ArrayAdapter
{
    private final List<Item> objects;
    private final int layoutResource;
    private final Context context;
    protected int size;

    @SuppressWarnings("unchecked")
    public ItemAdapter(Context context, int resource, int size, List<Item> objects)
    {
        super(context, resource, objects);
        this.context = context;
        this.size = size;
        this.objects = objects;
        this.layoutResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        GridView grid = (GridView)parent;
        final Item item = objects.get(position);
        ItemHolder holder = null;
        View row;

        if (!item.hasThumbnailView()) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResource, parent, false);
            // we need min SDK to 16 in order to use that
            //int size = grid.getRequestedColumnWidth();
            row.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, size));

            holder = new ItemHolder();
            holder.imgThumb = (WebImageView)row.findViewById(R.id.itemImageView);
            holder.txtTitle = (TextView)row.findViewById(R.id.itemTitleTextView);
            row.setTag(holder);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Start the Maps activity
                    Uri gmmIntentUri = Uri.parse(String.format("geo:0,0?q=%s,%s(%s)",
                            item.getLatitude(), item.getLongitude(), item.getTitle()));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    Activity activity = (Activity) context;
                    if (mapIntent.resolveActivity(activity.getPackageManager()) != null) {
                        activity.startActivity(mapIntent);
                    } else {
                        String msg = context.getString(R.string.toast_no_mapping_activity);
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            row = item.getThumbnailView();
            holder = (ItemHolder) row.getTag();
        }

        // Fill up the View with the item's data
        holder.txtTitle.setText(item.getThumbnailTitle());
        if (item.hasThumbnail()) {
            // The WebImageView uses internal LRU caches
            holder.imgThumb.setImageURL(item.getThumbnail());
        }

        item.setThumbnailView(row);

        return row;
    }

    static class ItemHolder
    {
        WebImageView imgThumb;
        TextView     txtTitle;
    }

}
