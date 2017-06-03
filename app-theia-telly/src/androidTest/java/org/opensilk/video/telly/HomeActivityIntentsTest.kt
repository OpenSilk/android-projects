package org.opensilk.video.telly

import android.app.Activity
import android.content.ComponentName
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by drew on 5/31/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeActivityIntentsTest {

    @Rule @JvmField
    val mActivityRule = IntentsTestRule<HomeActivity>(HomeActivity::class.java)

    @Test
    fun testClickOnServerItemOpensFolders() {
        val serveritem = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.title_text),
                ViewMatchers.withText("Mock CDService")))
        serveritem.perform(ViewActions.click())
        Intents.intended(Matchers.allOf(
                IntentMatchers.hasComponent(ComponentName(mActivityRule.activity, FolderActivity::class.java)),
                IntentMatchers.hasExtraWithKey(EXTRA_MEDIAITEM) //TODO verify mediaitem
        ))
    }

}