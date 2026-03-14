/*
 *
 * Copyright (C) 2025 The AviumUI Project
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
 *
 */

package org.avium.systemuiex.ui.selection

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import org.avium.systemuiex.R
import org.avium.systemuiex.model.AppInfo
import org.avium.systemuiex.util.AppListProvider
import org.avium.systemuiex.util.Config
import org.avium.systemuiex.util.PreferenceHelper

class AppSelectionActivity : CollapsingToolbarBaseActivity() {

    private val appList = mutableListOf<AppInfo>()
    private val selectedApps = mutableListOf<String>()
    private val maxSelectionCount = Config.MAX_ICONS - 1

    private lateinit var appListView: RecyclerView
    private lateinit var loading: ProgressBar
    private lateinit var adapter: AppSelectionAdapter
    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)
        title = getString(R.string.manage_apps)

        appListView = findViewById(R.id.app_list)
        loading = findViewById(R.id.loading)

        selectedApps.addAll(PreferenceHelper.getSelectedApps(this))

        adapter = AppSelectionAdapter()
        appListView.layoutManager = LinearLayoutManager(this)
        appListView.adapter = adapter
        appListView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

        touchHelper = ItemTouchHelper(SelectedAppsTouchHelperCallback())
        touchHelper.attachToRecyclerView(appListView)
        adapter.setItemTouchHelper(touchHelper)

        loadApps()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadApps() {
        loading.visibility = View.VISIBLE
        appListView.visibility = View.GONE
        lifecycleScope.launch {
            val loaded = AppListProvider.getLaunchableApps(this@AppSelectionActivity)
            appList.clear()
            appList.addAll(loaded)
            rebuildItems()
            loading.visibility = View.GONE
            appListView.visibility = View.VISIBLE
        }
    }

    private fun rebuildItems() {
        val pm = packageManager
        val validSelected = ArrayList<String>()
        for (pkg in selectedApps) {
            try {
                pm.getApplicationInfo(pkg, 0)
                validSelected.add(pkg)
            } catch (_: PackageManager.NameNotFoundException) {
                // Drop uninstalled apps
            }
        }
        if (validSelected != selectedApps) {
            selectedApps.clear()
            selectedApps.addAll(validSelected)
            PreferenceHelper.saveSelectedApps(this, selectedApps)
        }
        adapter.buildItems()
    }

    private inner class AppSelectionAdapter :
        RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemMoveCallback {

        private val items = ArrayList<UiItem>()
        private val pm = packageManager
        private var itemTouchHelper: ItemTouchHelper? = null

        override fun getItemViewType(position: Int): Int = items[position].type

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                UiItem.TYPE_HEADER -> HeaderViewHolder(
                    inflater.inflate(R.layout.item_section_header, parent, false)
                )
                UiItem.TYPE_EMPTY -> EmptyViewHolder(
                    inflater.inflate(R.layout.item_empty_state, parent, false)
                )
                else -> AppViewHolder(
                    inflater.inflate(R.layout.item_app_row, parent, false)
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            when (holder) {
                is HeaderViewHolder -> holder.bind(item.titleRes)
                is EmptyViewHolder -> holder.bind(item.titleRes)
                is AppViewHolder -> holder.bind(item)
            }
        }

        override fun getItemCount(): Int = items.size

        fun setItemTouchHelper(helper: ItemTouchHelper) {
            itemTouchHelper = helper
        }

        fun buildItems() {
            val newItems = ArrayList<UiItem>()
            newItems.add(UiItem.header(R.string.selected_apps))

            val selectedInfos = ArrayList<AppInfo>()
            for (pkg in selectedApps) {
                resolveAppInfo(pkg)?.let { selectedInfos.add(it) }
            }
            if (selectedInfos.isEmpty()) {
                newItems.add(UiItem.empty(R.string.no_apps_selected))
            } else {
                selectedInfos.forEach { newItems.add(UiItem.selected(it)) }
            }

            newItems.add(UiItem.header(R.string.all_apps))
            val unselected = appList
                .filter { !selectedApps.contains(it.packageName) }
                .sortedBy { it.appName.lowercase() }
            unselected.forEach { newItems.add(UiItem.all(it)) }

            val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(
                UiItemDiffCallback(items, newItems)
            )
            items.clear()
            items.addAll(newItems)
            diff.dispatchUpdatesTo(this)
        }

        fun isSelectedItem(position: Int): Boolean {
            return items.getOrNull(position)?.isSelectedSection == true
        }

        override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
            if (!isSelectedItem(fromPosition) || !isSelectedItem(toPosition)) {
                return false
            }
            val fromIndex = selectedIndexForAdapterPos(fromPosition)
            val toIndex = selectedIndexForAdapterPos(toPosition)
            if (fromIndex < 0 || toIndex < 0) return false

            val pkg = selectedApps.removeAt(fromIndex)
            val insertIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
            selectedApps.add(insertIndex, pkg)

            val moved = items.removeAt(fromPosition)
            items.add(toPosition, moved)
            notifyItemMoved(fromPosition, toPosition)
            PreferenceHelper.saveSelectedApps(this@AppSelectionActivity, selectedApps)
            return true
        }

        private fun selectedIndexForAdapterPos(position: Int): Int {
            var idx = 0
            for (i in items.indices) {
                val item = items[i]
                if (item.isSelectedSection) {
                    if (i == position) return idx
                    idx++
                }
            }
            return -1
        }

        private fun resolveAppInfo(packageName: String): AppInfo? {
            return try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                AppInfo(label, packageName, pm.getApplicationIcon(appInfo), true)
            } catch (_: Exception) {
                null
            }
        }

        private inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val title: TextView = view.findViewById(R.id.section_title)
            fun bind(titleRes: Int) {
                title.setText(titleRes)
            }
        }

        private inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val text: TextView = view.findViewById(R.id.empty_text)
            fun bind(textRes: Int) {
                text.setText(textRes)
            }
        }

        private inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val root: com.android.settingslib.widget.DrawableStateLinearLayout? =
                view as? com.android.settingslib.widget.DrawableStateLinearLayout
            private val icon: ImageView = view.findViewById(R.id.app_icon)
            private val name: TextView = view.findViewById(R.id.app_name)
            private val pkg: TextView = view.findViewById(R.id.app_package)
            private val check: android.widget.CheckBox =
                view.findViewById(R.id.app_check)
            private val dragHandle: ImageView = view.findViewById(R.id.app_drag)

            fun bind(item: UiItem) {
                val app = item.app ?: return
                root?.extraDrawableState = intArrayOf(android.R.attr.state_single)
                icon.setImageDrawable(app.icon)
                icon.contentDescription = app.appName
                name.text = app.appName
                pkg.text = app.packageName
                check.setOnCheckedChangeListener(null)
                check.isChecked = item.isSelectedSection
                dragHandle.visibility = if (item.isSelectedSection) View.VISIBLE else View.INVISIBLE

                check.setOnCheckedChangeListener { _, isChecked ->
                    handleToggle(app, isChecked)
                }
                itemView.setOnClickListener {
                    check.toggle()
                }

                dragHandle.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN && item.isSelectedSection) {
                        itemTouchHelper?.startDrag(this)
                        return@setOnTouchListener true
                    }
                    false
                }
            }
        }

        private fun handleToggle(app: AppInfo, checked: Boolean) {
            if (checked) {
                if (selectedApps.size >= maxSelectionCount) {
                    val msg = getString(R.string.selection_limit_toast, maxSelectionCount)
                    Toast.makeText(this@AppSelectionActivity, msg, Toast.LENGTH_SHORT).show()
                    return
                }
                if (!selectedApps.contains(app.packageName)) {
                    selectedApps.add(app.packageName)
                }
            } else {
                selectedApps.remove(app.packageName)
            }
            PreferenceHelper.saveSelectedApps(this@AppSelectionActivity, selectedApps)
            buildItems()
        }
    }

    private interface ItemMoveCallback {
        fun onItemMove(fromPosition: Int, toPosition: Int): Boolean
    }

    private inner class SelectedAppsTouchHelperCallback : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0
    ) {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return if (adapter.isSelectedItem(viewHolder.bindingAdapterPosition)) {
                makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
            } else {
                0
            }
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // no-op
        }

        override fun isLongPressDragEnabled(): Boolean = false
    }

    private data class UiItem(
        val type: Int,
        val titleRes: Int = 0,
        val app: AppInfo? = null,
        val isSelectedSection: Boolean = false
    ) {
        companion object {
            const val TYPE_HEADER = 0
            const val TYPE_EMPTY = 1
            const val TYPE_APP = 2

            fun header(titleRes: Int) = UiItem(TYPE_HEADER, titleRes = titleRes)
            fun empty(textRes: Int) = UiItem(TYPE_EMPTY, titleRes = textRes)
            fun selected(app: AppInfo) = UiItem(TYPE_APP, app = app, isSelectedSection = true)
            fun all(app: AppInfo) = UiItem(TYPE_APP, app = app, isSelectedSection = false)
        }
    }

    private class UiItemDiffCallback(
        private val oldItems: List<UiItem>,
        private val newItems: List<UiItem>
    ) : androidx.recyclerview.widget.DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldItems.size
        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem.type != newItem.type) return false
            return when (oldItem.type) {
                UiItem.TYPE_APP -> oldItem.app?.packageName == newItem.app?.packageName
                UiItem.TYPE_HEADER, UiItem.TYPE_EMPTY -> oldItem.titleRes == newItem.titleRes
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
