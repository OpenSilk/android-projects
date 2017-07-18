package org.opensilk.video.telly

import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector

/**
 *
 */
@ActivityScope
@Subcomponent
interface MockHomeComponent: HomeComponent {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<HomeFragment>()
}

/**
 *
 */
@Module(subcomponents = arrayOf(MockHomeComponent::class))
abstract class MockHomeModule