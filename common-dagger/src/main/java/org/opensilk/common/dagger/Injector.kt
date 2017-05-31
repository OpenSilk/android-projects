package org.opensilk.common.dagger

/**
 * Created by drew on 5/30/17.
 */
interface Injector<in T> {
    fun inject(t: T)
    interface Factory<in T> {
        fun create(t: T): Injector<T>
    }
    abstract class Builder<in T>: Factory<T> {
        override fun create(t: T): Injector<T> {
            return build()
        }
        abstract fun build(): Injector<T>
    }
}