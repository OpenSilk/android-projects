package org.opensilk.video.telly

import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by drew on 5/30/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeActivityUITest {

    @Rule @JvmField
    val mActivityRule = ActivityTestRule<HomeActivity>(HomeActivity::class.java)

    @Test
    fun activity_starts() {
        assertThat(mActivityRule.activity).isInstanceOf(HomeActivity::class.java)
    }


}