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

import java.util.function.Predicate;

/**
 * An {@code Option} describes a parameter for objects of the MiDEO
 * framework. It consists of a name, a documentation describing the
 * parameter, a value, and a predicate.
 *
 * Example: <pre>{@code
 * private Option<Integer> ensembleSize = new Option<>(
 *     "ensembleSize",                          // name
 *     "the number of classifier chains",       // documentation
 *     1,                                       // value
 *     s -> (s > 0));                           // predicate
 * }</pre>
 *
 * In this example, the name is a unique identifier within the object
 * having this option. {@code documentation} is a description that
 * should help the user to understand when and how the option can be
 * used. {@code value} is the value that has been set. {@code predicate}
 * specifies which values are allowed, i.e., only the value for which
 * the predicate is evaluated as true are permitted.
 *
 * @author Michael Geilke
 */
public class Option<T> {

    private String name;
    private String documentation;    
    private T value;
    private T defaultValue;
    private Predicate<T> predicate;

    /**
     * @param name the name of the option, which should be unique within
     * the object having this option
     * @param doc some explanation when and how the option can be used
     * @param defaultVal the value that this option has by default
     * @param p a predicate specifying which values are permitted
     */
    public Option(String name, String doc, T defaultVal, Predicate<T> p) {
        this.name = name;
        this.documentation = doc;
        this.value = defaultVal;
        this.defaultValue = defaultVal;
        this.predicate = p;
    }

    /**
     * @param name the name of the option, which should be unique within
     * the object having this option
     * @param doc some explanation when and how the option can be used
     * @param defaultVal the value that this option has by default
     */
    public Option(String name, String doc, T defaultVal) {
        this(name, doc, defaultVal, value -> true);
    }

    /**
     * @param name the name of the option, which should be unique within
     * the object having this option
     */
    public Option(String name) {
        this(name, null, null, value -> true);
    }

    /**
     * @return the name of the option, which should be unique within the
     * object having this option
     */
    public String getName() {
        return name;
    }

    /**
     * @return some explanation when and how the option can be used
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * @param value the value of the option
     * @throws IllegalArgumentException if {@code
     * !getPredicate().test(value)}.
     */
    public void setValue(T value) throws IllegalArgumentException {
        if (predicate.test(value)) {
            this.value = value;
        } else {
            String msg = "Illegal value for option " + getName();
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * @return the value of the option
     */
    public T getValue() {
        return value;
    }

    /**
     * Only the values for which the predicate evaluates to true are
     * permitted for this option.
     * @return a predicate specifying which values are permitted
     */
    public Predicate<T> getPredicate() {
        return predicate;
    }
}
