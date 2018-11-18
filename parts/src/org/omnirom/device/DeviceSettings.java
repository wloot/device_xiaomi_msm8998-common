/*
* Copyright (C) 2016 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.omnirom.device;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.TwoStatePreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.util.Log;

public class DeviceSettings extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_CATEGORY_DISPLAY = "display";
    private static final String KEY_CATEGORY_CAMERA = "camera";
    private static final String ENABLE_HAL3_KEY = "hal3";
    private static final String SPECTRUM_KEY = "spectrum";
    private static final String SPECTRUM_SYSTEM_PROPERTY = "persist.spectrum.profile";
    public static final String S2S_KEY = "sweep2sleep";
    public static final String KEY_TAPTOWAKE_SWITCH = "taptowake";
    public static final String KEY_VIBSTRENGTH = "vib_strength";
    public static final String KEY_S2S_VIBSTRENGTH = "s2s_vib_strength";
    public static final String FILE_S2S_TYPE = "/sys/sweep2sleep/sweep2sleep";

    final String KEY_DEVICE_DOZE = "device_doze";
    final String KEY_DEVICE_DOZE_PACKAGE_NAME = "org.lineageos.settings.doze";

    private TwoStatePreference mTapToWakeSwitch;
    private VibratorStrengthPreference mVibratorStrength;
    private static final String HAL3_SYSTEM_PROPERTY = "persist.camera.HAL3.enabled";

    private S2SVibratorStrengthPreference mVibratorStrengthS2S;
    private SwitchPreference mEnableHAL3;
    private ListPreference mS2S;
    private Preference mKcalPref;
    private ListPreference mSPECTRUM;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main, rootKey);

        mKcalPref = findPreference("kcal");
        mKcalPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getContext(), DisplayCalibration.class);
                startActivity(intent);
                return true;
            }
        });

        mEnableHAL3 = (SwitchPreference) findPreference(ENABLE_HAL3_KEY);
        mEnableHAL3.setChecked(SystemProperties.getBoolean(HAL3_SYSTEM_PROPERTY, false));
        mEnableHAL3.setOnPreferenceChangeListener(this);

        mS2S = (ListPreference) findPreference(S2S_KEY);
        mS2S.setValue(Utils.getFileValue(FILE_S2S_TYPE, "0"));
        mS2S.setOnPreferenceChangeListener(this);

        mSPECTRUM = (ListPreference) findPreference(SPECTRUM_KEY);
        if( mSPECTRUM != null ) {
            mSPECTRUM.setValue(SystemProperties.get(SPECTRUM_SYSTEM_PROPERTY, "0"));
            mSPECTRUM.setOnPreferenceChangeListener(this);
        }

        mVibratorStrength = (VibratorStrengthPreference) findPreference(KEY_VIBSTRENGTH);
        if (mVibratorStrength != null) {
            mVibratorStrength.setEnabled(VibratorStrengthPreference.isSupported());
        }

        mTapToWakeSwitch = (TwoStatePreference) findPreference(KEY_TAPTOWAKE_SWITCH);
        mTapToWakeSwitch.setEnabled(TapToWakeSwitch.isSupported());
        mTapToWakeSwitch.setChecked(TapToWakeSwitch.isCurrentlyEnabled(this.getContext()));
        mTapToWakeSwitch.setOnPreferenceChangeListener(new TapToWakeSwitch());

        mVibratorStrengthS2S = (S2SVibratorStrengthPreference) findPreference(KEY_S2S_VIBSTRENGTH);
        if (mVibratorStrengthS2S != null) {
            mVibratorStrengthS2S.setEnabled(S2SVibratorStrengthPreference.isSupported());
        }

        if (!isAppInstalled(KEY_DEVICE_DOZE_PACKAGE_NAME)) {
            PreferenceCategory displayCategory = (PreferenceCategory) findPreference(KEY_CATEGORY_DISPLAY);
            displayCategory.removePreference(findPreference(KEY_DEVICE_DOZE));
        }

    }

   private void setEnableHAL3(boolean value) {
        if(value) {
            SystemProperties.set(HAL3_SYSTEM_PROPERTY, "1");
        } else {
            SystemProperties.set(HAL3_SYSTEM_PROPERTY, "0");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        boolean value;
        String strvalue;
        if (ENABLE_HAL3_KEY.equals(key)) {
            value = (Boolean) newValue;
            mEnableHAL3.setChecked(value);
            setEnableHAL3(value);
            return true;
        } else if (S2S_KEY.equals(key)) {
            strvalue = (String) newValue;
            Utils.writeValue("/sys/sweep2sleep/sweep2sleep", strvalue);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            editor.putString(S2S_KEY, strvalue);
            editor.commit();
            return true;
        } else if (SPECTRUM_KEY.equals(key)) {
            strvalue = (String) newValue;
            SystemProperties.set(SPECTRUM_SYSTEM_PROPERTY, strvalue);
            return true;
        }

        return true;
    }

    private boolean isAppInstalled(String uri) {
        PackageManager pm = getContext().getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }

        return false;
    }
}
