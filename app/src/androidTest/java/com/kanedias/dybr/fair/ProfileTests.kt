package com.kanedias.dybr.fair

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import android.view.WindowManager
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.j256.ormlite.table.TableUtils
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.database.entities.Account
import com.kanedias.dybr.fair.dto.Auth
import org.apache.commons.text.RandomStringGenerator
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.awaitility.Duration.*
import org.hamcrest.Matchers.*
import org.junit.*

import org.junit.runner.RunWith

import org.junit.runners.MethodSorters
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProfileTests: StandardFairTest() {


    private val nickname = strGen.generate(10)

    @Test
    fun addDeleteRandomProfile() {
        addKnownAccount()

        await().atMost(TEN_SECONDS).until { Auth.user !== Auth.guest }
        onView(withText(R.string.switch_profile)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.create_new)).inRoot(isDialog()).perform(click())

        // fragment with profile addition should now be active
        onView(withId(R.id.prof_nickname_input)).perform(typeText(nickname))
        onView(withId(R.id.prof_birthday_input)).perform(typeText("1992-10-12"))
        onView(withId(R.id.prof_description_input)).perform(typeText("${gen10()}\n${gen10()}\n${gen10()}"))
        closeSoftKeyboard()

        // click create
        onView(withId(R.id.prof_create_button)).perform(click())

        // open drawer
        onView(withContentDescription(R.string.open)).perform(click())

        // profile should be created after dialog finishes
        onView(withId(R.id.current_user_name)).check(matches(withText((nickname))))

        // click on profile switcher button
        onView(withId(R.id.switch_profile)).perform(click())

        onView(withText(nickname)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.profile_remove), hasSibling(withText(nickname)))).inRoot(isDialog()).perform(click())

        // this one is above previous
        onView(withText(R.string.confirm_profile_deletion)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.confirm)).inRoot(isDialog()).perform(click())

        // now return back to the main one, should disappear
        Espresso.pressBack()

        // check we're the guest now
        onView(withId(R.id.current_user_name)).check(matches(withText((KNOWN_ACCOUNT_EMAIL))))
    }
}
