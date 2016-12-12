package org.give2peer.karma.utils;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

/**
 * Some static utilities for LatLng objects, mostly.
 * They should be pretty self-explanatory. Notice there are no comments.
 */
public class LatLngUtils {

    public static LatLngBounds getLatLngBounds(List<LatLng> latLngs) {
        LatLngBounds.Builder bc = new LatLngBounds.Builder();
        for (LatLng latLng : latLngs) { bc.include(latLng); }

        return bc.build();
    }

    public static LatLng getLatLngCentroid(List<LatLng> latLngs) {
        if (latLngs.size() == 0) return null;

        return getLatLngBounds(latLngs).getCenter();
    }

    public static LatLng locationToLatLng (Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

}
