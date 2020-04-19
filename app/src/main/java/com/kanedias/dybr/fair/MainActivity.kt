package com.kanedias.dybr.fair

import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import butterknife.BindView
import butterknife.OnClick
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.auth0.android.jwt.JWT
import com.ftinc.scoop.Scoop
import com.ftinc.scoop.StyleLevel
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.database.entities.Account
import com.kanedias.dybr.fair.database.entities.SearchGotoInfo
import com.kanedias.dybr.fair.database.entities.SearchGotoInfo.*
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.ui.Sidebar
import com.kanedias.dybr.fair.misc.getTopFragment
import com.kanedias.dybr.fair.scheduling.SyncNotificationsWorker
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Main activity with drawer and sliding tabs where most of user interaction happens.
 * This activity is what you see when you start Fair app.
 *
 * @author Kanedias
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val MY_DIARY_TAB = 0
        private const val FAV_TAB = 1
        private const val WORLD_TAB = 2
        private const val NOTIFICATIONS_TAB = 3
    }

    /**
     * Main drawer, i.e. two panes - content and sidebar
     */
    @BindView(R.id.main_drawer_layout)
    lateinit var drawer: DrawerLayout

    /**
     * AppBar layout with toolbar and tabs
     */
    @BindView(R.id.header)
    lateinit var appBar: AppBarLayout

    /**
     * Actionbar header (where app title is written)
     */
    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.content_view)
    lateinit var pager: ViewPager
    /**
     * Tabs with favorites that are placed under actionbar
     */
    @BindView(R.id.sliding_tabs)
    lateinit var tabs: TabLayout

    /**
     * Top-level sidebar content list view
     */
    @BindView(R.id.main_sidebar_area)
    lateinit var sidebarArea: LinearLayout

    /**
     * Floating button
     */
    @BindView(R.id.floating_button)
    lateinit var actionButton: FloatingActionButton

    /**
     * Sidebar that opens from the left (the second part of drawer)
     */
    private lateinit var sidebar: Sidebar

    /**
     * App-global shared preferences for small config changes not suitable for database
     * (seen-marks, first launch options etc.)
     */
    private lateinit var preferences: SharedPreferences

    private lateinit var donateHelper: DonateHelper

    /**
     * Style level for the activity. This is intentionally made public as various fragments that are not shown
     * in fullscreen should use this level from the activity.
     *
     * Fullscreen fragments and various dialogs should instead create and use their own style level.
     */
    lateinit var styleLevel: StyleLevel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        // init preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        donateHelper = DonateHelper(this)

        // set app bar
        setSupportActionBar(toolbar)
        // setup click listeners, adapters etc.
        setupUI()
        // Setup profile themes
        setupTheming()
        // load user profile and initialize tabs
        reLogin(Auth.user)
    }

    private fun setupUI() {
        // init drawer and sidebar
        sidebar = Sidebar(drawer, this)

        // cross-join drawer and menu item in header
        val drawerToggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.open, R.string.close)
        drawer.addDrawerListener(drawerToggle)
        drawer.addDrawerListener(object: DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                // without this buttons are non-clickable after activity layout changes
                sidebarArea.bringToFront()
            }
        })
        drawerToggle.syncState()

        // setup tabs
        tabs.setupWithViewPager(pager, true)

        // handle first launch
        if (preferences.getBoolean("first-app-launch", true)) {
            drawer.openDrawer(GravityCompat.START)
            preferences.edit().putBoolean("first-app-launch", false).apply()
        }

        // hack: resume fragment that is activated on tapping "back"
        supportFragmentManager.addOnBackStackChangedListener {
            val backStackEntryCount = supportFragmentManager.backStackEntryCount
            if (backStackEntryCount == 0) {
                // we're in the main activity
                Scoop.getInstance().setLastLevel(styleLevel)
                styleLevel.rebind()
                return@addOnBackStackChangedListener
            }

            // rebind style levels on top
            val top = supportFragmentManager.fragments.findLast { it is UserContentListFragment }
            (top as? UserContentListFragment)?.styleLevel?.apply {
                Scoop.getInstance().setLastLevel(this)
                this.rebind()
            }
        }
    }

    private fun setupTheming() {
        styleLevel = Scoop.getInstance().addStyleLevel()
        lifecycle.addObserver(styleLevel)

        styleLevel.bindStatusBar(this, STATUS_BAR)


        styleLevel.bind(TOOLBAR, toolbar)
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarIconsAdapter())

        styleLevel.bind(TOOLBAR, tabs)
        styleLevel.bind(TOOLBAR_TEXT, tabs, TabLayoutTextAdapter())
        styleLevel.bind(TOOLBAR_TEXT, tabs, TabLayoutLineAdapter())

        styleLevel.bind(ACCENT, actionButton, BackgroundTintColorAdapter())
        styleLevel.bind(ACCENT_TEXT, actionButton, FabIconAdapter())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_action_bar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        setupTopSearch(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setupTopSearch(menu: Menu) {
        val searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView

        // apply theme to search view
        styleLevel.bind(TOOLBAR_TEXT, searchView, SearchIconsAdapter())
        styleLevel.bind(TOOLBAR_TEXT, searchView, SearchTextAdapter())

        val initialSuggestions = constructSuggestions("")
        val searchAdapter = SimpleCursorAdapter(this, R.layout.activity_main_search_row, initialSuggestions,
                arrayOf("name", "type", "source"),
                intArrayOf(R.id.search_name, R.id.search_type, R.id.search_source), 0)

        // initialize adapter, text listener and click handler
        searchView.queryHint = getString(R.string.go_to)
        searchView.suggestionsAdapter = searchAdapter
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            private fun handle(query: String?): Boolean {
                if (query.isNullOrEmpty())
                    return true

                searchAdapter.changeCursor(constructSuggestions(query))
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return handle(query)
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return handle(newText)
            }

        })
        searchView.setOnSuggestionListener(object: SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                searchItem.collapseActionView()

                val cursor = searchAdapter.getItem(position) as Cursor
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val type = EntityType.valueOf(cursor.getString(cursor.getColumnIndex("type")))

                // jump to respective blog or profile if they exist
                lifecycleScope.launch {
                    try {
                        when (type) {
                            EntityType.PROFILE -> {
                                val fragment = ProfileListSearchFragment().apply {
                                    arguments = Bundle().apply {
                                        putSerializable("filters", HashMap(mapOf("nickname|contains" to name)))
                                    }
                                }
                                showFullscreenFragment(fragment)
                            }
                            EntityType.BLOG -> {
                                val fragment = ProfileListSearchFragment().apply {
                                    arguments = Bundle().apply {
                                        putSerializable("filters", HashMap(mapOf("blog-slug|contains" to name)))
                                    }
                                }
                                showFullscreenFragment(fragment)
                            }
                            EntityType.TAG -> {
                                val searchFragment = EntryListSearchFragmentFull().apply {
                                    arguments = Bundle().apply {
                                        putSerializable("filters", hashMapOf("tag" to name))
                                    }
                                }
                                showFullscreenFragment(searchFragment)
                            }
                        }
                        persistSearchSuggestion(type, name)
                    } catch (ex: Exception) {
                        Network.reportErrors(this@MainActivity, ex, mapOf(404 to R.string.not_found))
                    }
                }

                return true
            }

            private fun persistSearchSuggestion(type: EntityType, name: String) {
                // persist to saved searches if unique index does not object
                val dao = DbProvider.helper.gotoDao
                val unique = dao.queryBuilder().apply { where().eq("type", type).and().eq("name", name) }.query()
                if (unique.isEmpty()) { // don't know how to make this check better, like "replace-on-conflict"
                    dao.createOrUpdate(SearchGotoInfo(type, name))
                }
            }
        })
    }

    private fun constructSuggestions(prefix: String): Cursor {
        // first, collect suggestions from the favorites if we have them
        val favProfiles = Auth.profile?.favorites?.get(Auth.profile?.document) ?: emptyList()
        val suitableFavs = favProfiles.filter { it.nickname.startsWith(prefix) }

        // second, collect suggestions that we already searched for
        val suitableSaved = DbProvider.helper.gotoDao.queryBuilder()
                .where().like("name", "$prefix%").query()

        // we need a counter as MatrixCursor requires _id field unfortunately
        var counter = 0
        val cursor = MatrixCursor(arrayOf("_id", "name", "type", "source"), suitableFavs.size)
        if (prefix.isNotEmpty()) {
            // add two service rows with just prefix
            cursor.addRow(arrayOf(++counter, prefix, EntityType.BLOG.name, getString(R.string.search_by_substring)))
            cursor.addRow(arrayOf(++counter, prefix, EntityType.PROFILE.name, getString(R.string.search_by_substring)))
            cursor.addRow(arrayOf(++counter, prefix, EntityType.TAG.name, getString(R.string.direct_jump)))
        }

        // add suitable addresses from favorites and database
        suitableFavs.forEach { cursor.addRow(arrayOf(++counter, it.nickname, EntityType.PROFILE.name, getString(R.string.from_profile_favorites))) }
        suitableSaved.forEach { cursor.addRow(arrayOf(++counter, it.name, it.type, getString(R.string.from_saved_searches))) }

        return cursor
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_donate -> donateHelper.donate()
            R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.menu_about -> showFullscreenFragment(AboutFragment())
            else -> return super.onOptionsItemSelected(item)
        }

        // it was handled in `when` block or we wouldn't be at this point
        // confirm it
        return true
    }

    override fun onBackPressed() {
        // close drawer if it's open and stop processing further back actions
        if (drawer.isDrawerOpen(GravityCompat.START) || drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawers()
            return
        }

        super.onBackPressed()
    }

    /**
     * Logs in with specified account. This must be the very first action after opening the app.
     *
     * @param acc account to be logged in with
     */
    fun reLogin(acc: Account) {
        // skip re-login if we're logging in as guest
        if (acc === Auth.guest) {
            Auth.updateCurrentUser(Auth.guest)
            refresh()
            return
        }

        // skip re-login if it's the same profile, our token is still valid and we have profile loaded
        if (acc == Auth.user && Auth.user.accessToken != null && Auth.profile != null) {
            if (JWT(Auth.user.accessToken!!).expiresAt!! > Date()) {
                refresh()
                return
            }
        }

        lifecycleScope.launch {
            val progressDialog = MaterialDialog(this@MainActivity)
                    .cancelable(false)
                    .title(R.string.please_wait)
                    .message(R.string.logging_in)
            progressDialog.showThemed(styleLevel)
            try {
                // login with this account and reset profile/blog links
                withContext(Dispatchers.IO) { Network.login(acc) }

                if (Auth.user.lastProfileId == null) {
                    // first time we're loading this account, select profile
                    startProfileSelector()
                } else {
                    // we already have loaded profile
                    // all went well, report if we should
                    Toast.makeText(this@MainActivity, R.string.login_successful, Toast.LENGTH_SHORT).show()
                }
            } catch (ex: Exception) {
                Network.reportErrors(this@MainActivity, ex)
                becomeGuest()
            }

            progressDialog.dismiss()
            refresh()
        }
    }

    override fun onNewIntent(received: Intent) {
        super.onNewIntent(received)
        handleIntent(received)
    }

    /**
     * Handle the passed intent. This is invoked whenever we need to actually react to the intent that was
     * passed to this activity, this can be just activity start from the app manager, click on a link or
     * on a notification belonging to this app
     * @param cause the passed intent. It will not be modified within this function.
     */
    private fun handleIntent(cause: Intent?) {
        if (cause == null)
            return

        when (cause.action) {
            ACTION_NOTIF_OPEN -> {
                val notification = cause.getSerializableExtra(EXTRA_NOTIFICATION) as? Notification
                if (notification != null) {
                    // we have notification, can handle notification click
                    lifecycleScope.launch {
                        try {
                            // load entry
                            val entry = withContext(Dispatchers.IO) { Network.loadEntry(notification.entryId) }

                            // launch comment list
                            val frag = CommentListFragment().apply { this.entry = entry }
                            showFullscreenFragment(frag)

                            // mark notification read
                            SyncNotificationsWorker.markRead(this@MainActivity, notification)
                        } catch (ex: Exception) {
                            Network.reportErrors(this@MainActivity, ex)
                        }
                    }
                    return
                }

                // we don't have entry or notification, open notifications tab
                val backToNotifications = {
                    lifecycleScope.launchWhenResumed {
                        for (i in 0..supportFragmentManager.backStackEntryCount) {
                            supportFragmentManager.popBackStack()
                        }
                        pager.setCurrentItem(NOTIFICATIONS_TAB, true)

                        // if the fragment is already loaded, try to refresh it
                        val notifFragment = getTopFragment(NotificationListFragment::class)
                        notifFragment?.loadMore(reset = true)
                    }
                }

                if (supportFragmentManager.backStackEntryCount > 0) {
                    // confirm dismiss of all top fragments
                    MaterialDialog(this)
                            .title(R.string.confirm_action)
                            .message(R.string.return_to_notifications)
                            .negativeButton(android.R.string.cancel)
                            .positiveButton(android.R.string.ok, click = { backToNotifications() })
                            .showThemed(styleLevel)
                } else {
                    backToNotifications()
                }
            }

            Intent.ACTION_VIEW -> lifecycleScope.launch {
                // try to detect if it's someone trying to open the dybr.ru link with us
                if (cause.data?.authority?.contains("dybr.ru") == true) {
                    consumeCallingUrl(cause)
                }
            }
        }
    }

    /**
     * Take URI from the activity's intent, try to shape it into something usable
     * and handle the action user requested in it if possible. E.g. clicking on link
     * https://dybr.ru/blog/... should open that blog or entry inside the app so try
     * to guess what user wanted with it as much as possible.
     */
    private suspend fun consumeCallingUrl(cause: Intent) {
        try {
            val address = cause.data?.pathSegments ?: return // it's in the form of /blog/<slug>/[<entry>]
            when(address[0]) {
                "blog" -> {
                    val fragment = when (address.size) {
                        2 -> {  // the case for /blog/<slug>
                            val prof = withContext(Dispatchers.IO) { Network.loadProfileBySlug(address[1]) }
                            EntryListFragmentFull().apply { this.profile = prof }
                        }
                        3 -> { // the case for /blog/<slug>/<entry>
                            val entry = withContext(Dispatchers.IO) { Network.loadEntry(address[2]) }
                            CommentListFragment().apply { this.entry = entry }
                        }
                        else -> return
                    }

                    lifecycleScope.launchWhenResumed {
                        showFullscreenFragment(fragment)
                    }
                }
                "profile" -> {
                    val fragment = when (address.size) {
                        2 -> { // the case for /profile/<id>
                            val profile = withContext(Dispatchers.IO) { Network.loadProfile(address[1]) }
                            ProfileFragment().apply { this.profile = profile }
                        }
                        else -> return
                    }

                    lifecycleScope.launchWhenResumed {
                        fragment.show(supportFragmentManager, "Showing intent-requested profile fragment")
                    }
                }
                else -> return
            }
        } catch (ex: Exception) {
            Network.reportErrors(this@MainActivity, ex)
        }
    }

    /**
     * Load profiles for logged in user and show selector dialog to user.
     * If there's just one profile, select it right away.
     * If there are no any profiles for this user, suggest to create it.
     *
     * This should be invoked after [Auth.user] is populated
     *
     * @param showIfOne whether to show dialog if only one profile is available
     */
    suspend fun startProfileSelector(showIfOne: Boolean = false) {
        // retrieve profiles from server
        val profiles = withContext(Dispatchers.IO) { Network.loadUserProfiles() }

        // predefine what to do if new profile is needed

        if (profiles.isEmpty()) {
            // suggest user to create profile
            drawer.closeDrawers()
            MaterialDialog(this)
                    .title(R.string.switch_profile)
                    .message(R.string.no_profiles_create_one)
                    .positiveButton(R.string.create_new, click = { addProfile() })
                    .showThemed(styleLevel)
            return
        }

        if (profiles.size == 1 && !showIfOne) {
            // we have only one profile, use it
            Auth.updateCurrentProfile(profiles[0])
            refresh()
            return
        }

        // we have multiple accounts to select from
        val profAdapter = ProfileListAdapter(profiles.toMutableList())
        MaterialDialog(this)
                .title(R.string.switch_profile)
                .customListAdapter(profAdapter)
                .positiveButton(R.string.create_new, click = { addProfile() })
                .show { profAdapter.toDismiss = this }

    }

    fun showProfilePreferences() {
        val profile = Auth.profile ?: return

        showFullscreenFragment(ProfilePreferencesFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ProfilePreferencesFragment.PROFILE, profile)
            }
        })
    }

    /**
     * Called when user selects profile from a dialog
     */
    private fun selectProfile(prof: OwnProfile) {
        // need to retrieve selected profile fully, i.e. with favorites and stuff
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { Network.makeProfileActive(prof) }
                val fullProf = withContext(Dispatchers.IO) { Network.loadProfile(prof.id) }
                Auth.updateCurrentProfile(fullProf)
                refresh()
            } catch (ex: Exception) {
                Network.reportErrors(this@MainActivity, ex)
                becomeGuest()
            }
        }
    }

    /**
     * Show "Add profile" fragment
     */
    fun addProfile() {
        showFullscreenFragment(AddProfileFragment())
    }

    /**
     * Show "Add blog" fragment. Only one blog can belong to specific profile
     */
    fun createBlog() {
        showFullscreenFragment(AddBlogFragment())
    }


    /**
     * Delete specified profile from server and report it
     * @param prof profile to remove
     */
    private fun deleteProfile(prof: OwnProfile) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { Network.removeProfile(prof) }
                Toast.makeText(this@MainActivity, R.string.profile_deleted, Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Network.reportErrors(this@MainActivity, ex)
            }

            if (Auth.profile == prof) {
                Auth.user.lastProfileId = null
                DbProvider.helper.accDao.update(Auth.user)

                // we deleted current profile, need to become guest
                becomeOrphan()
            }
        }
    }

    /**
     * Become a user with no profile
     * What to do if current profile was deleted
     */
    private fun becomeOrphan() {
        Auth.updateCurrentProfile(null)
        drawer.openDrawer(GravityCompat.START)
        refresh()
    }

    /**
     * Become a guest user with no profile
     * What to do if auth failed/current account/profile was deleted
     */
    private fun becomeGuest() {
        Auth.updateCurrentUser(Auth.guest)
        refresh()
        drawer.openDrawer(GravityCompat.START)
    }

    /**
     * Refresh sliding tabs, sidebar (usually after auth/settings change)
     */
    fun refresh() {

        // apply theme if present
        Auth.profile?.let { applyTheme(this, it, styleLevel) }

        // load current blog and favorites
        lifecycleScope.launch {
            refreshUI()

            // the whole application may be started because of link click
            handleIntent(intent)
            intent = null // reset intent of the activity
        }
    }

    private fun refreshUI() {
        sidebar.updateSidebar()

        // don't show add entry button if we don't have blog to add it
        // can't move this code to EntryListFragments because context
        // is not attached when their user visible hint is set
        when (Auth.profile?.blogSlug) {
            null -> actionButton.hide()
            else -> actionButton.show()
        }

        when {
            // browsing as guest
            Auth.user === Auth.guest -> pager.adapter = GuestTabAdapter()

            // profile not yet created
            Auth.profile === null -> pager.adapter = GuestTabAdapter()

            // have account and profile
            else -> pager.adapter = TabAdapter(Auth.profile)
        }
    }

    /**
     * Add entry handler. Shows header view for adding diary entry.
     * @see Entry
     */
    @OnClick(R.id.floating_button)
    fun addEntry() {
        // use `instantiate` here because getItem returns new item with each invocation
        // we know that fragment is already present so it will return cached one
        val currFragment = pager.adapter!!.instantiateItem(pager, pager.currentItem) as? EntryListFragment ?: return

        if (currFragment.ribbonRefresher.isRefreshing) {
            // diary is not loaded yet
            Toast.makeText(this, R.string.still_loading, Toast.LENGTH_SHORT).show()
            return
        }

        currFragment.addCreateNewEntryForm()
    }

    inner class GuestTabAdapter: FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount() = 1 // only world

        override fun getItem(position: Int) = EntryListFragment().apply { profile = Auth.worldMarker }

        override fun getPageTitle(position: Int): CharSequence? = getString(R.string.world)
    }

    inner class TabAdapter(private val self: OwnProfile?): FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount() = 4 // own blog, favorites, world and notifications

        override fun getItem(position: Int) = when(position) {
            MY_DIARY_TAB -> EntryListFragment().apply { profile = this@TabAdapter.self }
            FAV_TAB  -> EntryListFragment().apply { profile = Auth.favoritesMarker }
            WORLD_TAB -> EntryListFragment().apply { profile = Auth.worldMarker }
            NOTIFICATIONS_TAB -> NotificationListFragment()
            else -> EntryListFragment().apply { profile = null }
        }

        override fun getPageTitle(position: Int): CharSequence? = when (position) {
            MY_DIARY_TAB -> getString(R.string.my_diary)
            FAV_TAB -> getString(R.string.favorite)
            WORLD_TAB -> getString(R.string.world)
            NOTIFICATIONS_TAB -> getString(R.string.notifications)
            else -> ""
        }
    }

    /**
     * Profile selector adapter. We can't use standard one because we need a way to delete profile or add another.
     */
    inner class ProfileListAdapter(private val profiles: MutableList<OwnProfile>) : RecyclerView.Adapter<ProfileListAdapter.ProfileViewHolder>() {

        /**
         * Dismiss this if item is selected
         */
        lateinit var toDismiss: MaterialDialog

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
            val inflater = LayoutInflater.from(this@MainActivity)
            val v = inflater.inflate(R.layout.activity_main_profile_selection_row, parent, false)
            return ProfileViewHolder(v)
        }

        override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) = holder.setup(position)

        override fun getItemCount() = profiles.size

        inner class ProfileViewHolder(v: View): RecyclerView.ViewHolder(v) {

            @BindView(R.id.profile_name)
            lateinit var profileName: TextView

            @BindView(R.id.profile_remove)
            lateinit var profileRemove: ImageView

            init {
                ButterKnife.bind(this, v)
            }

            fun setup(pos: Int) {
                val prof = profiles[pos]
                profileName.text = prof.nickname

                profileRemove.setOnClickListener {
                    MaterialDialog(this@MainActivity)
                            .title(R.string.confirm_action)
                            .message(R.string.confirm_profile_deletion)
                            .negativeButton(android.R.string.no)
                            .positiveButton(R.string.confirm, click = {
                                removeItem(pos)
                                deleteProfile(prof)
                            }).showThemed(styleLevel)
                }

                itemView.setOnClickListener {
                    selectProfile(prof)
                    toDismiss.dismiss()
                }
            }

            private fun removeItem(pos: Int) {
                profiles.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }
    }
}
