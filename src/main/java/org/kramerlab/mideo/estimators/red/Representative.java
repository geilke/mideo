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

import java.util.List;
import java.util.ArrayList;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import org.kramerlab.mideo.estimators.edo.EDO;

/**
 *
 * @author Michael Geilke
 */
public class Representative extends Cluster {

    private final int MAX_BUFFER_SIZE = 1000;

    protected EDO estimator;
    protected double weight;

    protected long numObservations;
    protected double ll;

    /** 
     * Creates a candidate with cluster center {@code obs}, which will
     * also be the representative observation of this cluster ({@link
     * #getRepresentativeObservation}).
     * @param obs the cluster center of the multi-variate Gaussian and
     * the representative observation of this cluster
     * @param mahalanobisDistance defines the maximal allowed distance
     * for an observation, such that this instance still belongs to the
     * cluster
     * @param edo the underlying density estimator, which has been fully
     * initialized
     */
    public Representative(Observation obs, EDO edo) {
        super(obs);
	this.mahalanobisDistance = mahalanobisDistance;
        this.estimator = edo;        
    }

    @Override
    public void init(List<Observation> sample, double mahalanobisDistance) {
        super.init(sample, mahalanobisDistance);

        ll = 0.0;
        numObservations = 0;

        // update estimator with given observations
	for (Observation obs : sample) {
	    this.addObservation(obs);
	}
    }
    
    /**
     * Updates the underlying density estimator with the given
     * observation.
     * @param obs a new observation
     */
    public void addObservation(Observation obs) {
        super.addObservation(obs);

	estimator.update(obs.getDistance());
	timestamp = obs.getTimestamp();

	// update average log likelihood
        if (numObservations > 500) { // TODO 
            ll += estimator.getDensityValue(obs.getDistance());
        }
    }

    /**
     * Computes the density value of the given observation and updates
     * the timestamp of the representative.
     * @param obs an observation
     * @return the density value of the observation {@code obs}
     */
    public double getDensityValue(Observation obs) {
	timestamp = obs.getTimestamp();
	return estimator.getDensityValue(obs.getDistance());
    }

    /**
     * Computes the density value of the given observation. Please
     * notice that the timestamp of the representative is not updated.
     * @param obs an observation
     * @return the density value of the observation {@code obs}
     */
    public double getDensityValue(Instance d) {
	return estimator.getDensityValue(d);
    }

    public void setWeight(double weight) {
	this.weight = weight;
    }

    public double getWeight() {
	return weight;
    }

    /**
     * @return the average log likelihood of the underlying density
     * estimator
     */
    public double getAvgLL() {
	return ll / numObservations;
    }
}
