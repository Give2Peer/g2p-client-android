package org.give2peer.karma.task;

import android.app.Activity;
import android.os.AsyncTask;

import org.give2peer.karma.Application;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.exception.AuthorizationException;
import org.give2peer.karma.exception.ErrorResponseException;
import org.give2peer.karma.exception.MaintenanceException;
import org.give2peer.karma.exception.QuotaException;
import org.give2peer.karma.response.CreateItemResponse;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;


public class NewItemTask extends AsyncTask<Void, Void, Item>
{
    public Application app;
    public Activity activity;
    private final Item item;
    private final List<File> pictures;

    Exception exception;

    public NewItemTask(Application app, Activity activity, Item item, List<File> pictures)
    {
        this.app = app;
        this.activity = activity;
        this.item = item;
        this.pictures = pictures;
    }

    protected Item doInBackground(Void... nope)
    {
        Item item;

        // Upload the item properties (at least try to)
        try {
            item = app.getRestService().createItem(this.item).getItem();
        } catch (Exception e) {
            exception = e;
            return null;
        }

        // And if successful then upload the picture
        // We only upload the first one for now, but ideally we should try to upload all of them
        try {
            if ( ! pictures.isEmpty()) {
                File picture = pictures.get(0);
                item = app.getRestService().pictureItem(item, picture).getItem();
            }
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