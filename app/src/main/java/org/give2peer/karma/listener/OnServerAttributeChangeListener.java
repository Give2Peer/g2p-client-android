package org.give2peer.karma.listener;

import android.preference.Preference;

import org.give2peer.karma.entity.Server;

abstract public class OnServerAttributeChangeListener implements Preference.OnPreferenceChangeListener
{
    Server server;
    OnServerAttributeChangeListener(Server server)
    {
        this.server = server;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        preference.setSummary((String)newValue);

        return true;
    }
}