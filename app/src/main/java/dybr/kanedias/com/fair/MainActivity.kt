package dybr.kanedias.com.fair

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.GravityCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick

/**
 * Main activity with drawer and sliding tabs where most of user interaction happens.
 * This activity is what you see when you start Fair app.
 *
 * @author Kanedias
 */
class MainActivity : AppCompatActivity() {

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

    /**
     * Tabs with favorites that are placed under actionbar
     */
    @BindView(R.id.sliding_tabs)
    lateinit var tabs: TabLayout

    /**
     * Sidebar header
     */
    @BindView(R.id.back)
    lateinit var sidebarHeader: RelativeLayout

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        // set app bar
        setSupportActionBar(toolbar)
        setupUI()
    }

    @OnClick(R.id.back)
    private fun toggleHeader() {
        if (accountsArea.visibility == View.GONE) {
            expand(accountsArea)
            flipAnimator(false, headerFlip).start()
        } else {
            collapse(accountsArea)
            flipAnimator(true, headerFlip).start()
        }
    }

    private fun setupUI() {

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
