package dybr.kanedias.com.fair.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.FragmentTransaction
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v4.widget.DrawerLayout
import android.view.View
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import dybr.kanedias.com.fair.AddAccountFragment
import dybr.kanedias.com.fair.MainActivity
import dybr.kanedias.com.fair.R
import dybr.kanedias.com.fair.database.DbProvider
import dybr.kanedias.com.fair.entities.Auth
import dybr.kanedias.com.fair.entities.Account

/**
 * Sidebar views and controls.
 * This represents sidebar that can be shown by dragging from the left of main window.
 *
 * @see MainActivity
 * @author Kanedias
 *
 * Created on 05.11.17
 */
class Sidebar(private val drawer: DrawerLayout, private val parent: MainActivity) {

    private val fragManager = parent.supportFragmentManager

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
     * Label that shows current username near welcome text
     */
    @BindView(R.id.current_user_name)
    lateinit var currentUsername: TextView

    init {
        ButterKnife.bind(this, parent)
        updateAccountsArea()
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
                .replace(R.id.main_drawer_layout, AddAccountFragment())
                .commit()
    }

    /**
     * Update accounts area after possible account change
     */
    fun updateAccountsArea() {
        val inflater = parent.layoutInflater

        // set welcome message to current user nickname
        Auth.user.currentProfile?.let { prof ->
            currentUsername.text = prof.nickname
        }

        // update account area views
        // remove previous accounts, they may be invalid
        accountsArea.removeViews(1, accountsArea.childCount - 1)

        // populate account list
        val allAccs = DbProvider.helper.accDao.queryForAll()
        for (acc in allAccs) {
            val view = inflater.inflate(R.layout.activity_main_sidebar_account_row, accountsArea, false)
            val accName = view.findViewById<TextView>(R.id.account_name)
            val accRemove = view.findViewById<ImageView>(R.id.account_remove)

            accName.text = acc.email
            accName.setOnClickListener {
                drawer.closeDrawers()
                parent.reLogin(acc)
                updateAccountsArea()
            }

            accRemove.setOnClickListener {
                // "delete account" confirmation dialog
                MaterialDialog.Builder(view.context)
                        .title(R.string.delete_account)
                        .content(R.string.are_you_sure)
                        .positiveText(android.R.string.yes)
                        .negativeText(android.R.string.no)
                        .onPositive{ _, _ -> deleteAccount(acc) }
                        .show()
            }
            accountsArea.addView(view)
        }

        // inflate guest account
        val guestRow = inflater.inflate(R.layout.activity_main_sidebar_account_row, accountsArea, false)
        guestRow.findViewById<ImageView>(R.id.account_remove).visibility = View.GONE
        val guestName = guestRow.findViewById<TextView>(R.id.account_name)
        guestName.text = parent.getString(R.string.guest)
        guestName.setOnClickListener {
            drawer.closeDrawers()
            parent.reLogin(Auth.guest)
        }
        accountsArea.addView(guestRow)
    }

    /**
     * Delete account from local database, delete cookies and re-login as guest
     * if it's current account that was deleted.
     */
    private fun deleteAccount(acc: Account) {
        // if we deleted current account, set it to guest
        if (Auth.user.email == acc.email) {
            drawer.closeDrawers()
            parent.reLogin(Auth.guest)
        }

        // all accounts are present in the DB, inner id is set either on query
        // or in Register/Login persist step, see AddAccountFragment
        DbProvider.helper.accDao.delete(acc)
        updateAccountsArea()
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