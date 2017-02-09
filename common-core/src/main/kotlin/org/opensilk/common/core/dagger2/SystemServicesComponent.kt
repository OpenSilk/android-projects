/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.common.core.dagger2

import android.app.AlarmManager
import android.app.NotificationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import dagger.Component
import javax.inject.Singleton

/**
 * Created by drew on 10/31/15.
 */
@Singleton
@Component(modules = arrayOf(AppContextModule::class, SystemServicesModule::class))
abstract class SystemServicesComponent {
    abstract fun notificationManager(): NotificationManager
    abstract fun alarmManager(): AlarmManager
    abstract fun audioManager(): AudioManager
    abstract fun powerManager(): PowerManager
    abstract fun wifiManager(): WifiManager
    abstract fun connectivityManager(): ConnectivityManager
}
