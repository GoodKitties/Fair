package com.kanedias.dybr.fair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import com.ftinc.scoop.Scoop
import com.ftinc.scoop.StyleLevel
import com.ftinc.scoop.adapters.TextViewColorAdapter
import com.kanedias.dybr.fair.dto.OwnProfile
import com.kanedias.dybr.fair.dto.ProfileCreateRequest
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.ui.showToastAtView
import kotlinx.coroutines.*

/**
 * @author Kanedias
 *
 * Created on 18.08.19
 */
class ProfilePreferencesFragment: Fragment() {

    companion object {
        const val PROFILE = "profile"
    }

    @BindView(R.id.profile_preferences_toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.new_posts_in_favorites_switch)
    lateinit var newPostsInFavs: SwitchCompat

    @BindView(R.id.new_comments_in_subscribed_switch)
    lateinit var newCommentsInSubs: SwitchCompat

    @BindView(R.id.participate_in_feed_switch)
    lateinit var participateInFeed: SwitchCompat

    @BindView(R.id.reactions_global_switch)
    lateinit var reactionsGlobal: SwitchCompat

    @BindView(R.id.reactions_in_blog_switch)
    lateinit var reactionsInBlog: SwitchCompat

    @BindViews(
            R.id.new_posts_in_favorites_switch, R.id.new_comments_in_subscribed_switch,
            R.id.participate_in_feed_switch, R.id.reactions_global_switch,
            R.id.reactions_in_blog_switch
    )
    lateinit var switches: List<@JvmSuppressWildcards SwitchCompat>

    @BindViews(
            R.id.notifications_screen_text, R.id.privacy_screen_text,
            R.id.reactions_screen_text
    )
    lateinit var titles: List<@JvmSuppressWildcards TextView>

    /**
     * Profile that we are editing
     */
    private lateinit var profile: OwnProfile

    private lateinit var styleLevel: StyleLevel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        profile = arguments!!.get(PROFILE) as OwnProfile
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_profile_preferences, container, false)
        ButterKnife.bind(this, view)

        setupUI()
        setupTheming(view)

        return view
    }

    private fun setupTheming(view: View) {
        styleLevel = Scoop.getInstance().addStyleLevel()
        lifecycle.addObserver(styleLevel)

        styleLevel.bind(TEXT_BLOCK, view, BackgroundNoAlphaAdapter())

        styleLevel.bind(TOOLBAR, toolbar)
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarIconsAdapter())

        styleLevel.bindStatusBar(activity, STATUS_BAR)

        switches.forEach {
            styleLevel.bind(TEXT_LINKS, it, SwitchColorThumbAdapter())
            styleLevel.bind(TEXT_LINKS, it, SwitchDrawableColorAdapter())
            styleLevel.bind(TEXT, it, SwitchColorAdapter())
            styleLevel.bind(TEXT, it, TextViewColorAdapter())
        }

        titles.forEach { styleLevel.bind(TEXT_HEADERS, it) }
    }

    /**
     * Apply settings from profile to switches
     */
    private fun setupUI() {
        toolbar.setTitle(R.string.profile_settings)
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { fragmentManager?.popBackStack() }

        newPostsInFavs.isChecked = profile.settings.notifications.entries.enable
        newPostsInFavs.setOnCheckedChangeListener { view, isChecked ->
            profile.settings.notifications.apply { entries = entries.copy(enable = isChecked) }
            applySettings(view)
        }

        newCommentsInSubs.isChecked = profile.settings.notifications.entries.enable
        newCommentsInSubs.setOnCheckedChangeListener { view, isChecked ->
            profile.settings.notifications.apply { comments = comments.copy(enable = isChecked) }
            applySettings(view)
        }

        participateInFeed.isChecked = profile.settings.privacy.dybrfeed
        participateInFeed.setOnCheckedChangeListener { view, isChecked ->
            profile.settings.apply { privacy = privacy.copy(dybrfeed = isChecked) }
            applySettings(view)
        }

        reactionsGlobal.isChecked = !profile.settings.reactions.disable
        reactionsGlobal.setOnCheckedChangeListener { view, isChecked ->
            profile.settings.reactions.disable = !isChecked
            reactionsInBlog.isEnabled = isChecked
            applySettings(view)
        }

        reactionsInBlog.isChecked = !profile.settings.reactions.disableInBlog
        reactionsInBlog.isEnabled = reactionsGlobal.isChecked
        reactionsInBlog.setOnCheckedChangeListener { view, isChecked ->
            profile.settings.reactions.disableInBlog = !isChecked
            applySettings(view)
        }
    }

    private fun applySettings(view: View) {
        val profReq = ProfileCreateRequest().apply {
            id = profile.id
            settings = profile.settings
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { Network.updateProfile(profReq) }
                showToastAtView(view, getString(R.string.applied))
            } catch (ex: Exception) {
                Network.reportErrors(context, ex)
            }
        }
    }
}