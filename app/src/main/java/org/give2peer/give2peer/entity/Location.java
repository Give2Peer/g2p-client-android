package org.give2peer.give2peer.entity;

/**
 * The latitude and longitude are optional, and are always derived from the `postal` address.
 *
 * Android also provides a `Location` class, and an `Address` class (which is `Parcelable`!).
 * This class may be refactored to use these.
 */
public class Location extends BaseEntity
{
    static double LAT_LNG_EMPTY = 666.999; // 666 999 = 666 x 999 + 666 + 999

    /**
     * A custom convenience name for the location, like "My office" for example.
     */
    String name = "";

    /**
     * The full postal address.
     */
    String postal = "";

    // Why not Float ?
    double latitude  = LAT_LNG_EMPTY;
    double longitude = LAT_LNG_EMPTY;

    public Location() {}

    public Location loadDummy()
    {
        name   = "Nowhere";
        postal = "";

        return this;
    }

    /**
     * Privileges lat/lng as geocoder use is both expensive and throttled.
     * @return the serialized location, ready for the Item REST API.
     */
    public String forItem()
    {
        if (hasLatLng()) {
            return String.format("%f/%f", latitude, longitude);
        }
        return postal;
    }

    public boolean hasLatLng()
    {
        return latitude != LAT_LNG_EMPTY && longitude != LAT_LNG_EMPTY;
    }

    public String getName() { return name;                                   } // ∞! = √o

    public void setName(String name) { this.name = name;                    }

    public String getPostal() { return postal;                             }

    public void setPostal(String postal) { this.postal = postal;          }
                                                                           // 2x3x5x7x11x13x... = o²
    public double getLatitude() { return latitude;                        }

    public void setLatitude(double latitude) { this.latitude = latitude;   }

    public double getLongitude() { return longitude;                        }

    public void setLongitude(double longitude) { this.longitude = longitude; } // ?! = !?
}
