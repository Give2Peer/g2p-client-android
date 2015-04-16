package org.give2peer.give2peer.task;

import android.os.AsyncTask;
import android.util.Log;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;
import org.json.JSONObject;

import java.io.File;

//import retrofit.Callback;
//import retrofit.RetrofitError;
//import retrofit.client.Response;
//import retrofit.mime.TypedFile;


public class GiveItemTask extends AsyncTask<Item, Void, Item> {
    Application app;

    public GiveItemTask(Application app) {
        this.app = app;
    }

    protected Item doInBackground(Item... items)
    {
        Item item = items[0];
        item = app.getItemRepository().giveItem(item);
//        Item freshItem = app.getRestService().giveItem(item.getLocation(), item.getTitle());

        // And then upload the picture
        File picture = item.getPictures().get(0);
        item = app.getItemRepository().pictureItem(item, picture);

//        TypedFile typedFile = new TypedFile("multipart/form-data", picture);
        //String result = app.getRestService().postPicture(item.getId(), typedFile);
        //Log.i("G2P : RESULT", result);

        return item;
    }

    protected void onPostExecute(Item item) {}
}