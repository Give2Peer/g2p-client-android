package org.give2peer.give2peer.listener;

import android.preference.Preference;
import android.preference.PreferenceScreen;

import org.give2peer.give2peer.entity.Server;

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