package org.give2peer.karma.activity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.entity.Server;
import org.give2peer.karma.exception.ErrorResponseException;
import org.give2peer.karma.exception.UnavailableEmailException;
import org.give2peer.karma.exception.UnavailableUsernameException;
import org.give2peer.karma.response.RegistrationResponse;

//import android.support.v7.internal.widget.AdapterViewCompat;
//import org.give2peer.karma.entity.Location;

//import im.delight.android.keyvaluespinner.KeyValueSpinner;

/**
 * This activity is never used.
 */
public class RegistrationActivity extends ActionBarActivity
{
    static int COLOR_ERROR = Color.argb(255, 255, 0, 0);

    Application app;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        Log.d("G2P", "Starting registration activity.");

        app = (Application) getApplication();

        // TEXT
        Server server = app.getCurrentServer();
        TextView help = (TextView) findViewById(R.id.registrationTopTextView);
        help.setText(String.format("Create a new account on the server %s by filling the form below.", server.getName()));

        // FORM
        final EditText usrInput = (EditText) findViewById(R.id.registrationUsernameEditText);
        final EditText emlInput = (EditText) findViewById(R.id.registrationEmailEditText);
        final EditText pwdInput = (EditText) findViewById(R.id.registrationPasswordEditText);
        final EditText pw2Input = (EditText) findViewById(R.id.registrationPassword2EditText);

        final int color = pw2Input.getCurrentTextColor();

        // PASSWORDS MATCH
        TextWatcher pwdMatchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s)
            {
                if (pwdInput.getText().toString().equals(pw2Input.getText().toString())) {
                    pw2Input.setTextColor(color);
                } else {
                    pw2Input.setTextColor(COLOR_ERROR);
                }
            }
        };
        pwdInput.addTextChangedListener(pwdMatchWatcher);
        pw2Input.addTextChangedListener(pwdMatchWatcher);

        // USERNAME AVAILABILITY
        TextWatcher usrAvailableWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s)
            {
                usrInput.setTextColor(color);
            }
        };
        usrInput.addTextChangedListener(usrAvailableWatcher);

        // EMAIL AVAILABILITY
        TextWatcher emlAvailableWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s)
            {
                emlInput.setTextColor(color);
            }
        };
        emlInput.addTextChangedListener(emlAvailableWatcher);


    }

    /**
     * Send the new item data to the server, in a async task.
     */
    public void send()
    {
        // Update the UI
        disableSending();

        // Collect inputs from the form
        final EditText usrInput = (EditText) findViewById(R.id.registrationUsernameEditText);
        final EditText emlInput = (EditText) findViewById(R.id.registrationEmailEditText);
        final EditText pwdInput = (EditText) findViewById(R.id.registrationPasswordEditText);
        final EditText pw2Input = (EditText) findViewById(R.id.registrationPassword2EditText);

        final String username = usrInput.getText().toString();
        final String password = pwdInput.getText().toString();
        final String email    = emlInput.getText().toString();

        // Check the password confirmation
        if (! pwdInput.getText().toString().equals(pw2Input.getText().toString())) {
            app.toast("The passwords do not match.");
            enableSending();
            return;
        }

        // Wrap the HTTP query in an async task
        new AsyncTask<Void, Void, RegistrationResponse>() {
            Exception exception;

            @Override
            protected RegistrationResponse doInBackground(Void... nope)
            {
                return app.getRestClient().register(email, username, password);
            }

            @Override
            protected void onPostExecute(RegistrationResponse nope)
            {
                super.onPostExecute(nope);

                if (null == nope) {
                    enableSending();
//                    if (exception instanceof UnavailableUsernameException) {
//                        app.toast("That username is already taken.");
//                        usrInput.setTextColor(COLOR_ERROR);
//                    }
//                    else if (exception instanceof UnavailableEmailException) {
//                        app.toast("That email is already taken or invalid.");
//                        emlInput.setTextColor(COLOR_ERROR);
//                    }
//                    else if (exception instanceof ErrorResponseException) {
//                        app.toast("The server denied the registration.");
//                        exception.printStackTrace();
//                    }
//                    else {
//                        app.toast("An error occurred.\nCheck your internet connection.");
//                        exception.printStackTrace();
//                    }
                } else {
                    // Everything went smoothly, let's save the username and password
                    Server server = app.getCurrentServer();
                    server.setUsername(username);
                    server.setPassword(password);
                    server.save();
                    // Update the REST service
                    app.setServerConfiguration(server);
                    // In the prefs, too. Yeah, technical debt...
                    SharedPreferences prefs = app.getPrefs();
                    String usrKey = String.format("server_%d_username", server.getId());
                    String pwdKey = String.format("server_%d_password", server.getId());
                    prefs.edit().putString(usrKey, username).putString(pwdKey, password).apply();

                    app.toast("Registration successful !");
                    finish();
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
        Button      sendButton   = (Button)      findViewById(R.id.registrationSendButton);
        ProgressBar sendProgress = (ProgressBar) findViewById(R.id.registrationProgressBar);

        sendButton.setEnabled(true);
        sendProgress.setVisibility(View.GONE);
    }

    protected void disableSending()
    {
        Button      sendButton   = (Button)      findViewById(R.id.registrationSendButton);
        ProgressBar sendProgress = (ProgressBar) findViewById(R.id.registrationProgressBar);

        sendButton.setEnabled(false);
        sendProgress.setVisibility(View.VISIBLE);
    }

}
