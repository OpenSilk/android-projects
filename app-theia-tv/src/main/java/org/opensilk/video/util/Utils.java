/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opensilk.video.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Point;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.videolan.libvlc.Media;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    public static void assertMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Must call from main thread!");
        }
    }

    public static void ensureNotOnMainThread(Context context) {
        Looper looper = Looper.myLooper();
        if (looper != null && looper == context.getMainLooper()) {
            throw new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
        }
    }

    /**
     * Returns the screen/display size
     */
    public static Point getDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    /**
     * Shows a (long) toast
     */
    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a (long) toast.
     */
    public static void showToast(Context context, int resourceId) {
        Toast.makeText(context, context.getString(resourceId), Toast.LENGTH_LONG).show();
    }

    public static int convertDpToPixel(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    /**
     * Formats time in milliseconds to hh:mm:ss string format.
     */
    public static String formatMillis(int millis) {
        String result = "";
        int hr = millis / 3600000;
        millis %= 3600000;
        int min = millis / 60000;
        millis %= 60000;
        int sec = millis / 1000;
        if (hr > 0) {
            result += hr + ":";
        }
        if (min >= 0) {
            if (min > 9) {
                result += min + ":";
            } else {
                result += "0" + min + ":";
            }
        }
        if (sec > 9) {
            result += sec;
        } else {
            result += "0" + sec;
        }
        return result;
    }

    public static Activity findActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            ContextWrapper contextWrapper = (ContextWrapper) context;
            return findActivity(contextWrapper.getBaseContext());
        } else {
            throw new AssertionError("Unknown context type " + context.getClass().getName());
        }
    }

    public static void logMediaMeta(Media media) {
//        media.parse(Media.Parse.ParseNetwork);
        StringBuilder sb = new StringBuilder(100);
        for (int ii = Media.Meta.Title; ii< Media.Meta.MAX; ii++) {
            sb.append(ii).append("=").append(media.getMeta(ii)).append(" ");
        }
        Timber.d("uri=%s meta: %s", media.getUri(), sb.toString());
    }

    private static final DecimalFormat READABLE_DECIMAL_FORMAT = new DecimalFormat("#,##0.#");
    private static final CharSequence UNITS = "KMGTPE";

    //http://stackoverflow.com/a/3758880
    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format(Locale.US, "%s %siB",
                READABLE_DECIMAL_FORMAT.format(bytes / Math.pow(1024, exp)), UNITS.charAt(exp - 1));
    }

    public static String humanReadableBitrate(long rate) {
        if (rate < 1024) return rate + "B/s";
        int exp = (int) (Math.log(rate) / Math.log(1024));
        return String.format(Locale.US, "%s%sB/s",
                READABLE_DECIMAL_FORMAT.format(rate / Math.pow(1024, exp)), UNITS.charAt(exp - 1));
    }

    private static final String DURATION_FORMAT_M = "mm'm 'ss's'";
    private static final String DURATION_FORMAT_H = "HH'h 'mm'm 'ss's'";

    public static String humanReadableDuration(long durationMilli) {
        if (durationMilli < 3600000) {
            return DurationFormatUtils.formatDuration(durationMilli, DURATION_FORMAT_M);
        }
        return DurationFormatUtils.formatDuration(durationMilli, DURATION_FORMAT_H);
    }

    // eg s01e01 or 101
    private static final Pattern TV_REGEX = Pattern.compile(
            //reluctant anything (.| ) (s##e##|###) (.| |-|<end>) anything
            //TODO find way for the ### to work with double digit seasons (####)
            "(.+?)(?:[\\. ])(s\\d{2}e\\d{2}|[1-9][0-9]{2})(?:[\\. -]|$).*", Pattern.CASE_INSENSITIVE);
    // has a year eg 1999 or 2012
    private static final Pattern MOVIE_REGEX = Pattern.compile(
            //reluctant anything (.| |() (19##|20##) (.| |)|-|<end>) anything
            "(.+?)(?:[\\. \\(])((19|20)\\d{2})(?:[\\. \\)-]|$).*", Pattern.CASE_INSENSITIVE);

    public static boolean matchesTvEpisode(CharSequence title) {
        return title != null && TV_REGEX.matcher(title).matches();
    }

    public static boolean matchesMovie(CharSequence title) {
        return title != null && MOVIE_REGEX.matcher(title).matches();
    }

    public static @Nullable String extractSeriesName(CharSequence title) {
        if (title == null) {
            return null;
        }
        Matcher m = TV_REGEX.matcher(title);
        if (m.matches()) {
            String series = m.group(1);
            if (!StringUtils.isEmpty(series)) {
                return StringUtils.replace(series, ".", " ").trim().toLowerCase();
            }
        }
        return null;
    }

    public static int extractSeasonNumber(CharSequence title) {
        int num = -1;
        if (!StringUtils.isEmpty(title)) {
            Matcher m = TV_REGEX.matcher(title);
            if (m.matches()) {
                String episodes = m.group(2);
                if (!StringUtils.isEmpty(episodes)) {
                    if (StringUtils.isNumeric(episodes)) {
                        //101 style
                        num = Character.getNumericValue(episodes.charAt(0));
                    } else {
                        //s01e01 style
                        int eidx = StringUtils.indexOfAny(episodes, "Ee");
                        num = Integer.valueOf(episodes.substring(1, eidx));
                    }
                }
            }
        }
        return num;
    }

    public static int extractEpisodeNumber(CharSequence title) {
        int num = -1;
        if (!StringUtils.isEmpty(title)) {
            Matcher m = TV_REGEX.matcher(title);
            if (m.matches()) {
                String episodes = m.group(2);
                if (!StringUtils.isEmpty(episodes)) {
                    if (StringUtils.isNumeric(episodes)) {
                        //101 style
                        num = Integer.valueOf(episodes.substring(1));
                    } else {
                        //s01e01 style
                        int eidx = StringUtils.indexOfAny(episodes, "Ee");
                        num = Integer.valueOf(episodes.substring(eidx+1));
                    }
                }
            }
        }
        return num;
    }

    public static String extractMovieName(CharSequence title) {
        if (title == null) {
            return null;
        }
        Matcher m = MOVIE_REGEX.matcher(title);
        if (m.matches()) {
            String name = m.group(1);
            if (!StringUtils.isEmpty(name)) {
                return StringUtils.replace(name, ".", " ").trim().toLowerCase();
            }
        }
        return null;
    }

    public static String extractMovieYear(CharSequence title) {
        if (title == null) {
            return null;
        }
        Matcher m = MOVIE_REGEX.matcher(title);
        if (m.matches()) {
            String year = m.group(2);
            if (!StringUtils.isEmpty(year)) {
                return year.trim();
            }
        }
        return null;
    }

    public static File getCacheDir(Context context, String path) {
        File dir = context.getExternalCacheDir();
        if (dir == null || !dir.exists() || !dir.canWrite()) {
            dir = context.getCacheDir();
        }
        return new File(dir, path);
    }

    public static Uri makeResourceUri(Context context, int resource) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName())
                .appendPath(String.valueOf(resource))
                .build();
    }

}
