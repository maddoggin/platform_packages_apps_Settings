/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.R;
import java.util.List;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.custom.colorpicker.ColorPickerView;

public class WallpaperTypeSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final int REQUEST_CODE_BG_WALLPAPER = 1024;

    private static final int LOCKSCREEN_BACKGROUND_COLOR_FILL = 0;
    private static final int LOCKSCREEN_BACKGROUND_CUSTOM_IMAGE = 1;
    private static final int LOCKSCREEN_BACKGROUND_DEFAULT_WALLPAPER = 2;

    private static final String KEY_BACKGROUND = "lockscreen_background";

    private ListPreference mCustomBackground;

    private File mWallpaperImage;
    private File mWallpaperTemporary;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wallpaper_settings);
        populateWallpaperTypes();

        mCustomBackground = (ListPreference) findPreference(KEY_BACKGROUND);
        mCustomBackground.setOnPreferenceChangeListener(this);
        updateCustomBackgroundSummary();

        mWallpaperImage = new File(getActivity().getFilesDir() + "/lockwallpaper");
        mWallpaperTemporary = new File(getActivity().getCacheDir() + "/lockwallpaper.tmp");
    }

    private void updateCustomBackgroundSummary() {
        int resId;
        String value = Settings.System.getString(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND);
        if (value == null) {
            resId = R.string.lockscreen_background_default_wallpaper;
            mCustomBackground.setValueIndex(LOCKSCREEN_BACKGROUND_DEFAULT_WALLPAPER);
        } else if (value.isEmpty()) {
            resId = R.string.lockscreen_background_custom_image;
            mCustomBackground.setValueIndex(LOCKSCREEN_BACKGROUND_CUSTOM_IMAGE);
        } else {
            resId = R.string.lockscreen_background_color_fill;
            mCustomBackground.setValueIndex(LOCKSCREEN_BACKGROUND_COLOR_FILL);
        }
        mCustomBackground.setSummary(getResources().getString(resId));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BG_WALLPAPER) {
            int hintId;

            if (resultCode == Activity.RESULT_OK) {
                if (mWallpaperTemporary.exists()) {
                    mWallpaperTemporary.renameTo(mWallpaperImage);
                }
                mWallpaperImage.setReadOnly();
                hintId = R.string.lockscreen_background_result_successful;
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND, "");
                updateCustomBackgroundSummary();
            } else {
                if (mWallpaperTemporary.exists()) {
                    mWallpaperTemporary.delete();
                }
                hintId = R.string.lockscreen_background_result_not_successful;
            }
            Toast.makeText(getActivity(),
                    getResources().getString(hintId), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();

        if (preference == mCustomBackground) {
            int selection = mCustomBackground.findIndexOfValue(objValue.toString());
            return handleBackgroundSelection(selection);
        }
        return false;
    }

    private boolean handleBackgroundSelection(int selection) {
        if (selection == LOCKSCREEN_BACKGROUND_COLOR_FILL) {
            final ColorPickerView colorView = new ColorPickerView(getActivity());
            int currentColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND, -1);

            if (currentColor != -1) {
                colorView.setColor(currentColor);
            }
            colorView.setAlphaSliderVisible(true);

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.lockscreen_custom_background_dialog_title)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getContentResolver(),
                                    Settings.System.LOCKSCREEN_BACKGROUND, colorView.getColor());
                            updateCustomBackgroundSummary();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setView(colorView)
                    .show();
        } else if (selection == LOCKSCREEN_BACKGROUND_CUSTOM_IMAGE) {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());

            final Display display = getActivity().getWindowManager().getDefaultDisplay();
            final Rect rect = new Rect();
            final Window window = getActivity().getWindow();

            window.getDecorView().getWindowVisibleDisplayFrame(rect);

            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;

            int width = display.getWidth();
            int height = display.getHeight() - titleBarHeight;

            intent.putExtra("aspectX", isPortrait ? width : height);
            intent.putExtra("aspectY", isPortrait ? height : width);

            try {
                mWallpaperTemporary.createNewFile();
                mWallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mWallpaperTemporary));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQUEST_CODE_BG_WALLPAPER);
            } catch (IOException e) {
                // Do nothing here
            } catch (ActivityNotFoundException e) {
                // Do nothing here
            }
        } else if (selection == LOCKSCREEN_BACKGROUND_DEFAULT_WALLPAPER) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND, null);
            updateCustomBackgroundSummary();
            return true;
        }

        return false;
    }

    private void populateWallpaperTypes() {
        // Search for activities that satisfy the ACTION_SET_WALLPAPER action
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        final PackageManager pm = getPackageManager();
        List<ResolveInfo> rList = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        final PreferenceScreen parent = getPreferenceScreen();
        parent.setOrderingAsAdded(false);
        // Add Preference items for each of the matching activities
        for (ResolveInfo info : rList) {
            Preference pref = new Preference(getActivity());
            Intent prefIntent = new Intent(intent);
            prefIntent.setComponent(new ComponentName(
                    info.activityInfo.packageName, info.activityInfo.name));
            pref.setIntent(prefIntent);
            CharSequence label = info.loadLabel(pm);
            if (label == null) label = info.activityInfo.packageName;
            pref.setTitle(label);
            parent.addPreference(pref);
        }
    }
}
