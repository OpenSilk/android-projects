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

package org.opensilk.upnp.cds.browser

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.os.CancellationSignal
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.DocumentsProvider
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.getDaggerComponent
import javax.inject.Inject

/**
 * Created by drew on 12/21/16.
 */
class CDSDocProvider() : DocumentsProvider(), ServiceConnection {

    @Inject lateinit var mUpnpService: CDSUpnpService
    @Inject lateinit var mDBClient: CDSDatabaseClient

    override fun onCreate(): Boolean {
        val appComponent: AppContextComponent = context.getDaggerComponent()
        context.bindService(Intent(context, CDSHolderService::class.java), this, Context.BIND_AUTO_CREATE)
        return true
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        return super.isChildDocument(parentDocumentId, documentId)
    }

    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryChildDocuments(parentDocumentId: String?, projection: Array<out String>?, sortOrder: String?): Cursor {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        return mDBClient.queryRemoteDevices(projection)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mHolderService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        mHolderService = service as? CDSHolderService.Holder
        mHolderService?.setCDSUpnpService(mUpnpService)
    }
}