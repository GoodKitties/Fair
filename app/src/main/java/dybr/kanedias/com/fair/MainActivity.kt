package dybr.kanedias.com.fair

import android.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.*
import butterknife.BindView
import butterknife.OnClick
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import dybr.kanedias.com.fair.database.DbProvider
import dybr.kanedias.com.fair.entities.Account
import dybr.kanedias.com.fair.entities.Auth
import dybr.kanedias.com.fair.ui.Sidebar
import dybr.kanedias.com.fair.entities.DiaryEntry
import dybr.kanedias.com.fair.entities.OwnProfile
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

/**
 * Main activity with drawer and sliding tabs where most of user interaction happens.
 * This activity is what you see when you start Fair app.
 *
 * @author Kanedias
 */
class MainActivity : AppCompatActivity() {

    private val FAV_TAB = 0
    private val MY_DIARY_TAB = 1

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

    /**
     * Main drawer, i.e. two panes - content and sidebar
     */
    @BindView(R.id.main_drawer_layout)
    lateinit var drawer: DrawerLayout

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
    @BindView(R.id.sidebar_content)
    lateinit var sidebarContent: ListView

    /**
     * Floating button
     */
    @BindView(R.id.floating_button)
    lateinit var actionButton: FloatingActionButton

    /**
     * Sidebar that opens from the left (the second part of drawer)
     */
    lateinit var sidebar: Sidebar

    /**
     * ancillary progress dialog for use in various places
     */
    private lateinit var progressDialog: MaterialDialog

