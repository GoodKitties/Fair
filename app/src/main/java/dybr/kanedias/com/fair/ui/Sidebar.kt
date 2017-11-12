package dybr.kanedias.com.fair.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.FragmentTransaction
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import dybr.kanedias.com.fair.AddAccountFragment
import dybr.kanedias.com.fair.R

/**
 * @author Kanedias
 *
 * Created on 05.11.17
 */
class Sidebar(parent: AppCompatActivity) {

    init {
        ButterKnife.bind(this, parent)
    }

    private val context = parent

    /**
     * Sidebar header
     */
    @BindView(R.id.sidebar_header_area)
    lateinit var sidebarHeader: RelativeLayout

    /**
     * Sidebar accounts area (bottom of header)
     */
    @BindView(R.id.accounts_area)
    lateinit var accountsArea: LinearLayout

    @BindView(R.id.add_account_row)
    lateinit var addAccountRow: LinearLayout

    /**
     * Sidebar header up/down image (to the right of welcome text)
     */
    @BindView(R.id.header_flip)
    lateinit var headerFlip: ImageView

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

    @OnClick(R.id.add_account_row)
    fun addAccount() {
        val accFragment = AddAccountFragment()
        context.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing account fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                //.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.main_drawer_layout, accFragment)
                .commit()
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