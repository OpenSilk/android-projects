package org.opensilk.traveltime

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey

/**
 * This feature adds Injection to the call pipeline
 *
 * Created by drew on 12/28/17.
 */
class DaggerFeature(configuration: Configuration) {
    val injection = configuration.injection

    class Configuration {
        var injection: Injection = NoopInjection
    }

    companion object Feature: ApplicationFeature<ApplicationCallPipeline, Configuration, DaggerFeature> {
        override val key: AttributeKey<DaggerFeature> = AttributeKey<DaggerFeature>("dagger")
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): DaggerFeature {
            val configuration = Configuration().apply(configure)
            val feature = DaggerFeature(configuration)
            //add the injection to the attributes
            pipeline.intercept(ApplicationCallPipeline.Infrastructure) {
                context.attributes.put(injectionKey, feature.injection)
            }
            return feature
        }
    }

}

/**
 * Attribute Key of Injection
 */
val injectionKey = AttributeKey<Injection>("injection")

/**
 * Helper to retrieve Injection from the call attributes
 */
val ApplicationCall.injection: Injection
    get() = attributes[injectionKey]

interface Injection
object NoopInjection: Injection
