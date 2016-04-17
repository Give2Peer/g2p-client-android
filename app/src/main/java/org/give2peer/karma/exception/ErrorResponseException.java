package org.give2peer.karma.exception;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * TODO: This should also mirror the error codes and provide localized messages when available.
 */
public class ErrorResponseException extends Exception
{
    public ErrorResponseException(String detailMessage) { super(detailMessage); }

    @Override
    public String getMessage()
    {
        String json = super.getMessage();

        try {
            JSONObject jo = new JSONObject(json);
            return jo.getJSONObject("error").getString("message");
        } catch (JSONException e) {
            Log.e("G2P", "Failed to parse JSON response: "+json);
            return "Could not parse response: " + e.getMessage();
        }
    }
}
