package com.kanedias.dybr.fair

import android.view.Gravity
import android.view.WindowManager
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.RootMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import com.j256.ormlite.table.TableUtils
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.database.entities.Account
import org.apache.commons.text.RandomStringGenerator
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Kanedias
 *
 * Created on 15.03.19
 */
open class StandardFairTest {

    protected val strGen: RandomStringGenerator = RandomStringGenerator.Builder()
            .usingRandom { maxInt -> Random().nextInt(maxInt)  }
            .withinRange('a'.toInt(), 'z'.toInt()).build()

    protected val gen10 = { strGen.generate(10) }

    @get:Rule
    var mActivityRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    protected lateinit var activity: MainActivity

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
    fun useTestDybrServer() {
        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        pref.edit().putString("home-server", "http://slonopotam.net/v2").apply()
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

    @After
    fun getRidOfAllAccounts() {
        TableUtils.clearTable(DbProvider.helper.connectionSource, Account::class.java)
    }

    protected fun addProfile(): String {
        val nickname = strGen.generate(10)

        onView(withText(R.string.create_new)).inRoot(isDialog()).perform(click())

        // fragment with profile addition should now be active
        onView(withId(R.id.prof_nickname_input)).perform(typeText(nickname))
        onView(withId(R.id.prof_birthday_input)).perform(typeText("1992-10-12"))
        onView(withId(R.id.prof_description_input)).perform(typeText("${gen10()}\n${gen10()}\n${gen10()}"))
        Espresso.closeSoftKeyboard()

        // click create
        onView(withId(R.id.prof_create_button)).perform(click())

        return nickname
    }

    protected fun addKnownAccount(email: String = KNOWN_ACCOUNT_EMAIL, password: String = KNOWN_ACCOUNT_PASSWORD) {
        if (!activity.drawer.isDrawerOpen(Gravity.START)) {
            // open drawer
            onView(withContentDescription(R.string.open)).perform(click())
        }

        // click "add account"
        onView(withText(R.string.add_an_account)).perform(click())

        // fill reg form
        onView(withId(R.id.acc_email_input)).perform(typeText(email))
        onView(withId(R.id.acc_password_input)).perform(typeText(password))
        Espresso.closeSoftKeyboard()

        // click confirm button
        onView(withId(R.id.confirm_button)).perform(click())
    }

    /**
     * @author Kanedias
     *
     * Created on 30.06.18
     */
    protected fun deleteAccount(activity: MainActivity, email: String) {
        if (!activity.drawer.isDrawerOpen(Gravity.START)) {
            // open drawer
            onView(withContentDescription(R.string.open)).perform(click())
        }

        // click on delete account icon
        onView(allOf(withId(R.id.account_remove), hasSibling(withText(email)))).perform(click())

        // dialog should open
        onView(withText(R.string.delete_account)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(android.R.string.yes)).inRoot(isDialog()).perform(click()) // confirm
    }

    protected fun deleteProfile(nickname: String) {
        if (!activity.drawer.isDrawerOpen(Gravity.START)) {
            // open drawer
            onView(withContentDescription(R.string.open)).perform(click())
        }

        onView(withId(R.id.switch_profile)).perform(click())
        onView(allOf(withId(R.id.profile_remove), hasSibling(withText(nickname)))).inRoot(isDialog()).perform(click())

        onView(withText(R.string.confirm)).inRoot(isDialog()).perform(click())

        // now return back to the main one, should disappear
        Espresso.pressBack()
    }
}