package dybr.kanedias.com.fair

import android.view.View
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

import android.support.test.espresso.AmbiguousViewMatcherException

/**
 * Matches a view with specific index to avoid [AmbiguousViewMatcherException]
 *
 * @author Kanedias
 *
 * Created on 02.01.18
 */
fun withIndex(matcher: Matcher<View>, index: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        internal var currentIndex = 0

        override fun describeTo(description: Description) {
            description.appendText("with index: ")
            description.appendValue(index)
            matcher.describeTo(description)
        }

        override fun matchesSafely(view: View): Boolean {
            return matcher.matches(view) && currentIndex++ == index
        }
    }
}