package org.give2peer.karma.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.entity.Server;
import org.give2peer.karma.service.RestService;

import java.io.IOException;
import java.net.URISyntaxException;


/**
 * Allows a user to set up his username and password for the current server.
 * This can also be done ine the preferences, but :
 * - this is prettier, and might pave the way for other login means.
 * - it is buried in the preferences, and hard to find.
 * - we cannot programmatically send the user in the prefs with an Intent (afaik). Complex business.
 */
@EActivity(R.layout.activity_login)
public class LoginActivity extends ActionBarActivity
{
    static int COLOR_ERROR = Color.argb(255, 255, 0, 0);

    Application app;

    @ViewById
    TextView    loginTopTextView;
    @ViewById
    ProgressBar loginProgressBar;
    @ViewById
    Button      loginSendButton;
    @ViewById
    EditText    loginUsernameEditText;
    @ViewById
    EditText    loginPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        app = (Application) getApplication();

        Log.d("G2P", "Starting login activity.");
    }

    @AfterViews
    void setTopTextView() {
        // Injected Views are not available in onCreate, so we use @AfterView.
        Server currentServer = app.getCurrentServer();
        if (null != currentServer) {
            loginTopTextView.setText(getString(R.string.login_top_text, currentServer.getName()));
        }
    }

    /**
     * Send the login credentials to the server, in a async task.
     */
    public void send()
    {
        // Update the UI
        disableSending();

        // Collect inputs from the form
//        final EditText loginUsernameEditText = (EditText) findViewById(R.id.loginUsernameEditText);
//        final EditText loginPasswordEditText = (EditText) findViewById(R.id.loginPasswordEditText);

        final String username = loginUsernameEditText.getText().toString();
        final String password = loginPasswordEditText.getText().toString();

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
                    return rs.checkServerAndAuthentication();
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
                        // The REST service has already been updated
                        //
                        // ... in the prefs, too. (argh, this "preferences" hack is so hurtful)
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

    public void onRegister(View view)
    {
        // Go to the registration activity
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
        finish();
    }

    protected void enableSending()
    {
        loginSendButton.setEnabled(true);
        loginProgressBar.setVisibility(View.GONE);
    }

    protected void disableSending()
    {
        loginSendButton.setEnabled(false);
        loginProgressBar.setVisibility(View.VISIBLE);
    }

}
