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
package org.kramerlab.mideo.estimators.occd;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * A kernel for conditional densities with a single continuous target
 * variable.
 *
 * For details, we would like to refer you to the paper:
 *
 * <p>Michael Geilke, Andreas Karwath, Eibe Frank and Stefan Kramer
 * <br /> Online Estimation of Discrete, Continuous, and Conditional
 * Joint Densities using Classifier Chains<br /> In: Data Mining and
 * Knowledge Discovery, Springer 2017.</p>
 * 
 * @author Michael Geilke
 */
public class Kernel implements Serializable {

    private KernelFactor weight;
    private Double mean;
    private Double variance;

    public Kernel(double mean) {
        this.weight = null;
        this.mean = mean;
        this.variance = null;
    }

    public KernelFactor getWeight() {
        return weight;
    }

    public void setWeight(KernelFactor weight) {
        this.weight = weight;
    }

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
    }

    public Double getVariance() {
        return variance;
    }

    public void setVariance(Double variance) {
        this.variance = variance;
    }

    /**
     * Evaluates the kernel with respect to the given value of the
     * target variable and the weight vector.
     * @param y the value of the target variable
     * @param w the weight vector
     * @throws IllegalArgumentException if the weight vector does not
     * match the number of discretization bins.
     */
    public double evaluate(double y, double[] w) 
            throws IllegalArgumentException {
        if (getWeight() != null && w.length != getWeight().getNumberOfBins()) {
            String msg = "Weight vector does not match kernel";
            throw new IllegalArgumentException(msg);
        }
        //\frac{1}{\sigma\sqrt{2\pi}}e^{-\frac{(x-\mu)^2}{2\sigma^2}}
        double mu = getMean();
        double sigma = getVariance();
        double value = getWeight() != null ? getWeight().evaluate(w) : 1;
        value *= (1 / (sigma * Math.sqrt(2 * Math.PI)));
        value *= Math.exp((-Math.pow(y - mu, 2)) / (2 * Math.pow(sigma, 2)));
        return value;
    }
}
