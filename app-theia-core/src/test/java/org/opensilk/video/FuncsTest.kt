package org.opensilk.video

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by drew on 6/3/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class FuncsTest {

    @Test
    fun testMatchesTVEpisode() {
        assertThat(matchesTvEpisode("something with spaces s03e02")).isTrue()
        assertThat(matchesTvEpisode("something with spaces s03e02-foo")).isTrue()
        assertThat(matchesTvEpisode("something with spaces s03e02-foo.mkv")).isTrue()
        assertThat(matchesTvEpisode("something with spaces s03e02.mkv")).isTrue()
        assertThat(matchesTvEpisode("something with spaces s03e02.hdtv.x264-foo")).isTrue()
        assertThat(matchesTvEpisode("something with spaces s03e02.hdtv.x264-foo.mkv")).isTrue()
        assertThat(matchesTvEpisode("something with spaces s03e02.hdtv.x264.mkv")).isTrue()

        assertThat(matchesTvEpisode("something.with.dots.s03e02")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.s03e02-foo")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.s03e02-foo.mkv")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.s03e02.mkv")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.s03e02.hdtv.x264-foo")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.s03e02.hdtv.x264-foo.mkv")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.s03e02.hdtv.x264.mkv")).isTrue()

        assertThat(matchesTvEpisode("a.series.with.a-dash.s03e02-foo")).isTrue()

        assertThat(matchesTvEpisode("something with spaces 302")).isTrue()
        assertThat(matchesTvEpisode("something with spaces 302-foo")).isTrue()
        assertThat(matchesTvEpisode("something with spaces 302-foo.mkv")).isTrue()
        assertThat(matchesTvEpisode("something with spaces 302.mkv")).isTrue()
        assertThat(matchesTvEpisode("something with spaces 302.hdtv.x264-foo")).isTrue()
        assertThat(matchesTvEpisode("something with spaces 302.hdtv.x264-foo.mkv")).isTrue()
        assertThat(matchesTvEpisode("something with spaces 302.hdtv.x264.mkv")).isTrue()

        assertThat(matchesTvEpisode("something.with.dots.302")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.302-foo")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.302-foo.mkv")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.302.mkv")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.302.hdtv.x264-foo")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.302.hdtv.x264-foo.mkv")).isTrue()
        assertThat(matchesTvEpisode("something.with.dots.302.hdtv.x264.mkv")).isTrue()

        assertThat(matchesTvEpisode("a.series.with.a-dash.302-foo")).isTrue()

        //TODO dont mistake 720 (without p) for series
        //        assertThat(matchesTvEpisode("some.movie.name.1909.720.xvid-foo")).isFalse();
    }

    @Test
    fun testExtractSeriesName() {

        assertThat(extractSeriesName("some.name.s03e19"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.s03e19.hdtv.x264"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.s03e19.hdtv.x264-foo"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.319"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.319.hdtv.x264"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.319.hdtv.x264-foo"))
                .isEqualTo("some name")

        //resulution in title
        assertThat(extractSeriesName("some.name.s03e19.720p.bluray.x264"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.s03e19.720p.bluray.x264-foo"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.319.720p.bluray.x264"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.319.720p.bluray.x264-foo"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.s03e19.1080p.bluray.x264"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.s03e19.1080p.bluray.x264-foo"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.319.1080p.bluray.x264"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.319.1080p.bluray.x264-foo"))
                .isEqualTo("some name")

        //with text after
        assertThat(extractSeriesName("some.name.s03e19.with.text.after"))
                .isEqualTo("some name")
        assertThat(extractSeriesName("some.name.319.with.text.after"))
                .isEqualTo("some name")

        //with year
        assertThat(extractSeriesName("some.name.2005.319"))
                .isEqualTo("some name 2005")
        assertThat(extractSeriesName("some.name.2005.319.hdtv.x264"))
                .isEqualTo("some name 2005")
        assertThat(extractSeriesName("some.name.2005.319.hdtv.x264-foo"))
                .isEqualTo("some name 2005")
        assertThat(extractSeriesName("some.name.2005.s03e19.720p.bluray.x264"))
                .isEqualTo("some name 2005")
        assertThat(extractSeriesName("some.name.2005.s03e19.720p.bluray.x264-foo"))
                .isEqualTo("some name 2005")
        assertThat(extractSeriesName("some.name.2005.319.720p.bluray.x264"))
                .isEqualTo("some name 2005")
        assertThat(extractSeriesName("some.name.2005.319.720p.bluray.x264-foo"))
                .isEqualTo("some name 2005")


        //other combos
        assertThat(extractSeriesName("some.name.with-dash.s03e19.hdtv.x264"))
                .isEqualTo("some name with-dash")
        assertThat(extractSeriesName("some.name.with-dash.s03e19.hdtv.x264-foo"))
                .isEqualTo("some name with-dash")
        assertThat(extractSeriesName("Some Name with Spaces S03E19 Hdtv x264"))
                .isEqualTo("some name with spaces")
        assertThat(extractSeriesName("Some Name with Spaces S03E19 Hdtv x264-Foo"))
                .isEqualTo("some name with spaces")
        assertThat(extractSeriesName("Some Name with Spaces 2005 S03E19 Hdtv x264-Foo"))
                .isEqualTo("some name with spaces 2005")
        assertThat(extractSeriesName("Some Name with Spaces (2005) S03E19 Hdtv x264-Foo"))
                .isEqualTo("some name with spaces (2005)")
    }

    @Test
    fun testExtractSeriesNameFails() {
        assertThat(extractSeriesName("some.move.name.2016")).isNullOrEmpty()
        assertThat(extractSeriesName("some.move.name.2016.720p.x264")).isNullOrEmpty()
        assertThat(extractSeriesName("some.movie.name.1909.xvid")).isNullOrEmpty()
    }

    @Test
    fun testExtractSeasonNumber() {
        assertThat(extractSeasonNumber("some.name.with-dash.s03e19.htdv.x264"))
                .isEqualTo(3)
        assertThat(extractSeasonNumber("some.name.with-dash.s03e19.htdv.x264-foo"))
                .isEqualTo(3)
        assertThat(extractSeasonNumber("some.name.with-dash.319.htdv.x264"))
                .isEqualTo(3)
        assertThat(extractSeasonNumber("some.name.with-dash.319.htdv.x264-foo"))
                .isEqualTo(3)
    }

    @Test
    fun testExtractEpisodeNumber() {
        assertThat(extractEpisodeNumber("some.name.with-dash.s03e19.htdv.x264"))
                .isEqualTo(19)
        assertThat(extractEpisodeNumber("some.name.with-dash.319.htdv.x264"))
                .isEqualTo(19)
    }

    @Test
    fun testExtractMovieName() {
        assertThat(extractMovieName("some movie (2016)"))
                .isEqualTo("some movie")
        assertThat(extractMovieName("some movie (2016) x264"))
                .isEqualTo("some movie")
        assertThat(extractMovieName("some movie (2016) 720p x264"))
                .isEqualTo("some movie")
        assertThat(extractMovieName("some movie 2016"))
                .isEqualTo("some movie")
        assertThat(extractMovieName("some movie 2016 x264"))
                .isEqualTo("some movie")
        assertThat(extractMovieName("some movie 2016 720p x264"))
                .isEqualTo("some movie")
        assertThat(extractMovieName("some.movie.2016"))
                .isEqualTo("some movie")
        assertThat(extractMovieName("some.movie.2016.x264"))
                .isEqualTo("some movie")
        assertThat(extractMovieName("some.movie.2016.720p.x264"))
                .isEqualTo("some movie")
    }

    @Test
    fun testExtractMovieYear() {
        assertThat(extractMovieYear("some movie (2016)"))
                .isEqualTo("2016")
        assertThat(extractMovieYear("some movie (2016) x264"))
                .isEqualTo("2016")
        assertThat(extractMovieYear("some movie (2016) 720p x264"))
                .isEqualTo("2016")
        assertThat(extractMovieYear("some movie 2016"))
                .isEqualTo("2016")
        assertThat(extractMovieYear("some movie 2016 x264"))
                .isEqualTo("2016")
        assertThat(extractMovieYear("some movie 2016 720p x264"))
                .isEqualTo("2016")
        assertThat(extractMovieYear("some.movie.2016"))
                .isEqualTo("2016")
        assertThat(extractMovieYear("some.movie.2016.x264"))
                .isEqualTo("2016")
        assertThat(extractMovieYear("some.movie.2016.720p.x264"))
                .isEqualTo("2016")
    }

    /*
    @Test
    fun testDurationParser() {
        var duration = "0:00:25.678"
        assertThat(parseUpnpDuration(duration)).isEqualTo(25)
        duration = "0:05:53.645"
        assertThat(parseUpnpDuration(duration)).isEqualTo(353)
        duration = "0:05:53.1/2"
        assertThat(parseUpnpDuration(duration)).isEqualTo(353)
        duration = "0:05:53"
        assertThat(parseUpnpDuration(duration)).isEqualTo(353)
        duration = ":05:53.645"
        assertThat(parseUpnpDuration(duration)).isEqualTo(353)
        duration = "2:05:53.645"
        assertThat(parseUpnpDuration(duration)).isEqualTo(7553)
    }
    */

}