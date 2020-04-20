package com.kanedias.dybr.fair

import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ftinc.scoop.Scoop
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.themes.*
import moe.banana.jsonapi2.ArrayDocument
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment which displays profile lists. Needed mostly for showing search results
 *
 * @author Kanedias
 *
 * Created on 2020-04-20
 */
class ProfileListSearchFragment : UserContentListFragment() {

    @BindView(R.id.profile_list_toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.profile_list_area)
    lateinit var ribbonRefresher: SwipeRefreshLayout

    @BindView(R.id.profile_ribbon)
    lateinit var profileRibbon: RecyclerView

    override fun getRibbonView() = profileRibbon
    override fun getRefresher() = ribbonRefresher
    override fun getRibbonAdapter() = profileAdapter
    override fun retrieveData(pageNum: Int, starter: Long): () -> ArrayDocument<OwnProfile> = {
        Network.searchProfiles(filters = filters, pageNum = pageNum)
    }

    /**
     * Filters for search query
     */
    private lateinit var filters: Map<String, String>

    private lateinit var profileAdapter: LoadMoreAdapter

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        filters = requireArguments().getSerializable("filters") as Map<String, String>

        val view = inflater.inflate(R.layout.fragment_profile_list_fullscreen, container, false)
        activity = context as MainActivity
        profileAdapter = ProfileListAdapter()

        ButterKnife.bind(this, view)
        setupUI()
        setupTheming()
        loadMore()

        return view
    }

    private fun setupUI() {
        toolbar.title = getString(R.string.search)
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { fragmentManager?.popBackStack() }

        ribbonRefresher.setOnRefreshListener { loadMore(reset = true) }
        profileRibbon.adapter = profileAdapter
    }

    private fun setupTheming() {
        // this is a fullscreen fragment, add new style
        styleLevel = Scoop.getInstance().addStyleLevel()
        lifecycle.addObserver(styleLevel)

        styleLevel.bind(TOOLBAR, toolbar)
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarIconsAdapter())

        styleLevel.bind(BACKGROUND, profileRibbon)
        styleLevel.bindStatusBar(activity, STATUS_BAR)

        val backgrounds = mapOf<View, Int>(profileRibbon to BACKGROUND/*, toolbar to TOOLBAR*/)
        Auth.profile?.let { applyTheme(activity, it, styleLevel, backgrounds) }
    }

    /**
     * Main adapter of this fragment's recycler view. Shows profiles and handles
     * refreshing and page loading.
     */
    inner class ProfileListAdapter : UserContentListFragment.LoadMoreAdapter() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.fragment_profile_list_item, parent, false)
            return ProfileViewHolder(view, this@ProfileListSearchFragment)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val profile = items[position - headers.size] as OwnProfile
            (holder as ProfileViewHolder).setup(profile)
        }
    }

    /**
     * View holder for showing profiles as list.
     *
     * @param iv inflated view to be used by this holder
     * @param parentFragment fragment in which this holder is shown
     *
     * @author Kanedias
     */
    class ProfileViewHolder(iv: View, private val parentFragment: UserContentListFragment) : RecyclerView.ViewHolder(iv) {
        @BindView(R.id.profile_avatar)
        lateinit var profileAvatar: ImageView

        @BindView(R.id.profile_name)
        lateinit var profileName: TextView

        @BindView(R.id.profile_registration_date)
        lateinit var registrationDate: TextView

        init {
            ButterKnife.bind(this, iv)
            setupTheming()
        }

        fun setupTheming() {
            parentFragment.styleLevel.bind(TEXT_BLOCK, itemView, CardViewColorAdapter())
            parentFragment.styleLevel.bind(TEXT, profileName)
            parentFragment.styleLevel.bind(TEXT, profileName, TextViewDrawableAdapter())
            parentFragment.styleLevel.bind(TEXT, registrationDate)
        }

        private fun showProfile(profile: OwnProfile) {
            val profShow = ProfileFragment().apply { this.profile = profile }
            profShow.show(parentFragment.parentFragmentManager, "Showing user profile fragment")
        }

        private fun showBlog(profile: OwnProfile) {
            val browseFragment = EntryListFragmentFull().apply { this.profile = profile }
            parentFragment.activity?.showFullscreenFragment(browseFragment)
        }

        fun setup(profile: OwnProfile) {
            profileAvatar.setOnClickListener { showProfile(profile) }
            itemView.setOnClickListener { showBlog(profile) }

            profileName.text = profile.nickname
            when (profile.isCommunity) {
                true -> profileName.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.community, 0, 0, 0)
                false -> profileName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }
            registrationDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(profile.createdAt)


            // set avatar
            val avatar = Network.resolve(profile.settings.avatar) ?: Network.defaultAvatar()
            Glide.with(profileAvatar)
                    .load(avatar.toString())
                    .apply(RequestOptions().centerInside().circleCrop())
                    .into(profileAvatar)
        }
    }


}