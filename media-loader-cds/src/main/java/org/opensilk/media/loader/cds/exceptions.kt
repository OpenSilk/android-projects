package org.opensilk.media.loader.cds

/**
 * Created by drew on 8/10/17.
 */
class NoContentDirectoryFoundException: Exception()
class NoBrowseResultsException: Exception()
class DeviceNotFoundException: Exception("Upnp device not found")
class ServiceNotFoundException: Exception()
class FeatureListException: Exception()