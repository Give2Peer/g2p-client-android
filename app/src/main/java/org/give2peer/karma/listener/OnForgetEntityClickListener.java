package org.give2peer.karma.listener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;

import org.give2peer.karma.activity.SettingsActivity;
import org.give2peer.karma.entity.BaseEntity;
import org.give2peer.karma.fragment.ServerSettingsFragment;

public class OnForgetEntityClickListener implements Preference.OnPreferenceClickListener {
    ServerSettingsFragment fragment;
    BaseEntity entity;
    public OnForgetEntityClickListener(ServerSettingsFragment fragment, BaseEntity entity)
    {
        this.fragment = fragment;
        this.entity = entity;
    }
    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        // Confirmation alert
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setMessage("Are you sure?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Remove the server from the preference list
                        //fragment.servers.remove(server.getId());

                        // Delete the persisted entity
                        entity.delete();
                        // Quit the activity
                        fragment.getActivity().finish();
                        // And show the root of settings
                        Intent intent = new Intent(fragment.getActivity(), SettingsActivity.class);
                        fragment.startActivity(intent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

        return true;
    }
}
