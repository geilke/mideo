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
package org.kramerlab.mideo.core.discretization;

import java.io.Serializable;

/**
 * DiscretizationType can be used to distinguish between different
 * discretization techniques.
 *
 * @author Michael Geilke
 */
public enum DiscretizationType implements Serializable {
    /**
     * Discretizing a continuous random variable into a user-specified
     * number of bins of equal-width.
     */
    EQUAL_WIDTH, 

    /**
     * Discretizing a continuous random variable into a user-specified
     * number of bins, such that each bin contains the same number of
     * values.
     */
    EQUAL_FREQUENCY;
}
