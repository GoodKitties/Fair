package com.kanedias.dybr.fair

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.*
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.*
import android.support.test.espresso.matcher.RootMatchers.*
import android.support.test.espresso.matcher.ViewMatchers.*
import android.view.Gravity
import org.hamcrest.Matchers

const val KNOWN_ACCOUNT_EMAIL = "test.account@mail.ru"
const val KNOWN_ACCOUNT_PASSWORD = "123"

fun addKnownAccount(activity: MainActivity) {
    if (!activity.drawer.isDrawerOpen(Gravity.START)) {
        // open drawer
        onView(withContentDescription(R.string.open)).perform(click())
    }

    // click "add account"
    onView(withText(R.string.add_an_account)).perform(click())

    // fill reg form
    onView(withId(R.id.acc_email_input)).perform(typeText(KNOWN_ACCOUNT_EMAIL))
    onView(withId(R.id.acc_password_input)).perform(typeText(KNOWN_ACCOUNT_PASSWORD))
    Espresso.closeSoftKeyboard()

    // click register button
    onView(withId(R.id.confirm_button)).perform(click())
}

/**
 * @author Kanedias
 *
 * Created on 30.06.18
 */
fun deleteAccount(activity: MainActivity, email: String) {
    if (!activity.drawer.isDrawerOpen(Gravity.START)) {
        // open drawer
        onView(withContentDescription(R.string.open)).perform(click())
    }

    // click on delete account icon
    onView(Matchers.allOf(withId(R.id.account_remove), hasSibling(withText(email)))).perform(click())

    // dialog should open
    onView(withText(R.string.delete_account)).inRoot(isDialog()).check(matches(isDisplayed()))
    onView(withText(android.R.string.yes)).inRoot(isDialog()).perform(click()) // confirm
}