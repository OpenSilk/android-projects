package org.opensilk.video

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DurationFormatUtils
import java.io.File
import java.text.DecimalFormat
import java.util.*
import java.util.regex.Pattern

private val READABLE_DECIMAL_FORMAT = DecimalFormat("#,##0.#")
const private val UNITS = "KMGTPE"

//http://stackoverflow.com/a/3758880
fun humanReadableSize(bytes: Long): String {
    if (bytes < 1024) return bytes.toString() + " B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    return String.format(Locale.US, "%s %siB",
            READABLE_DECIMAL_FORMAT.format(bytes / Math.pow(1024.0, exp.toDouble())), UNITS[exp - 1])
}

fun humanReadableBitrate(rate: Long): String {
    if (rate < 1024) return rate.toString() + "B/s"
    val exp = (Math.log(rate.toDouble()) / Math.log(1024.0)).toInt()
    return String.format(Locale.US, "%s%sB/s",
            READABLE_DECIMAL_FORMAT.format(rate / Math.pow(1024.0, exp.toDouble())), UNITS[exp - 1])
}

const private val DURATION_FORMAT_M = "mm'm 'ss's'"
const private val DURATION_FORMAT_H = "HH'h 'mm'm 'ss's'"

fun humanReadableDuration(durationMilli: Long): String {
    if (durationMilli < 3600000) {
        return DurationFormatUtils.formatDuration(durationMilli, DURATION_FORMAT_M)
    }
    return DurationFormatUtils.formatDuration(durationMilli, DURATION_FORMAT_H)
}

// eg s01e01 or 101
private val TV_REGEX = Pattern.compile(
        //reluctant anything (.| ) (s##e##|###) (.| |-|<end>) anything
        //TODO find way for the ### to work with double digit seasons (####)
        "(.+?)(?:[\\. ])(s\\d{2}e\\d{2}|[1-9][0-9]{2})(?:[\\. -]|$).*", Pattern.CASE_INSENSITIVE)
// has a year eg 1999 or 2012
private val MOVIE_REGEX = Pattern.compile(
        //reluctant anything (.| |() (19##|20##) (.| |)|-|<end>) anything
        "(.+?)(?:[\\. \\(])((19|20)\\d{2})(?:[\\. \\)-]|$).*", Pattern.CASE_INSENSITIVE)

fun matchesTvEpisode(title: CharSequence?): Boolean {
    return title != null && TV_REGEX.matcher(title).matches()
}

fun matchesMovie(title: CharSequence?): Boolean {
    return title != null && MOVIE_REGEX.matcher(title).matches()
}

fun extractSeriesPart(title: CharSequence): String {
    if (title.isNullOrBlank()) return ""
    val m = TV_REGEX.matcher(title)
    if (m.matches()) {
        val series = m.group(1)
        if (series.isNotBlank()) {
            return series
        }
    }
    return ""
}

fun normalizeName(title: String): String {
    return StringUtils.replace(title, ".", " ").trim { it <= ' ' }.toLowerCase()
}

fun extractSeriesName(title: CharSequence): String {
    return normalizeName(extractSeriesPart(title))
}

fun extractSeasonNumber(title: CharSequence): Int {
    var num = -1
    if (!StringUtils.isEmpty(title)) {
        val m = TV_REGEX.matcher(title)
        if (m.matches()) {
            val episodes = m.group(2)
            if (!StringUtils.isEmpty(episodes)) {
                if (StringUtils.isNumeric(episodes)) {
                    //101 style
                    num = Character.getNumericValue(episodes[0])
                } else {
                    //s01e01 style
                    val eidx = StringUtils.indexOfAny(episodes, "Ee")
                    num = Integer.valueOf(episodes.substring(1, eidx))!!
                }
            }
        }
    }
    return num
}

fun extractEpisodeNumber(title: CharSequence): Int {
    var num = -1
    if (!StringUtils.isEmpty(title)) {
        val m = TV_REGEX.matcher(title)
        if (m.matches()) {
            val episodes = m.group(2)
            if (!StringUtils.isEmpty(episodes)) {
                if (StringUtils.isNumeric(episodes)) {
                    //101 style
                    num = Integer.valueOf(episodes.substring(1))!!
                } else {
                    //s01e01 style
                    val eidx = StringUtils.indexOfAny(episodes, "Ee")
                    num = Integer.valueOf(episodes.substring(eidx + 1))!!
                }
            }
        }
    }
    return num
}

fun extractMovieName(title: CharSequence?): String {
    if (title == null) {
        return ""
    }
    val m = MOVIE_REGEX.matcher(title)
    if (m.matches()) {
        val name = m.group(1)
        if (!StringUtils.isEmpty(name)) {
            return StringUtils.replace(name, ".", " ").trim { it <= ' ' }.toLowerCase()
        }
    }
    return ""
}

fun extractMovieYear(title: CharSequence?): String {
    if (title == null) {
        return ""
    }
    val m = MOVIE_REGEX.matcher(title)
    if (m.matches()) {
        val year = m.group(2)
        if (!StringUtils.isEmpty(year)) {
            return year.trim { it <= ' ' }
        }
    }
    return ""
}

fun Context.suitableCacheDir(path: String): File {
    var dir = externalCacheDir
    if (dir == null || !dir.exists() || !dir.canWrite()) {
        dir = cacheDir
    }
    return File(dir, path)
}

fun View.findActivity(): Activity {
    return this.context.findActivity()
}

fun Context.findActivity(): Activity {
    if (this is Activity) {
        return this
    } else if (this is ContextWrapper) {
        return this.findActivity()
    } else {
        throw AssertionError("Unknown context type " + javaClass.name)
    }
}

fun Int.zeroPad(len: Int): String {
    return this.toString().padStart(len, '0')
}

fun formatTime(timeSeconds: Long, sb: StringBuilder) {
    var seconds = timeSeconds
    var minutes = seconds / 60
    val hours = minutes / 60
    seconds -= minutes * 60
    minutes -= hours * 60

    sb.setLength(0)
    if (hours > 0) {
        sb.append(hours).append(':')
        if (minutes < 10) {
            sb.append('0')
        }
    }
    sb.append(minutes).append(':')
    if (seconds < 10) {
        sb.append('0')
    }
    sb.append(seconds)
}

/**
 * myriad scale (0-1000)
 */
fun calculateCompletion(current: Long, duration: Long): Int {
    //permyriad calculation (no floating point)
    return ((1000*current + duration/2)/duration).toInt()
}


