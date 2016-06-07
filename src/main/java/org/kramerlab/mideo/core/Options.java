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

import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

/**
 * Groups options of the same type and provides facilities to add and
 * retrieve them.
 *
 * @author Michael Geilke
 */
public class Options implements Serializable {

    private OptionType<String> stringOptions;
    private OptionType<Integer> integerOptions;
    private OptionType<Float> floatOptions;
    private OptionType<Boolean> booleanOptions;

    public Options() {
        this.stringOptions = new OptionType<>();
        this.integerOptions = new OptionType<>();
        this.floatOptions = new OptionType<>();
        this.booleanOptions = new OptionType<>();
    }

    /**
     * @return options of type String
     */
    public OptionType<String> getStringOptions() {
        return stringOptions;
    }

    /**
     * @return options of type Integer
     */
    public OptionType<Integer> getIntegerOptions() {
        return integerOptions;
    }

    /**
     * @return options of type Float
     */
    public OptionType<Float> getFloatOptions() {
        return floatOptions;
    }

    /**
     * @return options of type Boolean
     */
    public OptionType<Boolean> getBooleanOptions() {
        return booleanOptions;
    }

    /**
     * Is a group of options having type {@code T}. It provides methods
     * for managing these options, i.e., adding and retrieving options.
     */
    public class OptionType<T> implements Serializable {
     
        private Map<String, Option<T>> typeOptions;

        public OptionType() {
            this.typeOptions = new HashMap<>();
        }

        /**
         * Adds the option {@code option}. If an option with name {@code
         * option.getName()} already exists, it is overwritten with
         * {@code option}.
         * @param option the option to be added
         */
        public void addOption(Option<T> option) {
            typeOptions.put(option.getName(), option);
        }

        /**
         * @param name the name of an option
         * @return true iff an option with name {@code name} exists.
         */
        public boolean hasOption(String name) {
            return typeOptions.containsKey(name);
        }

        /**
         * @param name the name of an option
         * @return the option with name {@code name}
         * @throws IllegalArgumentException if no option with name
         * {@code name} exists
         */
        public Option<T> getOption(String name) 
                throws IllegalArgumentException {
            if (hasOption(name)) {
                return typeOptions.get(name);
            } else {
                String msg = "There is no such option with name ";
                msg += name + ". ";
                msg += "Are you sure that the type is correct?";
                throw new IllegalArgumentException(msg);
            }
        }
    }
}
