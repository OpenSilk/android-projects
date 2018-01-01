package org.opensilk.traveltime.ui

import android.Manifest
import android.accounts.AccountManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.content.PermissionChecker
import android.util.Log

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException

import javax.inject.Inject

import dagger.android.AndroidInjection
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.opensilk.traveltime.api.AuthHelper
import org.opensilk.traveltime.api.CalendarApi
import org.opensilk.traveltime.data.Settings

/**
 * This activity has no UI. It simply handles requesting permissions and
 * account selection. Once the user has selected the account it launches
 * the settings activity.
 *
 * Created by drew on 10/29/17.
 */
class LoginActivity : DaggerAppCompatActivity() {

    @Inject lateinit var authHelper: AuthHelper
    @Inject lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        Log.i("LOGIN", "FirebaseId=" + authHelper.getFirebaseToken())
        if (!authHelper.isAccountSelected) {
            openAccountPicker()
        } else {
            checkAccess()
        }
    }

    private fun checkAccess() {
        Single.fromCallable<Any> { CalendarApi(authHelper).poke() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ _ ->
                    openSettingsActivity()
                }, { t ->
                    if (t is UserRecoverableAuthIOException) {
                        startActivityForResult(t.intent, REQUEST_ACCESS)
                    } else {
                        Log.e("CHECK", "unhandled exception", t)
                    }
                })
    }

    private fun openSettingsActivity() {
        if (!settings.isChannelInitialized) {
            //startService(Intent(this, ChannelInitService::class.java))
        }
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }

    private fun openAccountPicker() {
        if (!hasAccountPermission() && Build.VERSION.SDK_INT >= 23) {
            requestPermissions(PERMS, REQUEST_PERM_GET_ACCOUNTS)
        } else {
            startActivityForResult(authHelper.accountPickerIntent, REQUEST_CHOOSE_ACCOUNT)
        }
    }

    private fun hasAccountPermission(): Boolean {
        return PermissionChecker.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PermissionChecker.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERM_GET_ACCOUNTS -> openAccountPicker()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHOOSE_ACCOUNT -> {
                if (resultCode == RESULT_OK && data != null && data.extras != null) {
                    val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    if (accountName != null) {
                        authHelper.selectAccount(accountName)
                        checkAccess()
                        return
                    }
                }
                openAccountPicker()
            }
            REQUEST_ACCESS -> checkAccess()
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {

        private val PERMS = arrayOf(Manifest.permission.GET_ACCOUNTS)

        internal val REQUEST_PERM_GET_ACCOUNTS = 2001
        internal val REQUEST_CHOOSE_ACCOUNT = 3001
        internal val REQUEST_ACCESS = 4001
    }
}
