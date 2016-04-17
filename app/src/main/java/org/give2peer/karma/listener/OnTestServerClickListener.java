package org.give2peer.karma.listener;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.preference.Preference;
import android.util.Log;

import org.give2peer.karma.Application;
import org.give2peer.karma.exception.AuthorizationException;
import org.give2peer.karma.exception.MaintenanceException;
import org.give2peer.karma.service.RestService;
import org.give2peer.karma.entity.Server;
import org.give2peer.karma.fragment.SettingsFragment;

public class OnTestServerClickListener implements Preference.OnPreferenceClickListener
{
    SettingsFragment fragment;
    Server server;

    static int NUMBER_OF_ATTEMPTS = 3;

    public OnTestServerClickListener(SettingsFragment fragment, Server server)
    {
        this.fragment = fragment;
        this.server = server;
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        final Application app = (Application) fragment.getActivity().getApplication();
        final ProgressDialog progress = new ProgressDialog(fragment.getActivity());
        progress.setMessage("Testing the connection...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(NUMBER_OF_ATTEMPTS);
        progress.show();

        final RestService restService = new RestService(server);

        new AsyncTask<Void, Void, Void>() {

            boolean areCredentialsBad = false;
            boolean isServerOk = false;
            boolean isServerUndergoingMaintenance = false;

            protected boolean shouldKeepTesting() {
                return !isServerOk && !areCredentialsBad && ! isServerUndergoingMaintenance;
            }

            @Override
            protected Void doInBackground(Void... params) {
                int currentAttempt = 0;
                while (shouldKeepTesting() && currentAttempt++ <= NUMBER_OF_ATTEMPTS) {
                    try {
                        isServerOk = restService.testServer();
                    } catch (AuthorizationException e) {
                        areCredentialsBad = true;
                    } catch (MaintenanceException e) {
                        isServerUndergoingMaintenance = true;
                    } catch (Exception e) {
                        Log.d("G2P", String.format("Test of server '%s' failed.", server.getUrl()));
                    }
                    progress.incrementProgressBy(1);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void nope) {
                super.onPostExecute(nope);
                progress.setProgress(NUMBER_OF_ATTEMPTS);
                if (isServerOk) {
                    app.toasty("Connection established successfully :)");
                } else {
                    if (areCredentialsBad) {
                        app.toasty("Bad username or password !");
                    } else if (isServerUndergoingMaintenance) {
                        app.toasty("The server is down for maintenance.\nTry again later.");
                    } else {
                        app.toasty("Connection failed :(");
                    }
                }
                progress.hide();
            }
        }.execute();

        return true;
    }
}
