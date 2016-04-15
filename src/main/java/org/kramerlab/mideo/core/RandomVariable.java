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

import java.io.Serializable;

import com.yahoo.labs.samoa.instances.Attribute;

/**
 * A <code>RandomVariable</code> represents the current instantiation of
 * an attribute, i.e., an attribute plus its value. It is supposed to
 * introduce random variables as an established term in the MiDEO
 * framework and is supposed to make computations such as computing the
 * entropy or mutual information as generic as possible without relying
 * on the dataset classes. In order to unify nominal and numeric
 * attributes in one class, a variable uses double values for indexing
 * purposes -- the same approach also pursued by WEKA and MOA.
 *
 * @author Michael Geilke
 */
public abstract class RandomVariable implements Serializable {

    protected String name;
    protected Attribute att;
    protected double value;

    /**
     * Intializes a variable with value 0.0.
     * @param name name of the random variable, which must not contain a
     *     dot and has to start with a letter
     * @param att the attribute information about this variable
     */
    public RandomVariable(String name, Attribute att) {
        this(name, att, 0.0);
    }

    /**
     * Intializes a variable with value <code>value</code>.
     * @param name name of the random variable, which must not contain a
     *     dot and has to start with a letter
     * @param att the attribute information about this variable
     * @param value the attribute value
     */
    public RandomVariable(String name, Attribute att, double value) {
        this.name = name;
        this.att = att;
        this.value = value;
    }

    /**
     * Returns the name of the random variable.
     * @return name of random variable
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the attribute associated with the variable. Among others,
     * it contains information about the values that the random variable
     * can take.
     * @return attribute associated with variable
     */
    public Attribute getAttribute() {
        return att;
    }

    /**
     * Associates an attribute with the variable and sets the variable
     * value to 0.0.
     * @param att attribute associated with the variable
     */
    public void setAttribute(Attribute att) {
        this.att = att;
        this.value = 0.0;
    }

    /**
     * Returns the variable value.
     * @return the value of the variable. 
     */
    public double getValue() {
        return value;
    }

    /**
     * Sets the value of the variable.
     * @param value the value that the variable should take
     * @return the value that the variable has taken
     * @throws IllegalArgumentException if the value does not match the
     *     dataset of {@link #getAttribute}
     */
    public abstract double setValue(double value) 
        throws IllegalArgumentException;

    @Override
    public boolean equals(Object o) {

        RandomVariable v = null;
        if (o instanceof RandomVariable) {
            v = (RandomVariable) o;
        }
        if (v == null) {
            return false;
        }

        return v.getName() == getName()
            && v.getAttribute() == getAttribute()
            && v.getValue() == getValue();
    }

    @Override
    public int hashCode() {
        return getName().hashCode() + ((int) getValue());
    }

    @Override
    public String toString() {
        return getName() + "." + Double.toString(getValue());
    }
}
