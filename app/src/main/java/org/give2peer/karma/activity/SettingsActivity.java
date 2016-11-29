package org.give2peer.karma.activity;

import android.preference.PreferenceActivity;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.PreferenceScreen;
import org.give2peer.karma.R;

@PreferenceScreen(R.xml.preferences)
@EActivity
public class SettingsActivity extends PreferenceActivity {

//    @PreferenceByKey(R.string.myPref1)
//    Preference myPreference1;
//
//    @PreferenceByKey(R.string.checkBoxPref)
//    CheckBoxPreference checkBoxPref;
//
//    @AfterPreferences
//    void initPrefs() {
//        checkBoxPref.setChecked(false);
//    }
}