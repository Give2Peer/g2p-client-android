package org.give2peer.karma.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.orm.SugarRecord;

import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.utils.StringUtils;
import org.give2peer.karma.exception.CriticalException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.List;
import java.util.Locale;

/**
 * GSON-friendly Item.
 *
 * Note: why not replace "Human" by "Display" in the helper methods ?
 */
public class Item extends SugarRecord implements Parcelable
{

    // Long  id; // provided by SugarRecord
    String   title;
    String   description;
    String   type = Item.TYPE_MOOP; // server will only accept one of Item.TYPE_****
    String   location;
    Float    latitude;
    Float    longitude;
    Float    distance; // in meters, not always provided by the server
    DateTime created_at;
    DateTime updated_at;
    String   thumbnail;

    List<String> tags;

    User author;

    // It would be best for these to be constants.
    static public String TYPE_MOOP = "moop";
    static public String TYPE_GIFT = "gift";
    static public String TYPE_LOST = "lost";

    static public int TITLE_LENGTH = 32;


    //// CONSTRUCTOR ///////////////////////////////////////////////////////////////////////////////

    public Item() {} // maybe needed, maybe not.


    //// PARCELABLE ////////////////////////////////////////////////////////////////////////////////

    protected Item(Parcel in) {
        setId(in.readLong());
        title = in.readString();
        description = in.readString();
        type = in.readString();
        location = in.readString();
        latitude = in.readFloat();
        longitude = in.readFloat();
        created_at = new DateTime(in.readLong(), DateTimeZone.forID(in.readString()));
        updated_at = new DateTime(in.readLong(), DateTimeZone.forID(in.readString()));
        thumbnail = in.readString();
        author = in.readParcelable(User.class.getClassLoader());
        tags = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(getId());
        parcel.writeString(title);
        parcel.writeString(description);
        parcel.writeString(type);
        parcel.writeString(location);
        parcel.writeFloat(latitude);
        parcel.writeFloat(longitude);
        parcel.writeLong(created_at.getMillis());
        parcel.writeString(created_at.getZone().getID());
        parcel.writeLong(updated_at.getMillis());
        parcel.writeString(updated_at.getZone().getID());
        parcel.writeString(thumbnail);
        parcel.writeParcelable(author, flags);
        parcel.writeStringList(tags);
    }

    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }


    //// DESCRIPTION ///////////////////////////////////////////////////////////////////////////////

    /**
     * @return the concatenated title and (humanly displayed) distance.
     */
    public String toString()
    {
        String s = getTitle();
        String distance = getHumanDistance();
        if ( ! distance.isEmpty()) {
            s += " " + distance;
        }

        return s;
    }

    /**
     * Create a non-empty title, gathering information about the item to complete if needed.
     * This is very much a work in progress. I expect to come back on this.
     *
     * Order :
     *     (type) (tags) (title)
     *       t0     t1     t2
     *
     * Priority :
     *   - title t2
     *   - type  t0
     *   - tags  t1
     * Only show subsequent parts if there's room for them in a TITLE_LENGTH long title.
     *
     * @param context we need a context for getString()
     * @return something readable as the item's title
     */
    public String getHumanTitle(Context context)
    {
        String title = getTitle();
        String tags = "";
        List<String> tagsList = getTags();
        if ( ! tagsList.isEmpty()) {
            // org.apache.commons.lang3.StringUtils? WTF I'm doing it wrong, or Java is.
            tags = org.apache.commons.lang3.StringUtils.join(tagsList, ' ');
        }
        String type;
        if (isGift()) {
            type = context.getString(R.string.new_item_type_gift);
        } else if (isLost()) {
            type = context.getString(R.string.new_item_type_lost);
        } else if (isMoop()) {
            type = context.getString(R.string.new_item_type_moop);
        } else {
            throw new CriticalException(String.format(
                    Locale.FRENCH, "There is no title nor type on item #%d.", getId()
            ));
        }

        // --- made with grilvhor (but not BY grilvhor T_T)

        String ht = "";

        if (title.length() > 3) {
            ht = title;
        } else {
            ht = type + " " + title;
        }

        if (ht.length() + tags.length() + 1 <= TITLE_LENGTH) {
            ht = tags + " " + ht;
        }

        // --- post-processing

        // Remove duplicate, leading and trailing whitespaces. (regex4life)
        ht = org.apache.commons.lang3.StringUtils.replacePattern(ht, "\\s{2,}", " ");
        ht = org.apache.commons.lang3.StringUtils.replacePattern(ht, "^\\s+", "");
        ht = org.apache.commons.lang3.StringUtils.replacePattern(ht, "\\s+$", "");

        return ht;
    }

    /**
     * Returns a string describing the distance in human-readable format, like :
     * - 42m
     * - 4,2km
     * - 42km
     *
     * This could be easily unit-tested. It isn't. But it could.
     *
     * Also, we don't support non-scientific measurement units, like miles. Nor we plan to.
     * (it's an ideological choice, not a technological one -- the fascism of the teacher I guess)
     * Argumentation: 1. Elegance. 2. Science is already hard enough.
     *
     * This method (and the one after) can (and probably should) be refactored elsewhere.
     */
    public String getHumanDistance()
    {
        Locale locale = Application.getLocale();
        String humanDistance = "";
        if (null != distance) {
            int meters = Math.round(distance);
            if (meters <= 999) {
                humanDistance = String.format(locale, "%dm", meters);
            } else if (meters > 999 && meters <= 9999) {
                humanDistance = String.format(locale, "%.1fkm", meters/1000.0);
            } else if (meters > 9999) {
                humanDistance = String.format(locale, "%dkm", Math.round(meters / 1000.0));
            } // what's next ? Astronomical Units ? MegaMeters feel so weird!
        }

        return humanDistance;
    }

    /**
     * @return a string describing when this item was last described, such as "2 minutes ago".
     */
    public String getHumanUpdatedAt()
    {
        return new PrettyTime().format(updated_at.toDate());
    }

    /**
     * A convenience method.
     * @return a `LatLng` object that some third-party applications (notably, Google Maps) use.
     */
    public LatLng getLatLng() { return new LatLng(getLatitude(), getLongitude()); }

    /**
     * See https://github.com/Polidea/AndroidImageCache/issues/6
     * Note: the other image cache lib we're using is choking on our dirty https too.
     * @return the thumbnail URL in `http`, not `https`.
     */
    public String getThumbnailNoSsl()
    {
        return StringUtils.httpsToHttp(getThumbnail());
    }


    //// MAP MARKERS ///////////////////////////////////////////////////////////////////////////////

    public int getMapMarkerIconResource() {
        int i; if (isGift()) {
            i = R.drawable.ic_marker_gift_48dp;
        } else if (isLost()) {
            i = R.drawable.ic_marker_lost_48dp;
        } else if (isMoop()) {
            i = R.drawable.ic_marker_moop_48dp;
        } else  {
            throw new CriticalException(String.format("Unknown item type '%s'.", getType()));
        }

        return i;
    }

    public BitmapDescriptor getMapMarkerIcon() {
        return BitmapDescriptorFactory.fromResource(getMapMarkerIconResource());
    }

    public float getMapMarkerU() { return 0.500f; } // derived from `assets/map_marker.xcf`
    public float getMapMarkerV() { return 0.916f; } // 1018.0 / 1111.0

    //// VANILLA ACCESSORS AND MUTATORS ////////////////////////////////////////////////////////////

