package org.opensilk.media.loader.cds

import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder
import org.fourthline.cling.model.DefaultServiceManager
import org.fourthline.cling.model.meta.*
import org.fourthline.cling.model.types.ServiceId
import org.fourthline.cling.model.types.UDADeviceType
import org.fourthline.cling.model.types.UDN
import org.opensilk.upnp.cds.browser.CDSserviceType

/**
 * Created by drew on 8/13/17.
 */
fun createMediaServerDevice(): LocalDevice {
    val cds = AnnotationLocalServiceBinder().read(BrowseService::class.java) as LocalService<BrowseService>
    cds.manager = DefaultServiceManager<BrowseService>(cds, BrowseService::class.java)
    return LocalDevice(
            DeviceIdentity(UDN("foo0")),
            UDADeviceType("MediaServer", 1),
            DeviceDetails("TestServer",
                    ManufacturerDetails("OpenSilk"),
                    ModelDetails("TestModel")),
            cds
    )
}
