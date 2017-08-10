package org.opensilk.music.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.browse.MediaBrowser
import android.os.*
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.ServiceScope
import org.opensilk.common.dagger.getDaggerComponent
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Created by drew on 6/27/16.
 */
class ScannerService(): Service() {

    @ServiceScope
    @dagger.Component(
            dependencies = arrayOf(
                    AppContextComponent::class
            ),
            modules = arrayOf(
                    Module::class
            )
    )
    interface Component {
        fun inject(service: ScannerService)
    }

    @dagger.Module
    class Module {

    }

    companion object {
        const val KEY_ERROR = "err"
        const val KEY_ITEM = "item"
        const val CODE_ERROR = 1
        const val CODE_SUCCESS = 2

        @JvmStatic var sSubscribeOn: Scheduler = Schedulers.io()
        @JvmStatic var sObserveOn: Scheduler = AndroidSchedulers.mainThread()

        fun scanFile(context: Context, mediaItem: MediaBrowser.MediaItem) {

        }
    }

    internal lateinit var mMetaExtractor: MetaExtactor.Default
        @Inject set

    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("onBind(%s)", intent)
        return ScannerBinder(this)
    }

    override fun onCreate() {
        Timber.d("onCreate()")
        super.onCreate()
        val rootCmp = applicationContext.getDaggerComponent<AppContextComponent>()
        val cmp = DaggerScannerService_Component.builder().appContextComponent(rootCmp).build()
        cmp.inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        return START_REDELIVER_INTENT;
    }

    internal fun scanItem(mediaItem: MediaBrowser.MediaItem): Single<MediaBrowser.MediaItem> {
        val meta = mediaItem._getMediaMeta()
        if (!meta.isAudio) {
            return IllegalArgumentException("Item $mediaItem is not an audio file").toSingle()
        }
        return mMetaExtractor.extractMeta(mediaItem);
    }

    /**
     * Aidl implementation
     */
    class ScannerBinder(
            service: ScannerService
    ): IScannerService.Stub() {
        private val mService: WeakReference<ScannerService>
        init {
            mService = WeakReference(service)
        }
        override fun scanItem(mediaItem: MediaBrowser.MediaItem?, cb: ResultReceiver?): ISubscription {
            val subscriber = singleSubscriber<MediaBrowser.MediaItem>()
                .onError { cb?.send(CODE_ERROR, bundle(KEY_ERROR, it.message ?: it.javaClass.simpleName) ) }
                .onSuccess { cb?.send(CODE_SUCCESS, bundle(KEY_ITEM, it)) }
            val sub = mService.get()!!.scanItem(mediaItem!!)
                .subscribe(subscriber)
            return object : ISubscription.Stub() {
                override fun unsubscribe() {
                    sub.unsubscribe()
                }
                override fun isUnsubscribed(): Boolean {
                    return sub.isUnsubscribed
                }
            }
        }
    }

    /**
     * Converts the ResultReceiver codes into onSuccess/onError callbacks
     */
    abstract class ScannerSubscriber(): rx.SingleSubscriber<MediaBrowser.MediaItem>() {
        val cb = object: ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                when (resultCode) {
                    CODE_ERROR -> onError(MediaExtractorException(resultData!!.getString(KEY_ERROR)))
                    CODE_SUCCESS -> onSuccess(resultData!!.getParcelable(KEY_ITEM))
                }
            }
        }
    }

    /**
     * Wrapper for ISubscriber
     */
    class ScannerSubscription(private val wrapped: ISubscription): rx.Subscription {
        override fun isUnsubscribed(): Boolean {
            val itis = try {
                wrapped.isUnsubscribed
            } catch (ignored: RemoteException) {
                false
            }
            return itis
        }

        override fun unsubscribe() {
            try {
                wrapped.unsubscribe();
            } catch (ignored: RemoteException) {

            }
        }
    }

    /**
     * The exception we wrap our error message in
     */
    class MediaExtractorException(msg: String?): Exception(msg)
}