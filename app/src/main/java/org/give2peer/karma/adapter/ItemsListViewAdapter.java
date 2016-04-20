package org.give2peer.karma.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import pl.polidea.webimageview.WebImageView;


/**
 * This adapter transforms items into their own thumbnail view.
 * Each thumbnail can be clicked on. Right now, doing so opens a third-party mapping activity.
 *
 * The `WebImageView` handles the local caching of the thumbnails, so we do not have to worry about
 * exceeding bandwidth usage. We hope there are no memory leaks in the lib we use.
 *
 * This is used in conjunction with the layout `items_list_view.xml`.
 */
public class ItemsListViewAdapter extends ArrayAdapter
{
    private final View nullView; // a dummy view to initially fill itemViews -- I'm doing it wrong
    private final List<View> itemViews; // cache for memoization of the expensive Views (bcz scrolling !)
    private final int layout;
    protected int size;


//    I'd like to understand what this is. Help ? The super() call is unchecked ? What ? The ? ...
//    @SuppressWarnings("unchecked")
    public ItemsListViewAdapter(Context context, int layout, List<Item> items)
    {
        super(context, layout, items);
        this.layout = layout; // can possibly be hardcoded, because this adapter depends on it a lot
        // dirty way to memoize item views (noob here, remember ?)
        this.nullView = new View(context);
        this.itemViews = new ArrayList<View>(Collections.nCopies(items.size(), this.nullView));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // We're going to create a View by inflating our item layout and filling its ~fields with
        // the item's properties. We'll want to memoize that View because it seems that scrolling
        // triggers getView() all the time.

        final Item item = (Item) getItem(position);
        View itemView = itemViews.get(position);

        // This can't possibly be the best way to memoize
        if (itemView == this.nullView) {

            // That item view has not been memoized yet, let's inflate it !

            LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
            itemView = inflater.inflate(layout, parent, false);

            WebImageView thumb = (WebImageView)itemView.findViewById(R.id.itemsListViewThumb);

            TextView Line1 = (TextView)itemView.findViewById(R.id.itemsListViewFirstLine);
            TextView Line2 = (TextView)itemView.findViewById(R.id.itemsListViewSecondLine);

            String sl1;
            String sl2;

            List<String> tags = item.getTags();

            sl1 = item.getTitle();
            if (sl1.isEmpty() && ! tags.isEmpty()) {
                // No title, try tags instead.
                sl1 = StringUtils.join();
            }
            if (sl1.isEmpty()) {
                // No title and no tags, move the location to the first line
                sl1 = item.getLocation();
                sl2 = item.getHumanUpdatedAt();
            } else {
                sl2 = String.format(Locale.getDefault(), "%s at %s", item.getHumanUpdatedAt(), item.getLocation());
            }
            Line1.setText(sl1);
            Line2.setText(sl2);

            // The WebImageView uses internal LRU caches so we don't have to care about caching.
            // https://github.com/Polidea/AndroidImageCache
            String thumbUrl = item.getThumbnail();
            if ( ! thumbUrl.isEmpty()) {
                thumb.setImageURL(thumbUrl);
            }

            // Memoize our newly inflated View.
            // /!\ RAM may explode if this gets too big !
            itemViews.set(position, itemView);
        }

        return itemView;
    }

    // I'm just keeping this because i still don't understand the Holder business and why it was useful
    // This is code I cut/pasted from the internet from various sources in the very early stages
    // This is a curiosity. Soooooo bad. WTFmeter exploded ! :]
//    static class ItemHolder
//    {
//        WebImageView imgThumb;
//        TextView     txtTitle;
//    }
//    @Override
//    public View getViewOld(int position, View convertView, ViewGroup parent)
//    {
//        // We're going to create a View by inflating our item layout and filling its ~fields with
//        // the item's properties. We'll want to memoize that View because it seems that scrolling
//        // triggers getView() all the time.
//
//        GridView grid = (GridView)parent;
//        final Item item = items.get(position);
//        ItemHolder holder = null;
//        View itemView;
//
//        if (!item.hasThumbnailView()) {
//            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
//            itemView = inflater.inflate(layout, parent, false);
//            // we need min SDK to 16 in order to use that
//            //int size = grid.getRequestedColumnWidth();
////            itemView.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, size));
//
//            holder = new ItemHolder();
//            holder.imgThumb = (WebImageView)itemView.findViewById(R.id.itemImageView);
//            holder.txtTitle = (TextView)itemView.findViewById(R.id.itemTitleTextView);
//            itemView.setTag(holder);
//
//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    // Start the Maps activity
//                    Uri gmmIntentUri = Uri.parse(String.format("geo:0,0?q=%s,%s(%s)",
//                            item.getLatitude(), item.getLongitude(), item.getTitle()));
//                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//                    Activity activity = (Activity) context;
//                    if (mapIntent.resolveActivity(activity.getPackageManager()) != null) {
//                        activity.startActivity(mapIntent);
//                    } else {
//                        String msg = context.getString(R.string.toast_no_mapping_activity);
//                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
//                    }
//                }
//            });
//
//        } else {
////            itemView = item.getThumbnailView();
//            holder = (ItemHolder) itemView.getTag();
//        }
//
//        // Fill up the View with the item's data
//        holder.txtTitle.setText(item.getThumbnailTitle());
//        if (item.hasThumbnail()) {
//            // The WebImageView uses internal LRU caches so we don't have to care about caching.
//            // ... I think. Not sure.
//            holder.imgThumb.setImageURL(item.getThumbnail());
//        }
//
////        item.setThumbnailView(itemView);
//
//        return itemView;
//    }

}
