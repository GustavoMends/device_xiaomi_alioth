/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

/** Thermal profile settings activity. */
class ThermalSettingsActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, ThermalSettingsFragment(), TAG_THERMAL)
            .commit()
    }

    companion object {
        private const val TAG_THERMAL = "thermal"
    }
}
