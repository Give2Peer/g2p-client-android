package org.give2peer.give2peer.listener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.util.Log;

import org.give2peer.give2peer.activity.SettingsActivity;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.fragment.SettingsFragment;

public class OnForgetServerClickListener implements Preference.OnPreferenceClickListener {
    SettingsFragment fragment;
    Server server;
    public OnForgetServerClickListener(SettingsFragment fragment, Server server)
    {
        this.fragment = fragment;
        this.server = server;
    }
    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        if (! fragment.servers.containsValue(server)) {
            Log.e("G2P", "NOPE NOPE NOPE NOPE THIS MUST NOT HAPPEN!");
        }

        // Confirmation alert
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setMessage("Are you sure you want to forget this server?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Remove the server from the preference list
                        fragment.servers.remove(server.getId());
                        // Delete the server persisted entity
                        server.delete();

                        // Quit the activity
                        fragment.getActivity().finish();
                        // And show the root of settings
                        Intent intent = new Intent(fragment.getActivity(), SettingsActivity.class);
                        fragment.startActivity(intent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // nothing is cool
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

        return true;
    }
}
