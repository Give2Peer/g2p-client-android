package org.give2peer.give2peer.listener;

import android.preference.Preference;
import android.preference.PreferenceScreen;

import org.give2peer.give2peer.entity.Location;
import org.give2peer.give2peer.entity.Server;

public class OnLocationNameChangeListener implements Preference.OnPreferenceChangeListener
{
    Location location;
    PreferenceScreen screen;
    public OnLocationNameChangeListener(Location location, PreferenceScreen screen)
    {
        this.location = location;
        this.screen = screen;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        preference.setSummary((String)newValue);
        screen.setTitle((String)newValue);

        location.setName((String)newValue);
        location.save();

        return true;
    }
}