package org.give2peer.give2peer.exception;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ErrorResponseException extends Exception
{
    private HttpResponse response;

    public ErrorResponseException(HttpResponse response)
    {
        this.response = response;
    }

    public HttpResponse getResponse()
    {
        return response;
    }

    @Override
    public String getMessage()
    {
        String json = "";
        try {
            json = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException e) {
            return e.getMessage();
        }

        try {
            JSONObject jo = new JSONObject(json);
            return jo.getJSONObject("error").getString("message");
        } catch (JSONException e) {
            return "Could not parse response: " + e.getMessage();
        }
    }
}