    /**
     * Tab adapter for [pager] <-> [tabs] synchronisation
     */
    private val tabAdapter = TabAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        // set app bar
        setSupportActionBar(toolbar)
        // setup click listeners, adapters etc.
        setupUI()
        // load user profile and initialize tabs
        reLogin(Auth.user)
    }

    private fun setupUI() {
        // init drawer and sidebar
        val header = layoutInflater.inflate(R.layout.activity_main_sidebar_header, sidebarContent, false)

        sidebarContent.dividerHeight = 0
        sidebarContent.descendantFocusability = ListView.FOCUS_BEFORE_DESCENDANTS
        sidebarContent.addHeaderView(header)
        sidebarContent.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, emptyList<Int>())
        sidebar = Sidebar(drawer, this)

        progressDialog = MaterialDialog.Builder(this@MainActivity)
                .progress(true, 0)
                .cancelable(false)
                .title(R.string.please_wait)
                .content(R.string.logging_in)
                .build()

        // cross-join drawer and menu item in header
        val drawerToggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.open, R.string.close)
        drawer.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // setup tabs
        tabs.setupWithViewPager(pager)
        pager.addOnPageChangeListener(object: ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                // we can now add posts only in our own diary
                when (position) {
                    MY_DIARY_TAB -> actionButton.show()
                    else -> actionButton.hide()
                }
            }
        })
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = MenuInflater(this)
        menuInflater.inflate(R.menu.main_action_bar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        // close drawer if it's open and stop processing further back actions
        if (drawer.isDrawerOpen(GravityCompat.START) || drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawers()
            return
        }

        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog.dismiss()
    }

    /**
     * Logs in with specified account. This must be the very first action after opening the app.
     *
     * @param acc account to be logged in with
     */
    fun reLogin(acc: Account) {
        if (acc === Auth.guest) {
            // we're logging in as guest, skip auth
            Auth.user = Auth.guest
            refresh()
            return
        }

        launch(UI) {
            progressDialog.setContent(R.string.logging_in)
            progressDialog.show()

            try {
                async(CommonPool) { Network.login(acc) }.await()
                if (Auth.user.lastProfileId == null) {
                    // first time we're loading this account, select profile
                    selectProfile()
                } else {
                    // we already have profile to load from
                    async(CommonPool) { Network.populateIdentity() }.await()
                    // all went well, report if we should
                    Toast.makeText(this@MainActivity, R.string.login_successful, Toast.LENGTH_SHORT).show()
                }
            } catch (ex: Exception) {
                Network.reportErrors(this@MainActivity, ex)
                becomeGuest()
            }

            progressDialog.hide()
            refresh()
        }
    }

    /**
     * Load profiles for logged in user and show selector dialog to user.
     * If there are no any profiles for this user, suggest to create it.
     * @param showIfOne whether to show dialog if only one profile is available
     */
    suspend fun selectProfile(showIfOne: Boolean = false) {
        // retrieve profiles from server
        val profiles = async(CommonPool) { Network.loadProfiles() }.await()

        // predefine what to do if new profile is needed
        val showAddProfile = {
            supportFragmentManager.beginTransaction()
                    .addToBackStack("Showing profile creation fragment")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.main_drawer_layout, AddProfileFragment())
                    .commit()
        }

        if (profiles.isEmpty()) {
            // suggest user to create profile
            drawer.closeDrawers()
            MaterialDialog.Builder(this)
                    .title(R.string.switch_profile)
                    .content(R.string.no_profiles_create_one)
                    .negativeText(android.R.string.no)
                    .positiveText(R.string.create_new)
                    .onPositive({ _, _ -> showAddProfile() })
                    .show()
            return
        }

        // predefine what we do on item selection
        val onSelection = fun(pos: Int) { // define as function as we use early-return
            Auth.updateCurrentProfile(profiles[pos])
            refresh()
        }

        if (profiles.size == 1 && !showIfOne) {
            // we have only one profile, use it
            onSelection(0)
            return
        }

        // we have multiple accounts to select from
        val profAdapter = ProfileListAdapter(profiles.toMutableList())
        val dialog = MaterialDialog.Builder(this)
                .title(R.string.switch_profile)
                .itemsCallback({ _, _, pos, _ -> onSelection(pos) })
                .adapter(profAdapter, LinearLayoutManager(this))
                .positiveText(R.string.create_new)
                .onPositive({_, _ -> showAddProfile() })
                .show()
        profAdapter.toDismiss = dialog
    }


    /**
     * Delete specified profile and report it
     * @param prof profile to remove
     */
    private fun deleteProfile(prof: OwnProfile) {
        launch(UI) {
            try {
                async(CommonPool) { Network.removeProfile(prof) }.await()
                Toast.makeText(this@MainActivity, R.string.profile_deleted, Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Network.reportErrors(this@MainActivity, ex)
            }

            if (Auth.user.currentProfile == prof) {
                // we deleted current profile, need to become guest
                becomeGuest()
            }
        }
    }

    /**
     * What to do if auth failed/current account/profile was deleted
     */
    private fun becomeGuest() {
        // couldn't log in, become guest
        Auth.user = Auth.guest
        refresh()
        drawer.openDrawer(GravityCompat.START)
    }

    /**
     * Refresh sliding tabs, sidebar (usually after auth/settings change)
     */
    fun refresh() {
        sidebar.updateAccountsArea()
        pager.adapter = tabAdapter
    }

    /**
     * Add entry handler. Shows header view for adding diary entry.
     * @see DiaryEntry
     */
    @OnClick(R.id.floating_button)
    fun addEntry() {
        // use `instantiate` here because getItem returns new item with each invocation
        // we know that fragment is already present so it will return cached one
        val currFragment = tabAdapter.instantiateItem(pager, pager.currentItem) as PostListFragment
        if (currFragment.refresher.isRefreshing) {
            // diary is not loaded yet
            Toast.makeText(this, R.string.still_loading, Toast.LENGTH_SHORT).show()
            return
        }

        appBar.setExpanded(false)
        currFragment.addCreateNewPostForm()
    }

    inner class TabAdapter: FragmentStatePagerAdapter(supportFragmentManager) {

        override fun getCount(): Int {
            // not working atm - no API for posting at all
            /*
            if (Auth.user === Auth.guest) {
                // guest can't see anything without logging in yet.
                return 0
            }
            var totalTabs = 2 // first tab is favorite post wall, second is own diary
            totalTabs += Auth.user.profile.favorites.size // add tabs for favorites
            return totalTabs
            */
            return 0
        }



        override fun getItem(position: Int): PostListFragment {
            // not working atm - no API for posting at all
            /*
            val ownFavEndpoint = "${Network.IDENTITY_ENDPOINT}/${Auth.user.profile.uris.last()}"
            val ownDiaryEndpoint = "${Network.ENTRIES_ENDPOINT}/${Auth.user.profile.uris.last()}"
            val selectedFavEndpoint = lazy { Auth.user.profile.favorites[position - 2].uris.last() }
            return when(position) {
                FAV_TAB -> PostListFragment().apply { uri = "$ownFavEndpoint/favorites" }
                MY_DIARY_TAB -> PostListFragment().apply { uri = ownDiaryEndpoint }
                else -> PostListFragment().apply { uri = "${Network.ENTRIES_ENDPOINT}/${selectedFavEndpoint.value}" }
            }
            */
            return PostListFragment()
        }

        // not working atm - no API for posting at all
        /*
        override fun getPageTitle(position: Int): CharSequence? = when (position) {
            FAV_TAB -> getString(R.string.favorite)
            MY_DIARY_TAB -> getString(R.string.my_diary)
            else -> Auth.user.profile.favorites[position - 2].name
        }
        */
    }

    /**
     * Profile selector adapter. We can't use standard one because we need a way to delete profile or add another.
     */
    inner class ProfileListAdapter(private val profiles: MutableList<OwnProfile>) : RecyclerView.Adapter<ProfileListAdapter.ProfileViewHolder>() {

        /**
         * Dismiss this if item is selected
         */
        lateinit var toDismiss: MaterialDialog

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ProfileViewHolder {
            val inflater = LayoutInflater.from(this@MainActivity)
            val v = inflater.inflate(R.layout.activity_main_profile_selection_row, parent, false)
            return ProfileViewHolder(v)
        }

        override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
            holder.setup(position)
        }

        override fun getItemCount(): Int {
            return profiles.size
        }

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
                    MaterialDialog.Builder(this@MainActivity)
                            .title(R.string.confirm_action)
                            .content(R.string.confirm_profile_deletion)
                            .negativeText(android.R.string.no)
                            .positiveText(R.string.confirm)
                            .onPositive {_, _ ->
                                deleteProfile(prof)
                                profiles.remove(prof)
                                this@ProfileListAdapter.notifyItemRemoved(pos)
                            }.show()
                }

                itemView.setOnClickListener {
                    Auth.updateCurrentProfile(prof)
                    toDismiss.dismiss()
                    refresh()
                }
            }

        }

    }
}
