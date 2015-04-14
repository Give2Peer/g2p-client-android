package org.give2peer.give2peer;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * To minimize battery usage, and because we don't need regular updates, here's a LocationListener
 * that will start listening to updates by itself, and stop listening by itself when the
 * LocationManager finds a location.
 */
public class OneTimeLocationListener implements LocationListener
{
    protected LocationManager locationManager;

    public OneTimeLocationListener(LocationManager lm, Criteria criteria)
    {
        locationManager = lm;
        locationManager.requestLocationUpdates(
                locationManager.getBestProvider(criteria, true),
                2000,
                1000,
                this
        );
    }

    @Override
    public void onLocationChanged(Location location) {
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}
