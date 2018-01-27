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
import org.apache.commons.math3.linear.SingularMatrixException;
    
/**
 * A cluster is collection of observations. Which observations belong to
 * this cluster is defined by a multi-variate Gaussian with mean mu and
 * covariance matrix sigma. Any observation that lies within a given
 * Mahalanobis distance to mu is considered as a member of this cluster.
 *
 * @author Michael Geilke
 */
public abstract class Cluster {
    
    private final int MAX_BUFFER_SIZE = 200;

    protected double mahalanobisDistance = 3.0;
    protected RealMatrix mu;
    protected RealMatrix sigma;
    protected long seed = 1L;

    protected Observation representativeObservation;
    protected Timestamp birth;
    protected Timestamp timestamp;

    protected List<Observation> buffer;
    protected long numObservations;

    /**
     * Creates a candidate with cluster center {@code obs}, which will
     * also be the representative observation of this cluster ({@link
     * #getRepresentativeObservation}).
     * @param obs the cluster center of the multi-variate Gaussian and
     * the representative observation of this cluster
     */
    public Cluster(Observation obs) {
	this.representativeObservation = obs;
	this.timestamp = representativeObservation.getTimestamp();
        this.birth = timestamp;
	this.buffer = new ArrayList<>();
        this.numObservations = 0;
    }

    /**
     * Computes mu and sigma from {@code sample} and associates a
     * multi-variate Gaussian with mean mu and covariance sigma.
     * @param sample a collection of observations from which the sample
     * mean and sample covariance should be computed
     * @param mahalanobisDistance observations with a Mahalonobis
     * distance below {@code mahalanobisDistance} will be considered as
     * members of this cluster.
     */
    public void init(List<Observation> sample, double mahalanobisDistance) {
	this.mahalanobisDistance = mahalanobisDistance;
	if (mu == null) {
	    mu = Util.computeMu(sample);
	}
	sigma = null;
	if (mu != null) {
	    sigma = Util.computeSigma(sample, mu);
	}
    }

    public void addObservation(Observation obs) {
        if (buffer.size() > MAX_BUFFER_SIZE) {
	    buffer.remove(0);
	}
	buffer.add(obs);
        numObservations++;
    }

    /**
     * @param mu the mean
     */
    public void setMu(RealMatrix mu) {
	this.mu = mu;
    }

    /**
     * @return the mean
     */    
    public RealMatrix getMu() {
	return mu;
    }

    /**
     * @param sigma the covariance matrix
     */
    public void setSigma(RealMatrix sigma) {
	this.sigma = sigma;
    }

    /**
     * @return the covariance matrix
     */
    public RealMatrix getSigma() {
	return sigma;
    }

    /**
     * @param seed the seed used for the internal random number
     * generator
     */
    public void setSeed(long seed) {
	this.seed = seed;
    }

    /**
     * Checks membership by testing whether the Mahalonobis distance
     * between the mean and {@code obs} is below the Mahalonobis
     * distance specified during initialization ({@link #init}).
     * @param obs the observation that is tested for membership
     * @return true iff {@code obs} belongs to the cluster
     */
    public boolean membership(Observation obs) {
	return membership(obs.getDistance());
    }
    
    /**
     * Checks membership by testing whether the Mahalonobis between the
     * mean and {@code d} is below the Mahalonobis distance specified
     * during initialization ({@link #init}).
     * @param d the distance vector that is tested for membership
     * @return true iff {@code d} belongs to the cluster
     */
    public boolean membership(Instance d) {

        // compute x
	int numAttributes =  d.dataset().numAttributes();
	double[][] xVector = new double[1][numAttributes];
	int row = 0;
	for (int i = 0; i < numAttributes; i++) {
	    xVector[row][i] = d.value(i);
	}
	RealMatrix x = MatrixUtils.createRealMatrix(xVector);

        // compute \Sigma^{-1}
	RealMatrix inv = null;
	Random random = new Random(seed);
	while (inv == null) {
	    try {
		int dim1 = sigma.getData().length;
		int dim2 = sigma.getData()[0].length;
		double[][] cov = new double[dim1][dim2];
		
		// add noise to ensure that the matrix is invertible
		double epsilon = Math.pow(10, -5);
		for (int i = 0; i < dim1; i++) {
		    for (int j = 0; j < dim2; j++) {
			double noise = random.nextDouble() * epsilon;
			cov[i][j] = sigma.getData()[i][j] + noise;
		    }
		}

		RealMatrix noisySigma = MatrixUtils.createRealMatrix(cov);
		LUDecomposition luDecomposition = new LUDecomposition(noisySigma);
		inv = luDecomposition.getSolver().getInverse();
	    } catch (SingularMatrixException ex) {
		inv = null;
	    }
	}

	// scale inverse matrix using first element
	double f = 1.0 / inv.getData()[0][0];
	inv = inv.scalarMultiply(f);
	
        // compute Mahalonobis distance
	RealMatrix xMinusMu = x.subtract(mu);
	RealMatrix leftPart = xMinusMu.transpose().preMultiply(inv);
	RealMatrix root = leftPart.preMultiply(xMinusMu);
	double distance = Math.sqrt(Math.abs(root.getData()[0][0]));

	return distance <= mahalanobisDistance;
    }

    /**
     * Returns the representative observation of this cluster, which is
     * also the cluster center.
     * @return the representative observation of this cluster
     */
    public Observation getRepresentativeObservation() {
	return representativeObservation;
    }

    /**
     * @return the observations that have been added to the candidate
     */
    public List<Observation> getBuffer() {
	return buffer;
    }

    /**
     * Returns the time point at which the cluster has been created.
     * @return timestamp of cluster creation
     */
    public Timestamp getBirth() {
	return timestamp;
    }

    /**
     * Returns the time point at which the last observation has been
     * added to the candidate.
     * @return the last time an observation has been added
     */
    public Timestamp getTimestamp() {
	return timestamp;
    }

    /**
     * @return the number of observations that have been added to the
     * cluster
     */
    public long getNumberOfObservations() {
        return numObservations;

    }
}
