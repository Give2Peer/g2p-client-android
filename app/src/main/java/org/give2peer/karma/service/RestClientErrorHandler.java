package org.give2peer.karma.service;

import android.app.Activity;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EService;
import org.androidannotations.api.rest.RestErrorHandler;
import org.give2peer.karma.adapter.DateTimeTypeAdapter;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.response.ErrorResponse;
import org.joda.time.DateTime;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class RestClientErrorHandler implements RestErrorHandler {

    protected ErrorResponseHandler handler;

    public RestClientErrorHandler(ErrorResponseHandler handler) {
        this.handler = handler;
    }

    /**
     * Remember, all of this is run in a background thread !
     * @param nre the exception thrown by the springframework
     */
    @Override
    public void onRestClientExceptionThrown(NestedRuntimeException nre) {
        if (nre instanceof HttpClientErrorException) {
            HttpClientErrorException hcee = (HttpClientErrorException) nre;
            String json = hcee.getResponseBodyAsString();
            HttpStatus code = hcee.getStatusCode();
            ErrorResponse errorResponse = null;
            try {
                Gson gson = createGson();
                errorResponse = gson.fromJson(json, ErrorResponse.class);
            } catch (JsonSyntaxException jse) {
                String m = "Not valid JSON on a %d :\n%s";
                throw new CriticalException(String.format(m, code.value(), json), hcee);
            }
            handler.handleErrorResponse(errorResponse);
        } else {
            // What else is there to do here ?
            throw nre;
        }
    }


    // UTILS ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Our own very puny Gson factory, to attach our DateTime adapter.
     */
    private Gson createGson()
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        // Register a Joda time adapter for ISO8601 strings. Otherwise it expects an object.
        gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeTypeAdapter());
        // We don't need to specify the DateFormat, Joda time accepts ISO8601 in our converter.
        // gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); // ISO8601
        return gsonBuilder.create();
    }

}
