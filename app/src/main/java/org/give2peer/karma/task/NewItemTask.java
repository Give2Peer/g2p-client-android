package org.give2peer.karma.task;

import android.app.Activity;
import android.os.AsyncTask;

import org.give2peer.karma.Application;
import org.give2peer.karma.Item;
import org.give2peer.karma.exception.AuthorizationException;
import org.give2peer.karma.exception.ErrorResponseException;
import org.give2peer.karma.exception.MaintenanceException;
import org.give2peer.karma.exception.QuotaException;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;


public class NewItemTask extends AsyncTask<Item, Void, Item>
{
    public Application app;
    public Activity activity;

    Exception exception;

    public NewItemTask(Application app, Activity activity) {
        this.app = app;
        this.activity = activity;
    }

    protected Item doInBackground(Item... items)
    {
        Item item = items[0];

        // Upload the item properties (at least try to)
        try {
            item = app.getRestService().giveItem(item);
        } catch (URISyntaxException | IOException | JSONException | ErrorResponseException |
                MaintenanceException | QuotaException | AuthorizationException e) {
            exception = e;
            return item;
        }

        // And if successful then upload the picture
        try {
            File picture = item.getPictures().get(0);
            item = app.getRestService().pictureItem(item, picture);
        } catch (Exception e) {
            exception = e;
            return item;
        }

        return item;
    }

    public boolean hasException()   { return null != exception; }

    public Exception getException() { return exception;         }

    /**
     * Override this to provide scoped logic.
     * @param item that was added
     */
    protected void onPostExecute(Item item) {}
}