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

import android.support.annotation.Nullable;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by drew on 4/7/16.
 */
public class Tree<T> {

    public static class Node<T> {
        private final List<Node> children = new ArrayList<>();
        private final T parent;
        private T self;

        public Node(T parent, T self) {
            this.parent = parent;
            this.self = self;
        }

        public @Nullable T getParent() {
            return parent;
        }

        public T getSelf() {
            return self;
        }

        public List<Node> getChildren() {
            return children;
        }
    }

    private final List<Node<T>> nodes = new ArrayList<>();

    public List<Node<T>> getNodes() {
        return nodes;
    }

    /**
     * Expands a flat list into an unsorted hierarchical structure
     *
     * @param list Pair (parent, self)
     * @param <T>
     * @return
     */
    public static <T> Tree<T> newTree(List<Pair<T, T>> list) {
        Tree<T> tree = new Tree<>();
        Map<T, Node<T>> idMap = new HashMap<>();
        List<Node<T>> orphans = new ArrayList<>();
        for (Pair<T, T> pair : list) {
            Node<T> node = new Node<>(pair.getLeft(), pair.getRight());
            idMap.put(node.self, node);
            if (node.parent == null) {
                tree.nodes.add(node);
            } else {
                Node<T> parent = idMap.get(node.parent);
                if (parent != null) {
                    parent.children.add(node);
                } else {
                    orphans.add(node);
                }
            }
        }
        for (Node<T> orphan : orphans) {
            Node<T> parent = idMap.get(orphan.parent);
            if (parent != null) {
                parent.children.add(orphan);
            } else {
                tree.nodes.add(orphan);
            }
        }
        return tree;
    }

}
