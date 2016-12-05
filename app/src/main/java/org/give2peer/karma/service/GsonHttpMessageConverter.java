package org.give2peer.karma.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.give2peer.karma.adapter.DateTimeTypeAdapter;
import org.joda.time.DateTime;

/**
 * Our very own message converter to add our DateTime (Joda) adapter for ISO8601 strings.
 * This also doubles as a factory for Gson. Refactor if that bothers you. :)
 */
public class GsonHttpMessageConverter
        extends org.springframework.http.converter.json.GsonHttpMessageConverter
{
    /**
     * Construct a new {@code GsonHttpMessageConverter} with a default {@link Gson#Gson() Gson}.
     */
    public GsonHttpMessageConverter() {
        super(createGson());
    }

    /**
     * Construct a new {@code GsonHttpMessageConverter}.
     *
     * @param serializeNulls true to generate json for null values
     */
    public GsonHttpMessageConverter(boolean serializeNulls) {
        super(serializeNulls ? createGson(true) : createGson());
    }

    /**
     * Our own very puny Gson factory that attaches our ISO8601-compatible DateTime adapter.
     */
    public static Gson createGson() {
        return createGson(false);
    }

    /**
     * Our own very puny Gson factory that attaches our ISO8601-compatible DateTime adapter.
     * @param serializeNulls true to generate json for null values
     */
    public static Gson createGson(boolean serializeNulls) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        // Should we serialize nulls (or ignore them, which is the default behavior)
        if (serializeNulls) gsonBuilder.serializeNulls();
        // Register a Joda time adapter for ISO8601 strings. Otherwise, it expects an object.
        gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeTypeAdapter());
        // We don't need to specify the DateFormat, Joda time already accepts ISO8601.
        // gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); // ISO8601
        return gsonBuilder.create();
    }
}
