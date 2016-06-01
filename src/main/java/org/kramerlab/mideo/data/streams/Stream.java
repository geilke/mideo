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
package org.kramerlab.mideo.data.streams;

import java.util.List;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.Configurable;

/**
 * {@code Stream} is an interface to classes, which provide data
 * instances as a stream. After its initialization with {@code init},
 * meta information about the structure of the data can be obtained
 * using {@code getRandomVariables}, {@code getNumberOfInstance}, and
 * {@code getHeader}. To navigate within the stream, {@code
 * nextInstance} and {@code restart} are the most important methods.
 *
 * @author Michael Geilke
 */
public interface Stream extends Configurable {

    /**
     * Initializes the data stream by providing meta data information
     * such as the involved random variables or the header format.
     *
     * @throws Exception if data source cannot be accessed or the data
     * is somehow corrupted
     */
    public void init() throws Exception;

    /**
     * Provides the attributes of the data stream as random variables.
     * @return the random variables of the data stream
     */
    public List<RandomVariable> getRandomVariables();

    /**
     * @return the number of available data instances
     */
    public long getNumberOfInstances();
    
    /**
     * Describes the structure of the data instances, which includes the
     * attributes and the values the attributes can take. It is mainly
     * used for compatibility with SAMOA.
     *
     * @return the structure of the data instances as described in SAMOA
     */
    public InstancesHeader getHeader();

    /**
     * @return true iff more instances are available in the data stream
     */
    public boolean hasMoreInstances();

    /**
     * @return the next instance of the data stream if such an instance
     * exists; otherwise, {@code null} is returned.
     */
    public Instance nextInstance();

    /**
     * Tells whether the stream can be reset to its initial state, which
     * is state after invoking {@code init}.
     *
     * @return true iff the stream can be reset to its initial state
     */
    public boolean isRestartable();
    
    /**
     * Resets the stream to its initial state.
     * @throws Exception if the state of the stream cannot be reset
     */
    public void restart() throws Exception;
}
