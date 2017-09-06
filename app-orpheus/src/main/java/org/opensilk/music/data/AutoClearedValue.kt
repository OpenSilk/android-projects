package org.opensilk.music.data

import org.opensilk.music.data.AutoClearedValue.Host
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Masquerade for late init nullable values that are tied to some lifecycle.
 * The reference is cleared out when [Host] decides it is no longer necessary.
 * It must not be used after clear is called until a new value is assigned.
 *
 * Created by drew on 9/5/17.
 */
class AutoClearedValue<in R, T> internal constructor(host: Host) : ReadWriteProperty<R, T> {

    interface Host {
        fun registerAutoClearedValue(value: AutoClearedValue<*, *>)
    }

    init {
        host.registerAutoClearedValue(this)
    }

    private var value: T? = null

    /**
     * Clears property reference
     */
    fun clear() {
        this.value = null
    }

    override fun getValue(thisRef: R, property: KProperty<*>): T =
            this.value ?: throw UninitializedPropertyAccessException()

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        this.value = value
    }

}

fun <T> AutoClearedValue.Host.autoClearedValue(): AutoClearedValue<AutoClearedValue.Host, T> =
        AutoClearedValue(this)
