package org.give2peer.karma;

import android.media.Image;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Items are first-class citizens in this app.
 * They are fetched as JSON from the server.
 * They handle their own image downloads through async tasks.
 *
 * DEPRECATED
 *
 * Use entity.Item instead, as this fails to be loaded by GSON, but only in the code
 * ran on devices (physical or emulated). IT WORKS in our unit-tests, though, which is UNNERVING.
 */
public class Item
{
    public Integer  id;
    public String   title;
    public String   description;
    public String   location;
    public Float    latitude;
    public Float    longitude;
    public Float    distance; // in meters
    public DateTime updatedAt;

    public String   thumbnail;
    public View     thumbnailView;

    // not sure this is used ?
    protected Image    picture;

    // Pictures to upload, (and maybe downloaded pictures, later on)
    protected List<File> pictures = new ArrayList<>();

    public Item() {}

    public Item(JSONObject json) throws JSONException { updateWithJSON(json); }

    public Item updateWithJSON(JSONObject json) throws JSONException
    {
        setId(json.getInt("id"));
        setTitle(json.optString("title"));
        setDescription(json.optString("description"));
        setLocation(json.getString("location"));
        setLatitude((float) json.getDouble("latitude"));
        setLongitude((float) json.getDouble("longitude"));
        setDistance((float) json.optDouble("distance", 0));
        setUpdatedAt(new DateTime(json.getString("updated_at")));
        setThumbnail(json.optString("thumbnail"));

        return this;
    }

    public String toString() { return getTitle() + ' ' + getHumanDistance(); }

    /**
     * Returns a string describing the distance in human-readable format, like :
     * - 42m
     * - 4,2km
     * - 42km
     *
     * This could be easily unit-tested. It isn't. But it could.
     *
     * Also, we don't support non-scientific units, Nor we plan to.
     * (it's an ideological choice, not a technological one)
     *
     * This method (and the one after) can (and probably should) be refactored elsewhere.
     */
    public String getHumanDistance()
    {
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

    public String getHumanUpdatedAt()
    {
        return new PrettyTime().format(updatedAt.toDate());
    }

    /**
     * @return a concatenation of the distance and the title, for the thumbnail.
     */
    public String getThumbnailTitle()
    {
        String s = getHumanDistance();
        if (title.length() > 0) {
            s = s + "  " + title;
        }
        return s;
    }

    /**
     * A convenience method to generate a `LatLng` object that some third-party applications
     * (notably, Google Maps) use.
     */
    public LatLng getLatLng() { return new LatLng(getLatitude(), getLongitude()); }


    // VANILLA ACCESSORS AND MUTATORS //////////////////////////////////////////////////////////////

    public Integer getId()                           { return id;                                  }

    public void setId(Integer id)                    { this.id = id;                               }

    public String getTitle()                         { return title;                               }

    public void setTitle(String title)               { this.title = title;                         }

    public String getDescription()                   { return description;                         }

    public void setDescription(String description)   { this.description = description;             }

    public String getLocation()                      { return location;                            }

    public void setLocation(String location)         { this.location = location;                   }

    public Float getLatitude()                       { return latitude;                            }

    public void setLatitude(Float latitude)          { this.latitude = latitude;                   }

    public Float getLongitude()                      { return longitude;                           }

    public void setLongitude(Float longitude)        { this.longitude = longitude;                 }

    public Float getDistance()                       { return distance;                            }

    public void setDistance(Float distance)          { this.distance = distance;                   }

    public DateTime getUpdatedAt()                   { return updatedAt;                           }

    public void setUpdatedAt(DateTime updatedAt)     { this.updatedAt = updatedAt;                 }

    public Image getPicture()                        { return picture;                             }

    public void setPicture(Image picture)            { this.picture = picture;                     }

    public boolean hasThumbnail()                    { return ! thumbnail.isEmpty();               }

    public String getThumbnail()                     { return thumbnail;                           }

    public void setThumbnail(String thumbnail)       { this.thumbnail = thumbnail;                 }

    public boolean hasThumbnailView()                { return null != thumbnailView;               }

    public View getThumbnailView()                   { return thumbnailView;                       }

    public void setThumbnailView(View thumbnailView) { this.thumbnailView = thumbnailView;         }

    public List<File> getPictures()                  { return pictures;                            }

    public void setPictures(List<File> pictures)     { this.pictures = pictures;                   }
}
