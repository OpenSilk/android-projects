/*
 * Copyright (c) 2016 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.common.lifecycle

import android.annotation.SuppressLint
import android.content.Context

import mortar.MortarScope
import rx.Observable
import rx.exceptions.Exceptions
import rx.functions.Func1
import rx.functions.Func2
import rx.subjects.BehaviorSubject

import java.lang.String.format

const val LIFECYCLE_SERVICE: String = "OPENSILK_LIFECYCLE_SERVICE";

fun MortarScope.Builder.withLifeCycleService(): MortarScope.Builder {
    return this.withService(LIFECYCLE_SERVICE, LifecycleService())
}

/**
 * @throws NoLifecycleServiceException if there is no LifecycleService attached to this scope
 * *
 * @return The Lifecycle associated with this context
 */
@SuppressLint("WrongConstant")
fun getLifecycleService(context: Context): LifecycleService {
    //noinspection ResourceType
    val lifecycleService = context.getSystemService(LIFECYCLE_SERVICE) as? LifecycleService
    if (lifecycleService != null) {
        return lifecycleService
    }
    throw NoLifecycleServiceException()
}


/**
 * @throws NoLifecycleServiceException if there is no LifecycleService attached to this scope
 * *
 * @return The Lifecycle associated with this scope
 */
fun getLifecycleService(scope: MortarScope): LifecycleService {
    if (scope.hasService(LIFECYCLE_SERVICE)) {
        return scope.getService<Any>(LIFECYCLE_SERVICE) as LifecycleService
    }
    throw NoLifecycleServiceException(scope)
}

class OutsideLifecycleException internal constructor(detailMessage: String) : IllegalStateException(detailMessage) {
    companion object {
        private val serialVersionUID = -3644015688130759619L
    }
}

class NoLifecycleServiceException : IllegalArgumentException {

    internal constructor() : super("No lifecycle service in given context") {
    }

    internal constructor(scope: MortarScope) : super("No lifecycle service found in scope ${scope.name}") {
    }

    companion object {
        private val serialVersionUID = -3789316706938152733L
    }
}

/**
 * Simplified version of trellos RxLifecycle (https://github.com/trello/RxLifecycle)
 * Allows presenters to bind observables to parent lifecycle without knowing who
 * controls the lifecycle

 * Created by drew on 10/12/15.
 */
class LifecycleService {

    private val lifeCycle = BehaviorSubject.create<Lifecycle>()

    fun onStart() {
        lifeCycle.onNext(Lifecycle.START)
    }

    fun onResume() {
        lifeCycle.onNext(Lifecycle.RESUME)
    }

    fun onPause() {
        lifeCycle.onNext(Lifecycle.PAUSE)
    }

    fun onStop() {
        lifeCycle.onNext(Lifecycle.STOP)
    }

    private fun <T> bindUntilEvent(event: Lifecycle): Observable.Transformer<in T, out T> {
        return Observable.Transformer<T, T> { source ->
            source.takeUntil(lifeCycle.takeFirst { lifecycleEvent -> lifecycleEvent === event })
        }
    }

    private fun <T> bind(correspondingEvents: Func1<Lifecycle, Lifecycle>): Observable.Transformer<in T, out T> {

        // Make sure we're truly comparing a single stream to itself
        val sharedLifecycle = lifeCycle.asObservable().share()

        // Keep emitting from source until the corresponding event occurs in the lifecycle
        return Observable.Transformer<T, T> { source ->
            source.takeUntil(
                    Observable.combineLatest(
                            sharedLifecycle.take(1).map(correspondingEvents),
                            sharedLifecycle.skip(1)
                    ) { bindUntilEvent, lifecycleEvent -> lifecycleEvent === bindUntilEvent }
                            .onErrorReturn(RESUME_FUNCTION)
                            .takeFirst(SHOULD_COMPLETE)
            )
        }
    }


    companion object {

        /**
         * Binds the given source to a Lifecycle.
         *
         *
         * When the lifecycle event occurs, the source will cease to emit any notifications.
         *
         *
         * Use with [Observable.compose]:
         * `source.compose(RxLifecycle.bindUntilEvent(lifecycle, FragmentEvent.STOP)).subscribe()`

         * @param context the context
         * *
         * @param event the event which should conclude notifications from the source
         * *
         * @return a reusable [Observable.Transformer] that unsubscribes the source at the specified event
         */
        fun <T> bindUntilLifecycleEvent(
                context: Context, event: Lifecycle): Observable.Transformer<in T, out T> {
            return getLifecycleService(context).bindUntilEvent<T>(event)
        }

        /**
         * @see {@link .bindLifecycle
         */
        fun <T> bindUntilLifecycleEvent(
                scope: MortarScope, event: Lifecycle): Observable.Transformer<in T, out T> {
            return getLifecycleService(scope).bindUntilEvent<T>(event)
        }

        /**
         * Binds the given source to a Lifecycle.
         *
         *
         * Use with [Observable.compose]:
         * `source.compose(RxLifecycle.bindActivity(lifecycle)).subscribe()`
         *
         *
         * This helper automatically determines (based on the lifecycle sequence itself) when the source
         * should stop emitting items. In the case that the lifecycle sequence is in the
         * creation phase (START, RESUME) it will choose the equivalent destructive phase (PAUSE,
         * STOP). If used in the destructive phase, the notifications will cease at the next event;
         * for example, if used in PAUSE, it will unsubscribe in STOP.

         * @param context the lifecycle sequence of an Activity
         * * * @return a reusable [Observable.Transformer] that unsubscribes the source during the Activity lifecycle
         */
        fun <T> bindLifecycle(context: Context): Observable.Transformer<in T, out T> {
            return getLifecycleService(context).bind<T>(LIFECYCLE)
        }

        /**
         * @see {@link .bindLifecycle
         */
        fun <T> bindLifecycle(scope: MortarScope): Observable.Transformer<in T, out T> {
            return getLifecycleService(scope).bind<T>(LIFECYCLE)
        }

        private val RESUME_FUNCTION = Func1<kotlin.Throwable, kotlin.Boolean> { throwable ->
            if (throwable is OutsideLifecycleException) {
                return@Func1 true
            }
            throw Exceptions.propagate(throwable)
        }

        private val SHOULD_COMPLETE = Func1<kotlin.Boolean, kotlin.Boolean> { shouldComplete -> shouldComplete }

        // Figures out which corresponding next lifecycle event in which to unsubscribe
        private val LIFECYCLE = Func1<Lifecycle, Lifecycle> { lastEvent ->
            when (lastEvent) {
                Lifecycle.START -> Lifecycle.STOP
                Lifecycle.RESUME -> Lifecycle.PAUSE
                Lifecycle.PAUSE -> Lifecycle.STOP
                Lifecycle.STOP -> throw OutsideLifecycleException("Cannot bind to Activity lifecycle when outside of it.")
                else -> throw UnsupportedOperationException("Binding to $lastEvent not yet implemented")
            }
        }
    }

}
