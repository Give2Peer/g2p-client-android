package org.give2peer.karma.service;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.apache.http.auth.AuthenticationException;
import org.give2peer.karma.R;
import org.give2peer.karma.exception.AlreadyDoneException;
import org.give2peer.karma.exception.AuthorizationException;
import org.give2peer.karma.exception.BadConfigException;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.exception.ErrorResponseException;
import org.give2peer.karma.exception.GeocodingException;
import org.give2peer.karma.exception.LevelTooLowException;
import org.give2peer.karma.exception.MaintenanceException;
import org.give2peer.karma.exception.NoInternetException;
import org.give2peer.karma.exception.QuotaException;


/**
 * Handles our different kinds of exceptions, from benign to critical.
 * Provides default behaviors you can override when you instantiate it.
 */
@Deprecated
public class ExceptionHandler
{

    Activity activity; // maybe Context would be enough, I'm never sure.
    Snackbar snackbar; // the last created snackbar, so we can later dismiss it when relevant.

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
        if (exception instanceof AlreadyDoneException) {
            on((AlreadyDoneException)exception);
            return true;
        }
        if (exception instanceof LevelTooLowException) {
            on((LevelTooLowException)exception);
            return true;
        }
        if (exception instanceof ErrorResponseException) {
            on((ErrorResponseException)exception);
            return true;
        }
        if (exception instanceof CriticalException) {
            on((CriticalException)exception);
            return true;
        }

        return false;
    }

    /**
     * @param exception to handle
     */
    public void handleExceptionOrFail(Exception exception) {
        boolean handled = handleException(exception);

        if ( ! handled) {
            Toast.makeText(activity,
                    activity.getString(R.string.toast_willingly_uncaught_error),
                    Toast.LENGTH_LONG
            ).show();
            throw new CriticalException(String.format(
                    "Unhandled %s when reporting an item.", exception.toString()
            ), exception);
        }
    }



    /**
     * A toaster that does not want to take over the world.
     * @param resid R.string.toast_xxxxxxxx
     */
    protected void toast(int resid)
    {
        Toast.makeText(activity, resid, Toast.LENGTH_LONG).show();
    }

    /**
     * Another toaster that does want to take over the world.
     */
    protected void toast(String msg)
    {
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Snackbars are better suited for these errors messages than toasts.
     * Still not the optimal solution but I don't know the good libs...
     *
     * Falls back on a toast if the current focus view cannot be found for some reason.
     */
    protected void snack(String msg)
    {
        View view = activity.getCurrentFocus();
        if (null == view) {
            Log.e("G2P", "activity.getCurrentFocus() is null !");
            toast(msg);
        } else {
            snackbar = Snackbar.make(activity.getCurrentFocus(), msg, Snackbar.LENGTH_INDEFINITE);
            snackbar.show();
        }
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

    protected void on(AlreadyDoneException exception)
    {
        toast(R.string.toast_already_done);
    }

    protected void on(LevelTooLowException exception)
    {
        toast(R.string.toast_level_too_low);
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


    /**
     * Most errors in the request will go through this hook.
     * By default, it toastes the (already localized by server) error message.
     * @param exception The exception raised by our REST service. Contains a localized message.
     */
    protected void on(ErrorResponseException exception)
    {
        snack(exception.getMessage());
    }

}
