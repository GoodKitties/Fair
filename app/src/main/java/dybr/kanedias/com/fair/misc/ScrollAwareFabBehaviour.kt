package dybr.kanedias.com.fair.misc

import android.content.Context
import android.view.animation.AnimationUtils
import android.support.v4.view.ViewCompat
import android.os.Build
import android.support.v4.view.ViewPropertyAnimatorListener
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import dybr.kanedias.com.fair.R


/**
 * FloatingActionButton behaviour that hides it when user scrolls down the inner view.
 *
 * @author Kanedias
 *
 * Created on 18.11.17
 */
@Suppress("UNUSED_PARAMETER") // these are instantiated by inflater
class ScrollAwareFabBehavior(context: Context, attrs: AttributeSet) : FloatingActionButton.Behavior() {

    private var mIsAnimatingOut = false

    override fun onStartNestedScroll(layout: CoordinatorLayout, child: FloatingActionButton,
                                     directTargetChild: View, target: View, nestedScrollAxes: Int, type: Int): Boolean {
        // Ensure we react to vertical scrolling
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
                || super.onStartNestedScroll(layout, child, directTargetChild, target, nestedScrollAxes, type)
    }

    override fun onNestedScroll(layout: CoordinatorLayout, child: FloatingActionButton,
                                target: View, dxConsumed: Int, dyConsumed: Int,
                                dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        super.onNestedScroll(layout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        if (dyConsumed > 0 && !mIsAnimatingOut && child.visibility == View.VISIBLE) {
            // User scrolled down and the FAB is currently visible -> hide the FAB
            animateOut(child)
        } else if (dyConsumed < 0 && child.visibility != View.VISIBLE) {
            // User scrolled up and the FAB is currently not visible -> show the FAB
            animateIn(child)
        }
    }

    // Same animation that FloatingActionButton.Behavior uses to hide the FAB when the AppBarLayout exits
    private fun animateOut(button: FloatingActionButton) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ViewCompat.animate(button)
                    .scaleX(0.0f).scaleY(0.0f).alpha(0.0f)
                    .setInterpolator(INTERPOLATOR).withLayer()
                    .setListener(object : ViewPropertyAnimatorListener {
                        override fun onAnimationStart(view: View) {
                            mIsAnimatingOut = true
                        }

                        override fun onAnimationCancel(view: View) {
                            mIsAnimatingOut = false
                        }

                        override fun onAnimationEnd(view: View) {
                            mIsAnimatingOut = false
                            view.visibility = View.INVISIBLE
                        }
                    }).start()
        } else {
            val anim = AnimationUtils.loadAnimation(button.context, R.anim.jump_to_down)
            anim.interpolator = INTERPOLATOR
            anim.duration = 200L
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    mIsAnimatingOut = true
                }

                override fun onAnimationEnd(animation: Animation) {
                    mIsAnimatingOut = false
                    button.visibility = View.INVISIBLE
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            button.startAnimation(anim)
        }
    }

    // Same animation that FloatingActionButton.Behavior uses to show the FAB when the AppBarLayout enters
    private fun animateIn(button: FloatingActionButton) {
        button.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= 14) {
            ViewCompat.animate(button).scaleX(1.0f).scaleY(1.0f).alpha(1.0f)
                    .setInterpolator(INTERPOLATOR).withLayer().setListener(null)
                    .start()
        } else {
            val anim = AnimationUtils.loadAnimation(button.context, R.anim.jump_from_down)
            anim.duration = 200L
            anim.interpolator = INTERPOLATOR
            button.startAnimation(anim)
        }
    }

    companion object {
        private val INTERPOLATOR = FastOutSlowInInterpolator()
    }
}