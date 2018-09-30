package com.kanedias.dybr.fair.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.FragmentTransaction
import android.content.Intent
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v4.widget.DrawerLayout
import android.view.View
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.*
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.dybr.fair.database.entities.Account
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.launch

/**
 * Sidebar views and controls.
 * This represents sidebar that can be shown by dragging from the left of main window.
 *
 * @see MainActivity
 * @author Kanedias
 *
 * Created on 05.11.17
 */
class Sidebar(private val drawer: DrawerLayout, private val activity: MainActivity) {

    private val fragManager = activity.supportFragmentManager

    /**
     * Sidebar header up/down image (to the right of welcome text)
     */
    @BindView(R.id.header_flip)
    lateinit var headerFlip: ImageView

    /**
     * Sidebar accounts area (bottom of header)
     */
    @BindView(R.id.accounts_area)
    lateinit var accountsArea: LinearLayout

    /**
     * "My Profile" row
     */
    @BindView(R.id.profile_area)
    lateinit var profileArea: RelativeLayout

    /**
     * "My Blog" row
     */
    @BindView(R.id.blog_area)
    lateinit var blogArea: RelativeLayout

    /**
     * Label that shows current username near welcome text
     */
    @BindView(R.id.current_user_name)
    lateinit var currentUsername: TextView

    init {
        ButterKnife.bind(this, activity)
        updateSidebar()
    }

    /**
     * Hides/shows add-account button and list of saved accounts
     * Positioned just below the header of the sidebar
     */
    @OnClick(R.id.sidebar_header_area)
    fun toggleHeader() {
        if (accountsArea.visibility == View.GONE) {
            expand(accountsArea)
            flipAnimator(false, headerFlip).start()
        } else {
            collapse(accountsArea)
            flipAnimator(true, headerFlip).start()
        }
    }

