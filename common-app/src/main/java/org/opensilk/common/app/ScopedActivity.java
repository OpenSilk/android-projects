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

package org.opensilk.common.app;

import android.app.Activity;
import android.os.Bundle;

import org.opensilk.common.core.mortar.HasScope;
import org.opensilk.common.core.util.ObjectUtils;

import java.util.UUID;

import mortar.MortarScope;
import mortar.bundler.BundleServiceRunner;
import timber.log.Timber;

/**
 * Even though this in a ui component its in core so simpler apps can pull it
 * without the bloat of common-ui
 *
 * Created by drew on 3/6/15.
 */
public abstract class ScopedActivity extends Activity implements HasScope {

    private String mScopeName;
    protected MortarScope mActivityScope;
    protected boolean mIsResumed;
    protected boolean mConfigurationChangeIncoming;

    protected abstract void onCreateScope(MortarScope.Builder builder);

    /**
     * Allows abstract subclasses to inject additional services
     * without worrying about their concrete offspring forgetting
     * to call super
     */
    protected void onPreCreateScope(MortarScope.Builder buidler) {
    }

    /**
     * Called after scope is created but before super.onCreate()
     * mostly useful for themeing
     */
    protected void onScopeCreated(MortarScope scope) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mActivityScope = MortarScope.findChild(getApplicationContext(), getScopeName());
        if (mActivityScope == null) {
            MortarScope.Builder builder = MortarScope.buildChild(getApplicationContext())
                    .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner());
            onPreCreateScope(builder);
            onCreateScope(builder);
            mActivityScope = builder.build(getScopeName());
            Timber.d("Created new scope %s", mActivityScope.getName());
        } else {
            Timber.d("Reusing old scope %s", mActivityScope.getName());
        }
        onScopeCreated(mActivityScope);
        super.onCreate(savedInstanceState);
        BundleServiceRunner.getBundleServiceRunner(this).onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        BundleServiceRunner.getBundleServiceRunner(this).onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mConfigurationChangeIncoming) {
            // Destroy our scope
            if (mActivityScope != null) {
                Timber.d("Destroying activity scope %s", mScopeName);
                mActivityScope.destroy();
            }
            mActivityScope = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsResumed = false;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mConfigurationChangeIncoming = true;
        return mScopeName;
    }

    @Override
    public Object getSystemService(String name) {
        //Note we dont create the scope here since this is usually called
        //before onCreate and we need to be able to fetch our scope name
        //on configuration changes
        return (mActivityScope != null && mActivityScope.hasService(name))
                ? mActivityScope.getService(name) : super.getSystemService(name);
    }

    @Override
    public MortarScope getScope() {
        return mActivityScope;
    }

    private String getScopeName() {
        if (mScopeName == null) {
            mScopeName = (String) getLastNonConfigurationInstance();
        }
        if (mScopeName == null) {
            mScopeName = ObjectUtils.<ScopedActivity>getClass(this).getSimpleName() + "-" + UUID.randomUUID().toString();
        }
        return mScopeName;
    }

}
