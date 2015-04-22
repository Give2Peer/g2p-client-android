package org.give2peer.give2peer.listener;

import android.preference.Preference;
import android.preference.PreferenceScreen;

import org.give2peer.give2peer.entity.Location;

public class OnLocationPostalChangeListener implements Preference.OnPreferenceChangeListener
{
    Location location;
    public OnLocationPostalChangeListener(Location location)
    {
        this.location = location;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        preference.setSummary((String)newValue);

        location.setPostal((String)newValue);
        location.save();

        return true;
    }
}