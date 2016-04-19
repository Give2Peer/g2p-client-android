package org.give2peer.karma.entity;

import org.joda.time.DateTime;

/**
 * GSON-friendly Item.
 */
public class Item
{
    Integer  id;
    String   title;
    String   description;
    String   location;
    Float    latitude;
    Float    longitude;
    Float    distance; // in meters
    DateTime createdAt;
    DateTime updatedAt;


    // CONSTRUCTOR /////////////////////////////////////////////////////////////////////////////////

    public Item() {} // maybe needed, maybe not


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

    public DateTime getCreatedAt()                   { return createdAt;                           }

    public void setCreatedAt(DateTime createdAt)     { this.createdAt = createdAt;                 }

    public DateTime getUpdatedAt()                   { return updatedAt;                           }

    public void setUpdatedAt(DateTime updatedAt)     { this.updatedAt = updatedAt;                 }

//    public Image getPicture()                        { return picture;                             }
//
//    public void setPicture(Image picture)            { this.picture = picture;                     }
//
//    public boolean hasThumbnail()                    { return ! thumbnail.isEmpty();               }
//
//    public String getThumbnail()                     { return thumbnail;                           }
//
//    public void setThumbnail(String thumbnail)       { this.thumbnail = thumbnail;                 }
//
//    public boolean hasThumbnailView()                { return null != thumbnailView;               }
//
//    public View getThumbnailView()                   { return thumbnailView;                       }
//
//    public void setThumbnailView(View thumbnailView) { this.thumbnailView = thumbnailView;         }
//
//    public List<File> getPictures()                  { return pictures;                            }
//
//    public void setPictures(List<File> pictures)     { this.pictures = pictures;                   }

}
