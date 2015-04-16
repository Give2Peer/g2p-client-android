package org.give2peer.give2peer.task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;
import android.os.AsyncTask;

import org.give2peer.give2peer.Item;

import java.io.*;


public class DownloadItemThumbTask extends AsyncTask<String, Void, Bitmap> {
    Item item;
    ImageView thumbView;

    public DownloadItemThumbTask(Item item, ImageView thumbView) {
        this.item = item;
        this.thumbView = thumbView;
    }

    protected Bitmap doInBackground(String... urls) {
        String urlDisplay = urls[0];
        Bitmap thumb = null;
        try {
            InputStream in = new java.net.URL(urlDisplay).openStream();
            thumb = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return thumb;
    }

    protected void onPostExecute(Bitmap thumb) {
        if (null != thumb) {
            item.setThumbnailBitmap(thumb);
            thumbView.setImageBitmap(thumb);
        }
    }
}