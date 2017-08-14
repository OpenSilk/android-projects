package org.opensilk.media.loader.cds

import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.model.meta.LocalDevice
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.UPNP_ROOT_ID
import org.opensilk.media.UpnpFolderId
import org.opensilk.media.testdata.upnpFolders
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Created by drew on 8/13/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class LoaderTest {

    lateinit var mTestDevice: LocalDevice
    lateinit var mUpnpService: CDSUpnpService
    lateinit var mLoader: UpnpBrowseLoaderImpl

    @Before
    fun setup() {
        mTestDevice = createMediaServerDevice()
        mUpnpService = CDSUpnpService(RuntimeEnvironment.application, OkHttpClient())
        mUpnpService.registry.addDevice(mTestDevice)
        mLoader = UpnpBrowseLoaderImpl(mUpnpService)
    }

    @After
    fun teardown() {
        mUpnpService.shutdown()
    }

    @Test
    fun test_it_works() {
        val deviceId = mTestDevice.identity.udn.identifierString
        val list = mLoader.directChildren(UpnpFolderId(deviceId, "0", UPNP_ROOT_ID)).blockingGet()
        assertThat(list).isEqualTo(upnpFolders())
    }
}