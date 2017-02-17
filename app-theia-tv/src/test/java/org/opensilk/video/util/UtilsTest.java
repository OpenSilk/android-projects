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

package org.opensilk.video.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.video.BuildConfig;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.assertj.core.api.Assertions.*;
import static org.opensilk.video.util.Utils.*;

/**
 * Created by drew on 4/3/16.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class UtilsTest {

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testmatchestvepisode() {
        assertThat(matchesTvEpisode("something with spaces s03e02")).isTrue();
        assertThat(matchesTvEpisode("something with spaces s03e02-foo")).isTrue();
        assertThat(matchesTvEpisode("something with spaces s03e02-foo.mkv")).isTrue();
        assertThat(matchesTvEpisode("something with spaces s03e02.mkv")).isTrue();
        assertThat(matchesTvEpisode("something with spaces s03e02.hdtv.x264-foo")).isTrue();
        assertThat(matchesTvEpisode("something with spaces s03e02.hdtv.x264-foo.mkv")).isTrue();
        assertThat(matchesTvEpisode("something with spaces s03e02.hdtv.x264.mkv")).isTrue();

        assertThat(matchesTvEpisode("something.with.dots.s03e02")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.s03e02-foo")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.s03e02-foo.mkv")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.s03e02.mkv")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.s03e02.hdtv.x264-foo")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.s03e02.hdtv.x264-foo.mkv")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.s03e02.hdtv.x264.mkv")).isTrue();

        assertThat(matchesTvEpisode("a.series.with.a-dash.s03e02-foo")).isTrue();

        assertThat(matchesTvEpisode("something with spaces 302")).isTrue();
        assertThat(matchesTvEpisode("something with spaces 302-foo")).isTrue();
        assertThat(matchesTvEpisode("something with spaces 302-foo.mkv")).isTrue();
        assertThat(matchesTvEpisode("something with spaces 302.mkv")).isTrue();
        assertThat(matchesTvEpisode("something with spaces 302.hdtv.x264-foo")).isTrue();
        assertThat(matchesTvEpisode("something with spaces 302.hdtv.x264-foo.mkv")).isTrue();
        assertThat(matchesTvEpisode("something with spaces 302.hdtv.x264.mkv")).isTrue();

        assertThat(matchesTvEpisode("something.with.dots.302")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.302-foo")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.302-foo.mkv")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.302.mkv")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.302.hdtv.x264-foo")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.302.hdtv.x264-foo.mkv")).isTrue();
        assertThat(matchesTvEpisode("something.with.dots.302.hdtv.x264.mkv")).isTrue();

        assertThat(matchesTvEpisode("a.series.with.a-dash.302-foo")).isTrue();

        //TODO dont mistake 720 (without p) for series
//        assertThat(matchesTvEpisode("some.movie.name.1909.720.xvid-foo")).isFalse();
    }

    @Test
    public void testExtractSeriesName() {

        assertThat(extractSeriesName("some.name.s03e19"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.s03e19.hdtv.x264"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.s03e19.hdtv.x264-foo"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.319"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.319.hdtv.x264"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.319.hdtv.x264-foo"))
                .isEqualTo("some name");

        //resulution in title
        assertThat(extractSeriesName("some.name.s03e19.720p.bluray.x264"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.s03e19.720p.bluray.x264-foo"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.319.720p.bluray.x264"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.319.720p.bluray.x264-foo"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.s03e19.1080p.bluray.x264"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.s03e19.1080p.bluray.x264-foo"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.319.1080p.bluray.x264"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.319.1080p.bluray.x264-foo"))
                .isEqualTo("some name");

        //with text after
        assertThat(extractSeriesName("some.name.s03e19.with.text.after"))
                .isEqualTo("some name");
        assertThat(extractSeriesName("some.name.319.with.text.after"))
                .isEqualTo("some name");

        //with year
        assertThat(extractSeriesName("some.name.2005.319"))
                .isEqualTo("some name 2005");
        assertThat(extractSeriesName("some.name.2005.319.hdtv.x264"))
                .isEqualTo("some name 2005");
        assertThat(extractSeriesName("some.name.2005.319.hdtv.x264-foo"))
                .isEqualTo("some name 2005");
        assertThat(extractSeriesName("some.name.2005.s03e19.720p.bluray.x264"))
                .isEqualTo("some name 2005");
        assertThat(extractSeriesName("some.name.2005.s03e19.720p.bluray.x264-foo"))
                .isEqualTo("some name 2005");
        assertThat(extractSeriesName("some.name.2005.319.720p.bluray.x264"))
                .isEqualTo("some name 2005");
        assertThat(extractSeriesName("some.name.2005.319.720p.bluray.x264-foo"))
                .isEqualTo("some name 2005");


        //other combos
        assertThat(extractSeriesName("some.name.with-dash.s03e19.hdtv.x264"))
                .isEqualTo("some name with-dash");
        assertThat(extractSeriesName("some.name.with-dash.s03e19.hdtv.x264-foo"))
                .isEqualTo("some name with-dash");
        assertThat(extractSeriesName("Some Name with Spaces S03E19 Hdtv x264"))
                .isEqualTo("Some Name with Spaces");
        assertThat(extractSeriesName("Some Name with Spaces S03E19 Hdtv x264-Foo"))
                .isEqualTo("Some Name with Spaces");
        assertThat(extractSeriesName("Some Name with Spaces 2005 S03E19 Hdtv x264-Foo"))
                .isEqualTo("Some Name with Spaces 2005");
        assertThat(extractSeriesName("Some Name with Spaces (2005) S03E19 Hdtv x264-Foo"))
                .isEqualTo("Some Name with Spaces (2005)");
    }

    @Test
    public void testExtractSeriesNameFails() {
        assertThat(extractSeriesName("some.move.name.2016")).isNullOrEmpty();
        assertThat(extractSeriesName("some.move.name.2016.720p.x264")).isNullOrEmpty();
        assertThat(extractSeriesName("some.movie.name.1909.xvid")).isNullOrEmpty();
    }

    @Test
    public void testExtractSeasonNumber() {
        assertThat(extractSeasonNumber("some.name.with-dash.s03e19.htdv.x264"))
                .isEqualTo(3);
        assertThat(extractSeasonNumber("some.name.with-dash.s03e19.htdv.x264-foo"))
                .isEqualTo(3);
        assertThat(extractSeasonNumber("some.name.with-dash.319.htdv.x264"))
                .isEqualTo(3);
        assertThat(extractSeasonNumber("some.name.with-dash.319.htdv.x264-foo"))
                .isEqualTo(3);
    }

    @Test
    public void testExtractEpisodeNumber() {
        assertThat(extractEpisodeNumber("some.name.with-dash.s03e19.htdv.x264"))
                .isEqualTo(19);
        assertThat(extractEpisodeNumber("some.name.with-dash.319.htdv.x264"))
                .isEqualTo(19);
    }

    @Test
    public void testExtractMovieName() {
        assertThat(extractMovieName("some movie (2016)"))
                .isEqualTo("some movie");
        assertThat(extractMovieName("some movie (2016) x264"))
                .isEqualTo("some movie");
        assertThat(extractMovieName("some movie (2016) 720p x264"))
                .isEqualTo("some movie");
        assertThat(extractMovieName("some movie 2016"))
                .isEqualTo("some movie");
        assertThat(extractMovieName("some movie 2016 x264"))
                .isEqualTo("some movie");
        assertThat(extractMovieName("some movie 2016 720p x264"))
                .isEqualTo("some movie");
        assertThat(extractMovieName("some.movie.2016"))
                .isEqualTo("some movie");
        assertThat(extractMovieName("some.movie.2016.x264"))
                .isEqualTo("some movie");
        assertThat(extractMovieName("some.movie.2016.720p.x264"))
                .isEqualTo("some movie");
    }

    @Test
    public void testExtractMovieYear() {
        assertThat(extractMovieYear("some movie (2016)"))
                .isEqualTo("2016");
        assertThat(extractMovieYear("some movie (2016) x264"))
                .isEqualTo("2016");
        assertThat(extractMovieYear("some movie (2016) 720p x264"))
                .isEqualTo("2016");
        assertThat(extractMovieYear("some movie 2016"))
                .isEqualTo("2016");
        assertThat(extractMovieYear("some movie 2016 x264"))
                .isEqualTo("2016");
        assertThat(extractMovieYear("some movie 2016 720p x264"))
                .isEqualTo("2016");
        assertThat(extractMovieYear("some.movie.2016"))
                .isEqualTo("2016");
        assertThat(extractMovieYear("some.movie.2016.x264"))
                .isEqualTo("2016");
        assertThat(extractMovieYear("some.movie.2016.720p.x264"))
                .isEqualTo("2016");
    }
}
