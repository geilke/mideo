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
import org.kramerlab.mideo.core.DiscreteRandomVariable;

/**
 * Soft evidence defines how likely a discrete random variable takes a
 * value. For example, let {@literal V} be a random variable that takes
 * the value {@literal red}, {@literal blue}, and {@literal green}. With
 * soft evidence, one could define that {@literal V} takes the value
 * {@literal red} with probability 0.2, the value {@literal blue} with
 * probability 0.4, and the value {@literal green} with probability 0.4,
 * where each of these statements is a separate instance of {@code
 * SoftEvidence}. It is also possible to set probabilities to 0 or to
 * 1. Please notice, however, that the probabilities for a random
 * variable must sum up to 1.
 *
 * @author Michael Geilke
 */
public class SoftEvidence extends Evidence {

    protected Double value;
    protected Double probability;

    /**
     * @param variable the discrete random variable for which the
     * evidence is given. Please notice that soft evidence for
     * continuous random variables is not supported, yet.
     */
    public SoftEvidence(RandomVariable variable) {
        setVariable(variable);
    }

    /**
     * @param rv the discrete random variable for which the evidence is
     * given. Please notice that soft evidence for continuous random
     * variables is not supported, yet.
     */
    @Override
    public void setVariable(RandomVariable rv) {
        if (rv instanceof DiscreteRandomVariable) {
            super.setVariable(rv);
        } else {
            String msg = "soft evidence is only supported for ";
            msg += "discrete random variables";
            throw new RuntimeException(msg);
        }
    }

    /**
     * Sets the probability that the random variable takes value {@code
     * value} to {@code probability}.
     * @param value the value of the random variable
     * @param probability the probability that is assigned to the value
     */
    public void setEvidence(double value, double probability) {
        this.value = value;
        this.probability = probability;
    }

    /**
     * @return the value of the random variable
     */
    public double getValue() {
        return value;
    }

    /**
     * @return the probability that the random variable takes value
     * {@code #getValue}
     */
    public double getProbability() {
        return probability;
    }

    @Override
    public boolean equals(Object o) {

        SoftEvidence v = null;
        if (o instanceof SoftEvidence) {
            v = (SoftEvidence) o;
        }
        if (v == null) {
            return false;
        }

        return getVariable().getName() == v.getVariable().getName()
            && getValue() == v.getValue()
            && getProbability() == v.getProbability();
    }

    @Override
    public int hashCode() {
        // If the evidence was a numeric value, we would not distinguish
        // a 1.0 from a 1.1 or 1.2. To lower this risk, we make sure
        // that the last five digits are also considered.
        int correction = (int) Math.pow(10, 5);
        return getVariable().getName().hashCode() 
            + (int) (getValue() * correction)
            + (int) (getProbability() * correction);
    }
}

