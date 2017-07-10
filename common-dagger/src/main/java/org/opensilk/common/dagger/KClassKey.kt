package org.opensilk.common.dagger

import dagger.MapKey
import kotlin.reflect.KClass

/**
 * Created by drew on 7/10/17.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class KClassKey(val value: KClass<*> = Any::class)