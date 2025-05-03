/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.PreferenceFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.settingslib.applications.ApplicationsState
import com.android.settingslib.widget.MainSwitchPreference
import org.lineageos.settings.R
import org.lineageos.settings.thermal.ThermalUtils.ThermalState

/**
 * Thermal profile settings fragment, shows list of apps with a drop-down spinner to select their
 * thermal profile.
 */
class ThermalSettingsFragment : PreferenceFragment(), ApplicationsState.Callbacks {

    private lateinit var allPackagesAdapter: AllPackagesAdapter
    private lateinit var applicationsState: ApplicationsState
    private lateinit var session: ApplicationsState.Session
    private lateinit var activityFilter: ActivityFilter
    private val entryMap = HashMap<String, ApplicationsState.AppEntry>()
    private lateinit var thermalUtils: ThermalUtils
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var mainSwitch: MainSwitchPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.thermal_settings)

        thermalUtils = ThermalUtils.getInstance(activity)
        mainSwitch =
            findPreference<MainSwitchPreference>(THERMAL_ENABLE_KEY)!!.apply {
                isChecked = thermalUtils.enabled
                addOnSwitchChangeListener { _, isChecked ->
                    thermalUtils.enabled = isChecked
                    appsRecyclerView.isVisible = isChecked
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationsState = ApplicationsState.getInstance(activity.application)
        session = applicationsState.newSession(this)
        activityFilter = ActivityFilter(activity.packageManager)
        allPackagesAdapter = AllPackagesAdapter(activity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appsRecyclerView =
            view.findViewById<RecyclerView>(R.id.thermal_rv_view)!!.apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = allPackagesAdapter
                isVisible = thermalUtils.enabled
            }
    }

    override fun onPause() {
        super.onPause()
        session.onPause()
    }

    override fun onResume() {
        super.onResume()
        session.onResume()
        rebuild()
    }

    override fun onDestroy() {
        super.onDestroy()
        session.onDestroy()
    }

    override fun onPackageListChanged() {
        activityFilter.updateLauncherInfoList()
        rebuild()
    }

    override fun onRebuildComplete(entries: ArrayList<ApplicationsState.AppEntry>?) {
        entries?.let {
            handleAppEntries(it)
            allPackagesAdapter.notifyDataSetChanged()
        }
    }

    override fun onLoadEntriesCompleted() {
        rebuild()
    }

    override fun onAllSizesComputed() {}

    override fun onLauncherInfoChanged() {}

    override fun onPackageIconChanged() {}

    override fun onPackageSizeChanged(packageName: String?) {}

    override fun onRunningStateChanged(running: Boolean) {}

    private fun handleAppEntries(entries: List<ApplicationsState.AppEntry>) {
        val sections = ArrayList<String>()
        val positions = ArrayList<Int>()
        val pm = activity.packageManager
        var lastSectionIndex: String? = null
        var offset = 0

        entries.forEach { entry ->
            val info = entry.info
            val label = info.loadLabel(pm).toString()
            val sectionIndex =
                when {
                    !info.enabled -> "--"
                    TextUtils.isEmpty(label) -> ""
                    else -> label.substring(0, 1).uppercase()
                }
            if (sectionIndex != lastSectionIndex) {
                sections.add(sectionIndex)
                positions.add(offset)
                lastSectionIndex = sectionIndex
            }
            offset++
        }

        allPackagesAdapter.setEntries(entries, sections, positions)
        entryMap.clear()
        entries.forEach { e -> entryMap[e.info.packageName] = e }
    }

    private fun rebuild() {
        session.rebuild(activityFilter, ApplicationsState.ALPHA_COMPARATOR)
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.app_name)!!
        val mode: Spinner = view.findViewById(R.id.app_mode)!!
        val icon: ImageView = view.findViewById(R.id.app_icon)!!
        val stateIcon: ImageView = view.findViewById(R.id.state)!!

        init {
            view.tag = this
        }
    }

    private inner class ModeAdapter(context: Context) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)
        private val items = ThermalState.values().map { it.label }

        override fun getCount() = items.size

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int) = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            (convertView as? TextView
                    ?: inflater.inflate(
                        android.R.layout.simple_spinner_dropdown_item,
                        parent,
                        false,
                    ) as TextView)
                .apply {
                    setText(items[position])
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                }
    }

    private inner class AllPackagesAdapter(context: Context) :
        RecyclerView.Adapter<ViewHolder>(), AdapterView.OnItemSelectedListener, SectionIndexer {

        var entries: List<ApplicationsState.AppEntry> = ArrayList()
        private var sections: Array<String> = arrayOf()
        private var positions: IntArray = intArrayOf()

        override fun getItemCount() = entries.size

        override fun getItemId(position: Int) = entries[position].id

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.thermal_list_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.run {
                val entry = entries[position]
                val state = thermalUtils.getStateForPackage(entry.info.packageName)
                mode.apply {
                    adapter = ModeAdapter(itemView.context)
                    onItemSelectedListener = this@AllPackagesAdapter
                    setSelection(state.id, false)
                    tag = entry
                }
                title.apply {
                    text = entry.label
                    setOnClickListener { mode.performClick() }
                }
                applicationsState.ensureIcon(entry)
                icon.setImageDrawable(entry.icon)
                stateIcon.setImageResource(state.icon)
            }

        fun setEntries(
            entries: List<ApplicationsState.AppEntry>,
            sections: List<String>,
            positions: List<Int>,
        ) {
            this.entries = entries
            this.sections = sections.toTypedArray()
            this.positions = positions.toIntArray()
            notifyDataSetChanged()
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val entry = parent?.tag as? ApplicationsState.AppEntry ?: return
            val currentState = thermalUtils.getStateForPackage(entry.info.packageName).id
            if (currentState != position) {
                thermalUtils.writePackage(entry.info.packageName, position)
                notifyDataSetChanged()
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun getPositionForSection(section: Int): Int =
            if (section < 0 || section >= sections.size) -1 else positions[section]

        override fun getSectionForPosition(position: Int): Int {
            if (position < 0 || position >= itemCount) return -1
            val index = positions.binarySearch(position)
            return if (index >= 0) index else -index - 2
        }

        override fun getSections(): Array<Any> = sections as Array<Any>
    }

    private inner class ActivityFilter(private val packageManager: PackageManager) :
        ApplicationsState.AppFilter {

        private val launcherResolveInfoList = ArrayList<String>()

        init {
            updateLauncherInfoList()
        }

        fun updateLauncherInfoList() {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
            synchronized(launcherResolveInfoList) {
                launcherResolveInfoList.clear()
                resolveInfoList.forEach { launcherResolveInfoList.add(it.activityInfo.packageName) }
            }
        }

        override fun init() {}

        override fun filterApp(entry: ApplicationsState.AppEntry): Boolean {
            var show =
                !allPackagesAdapter.entries
                    .map { it.info.packageName }
                    .contains(entry.info.packageName)
            if (show) {
                synchronized(launcherResolveInfoList) {
                    show = launcherResolveInfoList.contains(entry.info.packageName)
                }
            }
            return show
        }
    }

    companion object {
        private const val THERMAL_ENABLE_KEY = "thermal_enable"
    }
}
