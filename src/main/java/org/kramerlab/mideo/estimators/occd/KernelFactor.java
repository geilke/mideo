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

import java.io.Serializable;

/**
 * KernelFactor describes the weight of a kernel.
 *
 * @author Michael Geilke
 */
public class KernelFactor implements Serializable {

    /**
     * This is the number of bins into which the target variable is
     * discretized.
     */
    private int numBins;

    /**
     * These are the factors for each discretization bin.
     */
    private int[] oneDimFactor;

    /**
     * @param numBins the number of bins in which the target variable is
     * discretized.
     */
    public KernelFactor(int numBins) {
        this.numBins = numBins;
        this.oneDimFactor = new int[numBins];
        for (int i = 0; i < oneDimFactor.length; i++) {
            oneDimFactor[i] = 0;
        }
    }

    /**
     * @return the number of discretization bins of the target variable.
     */
    public int getNumberOfBins() {
        return numBins;
    }


    /**
     * Specifies the multiplier for the discretization bins {@code i}.
     * @param i the index of the discretization bin.
     * @param value the multiplier
     * @return the number of discretization bins of the target variable.
     */
    public void setMultiplier(int i, int value) {
        oneDimFactor[i] = value;
    }

    /**
     * @return the multiplier of the discretization bin with index
     * {@code i}.
     */
    public int getMultiplier(int i) {
        return oneDimFactor[i];
    }
    
    /**
     * Computes the sum of this kernel factor and {@code factor}. That
     * is, the values of the discretization bins with the same index are
     * added.
     * @param factor the factor to be added
     * @throws IllegalArgumentException if the number of discretization
     * bins do not match
     */
    public void add(KernelFactor factor) throws IllegalArgumentException {
        if (this.getNumberOfBins() != factor.getNumberOfBins()) {
            throw new IllegalArgumentException("Dimensions do not match.");
        }
        for (int i = 0; i < this.getNumberOfBins(); i++) {
            setMultiplier(i, getMultiplier(i) + factor.getMultiplier(i));
        }
    }

    /**
     * Evaluates the kernel factor with respect to the given weight
     * vector.
     * @param w the weight vector
     */
    public double evaluate(double[] w) {
        double weight = 0.0;
        for (int i = 0; i < oneDimFactor.length; i++) {
            int multiplier = oneDimFactor[i];
            weight += w[i] * multiplier;
        }
        return weight;
    }
}
