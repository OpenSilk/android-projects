package org.opensilk.video

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Created by drew on 7/19/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class DatabaseTest {

    @Test
    fun testDatabaseOpens() {
         assertThat(database().writableDatabase).isNotNull()
    }

    fun database(): Database {
        return Database(RuntimeEnvironment.application)
    }
}