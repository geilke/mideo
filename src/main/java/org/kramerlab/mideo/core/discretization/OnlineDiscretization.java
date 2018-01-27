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
 * Since the borders are subject to changes, the intervals defined by
 * the borders are further divided into safe-to-use regions and
 * so-called soft borders (TODO: ADD REFERENCE). Observations of a 
 * safe-to-use region will stay in that bin with high probability, 
 * whereas observation of soft borders can be assigned to a neighboring 
 * bin with a certain likelihood.
 *
 * <pre>
 * b0                 b1                 b2                             bk
 * |-------------[----|--]-----------[---|-----]----....----------------|
 * </pre>
 *
 * <p>@TODO sampling support</p> 
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
    
    /**
     * Checks whether observation {@code obs} belongs to a soft border.
     *
     * @param obs value to be tested
     * @return true iff {@code obs} belongs to a soft border
     */
    default boolean belongsToSoftBorder(double obs) {
        // parameters for Chernoff
        double precision = 0.000001;
        double delta = 0.90;

        // further parameters
        long I = 0;
        int k = getNumberOfBins();
        for (int i = 0; i < k; i++) {
            I += getBinCount(i);
        }

        // This is a simplified version of the approach presented in the
        // paper. Instead of the smallest and largest element close to
        // the border, we take the border itself. The smallest and
        // largest border are ignored, as these are the -infinity and
        // +infinity.
        boolean isInSomeBin = false;
        double attributeRange = getBorder(k-1) - getBorder(1);
        for (int i = 1; i < k - 1; i++) {
            if (obs >= getBorder(i) && obs <= getBorder(i+1)) {
                isInSomeBin = true;
                // determine lower and upper bound
                long total = 0;
                for (int j = 0; j < getNumberOfBins(); j++) {
                    total += getBinCount(j);
                }
                double p = 0.0;
                for (int j = 0; j <= i; j++) {
                    p += getBinCount(j);
                }
                p /= total;
                double l = findLambdaForLowerBound(I * p, precision, delta);
                p = 0.0;
                for (int j = 0; j <= i - 1; j++) {
                    p += getBinCount(j);
                }
                p /= total;
                double u = findLambdaForUpperBound(I * p, precision, delta);
                // check whether obs is outside the soft border
                double x_iA = getBorder(i + 1);
                double x_iB = getBorder(i);
                // The computed soft border need to be scaled to the
                // actual range, since we assumed a range of [0;1] here.
                l = (l / I) * attributeRange;
                u = (u / I) * attributeRange;
                if (obs > x_iB + u && obs < x_iA - l) {
                    return false;
                }
            }
        }

        return (isInSomeBin) ? true : false;
    }
}

