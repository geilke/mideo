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
 */
package org.kramerlab.mideo.core;

import java.util.function.BiFunction;

/**
 * This class provides several static methods that are related to Chernoff
 * bounds.
 *
 * The following passage is from [Rajeev Motwani and Prabhakar
 * Raghavan. Randomized Algorithms. Cambridge University Press, 1995.]:
 *
 * <blockquote><pre>
 * Let X1, X2 , ... , Xn be independent Poisson trials, such that, 
 * for 1 &lt;= i &lt;= n, Pr[Xi = 1] = pi, where 0 &lt; pi &lt; 1. Then, 
 * for X = \sum_{i=1}^n Xi, mu = E[X] = \sum_{i=1}^n pi, and any 
 * lambda &gt; 0, 
 * Pr(X &gt; (1+lambda) * mu) &lt; [e^lambda / (1+lambda)^(1+lambda)]^mu.
 * If 0 &lt; lambda &lt;= 1, then 
 * Pr(X &lt; (1-lambda) * mu) &lt; e^-((mu * lambda^2) / 2).</pre>
 *</blockquote>
 *
 * @author Michael Geilke
 */
public class ChernoffBounds {

    /**
     * Performs a binary search to find the largest lambda within the
     * given precision for which the upper Chernoff bound holds.
     *
     * @param mu the expectation of mu
     * @param precision this methods searches for lambda until lambda
     *     does not change within the given precision, i.e., |lambda_old
     *     - lambda_new| &lt; precision
     * @param delta the bound has to be larger or equal to 1-delta
     * @return lambda lambda &gt; 0
     */
    public static double findLambdaForUpperBound(double mu, double precision,
						 double delta) {
	double lower = 0;
	double upper = precision;
	
	// find starting point
	while (ChernoffBounds.upperBound(upper, mu) < 1 - delta) {
	    upper *= 2;
	}
	
        // binary search
        boolean maximize = true;
        return binarySearch(ChernoffBounds::upperBound, lower, upper, 
                            maximize, mu, precision, delta);
    }

    /**
     * Performs a binary search to find the smallest lambda within the given
     * precision for which the lower Chernoff bound holds.
     *
     * @param mu the expectation of mu
     * @param precision this methods searches for lambda until lambda
     *     does not change within the given precision, i.e., |lambda_old
     *     - lambda_new| &lt; precision
     * @param delta the bound has to be larger or equal to 1 - delta
     * @return lambda 0 &lt; lambda &lt;= 1
     */
    public static double findLambdaForLowerBound(double mu, double precision,
						 double delta) {
	double lower = precision;
	double upper = 1;

        // binary search
        boolean maximize = false;
        return binarySearch(ChernoffBounds::lowerBound, lower, upper, 
                            maximize, mu, precision, delta);
    }

    /**
     * Performs a binary search to find the smallest lambda within the
     * given precision for which the given bound holds.
     * 
     * @param bound lower or upper Chernoff bound
     *     (ChernoffBounds::lowerBound or ChernoffBounds::upperBound)
     * @param lowerLambda the lower bound of the search
     * @param upperLambda the upper bound of the search
     * @param maximize true iff a larger lamda yields a higher
     * probability (in terms of the given bound)
     * @param mu mu of Chernoff bound
     * @param precision acceptable deviation from true value
     * @param delta Chernoff delta
     * @return the smallest lambda within the given precision for which
     *     the given bound holds
     */
    public static double binarySearch(BiFunction<Double, Double, Double> bound,
                                      double lowerLambda, double upperLambda, 
                                      boolean maximize, double mu, 
                                      double precision, double delta) {
        boolean done = false;
        double lower = lowerLambda;
        double upper = upperLambda;

	while (!done) {
            // next lambda
	    double mid = upper - lower;
            mid = (mid / 2) + lower;
	    if (bound.apply(mid, mu) < 1 - delta) {
                if (maximize) {
                    lower = mid;
                } else {
                    upper = mid;
                }
	    } else {
                if (maximize) {
                    upper = mid;
                } else {
                    lower = mid;
                }
	    }
            
	    // If the deviation between lambda and previousLambda is below
	    // precision, we stop.
	    double d = upper - lower;
	    if (Math.abs(d) < precision) {
		done = true;
	    }
	}

        return maximize ? upper : lower;
    }

    /**
     * Computes an upper bound for the probability that X is larger than
     * (1 + lambda) \cdot mu.
     *
     * @param lambda Chernoff lambda
     * @param mu Chernoff mu
     * @return upper bound for Pr(X &gt; (1 + lambda) * mu) 
     */
    public static double upperBound(double lambda, double mu) {
        double numerator = Math.pow(Math.E, lambda);
        double denominator = Math.pow((1 + lambda), (1 + lambda));
	return Math.pow(numerator / denominator, mu);
    }

    /**
     * Computes a lower bound for the probability that X is smaller than
     * (1 - lambda) \cdot mu.
     *
     * @param lambda Chernoff lambda
     * @param mu Chernoff mu
     * @return lower bound for Pr(X &lt; (1 - lambda) * mu) 
     */
    public static double lowerBound(double lambda, double mu) {
	return Math.pow(Math.E, -((mu * Math.pow(lambda, 2))) / 2);
    }
}
