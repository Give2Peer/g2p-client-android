package org.give2peer.give2peer.activity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.exception.ErrorResponseException;
import org.give2peer.give2peer.exception.UnavailableEmailException;
import org.give2peer.give2peer.exception.UnavailableUsernameException;
import org.give2peer.give2peer.service.RestService;

import java.io.IOException;
import java.net.URISyntaxException;


/**
 * Allows a user to set up his username and password for the current server.
 * This can also be done ine the preferences, but :
 * - this is prettier, and might pave the way for other login means.
 * - it is buried in the preferences, and hard to find.
 * - we cannot programmatically send the user there with an Intent (afaik). Complex business.
 */
public class LoginActivity extends LocatorActivity
{
    static int COLOR_ERROR = Color.argb(255, 255, 0, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d("G2P", "Starting login activity.");

        // TEXT
        Server server = app.getCurrentServer();
        TextView help = (TextView) findViewById(R.id.loginTopTextView);
        String text = String.format("Log on the server %s.", server.getName());
        help.setText(text);
    }


    /**
     * Send the new item data to the server, in a async task.
     */
    public void send()
    {
        // Update the UI
        disableSending();

        // Collect inputs from the form
        final EditText usrInput = (EditText) findViewById(R.id.loginUsernameEditText);
        final EditText pwdInput = (EditText) findViewById(R.id.loginPasswordEditText);

        final String username = usrInput.getText().toString();
        final String password = pwdInput.getText().toString();

        // Grab the REST service and save its initial credentials
        final RestService rs = app.getRestService();
        final UsernamePasswordCredentials credentials = rs.getCredentials();

        // Wrap the HTTP query in an async task
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;

            @Override
            protected Boolean doInBackground(Void... nope)
            {
                try {
                    rs.setCredentials(username, password);
                    return rs.testServer();
                } catch (Exception e) {
                    exception = e;
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean success)
            {
                super.onPostExecute(success);

                if (exception != null) {
                    enableSending();
                    rs.setCredentials(credentials);
                    if (exception instanceof IOException) {
                        app.toast("An I/O error occurred.");
                    }
                    else if (exception instanceof URISyntaxException) {
                        app.toast("An grave error occurred.");
                    }
                    else {
                        app.toast("An error occurred.\nCheck your internet connection.");
                    }
                    exception.printStackTrace();
                } else {
                    if (! success) {
                        enableSending();
                        rs.setCredentials(credentials);
                        app.toast("Login failed !\nCheck your credentials.", Toast.LENGTH_LONG);
                    } else {
                        // Everything went smoothly, let's save the username and password
                        Server server = app.getCurrentServer();
                        server.setUsername(username);
                        server.setPassword(password);
                        server.save();
                        // In the prefs, too
                        SharedPreferences prefs = app.getPrefs();
                        String usrKey = String.format("server_%d_username", server.getId());
                        String pwdKey = String.format("server_%d_password", server.getId());
                        prefs.edit().putString(usrKey, username).putString(pwdKey, password).apply();

                        app.toast("Login successful !", Toast.LENGTH_LONG);
                        finish();
                    }
                }
            }
        }.execute();

    }


    //// UI ////////////////////////////////////////////////////////////////////////////////////////

    public void onSend(View view)
    {
        send();
    }

    protected void enableSending()
    {
        Button      sendButton   = (Button)      findViewById(R.id.loginSendButton);
        ProgressBar sendProgress = (ProgressBar) findViewById(R.id.loginProgressBar);

        sendButton.setEnabled(true);
        sendProgress.setVisibility(View.GONE);
    }

    protected void disableSending()
    {
        Button      sendButton   = (Button)      findViewById(R.id.loginSendButton);
        ProgressBar sendProgress = (ProgressBar) findViewById(R.id.loginProgressBar);

        sendButton.setEnabled(false);
        sendProgress.setVisibility(View.VISIBLE);
    }

}
