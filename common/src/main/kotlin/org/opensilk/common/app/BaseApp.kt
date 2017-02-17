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

package org.opensilk.common.app

import android.annotation.TargetApi
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.StrictMode

import org.opensilk.common.util.VersionUtils

import mortar.MortarScope
import org.opensilk.common.dagger2.withDaggerComponent
import org.opensilk.common.timber.DebugTreeWithThreadName
import org.opensilk.common.timber.ReleaseTreeWithThreadName
import rx.functions.Action1
import timber.log.Timber

/**
 * Created by drew on 4/30/15.
 */
abstract class BaseApp : Application() {

    protected val rootScope: MortarScope

    init {
        val builder = MortarScope.buildRootScope()
                .withDaggerComponent(rootComponent)
        onBuildRootScope(builder)
        rootScope = builder.build("ROOT")
    }

    override fun getSystemService(name: String): Any {
        if (rootScope.hasService(name)) {
            return rootScope.getService<Any>(name)
        }
        return super.getSystemService(name)
    }

    /**
     * @return Root Dagger Component for this process
     */
    protected abstract val rootComponent: Any

    /**
     * Add any additional services here
     * @param builder
     */
    protected open fun onBuildRootScope(builder: MortarScope.Builder) {

    }

    protected fun setupTimber(debug: Boolean, silentExceptionHandler: (Throwable) -> Unit = {}) {
        if (debug) {
            Timber.plant(DebugTreeWithThreadName())
        } else {
            Timber.plant(ReleaseTreeWithThreadName(silentExceptionHandler));
        }
    }

    protected fun enableStrictMode() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyFlashScreen()
        StrictMode.setThreadPolicy(threadPolicyBuilder.build())

        val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectAll().penaltyLog()
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    companion object {
        @TargetApi(19)
        fun isLowEndHardware(context: Context): Boolean {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (VersionUtils.hasKitkat()) {
                return am.isLowRamDevice
            } else if (VersionUtils.hasJellyBean()) {
                val mi = ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                return mi.totalMem < 512 * 1024 * 1024
            } else {
                return Runtime.getRuntime().availableProcessors() == 1
            }
        }
    }
}
