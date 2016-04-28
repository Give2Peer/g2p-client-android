package org.give2peer.karma.exception;

/**
 * The user probably screwed up the configuration ; or maybe we did, with upgrades ?
 */
public class BadConfigException extends Exception
{

    public static final String URL = "The server url you provided is probably wrong : `%s`.";
    public static final String CREDENTIALS = "The credentials you provided are probably wrong : `%s`.";

    public BadConfigException() {}

    public BadConfigException(String detailMessage, String value) {
        super(String.format(detailMessage, value));
    }

    public BadConfigException(String detailMessage, String value, Throwable throwable) {
        super(String.format(detailMessage, value), throwable);
    }

    public BadConfigException(Throwable throwable) {
        super(throwable);
    }

}
