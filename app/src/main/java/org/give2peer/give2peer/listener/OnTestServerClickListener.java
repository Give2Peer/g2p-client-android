package org.give2peer.give2peer.listener;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.preference.Preference;

import org.give2peer.give2peer.Application;
import org.give2peer.give2peer.service.RestService;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.fragment.SettingsFragment;

import java.io.IOException;
import java.net.URISyntaxException;

public class OnTestServerClickListener implements Preference.OnPreferenceClickListener
{
    SettingsFragment fragment;
    Server server;

    public OnTestServerClickListener(SettingsFragment fragment, Server server)
    {
        this.fragment = fragment;
        this.server = server;
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        final Application app = (Application) fragment.getActivity().getApplication();
        final int maxAttempts = 3;
        final ProgressDialog progress = new ProgressDialog(fragment.getActivity());
        progress.setMessage("Testing the connection...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progress.setIndeterminate(true);
        progress.setMax(maxAttempts);
        progress.show();

//        server.

        final RestService restService = new RestService(server);

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                boolean isServerOk = false;
                int curAttempt = 0;
                while (!isServerOk && curAttempt++ <= maxAttempts) {
                    try {
                        isServerOk = restService.testServer();
                    } catch (IOException |URISyntaxException e) {}
                    progress.incrementProgressBy(1);
                }

                return isServerOk;
            }

            @Override
            protected void onPostExecute(Boolean isServerOk) {
                super.onPostExecute(isServerOk);
                progress.setProgress(maxAttempts);
                if (isServerOk) {
                    app.toast("Connection established successfully :)");
                } else {
                    // todo: It would be great to have a more precise feedback here
                    app.toast("Connection failed :(");
                }
                progress.hide();
            }
        }.execute();

        return true;
    }
}
