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
package org.kramerlab.mideo.core.discretization;

import java.io.Serializable;
import java.util.List;

import static org.kramerlab.mideo.core.ChernoffBounds.findLambdaForLowerBound;
import static org.kramerlab.mideo.core.ChernoffBounds.findLambdaForUpperBound;

/**
 * Classes implementing the {@code OnlineDiscretization} interface allow
 * to perform various types of discretization in an online fashion.
 *
 * The discretizations are performed on a certain range {@literal [b0;
 * bk]} of a continuous random variable. This range is split into {@code
 * k} bins, where the bin with index {@code i} is representing the
 * interval {@code [bi; b{i+1})}.
 *
 * <pre>
 * b0                 b1                 b2                            bk
 * |------------------|------------------|----------....---------------|
 * </pre>
 *
 * 
 *
 * @author Michael Geilke
 */
public interface OnlineDiscretization extends Serializable {

    /**
     * Returns the discretization technique that is pursued by the
     * algorithm.
     *
     * @return the discretization technique that is pursued by the
     *     algorithm
     */
    DiscretizationType getDiscretizationType();

    /**
     * Returns the number of bins.
     *
     * @return number of bins.
     */
    int getNumberOfBins();

    /**
     * Adds observation {@code obs} and updates discretization.
     *
     * @param obs new observation for the variable under consideration
     */
    void addObservation(double obs);

    /**
     * Returns border bi.
     *
     * @param index i with 0 &lt;= i &lt; {@code getNumberOfBins()}
     * @return bi
     */
    double getBorder(int index) throws IndexOutOfBoundsException;

    /**
     * Returns the number of observations that lie in the bin {@literal
     * [bi; b{i+1}]}.
     *
     * @param index specifies the bin for which the values are
     *     counted. The index has to be in the range {@literal
     *     [0;getNumberOfBins())}.
     * @return the number of observations that lie in the interval
     *     {@literal [bi; b{i+1})}.
     */
    long getBinCount(int index) throws IndexOutOfBoundsException;

    /**
     * Increases the observation count of interval {@literal [bi;
     * b{i+1}]} by {@code count}.
     *
     * @param index i
     * @param count the number of observations that should be added to
     *     the interval [bi; b{i+1}).
     */
    void addCountToBin(int index, long count) throws IndexOutOfBoundsException;
    
    /**
     * Returns the probability masses of the discretized continuous
     * random variable.
     *
     * @return probability masses of the bins
     */
    double[] getBinDistribution();

    /**
     * Draws a value from the bin {@literal [bi; b{i+1})} uniformly at
     * random.
     *
     * @param index i
     * @return a random value from the interval {@literal [bi; b{i+1})}
     */
    double drawRandomValueFromBin(int index) throws IndexOutOfBoundsException;

    /**
     * Discretizes observation {@code obs}.
     *
     * @param obs observation that is supposed to be discretized
     * @return discretized observation (a value from [0;k-1))
     */
    default double apply(double obs) {
        for (int i = 1; i < getNumberOfBins(); i++) {
            if (obs <= getBorder(i)) {
                return i - 1;
            }
        }
        return getNumberOfBins() - 1;
    }
}

