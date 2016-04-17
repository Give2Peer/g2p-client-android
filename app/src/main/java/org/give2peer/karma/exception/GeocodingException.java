package org.give2peer.karma.exception;

/**
 * Meh. We could use an Exception instead, but this feels nice.
 */
public class GeocodingException extends Exception
{
    public GeocodingException(String detailMessage) { super(detailMessage); }
}
