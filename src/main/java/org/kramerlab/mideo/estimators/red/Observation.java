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
package org.kramerlab.mideo.estimators.red;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * An observations associates an instance with a distance and a
 * timestamp.
 * @author Michael Geilke
 */
public class Observation {

    protected Instance instance;
    protected Instance distance;
    protected Timestamp timestamp;

    /**
     * @param instance the instance associated with the observation
     * @param distance the distance between the instance and its
     * representative or candidate
     * @param timestamp the time point at which {@code instance} has
     * been observed
     */
    public Observation(Instance instance, Instance distance, 
                       Timestamp timestamp) {
	
	this.instance = instance;
	this.distance = distance;
	this.timestamp = timestamp;
    }

    /**
     * @return the instance associated with the observation
     */
    public Instance getInstance() {
	return instance;
    }

    /**
     * Returns the relative distance between the observation and the
     * representative or candidate to which it is associated.
     * @return the distance between the instance and its representative
     * or candidate
     */
    public Instance getDistance() {
	return distance;
    }

    /**
     * @return the time point at which {@link #getInstance} has been
     * observed
     */
    public Timestamp getTimestamp() {
	return timestamp;
    }

    @Override
    public String toString() {
	String s = instance.toString();
	s += " : " + distance.toString();
	s += " : " + timestamp.toString();

	return s;
    }
}
