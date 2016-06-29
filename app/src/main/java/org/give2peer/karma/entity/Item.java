package org.give2peer.karma.entity;

import com.google.android.gms.maps.model.LatLng;

import org.give2peer.karma.StringUtils;
import org.joda.time.DateTime;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.List;
import java.util.Locale;

/**
 * GSON-friendly Item.
 */
public class Item
{

    Integer  id;
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

    static public String TYPE_MOOP = "moop";
    static public String TYPE_GIFT = "gift";
    static public String TYPE_LOST = "lost";


    // CONSTRUCTOR /////////////////////////////////////////////////////////////////////////////////

    public Item() {} // maybe needed, maybe not


    // DESCRIPTION /////////////////////////////////////////////////////////////////////////////////

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
        Locale locale = Locale.getDefault();
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
     * @return the thumbnail URL in `http`, not `https`.
     */
    public String getThumbnailNoSsl()
    {
        return StringUtils.httpsToHttp(getThumbnail());
    }


    // VANILLA ACCESSORS AND MUTATORS //////////////////////////////////////////////////////////////

    public Integer getId()                           { return id;                                  }

    public void setId(Integer id)                    { this.id = id;                               }

    public String getType()                          { return type;                                }

    public void setType(String type)                 { this.type = type;                           }

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
