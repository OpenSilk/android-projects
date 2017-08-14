package org.opensilk.video

import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import io.reactivex.Maybe
import io.reactivex.Single
import org.opensilk.dagger2.ForApp
import org.opensilk.media.database.ApiHelper
import org.opensilk.tvdb.api.model.Token
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

const private val VERSION = 2

class VideoDatabaseMalfuction: Exception()

@Module
abstract class VideoAppProviderModule {
    @ContributesAndroidInjector
    abstract fun injector(): VideoAppProvider
    @Binds
    abstract fun apiHelper(impl: VideoAppDAO): ApiHelper
}

/**
 * Created by drew on 7/18/17.
 */
@Singleton
class VideoAppDAO
@Inject constructor(
        private val mResolver: ContentResolver,
        private val mUris: VideoAppDBUris,
        @Named("tvdb_banner_root") private val mTVDbBannerRoot: String
) : ApiHelper {

    override fun tvImagePosterUri(path: String): Uri {
        return Uri.parse(mTVDbBannerRoot).buildUpon().appendPath(path).build()
    }

    override fun tvImageBackdropUri(path: String): Uri {
        return Uri.parse(mTVDbBannerRoot).buildUpon().appendPath(path).build()
    }

    override fun movieImagePosterUri(path: String): Uri {
        return Uri.parse("${getMovieImageBaseUrl()}w342$path")
    }

    override fun movieImageBackdropUri(path: String): Uri {
        return Uri.parse("${getMovieImageBaseUrl()}w1280$path")
    }

    fun setTvLastUpdate(lastUpdate: Long) {
        val cv = ContentValues()
        cv.put("key", "last_update")
        cv.put("value", lastUpdate)
        mResolver.insert(mUris.tvConfig(), cv)
    }

    fun getTvLastUpdate(): Maybe<Long> {
        return Maybe.create { s ->
            mResolver.query(mUris.tvConfig(), arrayOf("value"),
                    "key='last_update'", null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.getString(0).toLong())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun setTvToken(token: Token) {
        val cv = ContentValues()
        cv.put("key", "token")
        cv.put("value", token.token)
        mResolver.insert(mUris.tvConfig(), cv)
    }

    fun getTvToken(): Single<Token> {
        return Single.create { s ->
            mResolver.query(mUris.tvConfig(), arrayOf("value"),
                    "key='token'", null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(Token(c.getString(0)))
                } else {
                    s.onError(Exception("Token not found"))
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    @Synchronized //TODO cache this value
    fun setMovieImageBaseUrl(imageBaseUrl: String): Boolean {
        val values = ContentValues()
        values.put("key", "image_base_url")
        values.put("value", imageBaseUrl)
        return mResolver.insert(mUris.movieConfig(), values) != null
    }

    @Synchronized //TODO cache this value
    fun getMovieImageBaseUrl(): String {
        return mResolver.query(mUris.movieConfig(), arrayOf("value"),
                "key='image_base_url'", null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                return@use c.getString(0)
            } else {
                return@use ""
            }
        } ?: throw VideoDatabaseMalfuction()
    }

}

/**
 * Created by drew on 7/18/17.
 */
class VideoAppProvider: ContentProvider() {

    @Inject internal lateinit var mDatabase: VideoAppDB
    @Inject internal lateinit var mUris: VideoAppDBUris

    override fun onCreate(): Boolean {
        AndroidInjection.inject(this)
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val table: String
        when (mUris.matcher.match(uri)) {
            M.TV_CONFIG -> {
                table = "tv_config"
            }
            M.MOVIE_CONFIG -> {
                table = "movie_config"
            }
            else -> throw IllegalArgumentException("Unmatched uri: $uri")
        }
        return mDatabase.readableDatabase.query(table, projection, selection,
                selectionArgs, null, null, sortOrder)
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        val db = mDatabase.writableDatabase
        when (mUris.matcher.match(uri)) {
            M.TV_CONFIG -> {
                db.insertWithOnConflict("tv_config", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.tvConfig()
            }
            M.MOVIE_CONFIG -> {
                db.insertWithOnConflict("movie_config", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.movieConfig()
            }
            else -> throw IllegalArgumentException("Unmatched uri: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        TODO("No delete allowed")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        TODO("No update allowed")
    }
}

private object M {
    val TV_CONFIG = 10
    val MOVIE_CONFIG = 11
}

/**
 * Created by drew on 7/18/17.
 */
class VideoAppDBUris
@Inject constructor(
        @Named("VideoDatabaseAuthority") private val mAuthority: String
) {

    val matcher = UriMatcher(UriMatcher.NO_MATCH)
    init {
        matcher.addURI(mAuthority, "config/tv", M.TV_CONFIG)
        matcher.addURI(mAuthority, "config/movie", M.MOVIE_CONFIG)
    }

    private fun base(): Uri.Builder {
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(mAuthority)
    }

    fun tvConfig(): Uri {
        return base().appendPath("config").appendPath("tv").build()
    }

    fun movieConfig(): Uri {
        return base().appendPath("config").appendPath("movie").build()
    }

}

/**
 *
 */
internal class VideoAppDB
@Inject constructor(
        @ForApp context: Context
) : SQLiteOpenHelper(context, "videoapp.sqlite", null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        onUpgrade(db, 0, VERSION)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < VERSION) {
            db.execSQL("DROP TABLE IF EXISTS tv_config;")
            db.execSQL("CREATE TABLE tv_config (" +
                    "key TEXT NOT NULL UNIQUE, " +
                    "value TEXT " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS movie_config")
            db.execSQL("CREATE TABLE movie_config (" +
                    "key TEXT NOT NULL UNIQUE, " +
                    "value TEXT " +
                    ");")

        }
    }

}