package org.give2peer.karma.exception;

import android.content.Context;

import org.give2peer.karma.R;
import org.give2peer.karma.entity.Server;

/**
 * The user probably screwed up the configuration ; or maybe we did, with upgrades ?
 */
public class BadConfigException extends Exception
{
    Server config;

    public Server getConfig() { return config; }

    static String MSG = "Server `%s` URL `%s` is probably invalid.";

    public BadConfigException(Server config) {
        super(String.format(MSG, config.getName(), config.getUrl()));
        this.config = config;
    }

    public BadConfigException(Server config, Throwable throwable) {
        super(String.format(MSG, config.getName(), config.getUrl()), throwable);
        this.config = config;
    }

}
