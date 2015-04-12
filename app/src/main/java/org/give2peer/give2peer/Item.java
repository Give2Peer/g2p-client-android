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
 * They handle their own image downloads through async tasks.
 */
public class Item
{
    protected Integer id;
    protected String  title;
    protected String  description;
    protected String  location;
    protected Float   latitude;
    protected Float   longitude;
    protected Float   distance;

    protected Bitmap                thumbnail;
    protected String                thumbnailUrl;
    protected View                  thumbnailView;
    protected DownloadItemThumbTask downloadThumbTask;

    protected Image   picture;

    public Item() {}

    public Item(JSONObject json) {
        updateWithJSON(json);
    }

    public Item updateWithJSON(JSONObject json) {
        try {
            setId(json.getInt("id"));
            setTitle(json.optString("title"));
            setDescription(json.optString("description"));
            setLocation(json.getString("location"));
            setLatitude((float)json.getDouble("latitude"));
            setLongitude((float)json.getDouble("longitude"));
            setDistance((float)json.getDouble("distance"));
            setThumbnailUrl(json.optString("thumbnail"));
        } catch (JSONException e) {
            Log.e("Item", e.getMessage());
            e.printStackTrace();
        }

        return this;
    }

    public String toString() {
        return getTitle() + ' ' + getHumanDistance();
    }

    /**
     * Returns a string describing the distance in human-readable format, like :
     * - 42m
     * - 4,2km
     * - 42km
     *
     * This could be easily unit-tested. It isn't. But it could.
     */
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

    /**
     * @return a concatenation of the distance and the title, for the thumbnail
     */
    public String getThumbnailTitle() {
        String s = getHumanDistance();
        if (title.length() > 0) {
            s = s + "  " + title;
        }
        return s;
    }

    /**
     * Download the thumbnail in an async task.
     */
    public void downloadThumbnail() {
        downloadThumbnail(null);
    }

    /**
     * Download the thumbnail in an async task.
     *
     * @param viewToUpdate An optional ImageView to update with the thumbnail
     */
    public void downloadThumbnail(ImageView viewToUpdate) {
        // Ignore subsequent calls to download if we already tried
        if (null != downloadThumbTask) return;

        // Download the thumbnail
        if (thumbnailUrl.length() > 0) {
            try {
                Log.i("DownloadItemThumbTask", "Downloading from " + thumbnailUrl);
                downloadThumbTask = new DownloadItemThumbTask(this, viewToUpdate);
                downloadThumbTask.execute(thumbnailUrl);
            } catch (Exception ex) {
                Log.e("DownloadItemThumbTask", ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // ACCESSORS AND MUTATORS //////////////////////////////////////////////////////////////////////

    public Integer getId() { return id; }

    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }

    public Float getLatitude() { return latitude; }

    public void setLatitude(Float latitude) { this.latitude = latitude; }

    public Float getLongitude() { return longitude; }

    public void setLongitude(Float longitude) { this.longitude = longitude; }

    public Float getDistance() { return distance; }

    public void setDistance(Float distance) { this.distance = distance; }

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

    public void setThumbnail(Bitmap thumbnail) { this.thumbnail = thumbnail; }

    public String getThumbnailUrl() { return thumbnailUrl; }

    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public boolean hasThumbnailView() { return null != thumbnailView; }

    public View getThumbnailView() { return thumbnailView; }

    public void setThumbnailView(View thumbnailView) { this.thumbnailView = thumbnailView; }

}
