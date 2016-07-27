package org.give2peer.karma.listener;

import android.util.Log;

import com.google.android.gms.maps.model.Marker;

import pl.polidea.webimageview.WebImageListener;

/**
 * Super-easily leaked ~wrapper class to scope the marker into the image loader response, because
 * we need to "refresh" the whole marker info window once the image is loaded.
 *
 * What's wrong with "Success" and "Failure" ?
 * Picasso is for minSdkVersion = 14, and we're still supporting API 10.
 * When we'll switch to Picasso, this class should be pretty similar.
 * Note: Picasso uses onError and onSuccess, which feels wrong too. To err...or not to err...or not
 */
@Deprecated
public class MarkerInfoPolideaWebImageListener implements WebImageListener {

    Marker marker;

    public MarkerInfoPolideaWebImageListener(Marker marker) {
        this.marker = marker;
    }

    // onSuccess
    public void onImageFetchedSuccessfully(String url) {
        Log.d("G2P", String.format("SUCCESS : show info window again of %s", marker.getTitle()));
        marker.showInfoWindow();
    }

    // onFailure
    public void onImageFetchedFailed(String url) {
        // todo: decide what should happen here. Nothing feels fine for now, in both senses.
        Log.e("G2P", String.format("I fixed the code by adding this line. %s", marker.getTitle()));
    }
}
