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
 * {@code Configurable} offers an interface to provide options. These
 * options can be configured automatically by other classes. For
 * example, {@link org.kramerlab.mideo.evaluation.JobCenter} is able to
 * read configuration files, to match the options by name, and to
 * automatically set the values of specified parameters.
 *
 * @author Michael Geilke
 */
public interface Configurable {

    /**
     * Specifies the available options. Options that are not provided by
     * {@code getOptions} are ignored.
     * @return the options of the object
     */
    Options getOptions();
}
