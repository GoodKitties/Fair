package com.kanedias.dybr.fair

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.*
import org.junit.*

import org.junit.runner.RunWith

import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import com.kanedias.dybr.fair.dto.Auth
import org.awaitility.Awaitility.*
import org.awaitility.Duration.*


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AccountTests: StandardFairTest() {

    private val email = "${strGen.generate(10)}@${strGen.generate(5)}.${strGen.generate(2, 4)}"
    private val password = strGen.generate(12)

    @Test
    fun testRegistrationSucceeds() {
        performRegistration(email, password)

        // click "ok" on dialog
        await().atMost(TEN_SECONDS).until { Auth.user !== Auth.guest }
        onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())

        // open drawer
        onView(withContentDescription(R.string.open)).perform(click())

        // check there's account added
        onView(withId(R.id.accounts_area)).check(matches(hasDescendant(withText(email))))
        onView(withId(R.id.add_profile)).check(matches(allOf(isDisplayed(), isEnabled())))

        // test adding same account fails
        addKnownAccount(email, password)

        onView(withText(R.string.email_already_added)).inRoot(withDecorView(not(activity.window.decorView))).check(matches(isDisplayed()))

        // wait till dialog closes and navigate back from this fragment
        Espresso.pressBack()

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
    fun secondRegistrationFails() {
        performRegistration(KNOWN_ACCOUNT_EMAIL, KNOWN_ACCOUNT_PASSWORD)

        // wait till dialog closes and navigate back from this fragment
        Espresso.pressBack()

        // open drawer
        onView(withContentDescription(R.string.open)).perform(click())

        // check there's no account added
        onView(withId(R.id.accounts_area)).check(matches(not(hasDescendant(withText(KNOWN_ACCOUNT_EMAIL)))))
    }

    @Test
    fun currentAccountDeletionMakesYouOrphan() {
        addKnownAccount()

        // click "ok" on success dialog
        await().atMost(TEN_SECONDS).until { Auth.user !== Auth.guest }
        Espresso.pressBack()

        deleteAccount(activity, KNOWN_ACCOUNT_EMAIL)

        // check that greeting is now for guest user
        onView(withId(R.id.current_user_name)).check(matches(withText((R.string.guest))))
    }

    @Test
    fun addingAccountWithoutProfilesAsksToCreateOne() {
        addKnownAccount()

        // dialog should pop up, asking to create profile
        await().atMost(TEN_SECONDS).until { Auth.user !== Auth.guest }
        onView(withText(R.string.switch_profile)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.no_profiles_create_one)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.create_new)).inRoot(isDialog()).check(matches(isDisplayed()))

        // we don't want to.. we're just checking
        Espresso.pressBack()

        deleteAccount(activity, KNOWN_ACCOUNT_EMAIL)
    }
}
