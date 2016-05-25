package org.give2peer.karma.exception;

/**
 * Most probably there's no Internet.
 */
public class NoInternetException extends Exception {
    public NoInternetException() {
        super();
    }
    public NoInternetException(Throwable throwable) {
        super(throwable);
    }
}
