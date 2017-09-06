package org.opensilk.media

const val STORAGE_PRIMARY_PFX = "STORAGE_PRIMARY"
const val STORAGE_SECONDARY_PFX = "STORAGE_SECONDARY"
const val STORAGE_EMULATED = "_EMULATED"
const val STORAGE_REMOVABLE = "_REMOVABLE"

/**
 * Generates a quasi unique identifier for (emulated) storage lacking a uuid.
 */
fun suitableFakeUuid(primary: Boolean, emulated: Boolean, removable: Boolean): String = when {
    primary -> STORAGE_PRIMARY_PFX + suitableFakeUuid2(emulated, removable)
    else -> STORAGE_SECONDARY_PFX + suitableFakeUuid2(emulated, removable)
}

private fun suitableFakeUuid2(emulated: Boolean, removable: Boolean): String = when {
    emulated && removable -> "$STORAGE_EMULATED$STORAGE_REMOVABLE"
    emulated -> STORAGE_EMULATED
    removable -> STORAGE_REMOVABLE
    else -> "FOO"
}

/**
 * Top level meta for storage model
 */
interface StorageMeta: MediaMeta

/**
 * Top level id for storage model
 */
interface StorageId: MediaId {
    val path: String
    val uuid: String
}

/**
 * Identifies storage containers
 */
interface StorageContainerId: StorageId

/**
 * Top level ref for storage model
 */
interface StorageRef: MediaRef {
    override val id: StorageId
    override val meta: StorageMeta
}
