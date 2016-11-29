package org.give2peer.karma.activity;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.PreferenceByKey;
import org.androidannotations.annotations.PreferenceClick;
import org.androidannotations.annotations.PreferenceScreen;
import org.give2peer.karma.Application;
import org.give2peer.karma.R;

@PreferenceScreen(R.xml.preferences)
@EActivity
public class SettingsActivity extends PreferenceActivity {

    Application app;

    @PreferenceByKey(R.string.settings_motd)
    Preference motdPreference; // Message Of The Day

    @PreferenceByKey(R.string.settings_pins_animated)
    SwitchPreference pinsPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (Application) getApplication();
    }

//    @AfterPreferences
//    void initPrefs() {
//        checkBoxPref.setChecked(false);
//    }

    @PreferenceClick(R.string.settings_motd)
    void motdPreferenceClick(Preference preference) {
        app.toasty("Yummy!");
    }
}