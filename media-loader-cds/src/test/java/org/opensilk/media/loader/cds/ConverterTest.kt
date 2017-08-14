package org.opensilk.media.loader.cds

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by drew on 8/13/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class ConverterTest {
    @Test
    fun testDurationParser() {
        var duration = "0:00:25.678"
        assertThat(parseUpnpDuration(duration)).isEqualTo(25678)
        duration = "0:05:53.645"
        assertThat(parseUpnpDuration(duration)).isEqualTo(353645)
        duration = "0:05:53.1/2"
        assertThat(parseUpnpDuration(duration)).isEqualTo(353000)
        duration = "0:05:53"
        assertThat(parseUpnpDuration(duration)).isEqualTo(353000)
        duration = ":05:53.645"
        assertThat(parseUpnpDuration(duration)).isEqualTo(353645)
        duration = "2:05:53.645"
        assertThat(parseUpnpDuration(duration)).isEqualTo(7553645)
    }
}