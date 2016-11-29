package org.give2peer.karma.listener;

import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.Marker;
import com.rsv.widget.WebImageView;

/**
 * Super-easily leaked ~wrapper class to scope the marker into the image loader response,
 * because we need to "refresh" the whole marker info window once the image is loaded.
 * We gotta ensure that this is not memory-leaked somehow !
 *
 * Picasso is for minSdkVersion = 14, and we're still supporting API 10.
 */
public class MarkerInfoWebImageListener implements WebImageView.WebImageProgressListener {

    private Marker marker;

    public MarkerInfoWebImageListener(Marker marker) {
        this.marker = marker;
    }

    public Marker getMarker() {
        return marker;
    }

    @Override
    public void onStart(WebImageView view) {}

    @Override
    public void onLoading(WebImageView view, int progress) {}

    @Override
    public void onLoad(WebImageView view) {
        view.setVisibility(View.VISIBLE);
        marker.showInfoWindow();
    }

    @Override
    public void onError(WebImageView view, Exception e) {
        Log.e("G2P", String.format(
                "Error loading image of marker '%s' : %s", marker.getTitle(), e.getMessage()
        ));
    }
}
