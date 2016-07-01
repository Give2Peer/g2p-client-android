package org.give2peer.karma.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.give2peer.karma.Application;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rsv.widget.WebImageView;


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
    public View getView(final int position, View convertView, ViewGroup parent)
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

            sl1 = item.getHumanTitle(getContext());
            sl2 = item.getHumanUpdatedAt();

            Line1.setText(sl1);
            Line2.setText(sl2);

            // The WebImageView uses internal LRU caches so we don't have to care about caching.
            // https://github.com/Polidea/AndroidImageCache
            String thumbUrl = item.getThumbnailNoSsl();
            if ( ! thumbUrl.isEmpty()) {
                thumb.setWebImageUrl(thumbUrl);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Activity a = (Activity) getContext();
                    ((Application) a.getApplication()).showItemPopup(a, item);
                }
            });

            // Memoize our newly inflated View.
            // /!\ RAM may explode if this gets too big !
            itemViews.set(position, itemView);
        }

        return itemView;
    }
}
