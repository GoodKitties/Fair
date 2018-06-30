package com.kanedias.dybr.fair

import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.text.Spannable
import android.view.WindowManager
import junit.framework.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.noties.markwon.Markwon
import ru.noties.markwon.spans.TableRowSpan

/**
 * @author Kanedias
 *
 * Created on 21.04.18
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MarkwonTableTest {

    @get:Rule
    var mActivityRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    private lateinit var activity: MainActivity

    @Before
    fun unlockScreen() {
        activity = mActivityRule.activity

        activity.runOnUiThread {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Test
    fun testMarkwonTables() {
        val table = javaClass.getResourceAsStream("/sample-table.md").bufferedReader().readText()
        val spanned = Markwon.markdown(activity, table) as Spannable
        val spans = spanned.getSpans(0, spanned.length, TableRowSpan::class.java)
        Assert.assertTrue("Should have table spans!", spans.isNotEmpty())

        val tableWithHeader = """
            With header everything is ok too
            =================================

                              """.trimIndent() + table
        val spannedHeader = Markwon.markdown(activity, tableWithHeader) as Spannable
        val spansHeader = spannedHeader.getSpans(0, spannedHeader.length, TableRowSpan::class.java)
        Assert.assertTrue("Should have table spans!", spansHeader.isNotEmpty())
    }
}