package org.give2peer.give2peer.listener;

import android.preference.Preference;
import android.preference.PreferenceScreen;

import org.give2peer.give2peer.entity.Server;

public class OnServerUrlChangeListener extends OnServerAttributeChangeListener
{
    public OnServerUrlChangeListener(Server server) { super(server); }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        super.onPreferenceChange(preference, newValue);

        server.setUrl((String) newValue);
        server.save();

        return true;
    }
}