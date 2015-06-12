package org.give2peer.give2peer.task;

import android.os.AsyncTask;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;
import org.give2peer.give2peer.exception.ErrorResponseException;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;


public class GiveItemTask extends AsyncTask<Item, Void, Item> {
    Application app;

    Exception exception;

    public GiveItemTask(Application app) {
        this.app = app;
    }

    protected Item doInBackground(Item... items)
    {
        Item item = items[0];

        // Upload the item properties (at least try to)
        try {
            item = app.getRestService().giveItem(item);
        } catch (URISyntaxException | IOException | JSONException | ErrorResponseException e) {
            exception = e;
            return item;
        }

        // And then upload the picture
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

    protected void onPostExecute(Item item) {}
}