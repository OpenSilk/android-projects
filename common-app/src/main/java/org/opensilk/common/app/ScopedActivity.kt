/*
 * Copyright (C) 2015 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.app

import android.app.Activity
import android.os.Bundle

import org.opensilk.common.mortar.HasScope
import org.opensilk.common.util.ObjectUtils

import java.util.UUID

import mortar.MortarScope
import mortar.bundler.BundleServiceRunner
import org.opensilk.common.dagger2.withDaggerComponent
import org.opensilk.common.lifecycle.lifecycleService
import org.opensilk.common.lifecycle.withLifeCycleService
import timber.log.Timber

/**
 *
 * Created by drew on 3/6/15.
 */
abstract class ScopedActivity : Activity(), HasScope {

    override final lateinit var scope: MortarScope
        private set
    protected var isScopeCreated: Boolean = false
    protected var isConfigurationChangeIncoming: Boolean = false
    private var calledSuper: Boolean = false

    protected abstract val activityComponent: Any

    protected open fun onCreateScope(builder: MortarScope.Builder) {
        calledSuper = true
    }

    /**
     * Called after scope is created but before super.onCreate()
     * mostly useful for themeing
     */
    protected open fun onScopeCreated(scope: MortarScope) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        ensureScope()
        onScopeCreated(scope)
        super.onCreate(savedInstanceState)
        lifecycleService().onCreate()
        BundleServiceRunner.getBundleServiceRunner(this).onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        BundleServiceRunner.getBundleServiceRunner(this).onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleService().onDestroy()
        if (!isConfigurationChangeIncoming) {
            // Destroy our scope
            Timber.d("Destroying activity scope %s", scope.name)
            scope.destroy()
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleService().onStart()
    }

    override fun onStop() {
        super.onStop()
        lifecycleService().onStop()
    }

    override fun onResume() {
        super.onResume()
        lifecycleService().onResume()
    }

    override fun onPause() {
        super.onPause()
        lifecycleService().onPause()
    }

    override fun onRetainNonConfigurationInstance(): Any {
        isConfigurationChangeIncoming = true
        return scope.name
    }

    override fun getSystemService(name: String): Any {
        //Note we dont create the scope here since this is usually called
        //before onCreate and we need to be able to fetch our scope name
        //on configuration changes
        return if (isScopeCreated && scope.hasService(name))
            scope.getService<Any>(name)
        else
            super.getSystemService(name)
    }

    private fun ensureScope() {
        isScopeCreated = true
        val scopeName = lastNonConfigurationInstance as? String ?: javaClass.simpleName + "-" + UUID.randomUUID().toString()
        var scope: MortarScope? = MortarScope.findChild(applicationContext, scopeName)
        if (scope != null) {
            Timber.d("Reusing old scope %s", scope.name)
            this.scope = scope
        } else {
            val builder = MortarScope.buildChild(applicationContext)
                    .withService(BundleServiceRunner.SERVICE_NAME, BundleServiceRunner())
                    .withDaggerComponent(activityComponent)
                    .withLifeCycleService()
            calledSuper = false
            onCreateScope(builder)
            if (!calledSuper) {
                throw IllegalStateException("Must call super.onCreateScope()")
            }
            scope = builder.build(scopeName)
            Timber.d("Created new scope %s", scope.name)
            this.scope = scope
        }
    }

}
