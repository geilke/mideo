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
package org.kramerlab.mideo.core.discretization;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.DiscreteRandomVariable;

/**
 * Acts like a {@link DiscreteRandomVariable} but discretizes a
 * continuous random variable on the fly.
 *
 * @author Michael Geilke
 */ 
public class DiscretizedRandomVariable extends DiscreteRandomVariable {

    private OnlineDiscretization discretization;

    /**             
     * @param name name of the random variable, which must not contain a
     *     dot and has to start with a letter
     * @param att attribute associated with the variable
     * @param type type of discretization, e.g., equal-width
     *     discretization or equal-frequency discretization
     * @param bins final number of bins of the discretization
     */  
    public DiscretizedRandomVariable(String name, Attribute att, 
                                     DiscretizationType type, int bins) {
        this(name, att, type, 0, 1, bins);
    }

    /**                  
     * @param name name of the random variable, which must not contain a
     *     dot and has to start with a letter
     * @param att attribute associated with the variable
     * @param type type of discretization, e.g., equal-width
     *     discretization or equal-frequency discretization
     * @param min an initial value for the minimal value of the variable
     * @param max an initial value for the maximal value of the variable
     * @param bins final number of bins of the discretization
     */  
    public DiscretizedRandomVariable(String name, Attribute att, 
                                     DiscretizationType type, 
                                     double min, double max, int bins) {
        super(name, att);
        this.discretization = new PartitionIncremental(bins, type, min, max);
    }

    /**                  
     * @return the number of bins, one for each attribute value
     */                  
    @Override            
    public int getNumberOfAttributeValues() {
        return discretization.getNumberOfBins();
    }
                    
    /**
     * @return specifies which kind of discretization is used to
     *     discretize the continuous random variable
     */
    public DiscretizationType getDiscretizationType() {
        return discretization.getDiscretizationType();
    }     

    /**                  
     * Adds {@code value} to the internal discretization object and sets
     * the variable to this value.
     * @param value new value of random variable
     */                  
    @Override            
    public double setValue(double value) {
        // We add the value to the discretization object and request the
        // discretized value from it afterwards. Assumption: value will
        // contain an integer.
        discretization.addObservation(value);
        this.value = discretization.apply(value);
        return this.value;
    }

    @Override            
    public boolean equals(Object o) {
        if (o instanceof DiscretizedRandomVariable) {
            DiscretizedRandomVariable v = (DiscretizedRandomVariable) o;
            return super.equals(o)
                && v.getDiscretizationType() == getDiscretizationType()
                && v.getNumberOfAttributeValues() 
                    == getNumberOfAttributeValues();
        } else {         
            return false;
        }                
    }
    
    @Override            
    public int hashCode() {
        return getAttribute().hashCode()
                + getDiscretizationType().ordinal()
                + getNumberOfAttributeValues()
                + ((int) getValue());
    }  
}
