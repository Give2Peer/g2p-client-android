package org.give2peer.karma.listener;

import android.preference.Preference;
import android.preference.PreferenceScreen;

import org.give2peer.karma.entity.Server;

public class OnServerNameChangeListener extends OnServerAttributeChangeListener
{
    PreferenceScreen screen;
    public OnServerNameChangeListener(Server server, PreferenceScreen screen)
    {
        super(server);
        this.screen = screen;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        super.onPreferenceChange(preference, newValue);

        screen.setTitle((String)newValue);

        server.setName((String)newValue);
        server.save();

        return true;
    }
}