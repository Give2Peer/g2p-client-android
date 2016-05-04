package org.give2peer.karma.exception;

/**
 * These are exceptions we want to know about when they happen in the wild.
 * Unlike internet or config errors, they _should never happen_ in the user flow.
 *
 * A word from Oracle :
 * If a client can reasonably be expected to recover from an exception, make it a checked exception.
 * If a client cannot do anything to recover from the exception, make it an unchecked exception.
 *
 * We use an unchecked RuntimeException because CriticalExceptions ought not be recovered from.
 */
public class CriticalException extends RuntimeException
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
