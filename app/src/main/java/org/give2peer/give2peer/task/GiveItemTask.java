package org.give2peer.give2peer.task;

import android.os.AsyncTask;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;


public class GiveItemTask extends AsyncTask<Item, Void, Item> {
    Application app;

    public GiveItemTask(Application app) {
        this.app = app;
    }

    protected Item doInBackground(Item... items) {
        Item item = items[0];
        item = app.getItemRepository().giveItem(item);
        return item;
    }

    protected void onPostExecute(Item item) {}
}