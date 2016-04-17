package org.give2peer.karma.listener;

import android.preference.Preference;

import org.give2peer.karma.entity.Server;

public class OnServerUsernameChangeListener extends OnServerAttributeChangeListener
{
    public OnServerUsernameChangeListener(Server server) { super(server); }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        super.onPreferenceChange(preference, newValue);

        server.setUsername((String) newValue);
        server.save();

        return true;
    }
}