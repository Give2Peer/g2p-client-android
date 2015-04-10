package org.give2peer.give2peer;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Items are first-class citizens in this app.
 * They are fetched as JSON from the server.
 * They handle their image downloads through async tasks.
 */
public class Item {
    protected Integer id;
    protected String  title;
    protected String  description;
    protected String  location;
    protected Float   latitude;
    protected Float   longitude;
    protected Float   distance;

    protected Bitmap                thumbnail;
    protected DownloadItemThumbTask downloadThumbTask;
    protected View                  thumbnailView;

    protected Image   picture;

    public Item() {}

    public Item(JSONObject json) {
        try {
            setId(json.getInt("id"));
            setTitle(json.optString("title"));
            setDescription(json.optString("description"));
            setLocation(json.getString("location"));
            setLatitude(new Float(json.getDouble("latitude")));
            setLongitude(new Float(json.getDouble("longitude")));
            setDistance(new Float(json.getString("distance")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return getTitle() + ' ' + getHumanDistance();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Float getLatitude() {
        return latitude;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public void setLongitude(Float longitude) {
        this.longitude = longitude;
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public String getHumanDistance() {
        int meters = Math.round(distance);
        if (meters <= 999) {
            return String.format("%dm", meters);
        } else if (meters > 999 && meters <= 9999) {
            return String.format("%.1fkm", meters/1000.0);
        } else if (meters > 9999) {
            return String.format("%dkm", Math.round(meters / 1000.0));
        }
        return String.format("%dm", meters);
    }

    public String getThumbnailTitle() {
        String s = getHumanDistance();
        if (title.length() > 0) {
            s = s + "  " + title;
        }
        return s;
    }

    public Image getPicture() {
        return picture;
    }

    public void setPicture(Image picture) {
        this.picture = picture;
    }

    public boolean hasThumbnail() {
        return null != thumbnail;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void downloadThumbnail() {
        downloadThumbnail(null);
    }
    public void downloadThumbnail(ImageView viewToUpdate) {
        if (null != downloadThumbTask) return;

        // We also need their images
        // fixme
        String imageUrl = "http://www.gamasutra.com/db_area/images/news/2014/Sep/226054/android_logo.jpg";
        if (getDistance() > 3000) {
            imageUrl = "http://www.51osos.com/wp-content/uploads/2011/12/linux.jpg";
        }
        // Get an Image
        try {
            Log.e("DownloadItemThumbTask", "Starting download of " + imageUrl.substring(11));
            downloadThumbTask = new DownloadItemThumbTask(this, viewToUpdate);
            downloadThumbTask.execute(imageUrl);
        } catch (Exception ex) {
            Log.e("DownloadItemThumbTask", ex.getMessage());
            ex.printStackTrace();
        }
    }

    public boolean hasThumbnailView() {
        return null != thumbnailView;
    }

    public View getThumbnailView() {
        return thumbnailView;
    }

    public void setThumbnailView(View thumbnailView) {
        this.thumbnailView = thumbnailView;
    }

    //    public static double round(double value, int places) {
//        if (places < 0) throw new IllegalArgumentException();
//
//        BigDecimal bd = new BigDecimal(value);
//        bd = bd.setScale(places, RoundingMode.HALF_UP);
//        return bd.doubleValue();
//    }
}
