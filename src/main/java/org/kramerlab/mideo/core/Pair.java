/**
 * MiDEO: a framework to perform data mining on probabilistic condensed 
 * representations
 * Copyright (C) 2016 Michael Geilke
 *
 * This file is part of MiDEO.
 * 
 * MiDEO is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 * 
 * MiDEO is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 */
package org.kramerlab.mideo.core;

/**
 * Pair is an ordered pair of two objects.
 */
public class Pair<A, B> {

    private A a;
    private B b;

    /**
     * Initializes Pair with <code>a</code> being the first object and
     * <code>b</code> being the second object.
     *
     * @param a first object
     * @param b second object
     */
    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    /**
     * @return first object
     */
    public A getFirst() {
        return a;
    }

    /**
     * @return second object
     */
    public B getSecond() {
        return b;
    }
}
