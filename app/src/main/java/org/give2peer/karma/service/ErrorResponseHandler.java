package org.give2peer.karma.service;

import org.give2peer.karma.response.ErrorResponse;

/**
 * Make your activities implement this and provide them to the {@link RestClientErrorHandler}.
 * This allows us to run handleErrorResponse in the UI thread if we need to, which we usually do.
 */
public interface ErrorResponseHandler {
    void handleErrorResponse(ErrorResponse errorResponse);
}
