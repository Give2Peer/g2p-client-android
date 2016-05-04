package org.give2peer.karma.service;

import android.app.Activity;
import android.widget.Toast;

import org.apache.http.auth.AuthenticationException;
import org.give2peer.karma.R;
import org.give2peer.karma.exception.AuthorizationException;
import org.give2peer.karma.exception.BadConfigException;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.exception.GeocodingException;
import org.give2peer.karma.exception.MaintenanceException;
import org.give2peer.karma.exception.NoInternetException;
import org.give2peer.karma.exception.QuotaException;


/**
 * Handles our different kinds of exceptions, from benign to critical.
 * Provides default behaviors you can override when you instantiate it.
 */
public class ExceptionHandler
{

    Activity activity; // maybe Context would be enough, I'm never sure.

    public ExceptionHandler(Activity activity)
    {
        this.activity = activity;
    }

    /**
     * @param exception to handle
     * @return whether or not the exception was handled.
     */
    public boolean handleException(Exception exception)
    {
        // The order below is important, as some Exceptions are children of others.

        if (exception instanceof NoInternetException) {
            on((NoInternetException)exception);
            return true;
        }
        if (exception instanceof AuthenticationException) {
            on((AuthenticationException)exception);
            return true;
        }
        if (exception instanceof AuthorizationException) {
            on((AuthorizationException)exception);
            return true;
        }
        if (exception instanceof MaintenanceException) {
            on((MaintenanceException)exception);
            return true;
        }
        if (exception instanceof BadConfigException) {
            on((BadConfigException)exception);
            return true;
        }
        if (exception instanceof QuotaException) {
            on((QuotaException)exception);
            return true;
        }
        if (exception instanceof GeocodingException) {
            on((GeocodingException)exception);
            return true;
        }
        if (exception instanceof CriticalException) {
            on((CriticalException)exception);
            return true;
        }

        return false;
    }

    /**
     * A toaster that do not want to take over the world.
     * @param resid R.string.toast_xxxxxxxx
     */
    protected void toast(int resid)
    {
        Toast.makeText(activity, resid, Toast.LENGTH_LONG).show();
    }

    /**
     * A toaster that does want to take over the world.
     */
    protected void toast(String msg)
    {
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
    }


    // TO OVERRIDE AT WILL /////////////////////////////////////////////////////////////////////////


    protected void on(NoInternetException exception)
    {
        toast(R.string.toast_no_internet_available);
    }

    protected void on(AuthenticationException exception)
    {
        toast(R.string.toast_authentication_failure);
    }

    protected void on(AuthorizationException exception)
    {
        toast(R.string.toast_authorization_failure);
    }

    protected void on(MaintenanceException exception)
    {
        toast(R.string.toast_server_maintenance);
    }

    protected void on(BadConfigException exception)
    {
        // here, if we could forward to the server configuration activity that would be great.
        toast(String.format(
                activity.getString(R.string.toast_bad_config),
                exception.getConfig().getName(),
                exception.getConfig().getUrl()
        ));
    }

    protected void on(GeocodingException exception)
    {
        toast(exception.getMessage());
    }

    protected void on(CriticalException exception)
    {
        // CriticalException extends RuntimeException because we want it to be unchecked and crash
        // the app so we can cheaply get the stacktrace from the Android error report.
        // It's not pretty, and if we implement a proper exception reporter it should go there.
        throw exception;
    }

    /**
     * This message is not very specific, so this should be overridden each time.
     */
    protected void on(QuotaException exception)
    {
        toast(R.string.toast_quota_reached);
    }

}
