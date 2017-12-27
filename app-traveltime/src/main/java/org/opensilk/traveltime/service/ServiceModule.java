package org.opensilk.traveltime.service;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Makes the specified classes injectable
 *
 * Created by drew on 10/30/17.
 */
@Module
public abstract class ServiceModule {
    @ContributesAndroidInjector
    abstract CalendarSyncJobService syncJobService();
    @ContributesAndroidInjector
    abstract ChannelInitService channelInitService();
    @ContributesAndroidInjector
    abstract FirebaseMessageService firebaseMessageService();
}
