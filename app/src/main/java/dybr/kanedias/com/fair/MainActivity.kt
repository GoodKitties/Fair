package dybr.kanedias.com.fair

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
import android.widget.*
import butterknife.BindView
import butterknife.OnClick
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import dybr.kanedias.com.fair.entities.Auth
import dybr.kanedias.com.fair.misc.Android
import dybr.kanedias.com.fair.ui.Sidebar
import dybr.kanedias.com.fair.entities.DiaryEntry
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.IOException

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
    private lateinit var sidebar: Sidebar

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
        loadProfile()
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
     * Loads profile for account. This should be done early when program starts.
     */
    private fun loadProfile() {
        // don't need identity for guest
        if (Auth.user === Auth.guest)
            return

        // retrieve our identity from site
        launch(Android) {
            progressDialog.setContent(R.string.loading_profile)
            progressDialog.show()

            try {
                val success = async(CommonPool) { Network.populateIdentity(Auth.user) }
                if (!success.await()) {
                    Toast.makeText(this@MainActivity, R.string.profile_not_found, Toast.LENGTH_LONG).show()
                }
            } catch (ioex: IOException) {
                val errorText = getString(R.string.error_connecting)
                Toast.makeText(this@MainActivity, "$errorText: ${ioex.localizedMessage}", Toast.LENGTH_SHORT).show()
            }

            progressDialog.hide()

            // main action, next interactions are held
            // inside respective fragments in view pager
            refreshTabs()
        }
    }

    /**
     * Logs in with an account specified in [Auth.user]
     */
    fun reLogin() {
        launch(Android) {
            progressDialog.setContent(R.string.logging_in)
            progressDialog.show()

            try {
                val success = async(CommonPool) { Network.login(Auth.user) }
                if (success.await()) {
                    Toast.makeText(this@MainActivity, R.string.login_successful, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, R.string.invalid_credentials, Toast.LENGTH_SHORT).show()
                }
            } catch (ioex: IOException) {
                val errorText = getString(R.string.error_connecting)
                Toast.makeText(this@MainActivity, "$errorText: ${ioex.localizedMessage}", Toast.LENGTH_SHORT).show()
            }

            progressDialog.hide()
            refreshTabs()
        }
    }

    /**
     * Refresh sliding tabs
     */
    fun refreshTabs() {
        pager.adapter = tabAdapter
    }

    inner class TabAdapter: FragmentStatePagerAdapter(supportFragmentManager) {

        override fun getCount(): Int {
            if (Auth.user === Auth.guest) {
                // guest can't see anything without logging in yet.
                return 0
            }
            var totalTabs = 2 // first tab is favorite post wall, second is own diary
            totalTabs += Auth.user.profile.favorites.size // add tabs for favorites
            return totalTabs
        }



        override fun getItem(position: Int): PostListFragment {
            val ownFavEndpoint = "${Network.IDENTITY_ENDPOINT}/${Auth.user.profile.uris.last()}"
            val ownDiaryEndpoint = "${Network.ENTRIES_ENDPOINT}/${Auth.user.profile.uris.last()}"
            val selectedFavEndpoint = lazy { Auth.user.profile.favorites[position - 2].uris.last() }
            return when(position) {
                FAV_TAB -> PostListFragment().apply { uri = "$ownFavEndpoint/favorites" }
                MY_DIARY_TAB -> PostListFragment().apply { uri = ownDiaryEndpoint }
                else -> PostListFragment().apply { uri = "${Network.ENTRIES_ENDPOINT}/${selectedFavEndpoint.value}" }
            }
        }

        override fun getPageTitle(position: Int): CharSequence? = when (position) {
            FAV_TAB -> getString(R.string.favorite)
            MY_DIARY_TAB -> getString(R.string.my_diary)
            else -> Auth.user.profile.favorites[position - 2].name
        }
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
        currFragment.showAddEntryForm()
    }
}
