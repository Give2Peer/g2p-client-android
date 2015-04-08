package org.give2peer.give2peer;

import android.media.Image;

import org.json.JSONException;
import org.json.JSONObject;

public class Item {
    protected Integer id;
    protected String  title;
    protected String  description;
    protected String  location;
    protected Float   latitude;
    protected Float   longitude;
    protected Float   distance;
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

    public Image getPicture() {
        return picture;
    }

    public void setPicture(Image picture) {
        this.picture = picture;
    }
}
