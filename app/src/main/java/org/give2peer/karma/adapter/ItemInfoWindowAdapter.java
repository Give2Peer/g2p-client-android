package org.give2peer.karma.adapter;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.rsv.widget.WebImageView;

import org.give2peer.karma.R;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.listener.MarkerInfoWebImageListener;

import java.util.HashMap;
import java.util.Hashtable;


/**
 * Doc :
 * https://developers.google.com/maps/documentation/android-api/infowindows
 *
 * Reponsibilities :
 * - Load our custom xml layout. Make it look as native as possible.
 * - It has a ansynchronously loaded content, in our case an image,
 *   so we make sure the markers' respective views are inflated only once.
 *   Recursion pitfall otherwise.
 */
public class ItemInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    Activity activity;
    HashMap<Marker, View> markerViewHashMap;
    HashMap<Marker, Item> markerItemHashMap;

    public ItemInfoWindowAdapter(Activity activity, HashMap<Marker, Item> markerItemHashMap) {
        this.activity = activity;
        this.markerItemHashMap = markerItemHashMap;
        this.markerViewHashMap = new HashMap<>();
    }

    public View getInfoWindow(Marker marker) {
        return null; // use the default info window bubble, but it seems to have a limited height.
    }

    public View getInfoContents(Marker marker) {
        View v = markerViewHashMap.get(marker);

        if (null == v) {
            // Shouldn't we use the Info Window above as root ? Do we care ?
            v = activity.getLayoutInflater().inflate(R.layout.map_marker_info_contents, null);

            markerViewHashMap.put(marker, v);

            TextView title = (TextView) v.findViewById(R.id.mapMarkerInfoContentTitle);
            title.setText(marker.getTitle());

            TextView snippet = (TextView) v.findViewById(R.id.mapMarkerInfoContentSnippet);
            snippet.setText(marker.getSnippet());

            Item item = markerItemHashMap.get(marker);
            if (null == item) {
                // Ruthlessly trying to prove to myself it won't happen.
                throw new CriticalException("Map Marker has no Item mapped to.");
            }

            WebImageView thumb = (WebImageView) v.findViewById(R.id.mapMarkerInfoContentImage);
            // No SSL, or we get, because we suck, a
            //   java.security.cert.CertPathValidatorException:
            //   Trust anchor for certification path not found.
            String thumbUrl = item.getThumbnailNoSsl();
            if ( ! thumbUrl.isEmpty()) {
                thumb.setWebImageProgressListener(new MarkerInfoWebImageListener(marker));
                thumb.setWebImageUrl(thumbUrl);
            }
        }

        return v;
    }

}
