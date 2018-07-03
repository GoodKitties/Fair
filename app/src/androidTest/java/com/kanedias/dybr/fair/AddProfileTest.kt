package com.kanedias.dybr.fair

import android.support.test.espresso.Espresso.closeSoftKeyboard
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingPolicies
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.WindowManager
import com.j256.ormlite.table.TableUtils
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.entities.Account
import org.apache.commons.text.RandomStringGenerator
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
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
@LargeTest
class AddProfileTest {

    @get:Rule
    var mActivityRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    private lateinit var activity: MainActivity

    // need to remember these to reuse in other tests
    companion object {

        private val strGen = RandomStringGenerator.Builder()
                .usingRandom { maxInt -> Random().nextInt(maxInt)  }
                .withinRange('a'.toInt(), 'z'.toInt()).build()
        private val nickname = strGen.generate(10)

    }


    private val gen10 = { strGen.generate(10) }

    @Before
    fun unlockScreen() {
        activity = mActivityRule.activity

        activity.runOnUiThread {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // API 26 is Google API only for now
            //val km: KeyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            //km.requestDismissKeyguard(activity, null)
            //activity.setTurnScreenOn(true)
        }
    }

    @Before
    fun espressoPreconditions() {
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
    }

    @After
    fun getRidOfAllAccounts() {
        TableUtils.clearTable(DbProvider.helper.connectionSource, Account::class.java)
    }

    /**
     * Close drawers so next test can open them
     */
    @After
    fun closeDrawers() {
        activity.runOnUiThread { activity.drawer.closeDrawers() }
    }

    @Test
    fun test1AddDeleteRandomProfile() {
        addKnownAccount(activity)

        // it has quite a lot of profiles already and profile deletion doesn't work... yet... so let's pretend
        // it was all just as planned!
        // it should show dialog with all profiles listed, create new one
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
        onView(withText(R.string.no_profile)).inRoot(isDialog()).perform(click())

        // check we're the guest now
        onView(withId(R.id.current_user_name)).check(matches(withText((R.string.guest))))
    }
}
