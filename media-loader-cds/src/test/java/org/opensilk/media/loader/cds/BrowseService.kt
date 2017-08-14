package org.opensilk.media.loader.cds

import org.fourthline.cling.model.types.UnsignedIntegerFourBytes
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService
import org.fourthline.cling.support.contentdirectory.DIDLParser
import org.fourthline.cling.support.model.BrowseFlag
import org.fourthline.cling.support.model.BrowseResult
import org.fourthline.cling.support.model.DIDLContent
import org.fourthline.cling.support.model.SortCriterion
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.container.StorageFolder
import org.opensilk.media.UPNP_ROOT_ID
import org.opensilk.media.testdata.upnpFolders

/**
 * Created by drew on 8/13/17.
 */
class BrowseService: AbstractContentDirectoryService() {


    override fun browse(objectID: String, browseFlag: BrowseFlag, filter: String?,
                        firstResult: Long, maxResults: Long,
                        orderby: Array<out SortCriterion>?): BrowseResult {

        val didlContent = DIDLContent()
        var size = 0
        when (objectID) {
            UPNP_ROOT_ID -> {
                upnpFolders().forEach { f ->
                    val c = Container(f.id.containerId, f.id.parentId, f.meta.title,
                            null, StorageFolder.CLASS, 0)
                    didlContent.addContainer(c)
                }
                size = upnpFolders().size
            }
        }
        return BrowseResult(
                DIDLParser().generate(didlContent),
                size.toLong(),
                size.toLong()
        )
    }

}