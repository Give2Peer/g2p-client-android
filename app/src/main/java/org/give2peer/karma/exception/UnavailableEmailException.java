package org.give2peer.karma.exception;

import org.give2peer.karma.response.ErrorResponse;

/**
 * Thrown when registration fails because the submitted email is already taken.
 */
public class UnavailableEmailException extends ErrorResponseException
{
    public UnavailableEmailException(ErrorResponse errorResponse) { super(errorResponse); }
}
