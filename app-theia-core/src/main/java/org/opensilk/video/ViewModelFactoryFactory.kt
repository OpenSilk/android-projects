package org.opensilk.video

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by drew on 8/6/17.
 */
@Singleton
class ViewModelFactoryFactory
@Inject constructor(
        val providersMap: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (providersMap.containsKey(modelClass)) {
            return providersMap[modelClass]!!.get() as T
        }
        throw IllegalArgumentException("No factory for $modelClass")
    }
}