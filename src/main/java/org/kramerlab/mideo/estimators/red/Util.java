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

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.LUDecomposition;

/**
 * Helper functions for estimating the parameters of multi-variate
 * Gaussians.
 *
 * @author Michael Geilke
 */
public class Util {
    
    /**
     * Computes the sample mean from observations.
     * @param sample observations from which the sample mean is computed
     * @return the sample mean if at least one instance is available;
     * otherwise, {@code null}.
     */
    public static RealMatrix computeMu(List<Observation> sample) {
	if (sample.size() > 0) {
            // dataset information
	    Instances dataset = sample.get(0).getDistance().dataset();
	    int numAttributes = dataset.numAttributes();
            // compute the sample mean
	    double[][] mu = new double[1][numAttributes];
	    int row = 0;
	    for (int i = 0; i < numAttributes; i++) {
		mu[row][i] = 0.0;
	    }
	    for (int i = 0; i < numAttributes; i++) {
		for (int j = 0; j < sample.size(); j++) {
		    mu[row][i] += sample.get(j).getDistance().value(i);
		}
	    }
	    for (int i = 0; i < numAttributes; i++) {
		mu[row][i] = mu[row][i] / sample.size();
	    }
	    return MatrixUtils.createRealMatrix(mu);
	} else {
	    return null;
	}
    }

    /**
     * Computes the sample covariance from observations.
     * @param sample observations from which the sample covariance is
     * computed
     * @param muMatrix the sample mean of {@code sample}
     * @return the sample covariance if at least one instance is available;
     * otherwise, {@code null}.
     */
    public static RealMatrix computeSigma(List<Observation> sample,
					  RealMatrix muMatrix) {
	if (sample.size() > 0) {
	    double[][] mu = muMatrix.getData();
	    int row = 0;	    
            // dataset information
	    Instances dataset = sample.get(0).getDistance().dataset();
	    int numAttributes = dataset.numAttributes();
            // compute the sample covariance
	    double[][] sigma = new double[numAttributes][numAttributes];
	    for (int i = 0; i < numAttributes; i++) {
		for (int j = 0; j < numAttributes; j++) {
		    double N = sample.size();
		    for (int k = 0; k < N; k++) {
			double xki = sample.get(k).getDistance().value(i);
			double xi = mu[row][i];
			double xkj = sample.get(k).getDistance().value(j);
			double xj = mu[row][j];
			sigma[i][j] = (1 / N - 1) * (xki - xi) * (xkj - xj);
		    }
		}
	    }
	    return MatrixUtils.createRealMatrix(sigma);
	} else {
	    return null;
	}
    }
}
