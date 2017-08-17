package org.opensilk.media

/**
 * Created by drew on 8/17/17.
 */
interface StorageRef: MediaRef {
    val meta: StorageMeta
}

interface StorageId: MediaId

interface StorageMeta {
    val title: String
}

data class StorageDeviceId(
        val uuid: String
): StorageId {
    override val json: String
        get() = TODO("not implemented")
}

data class StorageDeviceMeta(
        override val title: String
): StorageMeta

data class StorageDeviceRef(
        override val id: StorageDeviceId,
        override val meta: StorageDeviceMeta
): StorageRef


data class StorageDirectoryId(
        val path: String
): StorageId {
    override val json: String
        get() = TODO("not implemented")
}

data class StorageDirectoryMeta(
        override val title: String
): StorageMeta

data class StorageDirectoryRef(
        override val id: StorageDirectoryId,
        override val meta: StorageDirectoryMeta
): StorageRef


data class StorageVideoId(
        val path: String
): StorageId {
    override val json: String
        get() = TODO("not implemented")
}

data class StorageVideoMeta(
        override val title: String
): StorageMeta

data class StorageVideoRef(
        override val id: StorageVideoId,
        override val meta: StorageVideoMeta
): StorageRef