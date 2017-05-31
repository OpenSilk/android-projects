package org.opensilk.video.telly

import android.util.JsonReader
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaRefTest {

    @Test
    fun testToJson() {
        val ref = MediaRef(UPNP_DEVICE, "this is an id")
        val str = ref.toJson()
        JsonReader(StringReader(str)).use { jr ->
            var skipped = 0
            jr.beginObject()
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "kind" -> Assertions.assertThat(jr.nextString()).isEqualTo(UPNP_DEVICE)
                    "id" -> Assertions.assertThat(jr.nextString()).isEqualTo("this is an id")
                    else -> {
                        jr.skipValue()
                        skipped++
                    }
                }
            }
            jr.endObject()
            Assertions.assertThat(skipped).isEqualTo(1) //only version
        }
    }

    @Test
    fun testJsonSerialization() {
        val ref = MediaRef(UPNP_DEVICE, "this is an id")
        val json = ref.toJson()
        val refRR = newMediaRef(json)
        Assertions.assertThat(refRR).isEqualTo(ref)
    }

}