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

package org.opensilk.common.support.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import org.opensilk.common.mortar.HasScope

import java.util.UUID

import mortar.MortarScope
import mortar.bundler.BundleServiceRunner
import org.opensilk.common.dagger2.withDaggerComponent
import org.opensilk.common.lifecycle.LifecycleService
import org.opensilk.common.lifecycle.lifecycleService
import org.opensilk.common.lifecycle.withLifeCycleService
import timber.log.Timber

/**
 * Created by drew on 6/11/16.
 */
abstract class ScopedAppCompatActivity() : AppCompatActivity(), HasScope {

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

    override fun onRetainCustomNonConfigurationInstance(): Any? {
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
        val scopeName = lastCustomNonConfigurationInstance as? String ?:
                javaClass.simpleName + "-" + UUID.randomUUID().toString()
        var scope: MortarScope? = MortarScope.findChild(applicationContext, scopeName)
        if (scope != null) {
            Timber.d("Reusing old scope %s", scope.name)
            this.scope = scope;
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
