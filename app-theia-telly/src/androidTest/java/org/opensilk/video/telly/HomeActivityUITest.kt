package org.opensilk.video.telly

import android.support.test.espresso.Espresso
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.Matchers
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
    fun mockServiceIsShowing() {
        val serveritem = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.title_text),
                ViewMatchers.withText("Mock CDService")))
        serveritem.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }


}