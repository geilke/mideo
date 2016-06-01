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
 *
 */
package org.kramerlab.mideo.exceptions;

/**
 * UnsupportedConfiguration is thrown if the specified configuration
 * parameters do not match the capabilities of an object. In MiDEO,
 * objects such as density estimators or streams are initialized with
 * certain parameters (e.g., a dataset description or target
 * variables). If an object is not able to deal with the provided
 * parameters (e.g., because it does not support continuous attributes),
 * an {@code UnsupportedConfiguration} exception is thrown.
 *
 * @author Michael Geilke
 */ 
public class UnsupportedConfiguration extends Exception {

    /**
     * @param message error message
     */
    public UnsupportedConfiguration(String message) {
        super(message);
    }
}
