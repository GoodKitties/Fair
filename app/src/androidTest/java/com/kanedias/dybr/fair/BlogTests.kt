package com.kanedias.dybr.fair

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.kanedias.dybr.fair.dto.Auth
import org.awaitility.Awaitility.*
import org.awaitility.Duration.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Kanedias
 *
 * Created on 18.03.19
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BlogTests: StandardFairTest() {

    @Test
    fun createBlog() {
        addKnownAccount()

        await().atMost(TEN_SECONDS).until { Auth.user !== Auth.guest }
        val nickname = addProfile()

        val blogSlug = strGen.generate(15)
        val blogTitle = strGen.generate(10)





        deleteProfile(nickname)
    }

}