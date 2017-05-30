package org.opensilk.video.telly

import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Created by drew on 5/30/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeActivityTest {

    @Rule
    val mActivityRule = ActivityTestRule<HomeActivity>(HomeActivity::class.java)


}