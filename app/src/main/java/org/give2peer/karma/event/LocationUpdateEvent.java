package org.give2peer.karma.event;

import android.location.Location;

/**
 * Our `LocatorBaseActivity` posts such events through the EventBus when `getLocation()` succeeds.
 */
public class LocationUpdateEvent
{
    Location location;

    public LocationUpdateEvent(Location location)
    {
        this.location = location;
    }

    public Location getLocation() { return location; }
}
