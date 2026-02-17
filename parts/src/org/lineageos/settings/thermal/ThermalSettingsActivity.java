/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal;

import android.os.Bundle;
import android.view.MenuItem;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class ThermalSettingsActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG_THERMAL = "thermal";
    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                        new ThermalSettingsFragment(),
                        TAG_THERMAL)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }
}
