package org.give2peer.karma.exception;

import org.give2peer.karma.response.ErrorResponse;

/**
 * Thrown when (pre)registration or user edition fails because the submitted username is already
 * taken, or more generally unavailable (invalid, blacklisted, etc.)
 *
 * Activities should catch these and react accordingly, like highlighting the field and/or notifying
 * the user.
 */
public class UnavailableUsernameException extends ErrorResponseException
{
    public UnavailableUsernameException(ErrorResponse errorResponse) {    super(errorResponse);    }
}
