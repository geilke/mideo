/**
 * MiDEO: a framework to perform data mining on probabilistic condensed 
 * representations
 * Copyright (C) 2015 Michael Geilke
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
 */
package org.kramerlab.mideo.core;

import com.yahoo.labs.samoa.instances.Attribute;

/**
 * {@code ContinuousRandomVariable} represents a continuous random
 * variable. It can take arbitrary floating point numbers in the range
 * of {@code Double}.
 *
 * @author Michael Geilke
 */
public class ContinuousRandomVariable extends RandomVariable {

    /**
     * Intializes a variable with value {@code value}.
     * @param name name of the random variable, which must not contain a
     *     dot and has to start with a letter
     * @param att the attribute information about this variable
     * @param value the attribute value
     */
    public ContinuousRandomVariable(String name, Attribute att, double value) {
        super(name, att, value);
    }

    /**
     * Intializes a variable with value 0.0.
     * @param name name of the random variable, which must not contain a
     *     dot and has to start with a letter
     * @param att the attribute information about this variable
     */
    public ContinuousRandomVariable(String name, Attribute att) {
        this(name, att, 0.0);
    }

    @Override
    public double setValue(double value) throws IllegalArgumentException {
        this.value = value;
        return value;
    }
}