    /**
     * Shows add-account fragment instead of main view
     */
    @OnClick(R.id.add_account_row)
    fun addAccount() {
        drawer.closeDrawers()
        fragManager.beginTransaction()
                .addToBackStack("Showing account fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, AddAccountFragment())
                .commit()
    }

    @OnClick(R.id.settings_area)
    fun goToSettings() {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }

    @OnClick(R.id.my_profile)
    fun goToProfile() {
        if (Auth.profile == null)
            return

        val dialog = MaterialDialog.Builder(activity)
                .progress(true, 0)
                .cancelable(false)
                .title(R.string.please_wait)
                .content(R.string.loading_profile)
                .build()

        launch(UI) {
            dialog.show()

            try {
                val prof = async { Network.loadProfile(Auth.profile!!.id) }.await()
                val profShow = ProfileFragment().apply { profile = prof }
                profShow.show(activity.supportFragmentManager, "Showing my profile fragment")
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            dialog.dismiss()
        }
    }

    /**
     * Update sidebar after account change/refresh
     */
    fun updateSidebar() {
        updateAccountsArea()
        updateProfileRow()
        updateBlogRow()
    }

    /**
     * Update accounts area after possible account change
     */
    private fun updateAccountsArea() {
        val inflater = activity.layoutInflater

        // set welcome message to current user nickname
        currentUsername.text = Auth.profile?.nickname ?: Auth.user.email

        // update account area views
        // remove previous accounts, they may be invalid
        accountsArea.removeViews(1, accountsArea.childCount - 1)

        // populate account list
        val allAccs = DbProvider.helper.accDao.queryForAll()
        for (acc in allAccs) {
            val view = inflater.inflate(R.layout.activity_main_sidebar_account_row, accountsArea, false)
            val accName = view.findViewById<TextView>(R.id.account_name)
            val accRemove = view.findViewById<ImageView>(R.id.account_remove)

            // setup account row - set email as account name
            accName.text = acc.email
            accName.setOnClickListener {
                drawer.closeDrawers()
                activity.reLogin(acc)
                updateSidebar()
            }

            // setup account row - handle click on delete button
            accRemove.setOnClickListener {
                // "delete account" confirmation dialog
                MaterialDialog.Builder(view.context)
                        .title(R.string.delete_account)
                        .content(R.string.are_you_sure)
                        .positiveText(android.R.string.yes)
                        .negativeText(android.R.string.no)
                        .onPositive { _, _ -> deleteAccount(acc) }
                        .show()
            }

            // add finished account row to the layout
            accountsArea.addView(view)
        }

        // special setup item - inflate guest account row
        val guestRow = inflater.inflate(R.layout.activity_main_sidebar_account_row, accountsArea, false)
        guestRow.findViewById<ImageView>(R.id.account_remove).visibility = View.GONE
        val guestName = guestRow.findViewById<TextView>(R.id.account_name)
        guestName.text = activity.getString(R.string.guest)
        guestName.setOnClickListener {
            drawer.closeDrawers()
            activity.reLogin(Auth.guest)
        }
        accountsArea.addView(guestRow)
    }

    /**
     * update "My Profile" row
     */
    private fun updateProfileRow() {
        val profName = profileArea.findViewById<TextView>(R.id.my_profile)
        val profSwap = profileArea.findViewById<ImageView>(R.id.switch_profile)
        val profAdd = profileArea.findViewById<ImageView>(R.id.add_profile)

        if (Auth.profile == null) {
            // no profile, set to disabled
            profName.isEnabled = false
            profSwap.visibility = View.GONE
            profAdd.visibility = View.GONE

            // if account is present, enable "add-profile" button
            if (Auth.user !== Auth.guest) {
                profAdd.visibility = View.VISIBLE
                profAdd.setOnClickListener {
                    activity.addProfile()
                    drawer.closeDrawers()
                }
            }
            return
        }

        // We have a profile, then
        profName.isEnabled = true
        profAdd.visibility = View.GONE
        profSwap.visibility = View.VISIBLE

        // handle click on profile change button
        // we need to ignore subsequent clicks if profiles are already loading
        val profileSwapActor = actor<Unit>(UI) {
            for (event in channel) {
                val swapAnim = ValueAnimator.ofFloat(1f, -1f, 1f)
                swapAnim.interpolator = FastOutSlowInInterpolator()
                swapAnim.addUpdateListener { profSwap.scaleY = swapAnim.animatedValue as Float }
                swapAnim.duration = 1_000
                swapAnim.repeatCount = ValueAnimator.INFINITE
                swapAnim.start()

                try {
                    // force profile selection even if we only have one
                    activity.startProfileSelector(true)
                    drawer.closeDrawers()
                } catch (ex: Exception) {
                    Network.reportErrors(activity, ex)
                }

                swapAnim.repeatCount = 0 // stop gracefully
            }
        }

        profSwap.setOnClickListener {
            profileSwapActor.offer(Unit)
        }
    }

    /**
     * Updates blog row according to retrieved info in
     * @see [Auth.blog]
     * @see [Auth.profile]
     */
    private fun updateBlogRow() {
        val blogName = blogArea.findViewById<TextView>(R.id.my_blog)
        val blogAdd = blogArea.findViewById<ImageView>(R.id.add_blog)

        if (Auth.profile == null) {
            // no profile yet, disable everything
            blogName.isEnabled = false
            blogName.setText(R.string.my_blog)
            blogAdd.visibility = View.GONE
            return
        }

        if (Auth.blog == null) {
            // no blog yet, disable click, show "Add blog" button
            blogName.isEnabled = false
            blogName.setText(R.string.my_blog)
            blogAdd.visibility = View.VISIBLE
            blogAdd.setOnClickListener {
                activity.createBlog()
                drawer.closeDrawers()
            }
            return
        }

        // we have a blog, show it
        blogName.isEnabled = true
        blogName.hint = activity.getString(R.string.my_blog)
        blogName.text = Auth.blog?.title
        blogAdd.visibility = View.GONE
        blogName.setOnClickListener {
            for (i in 0..fragManager.backStackEntryCount) {
                fragManager.popBackStack()
            }
            activity.pager.setCurrentItem(0, true)
            drawer.closeDrawers()
        }
    }

    /**
     * Delete account from local database, delete cookies and re-login as guest
     * if it's current account that was deleted.
     */
    private fun deleteAccount(acc: Account) {
        // if we deleted current account, set it to guest
        if (Auth.user.email == acc.email) {
            drawer.closeDrawers()
            activity.reLogin(Auth.guest)
        }

        // all accounts are present in the DB, inner id is set either on query
        // or in Register/Login persist step, see AddAccountFragment
        DbProvider.helper.accDao.delete(acc)
        updateSidebar()
    }

    /**
     * Animates via slowly negating scaleY of target view. Used in arrow-like buttons
     * to turn ⌄ in ⌃ and back.
     * @return created animator
     */
    private fun flipAnimator(isFlipped: Boolean, v: View): ValueAnimator {
        val animator = ValueAnimator.ofFloat(if (isFlipped) -1f else 1f, if (isFlipped) 1f else -1f)
        animator.interpolator = FastOutSlowInInterpolator()

        animator.addUpdateListener { valueAnimator ->
            // update view height when flipping
            v.scaleY = valueAnimator.animatedValue as Float
        }
        return animator
    }

    /**
     * Animates via slowly changing target view height. Used to show/hide account list.
     * @return created animator
     */
    private fun slideAnimator(start: Int, end: Int, v: View): ValueAnimator {
        val animator = ValueAnimator.ofInt(start, end)
        animator.interpolator = FastOutSlowInInterpolator()

        animator.addUpdateListener { valueAnimator ->
            // update height
            val value = valueAnimator.animatedValue as Int
            val layoutParams = v.layoutParams
            layoutParams.height = value
            v.layoutParams = layoutParams
        }
        return animator
    }

    /**
     * Expands target layout by making it visible and increasing its height
     * @see slideAnimator
     */
    private fun expand(v: View) {
        // set layout visible
        v.visibility = View.VISIBLE

        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(widthSpec, heightSpec)

        val animator = slideAnimator(0, v.measuredHeight, v)
        animator.start()
    }

    /**
     * Collapses target layout by decreasing its height and making it gone
     * @see slideAnimator
     */
    private fun collapse(v: View) {
        val finalHeight = v.height
        val animator = slideAnimator(finalHeight, 0, v)

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                v.visibility = View.GONE
            }
        })
        animator.start()
    }
}