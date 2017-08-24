package org.opensilk.media

import android.content.Intent
import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter

val EMPTY_INTENT = Intent()

const val STORAGE_PRIMARY_PFX = "STORAGE_PRIMARY"
const val STORAGE_SECONDARY_PFX = "STORAGE_SECONDARY"
const val STORAGE_EMULATED = "_EMULATED"
const val STORAGE_REMOVABLE = "_REMOVABLE"

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
 * Created by drew on 8/17/17.
 */
interface StorageRef: MediaRef {
    val meta: StorageMeta
}

interface StorageId: MediaId

interface StorageMeta {
    val title: String
}




