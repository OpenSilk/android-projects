/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.tvdb.api;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilk.tvdb.api.model.ActorList;
import org.opensilk.tvdb.api.model.AllZipped;
import org.opensilk.tvdb.api.model.BannerList;
import org.opensilk.tvdb.api.model.SeriesInfo;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by drew on 3/19/16.
 */
public class AllZippedConverter implements Converter<ResponseBody, AllZipped> {

    final String language = "en";
    final Serializer serializer = new Persister();

    private static final AllZippedConverter INSTANCE = new AllZippedConverter();

    public static AllZippedConverter instance() {
        return INSTANCE;
    }

    @Override
    public AllZipped convert(ResponseBody value) throws IOException {
        final AllZipped.Builder builder = new AllZipped.Builder();
        final ZipInputStream in = new ZipInputStream(value.byteStream());
        try {
            ZipEntry entry = null;
            while ((entry = in.getNextEntry()) != null) {
                String xmlData = IOUtils.toString(in);
                try {
                    if (StringUtils.equals(entry.getName(), language + ".xml")) {
                        final SeriesInfo seriesData = serializer.read(SeriesInfo.class, xmlData);
                        builder.setSeries(seriesData.getSeries());
                        builder.setEpisodes(seriesData.getEpisodes());
                    } else if (StringUtils.equals(entry.getName(), "banners.xml")) {
                        final BannerList bannerList = serializer.read(BannerList.class, xmlData);
                        builder.setBanners(bannerList.getBanners());
                    } else if (StringUtils.equals(entry.getName(), "actors.xml")) {
                        final ActorList actorList = serializer.read(ActorList.class, xmlData);
                        builder.setActors(actorList.getActors());
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
            return builder.build();
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static class Factory extends retrofit2.Converter.Factory {
        private static final Factory INSTANCE = new Factory();

        public static Factory instance() {
            return INSTANCE;
        }

        @Override
        public Converter<ResponseBody, AllZipped> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            if (type != AllZipped.class) {
                return null;
            }
            return AllZippedConverter.instance();
        }
    }
}