//  Id is handled by SugarRecord.
//  public Long getId()                              { return id;                                  }
//  public void setId(Long id)                       { this.id = id;                               }

    public String getType()                          { return type;                                }

    public void setType(String type)                 { this.type = type;                           }

    public boolean isGift()                          { return TYPE_GIFT.equals(this.type);         }

    public boolean isLost()                          { return TYPE_LOST.equals(this.type);         }

    public boolean isMoop()                          { return TYPE_MOOP.equals(this.type);         }

    public String getTitle()                         { return title;                               }

    public void setTitle(String title)               { this.title = title;                         }

    public boolean hasDescription()                  { return ! description.isEmpty();             }

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

    public DateTime getCreatedAt()                   { return created_at;                          }

    public void setCreatedAt(DateTime createdAt)     { this.created_at = createdAt;                }

    public DateTime getUpdatedAt()                   { return updated_at;                          }

    public void setUpdatedAt(DateTime updatedAt)     { this.updated_at = updatedAt;                }

    public boolean hasThumbnail()                    { return ! thumbnail.isEmpty();               }

    public String getThumbnail()                     { return thumbnail;                           }

    public void setThumbnail(String thumbnail)       { this.thumbnail = thumbnail;                 }

    public List<String> getTags()                    { return tags;                                }

    public void setTags(List<String> tags)           { this.tags = tags;                           }

    public User getAuthor()                          { return author;                              }

//    public Image getPicture()                        { return picture;                             }
//
//    public void setPicture(Image picture)            { this.picture = picture;                     }
//
//    public List<File> getPictures()                  { return pictures;                            }
//
//    public void setPictures(List<File> pictures)     { this.pictures = pictures;                   }

}
