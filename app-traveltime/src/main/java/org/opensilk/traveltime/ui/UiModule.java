package org.opensilk.traveltime.ui;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Makes the specified classes injectable
 *
 * Created by drew on 11/12/17.
 */
@Module
public abstract class UiModule {
    @ContributesAndroidInjector
    abstract LoginActivity loginActivity();
}
