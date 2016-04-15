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
 * A {@code DiscreteRandomVariable} is a {@code RandomVariable} that
 * only takes values from a fixed finite set. For representing the value
 * of the variable, we pursue the same approach taken by WEKA and MOA,
 * i.e., we employ double values for indexing purposes.
 *
 * @author Michael Geilke
 */
public class DiscreteRandomVariable extends RandomVariable {

    /**
     * Intializes a variable with value {@code value}.
     * @param name name of the random variable, which must not contain a
     *     dot and has to start with a letter
     * @param att the attribute information about this variable
     * @param value the attribute value
     */
    public DiscreteRandomVariable(String name, Attribute att, double value) {
        super(name, att, value);
    }

    /**
     * Intializes a variable with value 0.0.
     * @param name name of the random variable, which must not contain a
     *     dot and has to start with a letter
     * @param att the attribute information about this variable
     */
    public DiscreteRandomVariable(String name, Attribute att) {
        this(name, att, 0.0);
    }

    /**
     * The number of possible values that the variable can take.
     * @return the number of distinct values that the variable can take
     */
    public int getNumberOfAttributeValues() {
        return att.numValues();
    }

    /**
     * Sets the value of the variable. Possible values are integers
     * between 1 and {@link #getNumberOfAttributeValues} represented as
     * a double. The corresponding attribute value is {@code
     * getAttribute().value(value)}.
     *
     * @param value possible values are dependent on the associated
     *      attribute
     */
    public double setValue(double value) throws IllegalArgumentException {
        int attValues = getNumberOfAttributeValues();
        if (attValues > 0 && value < attValues) {
            this.value = value;
            return value;
        } else {
            String msg = Double.toString(value) + " > ";
            msg += Integer.toString(attValues - 1);
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public String toString() {
        String attValue = getAttribute().value((int)getValue());
        return getAttribute().name() + "." + attValue;
    }
}
