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
package org.kramerlab.mideo.inference;

import org.kramerlab.mideo.core.RandomVariable;

/**
 * Hard evidence fixes the value of a random variable. For example, let
 * {@literal V} be a random variable that takes the value {@literal
 * red}, {@literal blue}, and {@literal green}. With hard evidence, one
 * could define that {@literal V} takes the value {@literal blue}.
 *
 * @author Michael Geilke
 */
public class HardEvidence extends Evidence {

    protected double evidence;

    /**
     * @param variable random variable for which the evidence is given
     * @param evidence the evidence, which could be the index pointing
     * to a nominal value or a numeric value
     */
    public HardEvidence(RandomVariable variable, double evidence) {
        setVariable(variable);
        setEvidence(evidence);
    }

    /**
     * @param evidence the evidence of the random variable, which could
     * be the index pointing to a nominal value or a numeric value
     */
    public void setEvidence(double evidence) {
        this.evidence = evidence;
    }

    /**
     * @return the evidence of the random variable, which could be the
     * index pointing to a nominal value or a numeric value
     */
    public double getEvidence() {
        return evidence;
    }

    /**
     * Hard evidence is considered equal if it refers to the same
     * variable.
     */
    @Override
    public boolean equals(Object o) {

        HardEvidence v = null;
        if (o instanceof HardEvidence) {
            v = (HardEvidence) o;
        }
        if (v == null) {
            return false;
        }

        return getVariable().getName() == v.getVariable().getName();
    }

    @Override
    public int hashCode() {
        // If the evidence was a numeric value, we would not distinguish
        // a 1.0 from a 1.1 or 1.2. To lower this risk, we make sure
        // that the last five digits are also considered.
        int correction = (int) Math.pow(10, 5);
        return getVariable().getName().hashCode() 
            + (int) (getEvidence() * correction);
    }
}

