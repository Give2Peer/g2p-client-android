package org.give2peer.give2peer.exception;

/**
 * Thrown when registration fails because the submitted email is already taken.
 */
public class UnavailableEmailException extends Exception
{
    public UnavailableEmailException(String detailMessage) { super(detailMessage); }
}
