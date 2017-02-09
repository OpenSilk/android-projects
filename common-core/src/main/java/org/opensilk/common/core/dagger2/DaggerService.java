/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
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

package org.opensilk.common.core.dagger2;

import android.annotation.SuppressLint;
import android.content.Context;
import java.lang.reflect.Method;

import mortar.MortarScope;

import static java.lang.String.format;

public class DaggerService {
    public static final String DAGGER_SERVICE = DaggerFuncsKt.DAGGER_SERVICE;

    /**
     * Caller is required to know the type of the component for this context.
     */
    @SuppressWarnings("unchecked") @SuppressLint("WrongConstant")
    public static <T> T getDaggerComponent(Context context) {
        //noinspection ResourceType
        return (T) context.getSystemService(DAGGER_SERVICE);
    }


    /**
     * Caller is required to know the type of the component for this scope.
     *
     * @throws IllegalArgumentException if there is no DaggerService attached to this scope
     * @return The Component associated with this scope
     */
    @SuppressWarnings("unchecked") //
    public static <T> T getDaggerComponent(MortarScope scope) {
        if (scope.hasService(DaggerService.DAGGER_SERVICE)) {
            return (T) scope.getService(DaggerService.DAGGER_SERVICE);
        }
        throw new IllegalArgumentException(format("No dagger service found in scope %s", scope.getName()));
    }

    /**
     * Magic method that creates a component with its dependencies set, by reflection. Relies on
     * Dagger2 naming conventions.
     */
    public static <T> T createComponent(Class<T> componentClass, Object... dependencies) {
        String fqn = componentClass.getName();

        String packageName = componentClass.getPackage().getName();
        // Accounts for inner classes, ie MyApplication$Component
        String simpleName = fqn.substring(packageName.length() + 1);
        String generatedName = (packageName + ".Dagger" + simpleName).replace('$', '_');

        try {
            Class<?> generatedClass = Class.forName(generatedName);
            Object builder = generatedClass.getMethod("builder").invoke(null);

            for (Method method : builder.getClass().getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1) {
                    Class<?> dependencyClass = params[0];
                    for (Object dependency : dependencies) {
                        if (dependencyClass.isAssignableFrom(dependency.getClass())) {
                            method.invoke(builder, dependency);
                            break;
                        }
                    }
                }
            }
            //noinspection unchecked
            return (T) builder.getClass().getMethod("build").invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
