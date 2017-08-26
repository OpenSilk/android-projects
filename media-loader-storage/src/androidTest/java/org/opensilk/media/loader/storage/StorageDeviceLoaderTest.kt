package org.opensilk.media.loader.storage

import android.app.Instrumentation
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.support.test.runner.AndroidJUnitRunner
import android.util.Log
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.*

/**
 * Created by drew on 8/20/17.
 */
@RunWith(AndroidJUnit4::class)
class StorageDeviceLoaderTest {

    lateinit var mLoader: StorageDeviceLoaderImpl

    @Before
    fun setup() {
        mLoader = StorageDeviceLoaderImpl(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun checkForRead() {
        val list = mLoader.storageDevices.blockingGet()
        list.forEach {
            Log.e("TEST", it.toString())
            val l = File(it.id.path).list()
            if (l != null) {
                Log.e("TEST", "Filelist=" + Arrays.toString(l))
            } else {
                Log.e("TEST", "Filelist=null")
            }
        }
        val storages = File("/storage").list()
        if (storages != null)
            Log.e("TEST", "Storages=" + Arrays.toString(storages))
        else
            Log.e("TEST", "Storages=null")
    }

}