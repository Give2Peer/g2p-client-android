package org.give2peer.give2peer.listener;

import android.preference.Preference;

import org.give2peer.give2peer.entity.Server;

public class OnServerPasswordChangeListener extends OnServerAttributeChangeListener
{
    public OnServerPasswordChangeListener(Server server) { super(server); }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Nope, we're setting the Summary ourselves below
        //super.onPreferenceChange(preference, newValue);

        preference.setSummary(new String(new char[((String)newValue).length()]).replace("\0", "*"));

        server.setPassword((String) newValue);
        server.save();

        return true;
    }
}