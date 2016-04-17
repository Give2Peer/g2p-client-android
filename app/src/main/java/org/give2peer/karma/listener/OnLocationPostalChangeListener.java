package org.give2peer.karma.listener;

import android.preference.Preference;

import org.give2peer.karma.entity.Location;

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