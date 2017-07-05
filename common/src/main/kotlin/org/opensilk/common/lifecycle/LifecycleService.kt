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

import android.content.Context

import mortar.MortarScope
import org.opensilk.common.mortar.HasScope
import rx.Observable
import rx.Single
import rx.exceptions.Exceptions
import rx.functions.Func1
import rx.subjects.BehaviorSubject
import timber.log.Timber

const val LIFECYCLE_SERVICE: String = "OPENSILK_LIFECYCLE_SERVICE"

fun MortarScope.Builder.withLifeCycleService(): MortarScope.Builder {
    return this.withService(LIFECYCLE_SERVICE, LifecycleService())
}

/**
 * @throws NoLifecycleServiceException if there is no LifecycleService attached to this scope
 * *
 * @return The Lifecycle associated with this context
 */
@Throws(NoLifecycleServiceException::class)
fun Context.lifecycleService(): LifecycleService {
    val lifecycleService = this.getSystemService(LIFECYCLE_SERVICE) as? LifecycleService
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
@Throws(NoLifecycleServiceException::class)
fun getLifecycleService(scope: MortarScope): LifecycleService {
    if (scope.hasService(LIFECYCLE_SERVICE)) {
        return scope.getService<Any>(LIFECYCLE_SERVICE) as LifecycleService
    }
    throw NoLifecycleServiceException(scope)
}

fun HasScope.lifecycleService(): LifecycleService {
    return if (this.scope.hasService(LIFECYCLE_SERVICE)) {
        scope.getService(LIFECYCLE_SERVICE)
    } else {
        throw NoLifecycleServiceException(this.scope);
    }
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

fun <T> Observable<T>.bindToLifeCycle(context: Context): Observable<T> {
    return this.compose<T>(context.lifecycleService().bind())
}

fun <T> Observable<T>.terminateOnDestroy(context: Context): Observable<T> {
    return this.compose<T>(context.lifecycleService().bindUntilEvent(Lifecycle.DESTROY))
}

fun <T> Observable<T>.terminateOnDestroy(lifecycleService: LifecycleService): Observable<T> {
    return this.compose<T>(lifecycleService.bindUntilEvent(Lifecycle.DESTROY))
}

fun <T> Observable<T>.connectedToPauseResume(context: Context): Observable<T> {
    return connectToEvents(Lifecycle.RESUME, Lifecycle.PAUSE, context)
}

fun <T> Observable<T>.connectToStartStop(context: Context): Observable<T> {
    return connectToEvents(Lifecycle.START, Lifecycle.STOP, context)
}

fun <T> Observable<T>.connectToEvents(start: Lifecycle, end: Lifecycle, context: Context): Observable<T> {
    return context.lifecycleService().lifeCycle
            //terminate on destroy
            .takeUntil { it === Lifecycle.DESTROY }
            //only let resume through
            .filter { it === start }
            .flatMap { this.compose<T>(context.lifecycleService().bindUntilEvent(end)) }
}

/**
 * Will send CancelationException when destroy called
 */
fun <T> Single<T>.cancelOnDestroy(context: Context): Single<T> {
    return this.compose { source ->
        source.takeUntil(context.lifecycleService().lifeCycle.takeFirst {
            it === Lifecycle.DESTROY
        }.toCompletable())
    }
}

/**
 * Simplified version of trellos RxLifecycle (https://github.com/trello/RxLifecycle)
 * Allows presenters to bind observables to parent lifecycle without knowing who
 * controls the lifecycle

 * Created by drew on 10/12/15.
 */
class LifecycleService {

    internal val lifeCycle = BehaviorSubject.create<Lifecycle>()

    fun onCreate() {
        lifeCycle.onNext(Lifecycle.CREATE)
    }

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

    fun onDestroy() {
        lifeCycle.onNext(Lifecycle.DESTROY)
    }

    fun <T> bindUntilEvent(event: Lifecycle): Observable.Transformer<in T, out T> {
        return Observable.Transformer<T, T> { source ->
            source.takeUntil(lifeCycle.takeFirst {
                lifecycleEvent -> lifecycleEvent === event
            })
        }
    }

    fun <T> bind(): Observable.Transformer<in T, out T> {
        // Make sure we're truly comparing a single stream to itself
        val sharedLifecycle = lifeCycle.asObservable().share()
        // Keep emitting from source until the corresponding event occurs in the lifecycle
        return Observable.Transformer<T, T> { source ->
            source.takeUntil(Observable.combineLatest(
                    sharedLifecycle.take(1).map(CORRESPONDING_EVENTS),
                    sharedLifecycle.skip(1),
                    { bindUntilEvent, lifecycleEvent -> lifecycleEvent === bindUntilEvent }
            ).takeFirst { shouldComplete -> shouldComplete })
        }
    }

    companion object {
        // Figures out which corresponding next lifecycle event in which to unsubscribe
        private val CORRESPONDING_EVENTS = Func1<Lifecycle, Lifecycle> { lastEvent ->
            when (lastEvent) {
                Lifecycle.CREATE -> Lifecycle.DESTROY
                Lifecycle.START -> Lifecycle.STOP
                Lifecycle.RESUME -> Lifecycle.PAUSE
                Lifecycle.PAUSE -> Lifecycle.STOP
                Lifecycle.STOP -> Lifecycle.DESTROY
                Lifecycle.DESTROY -> throw OutsideLifecycleException("Cannot bind to Activity lifecycle when outside of it.")
                else -> throw UnsupportedOperationException("Binding to $lastEvent not yet implemented")
            }
        }
    }

}
