package com.kanedias.dybr.fair

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.ButterKnife
import com.kanedias.dybr.fair.dto.OwnProfile
import com.kanedias.dybr.fair.dto.ProfileCreateRequest
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
    lateinit var newPostsInFavs: Switch

    @BindView(R.id.new_comments_in_subscribed_switch)
    lateinit var newCommentsInSubs: Switch

    @BindView(R.id.participate_in_feed_switch)
    lateinit var participateInFeed: Switch

    @BindView(R.id.reactions_global_switch)
    lateinit var reactionsGlobal: Switch

    @BindView(R.id.reactions_in_blog_switch)
    lateinit var reactionsInBlog: Switch

    /**
     * Profile that we are editing
     */
    private lateinit var profile: OwnProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        profile = arguments!!.get(PROFILE) as OwnProfile
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_profile_preferences, container, false)
        ButterKnife.bind(this, view)

        populateUI()

        return view
    }

    /**
     * Apply settings from profile to switches
     */
    private fun populateUI() {
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
        //reactionsInBlog.isEnabled = reactionsGlobal.isChecked
        reactionsInBlog.isEnabled = false
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