package org.give2peer.give2peer.exception;

/**
 * Thrown when registration fails because the submitted username is already taken.
 */
public class UnavailableUsernameException extends Exception
{
    public UnavailableUsernameException(String detailMessage) { super(detailMessage); }
}
