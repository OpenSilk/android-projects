package org.opensilk.video.telly

import android.content.Intent
import android.support.test.espresso.DataInteraction
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView
import android.view.View
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media._getMediaTitle
import org.opensilk.media.toMediaItem

/**
 * Created by drew on 6/4/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DetailActivityUITest {

    val mItem = testUpnpVideoMetas()[0].toMediaItem()
    @Rule @JvmField
    val mActivityRule = object : ActivityTestRule<DetailActivity>(DetailActivity::class.java) {
        override fun getActivityIntent(): Intent {
            return super.getActivityIntent().putExtra(EXTRA_MEDIAID, mItem.mediaId)
        }
    }

    @Test
    fun test1() {
        val fragment = mActivityRule.activity.fragmentManager.findFragmentById(R.id.detail_fragment) as DetailFragment
        //Scroll to the file info item
        Espresso.onView(ViewMatchers.withId(R.id.container_list))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(1))
        //make sure it exists
        Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.title), ViewMatchers.withText(mItem._getMediaTitle())))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

}