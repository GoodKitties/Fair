package dybr.kanedias.com.fair.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.FragmentTransaction
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v4.widget.DrawerLayout
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import dybr.kanedias.com.fair.AddAccountFragment
import dybr.kanedias.com.fair.MainActivity
import dybr.kanedias.com.fair.R
import dybr.kanedias.com.fair.database.DbProvider
import dybr.kanedias.com.fair.entities.Auth

/**
 * Sidebar views and controls.
 * This represents sidebar that can be shown by dragging from the left of main window.
 *
 * @see MainActivity
 * @author Kanedias
 *
 * Created on 05.11.17
 */
class Sidebar(drawer: DrawerLayout, parent: MainActivity) {

    init {
        ButterKnife.bind(this, parent)
    }

    private val drawer = drawer
    private val activity = parent
    private val fragManager = parent.supportFragmentManager

    /**
     * Sidebar accounts area (bottom of header)
     */
    @BindView(R.id.accounts_area)
    lateinit var accountsArea: LinearLayout

    /**
     * Sidebar header up/down image (to the right of welcome text)
     */
    @BindView(R.id.header_flip)
    lateinit var headerFlip: ImageView

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

        // refresh accounts whenever fragments change
        fragManager.addOnBackStackChangedListener { updateAccountsArea() }
    }

    /**
     * Update accounts area after possible account change
     */
    private fun updateAccountsArea() {
        val allAccs = DbProvider.helper.accDao.queryForAll()

        // find accounts we didn't add yet and those that were deleted
        for (idx in 0 until accountsArea.childCount) {
            val row = accountsArea.getChildAt(idx)
            val accName = row.findViewById<TextView>(R.id.account_name)

            // skip add account button and misc controls
            if (accName == null)
                continue

            val existingName = accName.text.toString()
            val deleted = allAccs.removeAll { acc -> acc.name == existingName } // I hope there won't be HUNDREDS of them
            if (!deleted) {
                // it was deleted in DB, purge it in next event loop iteration
                accountsArea.post { accountsArea.removeView(row) }
            }
        }

        // now add remaining accounts
        val inflater = activity.layoutInflater
        for (acc in allAccs) {
            val row = inflater.inflate(R.layout.activity_main_sidebar_account_row, accountsArea, true)
            val accName = row.findViewById<TextView>(R.id.account_name)
            val accRemove = row.findViewById<ImageView>(R.id.account_remove)

            // Account row consists of account name and delete button to the right

            // account name handler - switch to it
            accName.text = acc.name
            accName.setOnClickListener({
                Auth.user = acc
                activity.reLogin()
            })

            // account deleter handler
            val deleteBuilder = DbProvider.helper.accDao.deleteBuilder()
            deleteBuilder.where().eq("name", acc.name)
            accRemove.setOnClickListener {
                // delete account confirmation dialog
                MaterialDialog.Builder(activity)
                        .title(R.string.delete_account)
                        .content(R.string.are_you_sure)
                        .positiveText(android.R.string.yes)
                        .negativeText(android.R.string.no)
                        .onPositive{ _, _ ->
                            DbProvider.helper.accDao.delete(deleteBuilder.prepare())
                            accountsArea.removeView(row)
                        }.show()
            }
        }
    }

    /**
     * Returns created animator.
     * Animates via slowly negating scaleY of target view
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
     * Returns created animator.
     * Animates via slowly changing target view height
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
    private fun expand(v: LinearLayout) {
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
    private fun collapse(v: LinearLayout) {
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