package org.give2peer.karma.event;

import android.location.Location;

/**
 * Our `LocatorBaseActivity` posts such events through the EventBus when `getLocation()` fails.
 */
public class LocationFailureEvent
{
    int failType;

    public LocationFailureEvent(int failType)
    {
        this.failType = failType;
    }

    /**
     * @return One of `FailType.XXXX`.
     */
    public int getFailType() { return failType; }
}
