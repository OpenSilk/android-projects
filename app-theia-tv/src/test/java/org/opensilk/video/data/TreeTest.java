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

package org.opensilk.video.data;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.video.BuildConfig;
import org.opensilk.video.tv.ui.details.DetailsFileInfoRow;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.videolan.libvlc.Media;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 4/7/16.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class TreeTest {

    @Test
    public void testNewTree() {
        List<Pair<String, String>> lst = new ArrayList<>();
        lst.add(Pair.of("5", "6"));
        lst.add(Pair.of("2", "3"));
        lst.add(Pair.of("1", "2"));
        lst.add(Pair.of("2", "4"));
        lst.add(Pair.of("1", "5"));

        Tree<String> tree = Tree.newTree(lst);
        Assertions.assertThat(tree.getNodes().size()).isEqualTo(2);
        for (Tree.Node<String> node: tree.getNodes()) {
            if ("2".equals(node.getSelf())) {
                Assertions.assertThat(node.getChildren().size()).isEqualTo(2);
            } else if ("5".equals(node.getSelf())) {
                Assertions.assertThat(node.getChildren().size()).isEqualTo(1);
            }
        }
    }
}
