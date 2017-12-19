package dybr.kanedias.com.fair

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.closeSoftKeyboard
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingPolicies
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.WindowManager
import com.robotium.solo.Solo
import org.apache.commons.text.RandomStringGenerator
import org.awaitility.Awaitility.await

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.runners.MethodSorters
import java.util.concurrent.Callable
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

    private val strGen = RandomStringGenerator.Builder().withinRange('a'.toInt(), 'z'.toInt()).build()

    private val nickname = strGen.generate(10)


    @Before
    fun unlockScreen() {
        val activity = mActivityRule.activity
        val wakeUpDevice = {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // API 26 is Google API only for now
            //val km: KeyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            //km.requestDismissKeyguard(activity, null)
            //activity.setTurnScreenOn(true)
        }
        activity.runOnUiThread(wakeUpDevice)
    }

    @Before
    fun espressoPreconditions() {
        IdlingPolicies.setMasterPolicyTimeout(10, TimeUnit.MINUTES)
    }

    @Test
    fun testFirstRegistration() {
        val ctx = mActivityRule.activity
        val solo = Solo(InstrumentationRegistry.getInstrumentation(), ctx)

        // click open drawer button
        solo.pressSoftKeyboardDoneButton()
        onView(withContentDescription(R.string.open)).perform(click())

        // click "add account"
        onView(withText(R.string.add_an_account)).perform(click())

        // request registration, not login
        onView(withId(R.id.register_checkbox)).perform(click())

        // fill reg form
        val email = "${strGen.generate(10)}@${strGen.generate(5)}.${strGen.generate(2, 4)}"
        val password = strGen.generate(12)

        onView(withId(R.id.acc_username_input)).perform(typeText(nickname))
        onView(withId(R.id.acc_email_input)).perform(typeText(email))
        onView(withId(R.id.acc_password_input)).perform(typeText(password))
        onView(withId(R.id.acc_password_confirm_input)).perform(typeText(password))
        solo.hideSoftKeyboard()
        onView(withId(R.id.acc_termsofservice_checkbox)).perform(click())
        onView(withId(R.id.acc_is_over_18_checkbox)).perform(click())

        // click register button
        onView(withId(R.id.confirm_button)).perform(click())

        // wait till wait dialog hides
        await().atMost(30, TimeUnit.SECONDS).until { !solo.searchText(ctx.getString(R.string.please_wait)) }
        // check toast is here
        onView(withText(R.string.congrats_diary_registered)).check(matches(isDisplayed()))

        // click "ok" on dialog
        onView(withText(android.R.string.ok)).perform(click())
    }

    @Test
    fun testSecondRegistrationFails() {

    }
}
