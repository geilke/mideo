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

/**
 * Represents a point in time. For RED, we make the simplified
 * assumption that a timestamp is an integer that is increased by one
 * for every further instance that is retrieved from the data
 * stream. Hence, the first instance has timestamp 1, the second
 * instances has timestamp 2, ....
 *
 * @author Michael Geilke
 */
public class Timestamp {
    
    private long value;

    /**
     * @param value the value of the timestamp
     */
    public Timestamp(long value) {
	setValue(value);
    }

    /**
     * @param value the value of the timestamp
     */
    public void setValue(long value) {
	this.value = value;
    }

    /**
     * @return the value of the timestamp
     */
    public long getValue() {
	return value;
    }

    /**
     * Computes and returns the difference between the current timestamp
     * and {@code timestamp}.  
     * @param timestamp is the subtrahend
     * @return {@code this.getValue() - timestamp.getValue()}
     */
    public long difference(Timestamp timestamp) {
	return this.getValue() - timestamp.getValue();
    }

    @Override
    public String toString() {
	return Long.toString(value);
    }
}
