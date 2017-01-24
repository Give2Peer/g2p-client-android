package org.give2peer.karma.event;


/**
 * Sent by our Application's `requireAuthentication` method.
 */
public class AuthenticationEvent
{
    private boolean success;

    public AuthenticationEvent(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return ! success;
    }
}
