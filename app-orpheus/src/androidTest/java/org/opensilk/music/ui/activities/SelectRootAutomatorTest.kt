package org.opensilk.music.ui.activities

import android.content.Context
import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.By
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.support.test.uiautomator.Until
import android.util.Log

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.opensilk.music.BuildConfig

/**
 * Created by drew on 8/1/16.
 */
@RunWith(AndroidJUnit4::class)
class SelectRootAutomatorTest {

    internal lateinit var mUiDevice: UiDevice

    @Before
    fun setup() {
        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    fun launchApp() {
        // Start from the home screen
        mUiDevice.pressHome()

        // Wait for launcher
        val launcherPackage = mUiDevice.launcherPackageName
        Assertions.assertThat(launcherPackage).isNotNull()
        mUiDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), 5000)

        // Launch the app
        val context = InstrumentationRegistry.getContext()
        val intent = context.packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)

        // Wait for the app to appear
        mUiDevice.wait(Until.hasObject(By.pkg(BuildConfig.APPLICATION_ID).depth(0)), 5000)
    }

    @Test
    @Throws(Exception::class)
    fun testSelectRoot() {
        launchApp()
        //launch documents viewer
        mUiDevice.findObject(UiSelector().resourceId(BuildConfig.APPLICATION_ID+":id/menu_add_root")).click()
        //wait for app to appear
        mUiDevice.wait(Until.hasObject(By.pkg("com.android.documentsui")), 5000)
        //click drawer icon
        mUiDevice.findObject(UiSelector().description("Show roots")).click()
        //wait for drawer to animate in
        mUiDevice.wait(Until.hasObject(By.res("com.android.documentsui:id/container_roots")), 5000)
        //click the sdcard item in drawer
        mUiDevice.findObject(UiSelector().text("SDCARD").resourceId("android:id/title")).click()
        //wait until the list is populated
        mUiDevice.wait(Until.hasObject(By.text("Music").res("title")), 5000)
        //select the music folder
        mUiDevice.findObject(UiSelector().text("Music").resourceId("android:id/title")).click()
        //wait until finished loading
        mUiDevice.wait(Until.gone(By.text("Music").res("title")), 5000)
        //click the select button
        mUiDevice.findObject(UiSelector().text("Select").resourceId("android:id/button1")).click()
        //wait for item to show
        mUiDevice.wait(Until.hasObject(By.pkg(BuildConfig.APPLICATION_ID).depth(0)), 5000)
        mUiDevice.wait(Until.hasObject(By.text("Music")), 5000)
    }

}
