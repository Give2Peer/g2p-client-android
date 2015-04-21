package org.give2peer.give2peer.listener;

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

        fragment.servers.remove(server.getId());
        server.delete();
        fragment.getActivity().finish();

        Intent intent = new Intent(fragment.getActivity(), SettingsActivity.class);
        fragment.startActivity(intent);
        return true;
    }
}
