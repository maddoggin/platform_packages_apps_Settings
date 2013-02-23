/*
 * Copyright (C) 2012 CyanogenMod
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.custom;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class SystemSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_NAVIGATION_RING = "navigation_ring";
    private static final String KEY_NAVIGATION_BAR_CATEGORY = "navigation_bar_category";
    private static final String KEY_STATUS_BAR = "status_bar";
    private static final String KEY_NAV_BUTTONS_HEIGHT = "nav_buttons_height";
    /*private static final String KEY_QUICK_SETTINGS = "quick_settings_panel";
    private static final String KEY_POWER_MENU = "power_menu";
    private static final String KEY_PIE_CONTROL = "pie_control";*/

    private ListPreference mNavButtonsHeight;
    /*private PreferenceScreen mPieControl;*/
    private boolean mIsPrimary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();

        // Determine which user is logged in
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        if (mIsPrimary) {
            // NavButtons Height
        mNavButtonsHeight = (ListPreference) findPreference(KEY_NAV_BUTTONS_HEIGHT);
        mNavButtonsHeight.setOnPreferenceChangeListener(this);

        int statusNavButtonsHeight = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                 Settings.System.NAV_BUTTONS_HEIGHT, 48);
        mNavButtonsHeight.setValue(String.valueOf(statusNavButtonsHeight));
        mNavButtonsHeight.setSummary(mNavButtonsHeight.getEntry());

        if (!Utils.isPhone(getActivity()) || !getResources().getBoolean(
             com.android.internal.R.bool.config_showNavigationBar)) {
             prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR));
             prefScreen.removePreference(findPreference(KEY_NAVIGATION_RING));
             prefScreen.removePreference(findPreference(KEY_NAV_BUTTONS_HEIGHT));
            }
        } else {
            // Secondary user is logged in, remove all primary user specific preferences
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_RING));
            prefScreen.removePreference(findPreference(KEY_NAV_BUTTONS_HEIGHT));
            prefScreen.removePreference(findPreference(KEY_STATUS_BAR));
            /*prefScreen.removePreference(findPreference(KEY_QUICK_SETTINGS));
            prefScreen.removePreference(findPreference(KEY_POWER_MENU));*/
        }

        // Pie controls
        /*mPieControl = (PreferenceScreen) findPreference(KEY_PIE_CONTROL);*/
    }

    @Override
    public void onResume() {
        super.onResume();

        // All users
        /*if (mPieControl != null) {
            updatePieControlDescription();
        }*/
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /*private void updatePieControlDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) == 1) {
            mPieControl.setSummary(getString(R.string.pie_control_enabled));
        } else {
            mPieControl.setSummary(getString(R.string.pie_control_disabled));
        }
    }*/

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNavButtonsHeight) {
            int statusNavButtonsHeight = Integer.valueOf((String) objValue);
            int index = mNavButtonsHeight.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAV_BUTTONS_HEIGHT, statusNavButtonsHeight);
            mNavButtonsHeight.setSummary(mNavButtonsHeight.getEntries()[index]);
            return true;
        }
        return false;
    }
}
