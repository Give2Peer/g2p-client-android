package org.give2peer.give2peer.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.Item;
import org.give2peer.give2peer.R;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.exception.ErrorResponseException;
import org.give2peer.give2peer.exception.UnavailableEmailException;
import org.give2peer.give2peer.exception.UnavailableUsernameException;
import org.give2peer.give2peer.task.NewItemTask;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

//import android.support.v7.internal.widget.AdapterViewCompat;
//import org.give2peer.give2peer.entity.Location;

//import im.delight.android.keyvaluespinner.KeyValueSpinner;

/**
 *
 */
public class RegistrationActivity extends LocatorActivity
{
    static int COLOR_ERROR = Color.argb(255, 255, 0, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        Log.d("G2P", "Starting registration activity.");

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
        new AsyncTask<Void, Void, Void>() {
            Exception exception;

            @Override
            protected Void doInBackground(Void... nope)
            {
                try {
                    app.getRestService().register(username, password, email);
                } catch (Exception e) {
                    exception = e;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void nope)
            {
                super.onPostExecute(nope);

                if (exception != null) {
                    enableSending();
                    if (exception instanceof UnavailableUsernameException) {
                        app.toast("That username is already taken.");
                        usrInput.setTextColor(COLOR_ERROR);
                    }
                    else if (exception instanceof ErrorResponseException) {
                        app.toast("That email is already taken.");
                        emlInput.setTextColor(COLOR_ERROR);
                    }
                    else if (exception instanceof UnavailableEmailException) {
                        app.toast("The server denied the registration.");
                        exception.printStackTrace();
                    }
                    else {
                        app.toast("An error occurred.\nCheck your internet connection.");
                        exception.printStackTrace();
                    }
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
