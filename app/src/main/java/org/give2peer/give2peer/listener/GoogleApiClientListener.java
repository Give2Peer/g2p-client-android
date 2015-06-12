package org.give2peer.give2peer.listener;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * This is to be able to use a listener as parameter of `Application#buildGoogleLocator`.
 */
public interface GoogleApiClientListener extends GoogleApiClient.ConnectionCallbacks,
                                                 GoogleApiClient.OnConnectionFailedListener
{}
