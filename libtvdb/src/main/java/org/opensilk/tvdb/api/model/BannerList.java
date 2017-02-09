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

package org.opensilk.tvdb.api.model;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * Created by drew on 3/19/16.
 */
@Root
public class BannerList {

    private final List<Banner> banners;

    public BannerList(@ElementList(name = "Banner") List<Banner> banners) {
        this.banners = banners;
    }

    @ElementList(inline = true, entry = "Banner", type = Banner.class)
    public List<Banner> getBanners() {
        return banners;
    }
}
