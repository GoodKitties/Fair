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
class AddAccountTest {

    @get:Rule
    var mActivityRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    private lateinit var activity: MainActivity

    // need to remember these to reuse in other tests
    companion object {
        private val strGen = RandomStringGenerator.Builder()
                .usingRandom { maxInt -> Random().nextInt(maxInt) }
                .withinRange('a'.toInt(), 'z'.toInt()).build()

        private val email = "${strGen.generate(10)}@${strGen.generate(5)}.${strGen.generate(2, 4)}"
        private val password = strGen.generate(12)
    }



    @Before
    fun unlockScreen() {
        activity = mActivityRule.activity

        activity.runOnUiThread {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // API 24
            //activity.setTurnScreenOn(true)
            //activity.setShowWhenLocked(true)

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

    /**
     * Close drawers so next test can open them
     */
    @After
    fun closeDrawers() {
        activity.runOnUiThread { activity.drawer.closeDrawers() }
    }

    @Test
    fun test1RegistrationSucceeds() {
        performRegistration(email, password)

        // click "ok" on dialog
        onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())

        // open drawer
        onView(withContentDescription(R.string.open)).perform(click())

        // check there's account added
        onView(withId(R.id.accounts_area)).check(matches(hasDescendant(withText(email))))
        onView(withId(R.id.add_profile)).check(matches(allOf(isDisplayed(), isEnabled())))

        deleteAccount(activity, email)
    }

    private fun performRegistration(email: String, password: String) {
        onView(withContentDescription(R.string.open)).perform(click())

        // click "add account"
        onView(withText(R.string.add_an_account)).perform(click())

        // request registration, not login
        onView(withId(R.id.register_checkbox)).perform(click())

        // fill reg form
        onView(withId(R.id.acc_email_input)).perform(typeText(email))
        onView(withId(R.id.acc_password_input)).perform(typeText(password))
        onView(withId(R.id.acc_password_confirm_input)).perform(typeText(password))
        closeSoftKeyboard()
        onView(withId(R.id.acc_termsofservice_checkbox)).perform(click())
        onView(withId(R.id.acc_is_over_18_checkbox)).perform(click())

        // click register button
        onView(withId(R.id.confirm_button)).perform(click())
    }

    @Test
    fun test2SecondRegistrationFails() {
        performRegistration(email, password)

        // wait till dialog closes and navigate back from this fragment
        onView(withId(R.id.register_checkbox)).perform(pressBack())

        // open drawer
        onView(withContentDescription(R.string.open)).perform(click())

        // check there's no account added
        onView(withId(R.id.accounts_area)).check(matches(not(hasDescendant(withText(email)))))
    }

    @Test
    fun test3CurrentAccountDeletionMakesYouGuest() {
        val randEmail = "${strGen.generate(8)}@${strGen.generate(5)}.${strGen.generate(2)}"
        performRegistration(randEmail, strGen.generate(12))

        // click "ok" on success dialog
        onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())

        deleteAccount(activity, randEmail)

        // check that greeting is now for guest user
        onView(withId(R.id.current_user_name)).check(matches(withText((R.string.guest))))
    }

    @Test
    fun test4AddingAccountWithoutProfilesAsksToCreateOne() {
        addKnownAccount(activity)

        // dialog should pop up, asking to create profile
        onView(withText(R.string.switch_profile)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.no_profiles_create_one)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.create_new)).inRoot(isDialog()).check(matches(isDisplayed()))

        // we don't want to.. we're just checking
        onView(withText(R.string.no_profile)).perform(click())

        deleteAccount(activity, KNOWN_ACCOUNT_EMAIL)
    }
}
