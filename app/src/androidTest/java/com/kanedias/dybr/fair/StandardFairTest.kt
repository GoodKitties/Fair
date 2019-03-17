package com.kanedias.dybr.fair

import android.view.WindowManager
import androidx.preference.PreferenceManager
import androidx.test.espresso.IdlingPolicies
import androidx.test.rule.ActivityTestRule
import com.j256.ormlite.table.TableUtils
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.database.entities.Account
import org.apache.commons.text.RandomStringGenerator
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
}