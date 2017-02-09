/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
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

package org.opensilk.common.dagger

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager

import javax.inject.Singleton

import dagger.Module
import dagger.Provides

/**
 * Created by drew on 10/31/15.
 */
@Module
class SystemServicesModule {
    @Provides @Singleton
    fun provideNotificationManager(@ForApplication context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides @Singleton
    fun provideAlarmManager(@ForApplication context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @Provides @Singleton
    fun provideAudioManager(@ForApplication context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Provides @Singleton
    fun providePowerManager(@ForApplication context: Context): PowerManager {
        return context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    @Provides @Singleton
    fun provideWifiManager(@ForApplication context: Context): WifiManager {
        return context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    @Provides @Singleton
    fun provideConnectivityManager(@ForApplication context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}
