package org.give2peer.give2peer.entity;

import com.orm.SugarRecord;

/**
 * The latitude and longitude are optional, and are always derived from the `postal` address.
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
