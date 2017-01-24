package org.give2peer.karma.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Locale;


/**
 * Handles the different kinds of exceptions raised by our AA-powered RestClient.
 * Provides default behaviors you can override when you instantiate it.
 *
 * The reason we use this as a POJO class and not an implementation of RestErrorHandler is that
 * we were unable to work out how to get back to the UiThread from inside the handler.
 *
 * Here's an example usage, in an Activity or Application that implements RestErrorHandler :
 *
 * @RestService
 * RestClient restClient;
 *
 * @AfterInject
 * void setupRestClient() {
 *     restClient.setRootUrl(app.getCurrentServer().getUrl());
 *     restClient.setRestErrorHandler(this);
 * }
 *
 * @Override
 * @UiThread
 * public void onRestClientExceptionThrown(NestedRuntimeException e) {
 *     new RestExceptionHandler(app, this).handleException(e);
 * }
 *
 */
public class RestExceptionHandler
{
    private Application app;
    private Context context;

    public RestExceptionHandler(Application app, Context context) {
        this.app = app;
        this.context = context;
    }

    /**
     * Handle the provided Exception with the relevant default handler.
     * You can also override these handlers at will.
     *
     * If no handler can handle the Exception for any reason, we let it crash so that users can
     * report the crash. So long as this app is in Beta, this should stay like this.
     * Once we published a production release, we probably need to be more ... quiet.
     *
     * @param exception to handle
     */
    public void handleException(Exception exception) {
        boolean handled = handleExceptionSafe(exception);

        if ( ! handled) {
            Toast.makeText(context,
                    context.getString(R.string.toast_willingly_uncaught_error),
                    Toast.LENGTH_LONG
            ).show();
            throw new CriticalException(String.format(
                    "Willingly unhandled %s. Please report!", exception.toString()
            ), exception);
        }
    }

    /**
     * Handle the provided exception without raising an exception if it cannot be handled.
     * In the provided exception cannot be handled, return false. Otherwise, return true.
     *
     * @param exception to handle
     * @return whether or not the exception was handled.
     */
    public boolean handleExceptionSafe(Exception exception) {

        if (exception instanceof HttpClientErrorException) {
            HttpClientErrorException hcee = (HttpClientErrorException) exception;
            String json = hcee.getResponseBodyAsString();
            HttpStatus code = hcee.getStatusCode();
            Log.i("G2P", String.format("Server returned a %d with:\n%s", code.value(), json));

            ErrorResponse errorResponse = null;
            try {
                Gson gson = GsonHttpMessageConverter.createGson();
                errorResponse = gson.fromJson(json, ErrorResponse.class);
            } catch (JsonSyntaxException jse) {
                String m = "Invalid JSON on a %d :\n%s";
                throw new CriticalException(String.format(
                        Locale.ENGLISH, m, code.value(), json
                ), hcee);
            }

            if (code.value() == 401) {
                onAuthenticationException(hcee, errorResponse);
                return true;
            }

            if (code.value() == 403) {
                onAuthorizationException(hcee, errorResponse);
                return true;
            }

            onErrorResponseException(hcee, errorResponse);
            return true;
        }

        if (exception instanceof HttpServerErrorException) {
            onMaintenanceException(exception);
            return true;
        }

        if (exception instanceof ResourceAccessException) {
            onNoInternetException(exception);
            return true;
        }

        // Can be thrown too :
        // HttpMessageNotReadableException (JSON is not parsable)

        return false;
    }


    // TO OVERRIDE AT WILL /////////////////////////////////////////////////////////////////////////


    protected void onNoInternetException(Exception exception) {
        Log.e("G2P", "No Internet?");
        exception.printStackTrace();

        app.toasty(R.string.toast_no_internet_available);
    }

    protected void onMaintenanceException(Exception exception) {
        Log.e("G2P", "Maintenance?");
        exception.printStackTrace();

        app.toasty(R.string.toast_server_maintenance);
    }

    protected void onAuthenticationException(HttpClientErrorException exception, ErrorResponse er) {
        if (null != er) {
            app.toasty(er.getMessage());
        } else {
            app.toasty(R.string.toast_authentication_failure);
        }
    }

    protected void onAuthorizationException(HttpClientErrorException exception, ErrorResponse er) {
        if (null != er) {
            app.toasty(er.getMessage());
        } else {
            app.toasty(R.string.toast_authorization_failure);
        }
    }

    /**
     * Most errors in the request will go through this hook.
     * By default, it snacks the (already localized by server) error message.
     * @param exception The exception raised by our AA'd RestClient.
     * @param er        The ErrorResponse object, containing an already localized message.
     */
    protected void onErrorResponseException(HttpClientErrorException exception, ErrorResponse er) {
        app.toasty(er.getMessage());
    }

}
