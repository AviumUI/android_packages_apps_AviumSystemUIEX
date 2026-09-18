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
import androidx.core.widget.doAfterTextChanged
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

open class AppSelectionActivity : CollapsingToolbarBaseActivity() {

    private val appList = mutableListOf<AppInfo>()
    private val selectedApps = mutableListOf<String>()
    private val blacklistedApps = mutableListOf<String>()
    private val maxSelectionCount = Config.MAX_ICONS - 1

    private lateinit var appListView: RecyclerView
    private lateinit var loading: ProgressBar
    private lateinit var adapter: AppSelectionAdapter
    private lateinit var touchHelper: ItemTouchHelper

    private var searchQuery = ""
    private var privateSpaceActionInProgress = false
    private var mode: Int = MODE_SELECTION
    private var alphabetRail: android.widget.LinearLayout? = null
    private var selectedProfileType = android.os.UserManager.USER_TYPE_FULL_SYSTEM

    private var floatingBlurState: Boolean? = null
    private val floatingBackground = android.graphics.drawable.GradientDrawable()
    private val blurListener = java.util.function.Consumer<Boolean> { enabled ->
        updateFloatingBackground(enabled)
    }

    private fun updateFloatingBackground(blurEnabled: Boolean) {
        if (floatingBlurState == blurEnabled) return
        floatingBlurState = blurEnabled
        val density = resources.displayMetrics.density
        val color = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, color, true)
        val surfaceColor = if (color.resourceId != 0) getColor(color.resourceId) else color.data
        floatingBackground.cornerRadius = 28 * density
        floatingBackground.setColor((surfaceColor and 0x00ffffff) or
            ((if (blurEnabled) 120 else 255) shl 24))
        window.setBackgroundDrawable(floatingBackground)
        // One compositor blur layer is sufficient. Combining a rounded background-blur
        // region with blur-behind makes their edges animate independently during transitions.
        window.setBackgroundBlurRadius(0)
        val attributes = window.attributes
        attributes.blurBehindRadius = if (blurEnabled) (48 * density).toInt() else 0
        attributes.flags = if (blurEnabled) {
            attributes.flags or android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        } else {
            attributes.flags and android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        }
        window.attributes = attributes
        window.setDimAmount(if (blurEnabled) 0.10f else 0.25f)
    }

    private fun inSelectedProfile(app: AppInfo): Boolean = when (selectedProfileType) {
        android.os.UserManager.USER_TYPE_PROFILE_CLONE ->
            app.profileType == android.os.UserManager.USER_TYPE_PROFILE_CLONE
        android.os.UserManager.USER_TYPE_PROFILE_PRIVATE ->
            app.profileType == android.os.UserManager.USER_TYPE_PROFILE_PRIVATE
        else -> !app.selectionKey.contains(':')
    }

    private fun privateProfile(): android.os.UserHandle? {
        val launcher = getSystemService(android.content.pm.LauncherApps::class.java)
        return launcher.profiles.firstOrNull { user ->
            createContextAsUser(user, 0).getSystemService(android.os.UserManager::class.java)
                .isPrivateProfile
        }
    }

    private fun updatePrivateSpaceAction(): Boolean {
        val container = findViewById<android.widget.FrameLayout>(R.id.private_space_action)
        if (selectedProfileType != android.os.UserManager.USER_TYPE_PROFILE_PRIVATE) {
            container.visibility = View.GONE
            return false
        }
        val user = privateProfile()
        val users = getSystemService(android.os.UserManager::class.java)
        val unavailable = user == null || users.isQuietModeEnabled(user) || !users.isUserUnlocked(user)
        container.visibility = View.VISIBLE
        container.removeAllViews()
        // Material widgets require their own theme; the surrounding activity uses SettingsLib.
        val materialContext = android.view.ContextThemeWrapper(this, R.style.Theme_SystemUIEX_Material3Expressive)
        val padding = (20 * resources.displayMetrics.density).toInt()
        val card = com.google.android.material.card.MaterialCardView(materialContext).apply {
            radius = 24 * resources.displayMetrics.density
            cardElevation = 0f
        }
        val content = android.widget.LinearLayout(materialContext).apply {
            orientation = if (unavailable) android.widget.LinearLayout.VERTICAL
                else android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        content.addView(com.google.android.material.textview.MaterialTextView(materialContext).apply {
            setText(R.string.profile_private)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        }, android.widget.LinearLayout.LayoutParams(
            if (unavailable) ViewGroup.LayoutParams.MATCH_PARENT else 0,
            ViewGroup.LayoutParams.WRAP_CONTENT, if (unavailable) 0f else 1f))
        if (unavailable) content.addView(com.google.android.material.textview.MaterialTextView(materialContext).apply {
            setText(if (user == null) R.string.private_space_create_description
                else R.string.private_space_locked_description)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setPadding(0, padding / 2, 0, padding)
        })
        content.addView(com.google.android.material.button.MaterialButton(materialContext).apply {
            setText(when {
                user == null -> R.string.private_space_create
                unavailable -> R.string.private_space_unlock
                else -> R.string.private_space_lock
            })
            isEnabled = !privateSpaceActionInProgress
            setOnClickListener {
                val current = privateProfile()
                if (current == null) {
                    openPrivateSpaceSettings()
                } else {
                    if (privateSpaceActionInProgress) return@setOnClickListener
                    val lock = !unavailable
                    privateSpaceActionInProgress = true
                    isEnabled = false
                    if (lock) {
                        // Hide private app content as soon as locking starts.
                        appList.removeAll {
                            it.profileType == android.os.UserManager.USER_TYPE_PROFILE_PRIVATE
                        }
                        adapter.buildItems()
                    }
                    lifecycleScope.launch {
                        try {
                            // Match Launcher3: change quiet mode off the UI thread.
                            // The system presents its credential challenge when needed.
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                users.requestQuietModeEnabled(lock, current)
                            }
                        } catch (error: SecurityException) {
                            android.util.Log.e("AppSelectionActivity", "Cannot change private space lock state", error)
                            Toast.makeText(this@AppSelectionActivity,
                                if (lock) R.string.private_space_lock_failed
                                else R.string.private_space_unlock_failed, Toast.LENGTH_LONG).show()
                        } catch (_: IllegalArgumentException) {
                            // The profile may have been removed; refresh the create state below.
                        } finally {
                            privateSpaceActionInProgress = false
                            updatePrivateSpaceAction()
                            loadApps()
                        }
                    }
                }
            }
        })
        card.addView(content)
        container.addView(card, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return unavailable || privateSpaceActionInProgress
    }

    private fun openPrivateSpaceSettings() {
        Toast.makeText(this, R.string.private_space_settings_hint, Toast.LENGTH_LONG).show()
        startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
            .setPackage("com.android.settings"))
    }

    private fun setupProfileTabs() {
        val host = findViewById<android.widget.FrameLayout>(R.id.profile_tabs)
        val materialContext = android.view.ContextThemeWrapper(this, R.style.Theme_SystemUIEX_Material3)
        fun color(attribute: Int): Int {
            val value = android.util.TypedValue()
            materialContext.theme.resolveAttribute(attribute, value, true)
            return if (value.resourceId != 0) materialContext.getColor(value.resourceId) else value.data
        }
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val group = com.google.android.material.button.MaterialButtonToggleGroup(materialContext).apply {
            isSingleSelection = true
            isSelectionRequired = true
        }
        host.addView(group, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val types = listOf(
            android.os.UserManager.USER_TYPE_FULL_SYSTEM to R.string.profile_main,
            android.os.UserManager.USER_TYPE_PROFILE_CLONE to R.string.profile_clone,
            android.os.UserManager.USER_TYPE_PROFILE_PRIVATE to R.string.profile_private,
        )
        val userTypes = mutableMapOf<Int, String>()
        types.forEach { (type, title) ->
            val button = com.google.android.material.button.MaterialButton(materialContext, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                id = View.generateViewId()
                setText(title)
                isCheckable = true
                maxLines = 1
                setPadding((8 * resources.displayMetrics.density).toInt(), 0,
                    (8 * resources.displayMetrics.density).toInt(), 0)
                iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
                iconSize = (16 * resources.displayMetrics.density).toInt()
                iconPadding = (4 * resources.displayMetrics.density).toInt()
                backgroundTintList = android.content.res.ColorStateList(states, intArrayOf(
                    color(com.google.android.material.R.attr.colorPrimaryContainer),
                    android.graphics.Color.TRANSPARENT))
                strokeColor = android.content.res.ColorStateList.valueOf(
                    color(com.google.android.material.R.attr.colorOutline))
                setTextColor(android.content.res.ColorStateList(states, intArrayOf(
                    color(com.google.android.material.R.attr.colorOnPrimaryContainer),
                    color(com.google.android.material.R.attr.colorOnSurface))))
                iconTint = textColors
            }
            userTypes[button.id] = type
            group.addView(button, android.widget.LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        group.addOnButtonCheckedListener { buttons, checkedId, checked ->
            val button = buttons.findViewById<com.google.android.material.button.MaterialButton>(checkedId)
            button.icon = if (checked) materialContext.getDrawable(
                com.google.android.material.R.drawable.m3_ic_check_24px) else null
            if (checked) {
                selectedProfileType = userTypes.getValue(checkedId)
                adapter.buildItems()
            }
        }
        group.check(group.getChildAt(0).id)
    }


    private var loadingJob: kotlinx.coroutines.Job? = null
    private val profileReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            loadApps()
        }
    }

    override fun onStart() {
        super.onStart()
        if (mode == MODE_LAUNCH) {
            val bounds = windowManager.currentWindowMetrics.bounds
            val density = resources.displayMetrics.density
            window.setLayout(minOf((bounds.width() * 0.92f).toInt(), (560 * density).toInt()),
                (bounds.height() * 0.65f).toInt())
            window.setGravity(android.view.Gravity.CENTER)
            windowManager.addCrossWindowBlurEnabledListener(mainExecutor, blurListener)
        }
        registerReceiver(profileReceiver, android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_PROFILE_AVAILABLE)
            addAction(android.content.Intent.ACTION_PROFILE_ACCESSIBLE)
            addAction(android.content.Intent.ACTION_PROFILE_INACCESSIBLE)
            addAction(android.content.Intent.ACTION_PROFILE_ADDED)
            addAction(android.content.Intent.ACTION_PROFILE_REMOVED)
            addAction(android.content.Intent.ACTION_PROFILE_UNAVAILABLE)
            addAction(android.content.Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(android.content.Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        }, RECEIVER_EXPORTED)
        loadApps()
    }

    override fun onStop() {
        if (mode == MODE_LAUNCH) {
            windowManager.removeCrossWindowBlurEnabledListener(blurListener)
        }
        unregisterReceiver(profileReceiver)
        loadingJob?.cancel()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getIntExtra(EXTRA_MODE, MODE_SELECTION)
        if (mode == MODE_LAUNCH) {
            // Avoid the full Settings collapsing toolbar inside the floating picker.
            window.setContentView(R.layout.activity_app_selection)
            updateFloatingBackground(windowManager.isCrossWindowBlurEnabled)
            setFinishOnTouchOutside(true)
            val root = findViewById<android.widget.LinearLayout>(R.id.app_selection_root)
            root.clipToOutline = true
            root.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height,
                        28 * resources.displayMetrics.density)
                }
            }
            val header = android.widget.LinearLayout(this).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                val padding = (16 * resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
            }
            header.addView(com.google.android.material.textview.MaterialTextView(
                android.view.ContextThemeWrapper(this, R.style.Theme_SystemUIEX_Material3)).apply {
                setText(R.string.profile_launch_title)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            }, android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val closeSize = (48 * resources.displayMetrics.density).toInt()
            header.addView(android.widget.ImageButton(this).apply {
                setImageResource(com.google.android.material.R.drawable.ic_clear_black_24)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                contentDescription = getString(R.string.profile_picker_close)
                val tint = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.textColorPrimary, tint, true)
                imageTintList = if (tint.resourceId != 0) getColorStateList(tint.resourceId)
                    else android.content.res.ColorStateList.valueOf(tint.data)
                val color = imageTintList!!.defaultColor
                val circle = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.TRANSPARENT)
                }
                val mask = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.WHITE)
                }
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(
                        (color and 0x00ffffff) or 0x30000000), circle, mask)
                val padding = (14 * resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
                setOnClickListener { finish() }
            }, android.widget.LinearLayout.LayoutParams(closeSize, closeSize))
            root.addView(header, 0)
        } else {
            setContentView(R.layout.activity_app_selection)
        }

        title = if (mode == MODE_LAUNCH) {
            getString(R.string.profile_launch_title)
        } else if (mode == MODE_BLACKLIST) {
            getString(R.string.popup_notification_blacklist_title)
        } else {
            getString(R.string.manage_apps)
        }

        appListView = findViewById(R.id.app_list)
        loading = findViewById(R.id.loading)

        if (mode == MODE_BLACKLIST) {
            blacklistedApps.addAll(parseBlacklist(PreferenceHelper.getPopupNotificationBlacklist(this)))
        } else {
            selectedApps.addAll(PreferenceHelper.getSelectedApps(this))
        }

        adapter = AppSelectionAdapter()
        appListView.layoutManager = LinearLayoutManager(this)
        appListView.adapter = adapter
        appListView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        run {
            // Confine scrolling and edge effects to the list viewport, independently of the
            // window outline. Stretch overscroll can otherwise draw into the fixed header.
            appListView.clipToPadding = true
            appListView.clipChildren = true
            appListView.overScrollMode = View.OVER_SCROLL_NEVER
            appListView.itemAnimator = null
            val railWidth = (28 * resources.displayMetrics.density).toInt()
            (appListView.layoutParams as android.widget.FrameLayout.LayoutParams).apply {
                marginEnd = railWidth
                appListView.layoutParams = this
            }
            alphabetRail = object : android.widget.LinearLayout(this) {
                override fun onInterceptTouchEvent(event: MotionEvent): Boolean = true
            }.apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
            }
            (appListView.parent as android.widget.FrameLayout).addView(alphabetRail,
                android.widget.FrameLayout.LayoutParams(railWidth, ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.Gravity.END))
            val viewport = appListView.parent as ViewGroup
            viewport.clipChildren = true
            viewport.clipToPadding = true
            viewport.clipToOutline = true
            viewport.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height,
                        16 * resources.displayMetrics.density)
                }
            }
            val margin = (16 * resources.displayMetrics.density).toInt()
            (viewport.layoutParams as ViewGroup.MarginLayoutParams).apply {
                setMargins(margin, 0, margin, margin)
                viewport.layoutParams = this
            }
        }

        touchHelper = ItemTouchHelper(SelectedAppsTouchHelperCallback())
        touchHelper.attachToRecyclerView(appListView)
        adapter.setItemTouchHelper(touchHelper)
        setupProfileTabs()
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.app_search)
            .doAfterTextChanged { text ->
                searchQuery = text?.toString()?.trim().orEmpty()
                adapter.buildItems()
                appListView.scrollToPosition(0)
            }
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

    }

    private fun matchesSearch(app: AppInfo): Boolean = searchQuery.isEmpty() ||
        app.appName.contains(searchQuery, ignoreCase = true) ||
        app.packageName.contains(searchQuery, ignoreCase = true) ||
        app.sortKey.contains(searchQuery, ignoreCase = true)

    private fun parseBlacklist(blacklist: String): List<String> {
        return if (blacklist.isEmpty()) emptyList() else blacklist.split(";")
    }

    private fun saveBlacklist() {
        val blacklist = blacklistedApps.joinToString(";")
        PreferenceHelper.setPopupNotificationBlacklist(this, blacklist)
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
        loadingJob?.cancel()
        loadingJob = lifecycleScope.launch {
            val loaded = AppListProvider.getLaunchableApps(this@AppSelectionActivity)
            appList.clear()
            appList.addAll(loaded)
            rebuildItems()
            loading.visibility = View.GONE
            appListView.visibility = View.VISIBLE
        }
    }

    private fun rebuildItems() {
        if (selectedApps.removeAll { !AppListProvider.keepSelection(this, it) }) {
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
            val privateSpaceUnavailable = updatePrivateSpaceAction()
            val newItems = ArrayList<UiItem>()

            if (privateSpaceUnavailable) {
                // The locked/create card is the complete state; do not show empty app sections.
            } else if (mode == MODE_LAUNCH) {
                val apps = appList.filter(::inSelectedProfile).filter(::matchesSearch)
                apps.forEach { newItems.add(UiItem.all(it)) }
                if (apps.isEmpty()) {
                    newItems.add(UiItem.empty(if (searchQuery.isEmpty()) R.string.profile_no_apps else R.string.app_search_no_results))
                }
            } else if (mode == MODE_BLACKLIST) {
                newItems.add(UiItem.header(R.string.selected_apps))

                val blacklistedInfos = ArrayList<AppInfo>()
                for (pkg in blacklistedApps) {
                    resolveAppInfo(pkg)?.takeIf(::inSelectedProfile)?.takeIf(::matchesSearch)?.let { blacklistedInfos.add(it) }
                }
                if (blacklistedInfos.isEmpty()) {
                    newItems.add(UiItem.empty(if (searchQuery.isEmpty()) R.string.no_apps_selected else R.string.app_search_no_results))
                } else {
                    blacklistedInfos.forEach { newItems.add(UiItem.selected(it)) }
                }

                newItems.add(UiItem.header(R.string.all_apps))
                val unselected = appList.filter(::inSelectedProfile).filter(::matchesSearch)
                    .filter { !blacklistedApps.contains(it.packageName) }
                    .distinctBy { it.packageName }
                    .sortedWith(AppListProvider.appComparator)
                unselected.forEach { newItems.add(UiItem.all(it)) }
                if (unselected.isEmpty() && searchQuery.isNotEmpty()) {
                    newItems.add(UiItem.empty(R.string.app_search_no_results))
                }
            } else {
                newItems.add(UiItem.header(R.string.selected_apps))

                val selectedInfos = ArrayList<AppInfo>()
                for (pkg in selectedApps) {
                    resolveAppInfo(pkg)?.takeIf(::inSelectedProfile)?.takeIf(::matchesSearch)?.let { selectedInfos.add(it) }
                }
                if (selectedInfos.isEmpty()) {
                    newItems.add(UiItem.empty(if (searchQuery.isEmpty()) R.string.no_apps_selected else R.string.app_search_no_results))
                } else {
                    selectedInfos.forEach { newItems.add(UiItem.selected(it)) }
                }

                newItems.add(UiItem.header(R.string.all_apps))
                val unselected = appList.filter(::inSelectedProfile).filter(::matchesSearch)
                    .filter { !selectedApps.contains(it.selectionKey) }
                    .sortedWith(AppListProvider.appComparator)
                unselected.forEach { newItems.add(UiItem.all(it)) }
                if (unselected.isEmpty() && searchQuery.isNotEmpty()) {
                    newItems.add(UiItem.empty(R.string.app_search_no_results))
                }
            }

            val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(
                UiItemDiffCallback(items, newItems)
            )
            items.clear()
            items.addAll(newItems)
            diff.dispatchUpdatesTo(this)
            updateAlphabetRail()
        }

        private fun updateAlphabetRail() {
            val rail = alphabetRail ?: return
            rail.removeAllViews()
            val positions = linkedMapOf<String, Int>()
            // Index the alphabetized available section; selected shortcuts retain manual order.
            items.forEachIndexed { index, item ->
                if (!item.isSelectedSection) item.app?.let { positions.putIfAbsent(it.initial, index) }
            }
            rail.visibility = if (positions.isEmpty()) View.GONE else View.VISIBLE
            positions.forEach { (letter, position) ->
                val label = TextView(this@AppSelectionActivity).apply {
                    text = letter
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    contentDescription = getString(R.string.profile_jump_letter, letter)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        (appListView.layoutManager as LinearLayoutManager)
                            .scrollToPositionWithOffset(position, 0)
                    }
                }
                rail.addView(label, android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            }
            var lastIndex = -1
            rail.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        view.parent.requestDisallowInterceptTouchEvent(true)
                        if (rail.childCount > 0 && view.height > 0) {
                            val index = ((event.y / view.height) * rail.childCount).toInt()
                                .coerceIn(0, rail.childCount - 1)
                            if (index != lastIndex) {
                                for (i in 0 until rail.childCount) rail.getChildAt(i).isActivated = i == index
                                rail.getChildAt(index).performClick()
                                lastIndex = index
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        lastIndex = -1
                        for (i in 0 until rail.childCount) rail.getChildAt(i).isActivated = false
                        view.parent.requestDisallowInterceptTouchEvent(false)
                        true
                    }
                    else -> false
                }
            }
        }

        fun isSelectedItem(position: Int): Boolean {
            return items.getOrNull(position)?.isSelectedSection == true
        }

        override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
            if (mode != MODE_SELECTION || searchQuery.isNotEmpty()) return false
            if (!isSelectedItem(fromPosition) || !isSelectedItem(toPosition)) {
                return false
            }
            val fromIndex = selectedIndexForAdapterPos(fromPosition)
            val toIndex = selectedIndexForAdapterPos(toPosition)
            if (fromIndex < 0 || toIndex < 0) return false

            val pkg = selectedApps.removeAt(fromIndex)
            val insertIndex = toIndex
            selectedApps.add(insertIndex, pkg)

            val moved = items.removeAt(fromPosition)
            items.add(toPosition, moved)
            notifyItemMoved(fromPosition, toPosition)
            updateAlphabetRail()
            PreferenceHelper.saveSelectedApps(this@AppSelectionActivity, selectedApps)
            return true
        }

        private fun selectedIndexForAdapterPos(position: Int): Int {
            val key = items.getOrNull(position)?.app?.selectionKey ?: return -1
            return selectedApps.indexOf(key)
        }

        private fun resolveAppInfo(packageName: String): AppInfo? {
            if (mode == MODE_BLACKLIST) {
                return appList.firstOrNull { it.packageName == packageName && inSelectedProfile(it) }
            }
            return AppListProvider.resolveApp(this@AppSelectionActivity, packageName)
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
            private val root = view as android.widget.LinearLayout
            private val icon: ImageView = view.findViewById(R.id.app_icon)
            private val name: TextView = view.findViewById(R.id.app_name)
            private val pkg: TextView = view.findViewById(R.id.app_package)
            private val dragHandle: ImageView = view.findViewById(R.id.app_drag)

            init {
                val highlight = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.colorControlHighlight, highlight, true)
                val rippleColor = if (highlight.resourceId != 0) {
                    getColorStateList(highlight.resourceId)
                } else android.content.res.ColorStateList.valueOf(highlight.data)
                val mask = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 16 * resources.displayMetrics.density
                    setColor(android.graphics.Color.WHITE)
                }
                // Bound the pressed/focused state to this row, including during recycling.
                // Keep the content layer transparent so it does not cover the glass surface.
                root.background = android.graphics.drawable.RippleDrawable(rippleColor, null, mask)
                if (mode == MODE_LAUNCH) {
                    pkg.visibility = View.GONE
                    view.findViewById<View>(R.id.widget_frame).visibility = View.GONE
                }
            }

            fun bind(item: UiItem) {
                val app = item.app ?: return
                icon.setImageDrawable(app.icon)
                icon.contentDescription = app.appName
                name.text = app.appName
                pkg.text = app.packageName
                dragHandle.visibility = if (item.isSelectedSection && mode == MODE_SELECTION && searchQuery.isEmpty())
                    View.VISIBLE else View.GONE
                itemView.isSelected = item.isSelectedSection
                itemView.stateDescription = if (mode == MODE_LAUNCH) null else getString(
                    if (item.isSelectedSection) R.string.profile_remove_shortcut
                    else R.string.profile_add_shortcut)
                itemView.setOnTouchListener { v, event ->
                    v.drawableHotspotChanged(event.x, event.y)
                    false
                }
                itemView.setOnClickListener {
                    if (mode == MODE_LAUNCH) {
                        org.avium.systemuiex.util.AppLauncher.launchApp(
                            this@AppSelectionActivity, app.selectionKey)
                        finish()
                    } else handleToggle(app, !item.isSelectedSection)
                }

                dragHandle.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN && item.isSelectedSection && mode != MODE_BLACKLIST) {
                        itemTouchHelper?.startDrag(this)
                        return@setOnTouchListener true
                    }
                    false
                }
            }
        }

        private fun handleToggle(app: AppInfo, checked: Boolean) {
            if (mode == MODE_BLACKLIST) {
                if (checked) {
                    if (!blacklistedApps.contains(app.packageName)) {
                        blacklistedApps.add(app.packageName)
                    }
                } else {
                    blacklistedApps.remove(app.packageName)
                }
                saveBlacklist()
            } else {
                if (checked) {
                    if (selectedApps.size >= maxSelectionCount) {
                        val msg = getString(R.string.selection_limit_toast, maxSelectionCount)
                        Toast.makeText(this@AppSelectionActivity, msg, Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (!selectedApps.contains(app.selectionKey)) {
                        selectedApps.add(app.selectionKey)
                    }
                } else {
                    selectedApps.remove(app.selectionKey)
                }
                PreferenceHelper.saveSelectedApps(this@AppSelectionActivity, selectedApps)
            }
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
            return if (mode == MODE_SELECTION && searchQuery.isEmpty() &&
                adapter.isSelectedItem(viewHolder.bindingAdapterPosition)) {
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
                UiItem.TYPE_APP -> oldItem.app?.selectionKey == newItem.app?.selectionKey
                UiItem.TYPE_HEADER, UiItem.TYPE_EMPTY -> oldItem.titleRes == newItem.titleRes
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_SELECTION = 0
        const val MODE_BLACKLIST = 1
        const val MODE_LAUNCH = 2
    }
}

/** The system server owns the launch capabilities; this activity only returns the user decision. */
class AppLaunchApprovalActivity : android.app.Activity() {
    private val availabilityReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            respond(-1, 0)
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(availabilityReceiver, android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_PROFILE_UNAVAILABLE)
            addAction(android.content.Intent.ACTION_PROFILE_INACCESSIBLE)
            addAction(android.content.Intent.ACTION_PROFILE_REMOVED)
        }, RECEIVER_EXPORTED)
    }

    override fun onStop() {
        unregisterReceiver(availabilityReceiver)
        if (!isChangingConfigurations && !replied) respond(-1, 0)
        super.onStop()
    }

    private var reply: android.os.RemoteCallback? = null
    private var replied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addSystemFlags(android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS)
        reply = intent.getParcelableExtra("callback", android.os.RemoteCallback::class.java)
        val labels = intent.getStringArrayExtra("labels")
        if (reply == null || labels.isNullOrEmpty()) {
            finish()
            return
        }
        val spaces = intent.getIntArrayExtra("spaces")
        labels.indices.forEach { index ->
            val label = when (spaces?.getOrNull(index)) {
                1 -> R.string.profile_clone
                2 -> R.string.profile_private
                else -> R.string.profile_main
            }
            labels[index] = "${getString(label)} · ${labels[index]}"
        }
        val remembered = intent.getBooleanArrayExtra("remembered")
        if (labels.size > 1) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.app_launch_choose_space,
                    intent.getCharSequenceExtra("target")))
                .setItems(labels) { _, choice ->
                    if (remembered?.getOrNull(choice) == true) respond(choice, 1)
                    else showApproval(choice, labels[choice])
                }
                .setNegativeButton(R.string.app_launch_deny) { _, _ -> respond(-1, 0) }
                .setOnCancelListener { respond(-1, 0) }
                .show()
        } else if (remembered?.getOrNull(0) == true) {
            respond(0, 1)
        } else showApproval(0, labels[0])
    }

    private fun showApproval(selection: Int, destination: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.app_launch_request,
                intent.getCharSequenceExtra("source"), intent.getCharSequenceExtra("target")))
            .setMessage(destination)
            .setPositiveButton(R.string.app_launch_allow_once) { _, _ -> respond(selection, 1) }
            .setNeutralButton(R.string.app_launch_allow_always) { _, _ -> respond(selection, 2) }
            .setNegativeButton(R.string.app_launch_deny) { _, _ -> respond(-1, 0) }
            .setOnCancelListener { respond(-1, 0) }
            .show()
    }

    private fun respond(choice: Int, action: Int) {
        if (replied) return
        replied = true
        reply?.sendResult(Bundle().apply {
            putInt("choice", choice)
            putInt("action", action)
            putParcelable("response", android.os.RemoteCallback({ result ->
                val sender = result?.getParcelable("sender", android.content.IntentSender::class.java)
                if (sender != null && !isFinishing) {
                    if (action == 2 && result?.getBoolean("persisted", false) != true) {
                        android.widget.Toast.makeText(this@AppLaunchApprovalActivity,
                            R.string.app_launch_save_failed, android.widget.Toast.LENGTH_LONG).show()
                    }
                    try {
                        startIntentSenderForResult(sender, -1, null,
                            android.content.Intent.FLAG_ACTIVITY_FORWARD_RESULT,
                            android.content.Intent.FLAG_ACTIVITY_FORWARD_RESULT, 0,
                            android.app.ActivityOptions.makeBasic().apply {
                                setPendingIntentBackgroundActivityStartMode(
                                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                            }.toBundle())
                    } catch (_: android.content.IntentSender.SendIntentException) {
                        android.widget.Toast.makeText(this@AppLaunchApprovalActivity, R.string.cannot_launch_app,
                            android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                finish()
            }, android.os.Handler(mainLooper)))
        })
        if (action == 0) finish()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations && !replied) respond(-1, 0)
        super.onDestroy()
    }
}

/** Floating window used only by the gesture more-apps entry. */
class AppLaunchPickerActivity : AppSelectionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        intent.putExtra(EXTRA_MODE, MODE_LAUNCH)
        super.onCreate(savedInstanceState)
    }
}
