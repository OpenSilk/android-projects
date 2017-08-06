package org.opensilk.video

import android.net.Uri
import android.provider.DocumentsContract
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.media.*

class NoContentDirectoryFoundException: Exception()
class NoBrowseResultsException: Exception()
class DeviceNotFoundException: Exception()
class ServiceNotFoundException: Exception()
