package org.give2peer.karma.exception;

/**
 * These are exceptions we want to know about when they happen in the wild.
 * Unlike internet or config errors, they _should never happen_ in the user flow.
 */
public class CriticalException extends Exception
{
    public CriticalException() {}

    public CriticalException(String detailMessage) {
        super(detailMessage);
    }

    public CriticalException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public CriticalException(Throwable throwable) {
        super(throwable);
    }
}
