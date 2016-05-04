package org.give2peer.karma.exception;

/**
 * These happen when we use the client to geocode a postal location into lon/lat coordinates.
 * The server has its own geocoding service.
 *
 * These are not even thrown anymore, because the client never tries to geocode itself.
 * todo: throw GeocodingException when the server fails to geocode
 */
public class GeocodingException extends Exception
{
    public GeocodingException(String detailMessage) { super(detailMessage); }
}
