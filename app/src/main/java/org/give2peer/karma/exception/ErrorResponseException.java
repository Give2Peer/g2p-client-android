package org.give2peer.karma.exception;

import org.give2peer.karma.response.ErrorResponse;

/**
 * Raised when the server returned a digestible error response.
 * Its text contents should already be localized by the server.
 */
public class ErrorResponseException extends Exception
{
    private ErrorResponse errorResponse;

    public ErrorResponseException(ErrorResponse errorResponse) {
        super(errorResponse.getMessage());
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    // Not sure if this is relevant. Let's comment it for now.
//    public void setErrorResponse(ErrorResponse errorResponse) {
//        this.errorResponse = errorResponse;
//    }
}
