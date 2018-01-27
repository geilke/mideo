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
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;

import org.kramerlab.mideo.core.Options;
import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.estimators.edo.EDO;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;

/**
 *
 * @author Michael Geilke
 */
public class Layer {
    
    protected Options options;
    protected InstancesHeader header;
    protected List<RandomVariable> targetVars;
    protected List<RandomVariable> condVars;
        
    protected List<Candidate> candidates;
    protected List<Representative> representatives;
    protected Timestamp timestamp;
    protected long numObservations;

    protected RealMatrix defaultMu;
    protected RealMatrix defaultSigma;
    
    private Cluster lastUpdatedCluster;

    public Layer(Options options, InstancesHeader header, List<RandomVariable>
                 targetVars, List<RandomVariable> condVars) {
        this.options = options;
        this.header = header;
        this.targetVars = targetVars;
        this.condVars = condVars;

	this.candidates = new ArrayList<>();
	this.representatives = new ArrayList<>();
	this.timestamp = new Timestamp(0);
        this.numObservations = 0;
    }

    /**
     * @param mu will be used as default mean for the clusters
     */
    public void setDefaultMu(RealMatrix mu) {
	this.defaultMu = mu;
    }

    /**
     * @param sigma will be used as default covariance for the clusters
     */
    public void setDefaultSigma(RealMatrix sigma) {
	this.defaultSigma = sigma;
    }
    
    /**
     * @return the cluster that has been updated last
     */
    public Cluster getLastUpdatedCluster() {
	return lastUpdatedCluster;
    }

    /**
     * @return all candidates that are part of the layer
     */
    public List<Candidate> getCandidates() {
	return candidates;
    }

    /**
     * @return all representatives that are part of the layer
     */
    public List<Representative> getRepresentatives() {
	return representatives;
    }

    /**
     * @return the number of observations that have been added to the
     * layer
     */
    public long getNumberOfObservations() {
	return numObservations;
    }
    
    /**
     * @param r a representative of this layer
     * @return the fraction of observations that reached {@code r}
     */
    public double getWeight(Representative r) {
        return ((double) r.getNumberOfObservations()) / numObservations;
    }

    public Cluster findCluster(Instance distanceVector) throws Exception {

	for (int i = 0; i < representatives.size(); i++) {
	    Representative r = representatives.get(i);
	    if (r.membership(distanceVector)) {
		return r;
	    }
	}

	for (Candidate c : candidates) {
	    if (c.membership(distanceVector)) {
		return c;
	    }
	}

	return null;
    }

    public void addObservation(Observation obs) {

	boolean found = false;
        List<Representative> sortedReps = sortRepresentatives(obs);

        // find closest representative and add obs as observation
	if (sortedReps.size() > 0) {
            Representative r  = sortedReps.get(0);
            if (r.membership(obs)) {
                found = true;
                r.addObservation(obs);
                lastUpdatedCluster = r;
            }
        } 

	// If no representative has been found, check whether the
	// observation belongs to an existing candidate.
	if (!found) {
	    for (Candidate c : candidates) {
		if (c.membership(obs)) {
		    c.addObservation(obs);
		    found = true;
		    lastUpdatedCluster = c;
                    break;
		}
	    }
	}

	// If neither a matching representative nor a matching candidate
	// has been found, create a new candidate from obs.
	if (!found) {
	    candidates.add(createCandidate(obs, sortedReps));
	}

	// check whether candidates can be turned into representatives
        int thresholdBecomingRepresentative = options.getIntegerOptions()
            .getOption("thresholdBecomingRepresentative").getValue();
	List<Candidate> toBeRemoved = new ArrayList<>();
	for (Candidate c : candidates) {
            long clusterSize = c.getNumberOfObservations();
	    if (clusterSize > thresholdBecomingRepresentative) {
		representatives.add(convertToRepresentative(c));
		toBeRemoved.add(c);
	    }
	}
	candidates.removeAll(toBeRemoved);

	// perform garbage collection regularly
        int tresholdGarbageCollection = options.getIntegerOptions()
            .getOption("tresholdGarbageCollection").getValue();
	if (numObservations % tresholdGarbageCollection == 0) {
            garbageCollection();
        }

	timestamp.setValue(timestamp.getValue() + 1);
        numObservations++;
    }

    /**
     * Creates a sorted list of representatives. They are sorted
     * according to their distance to the given observation, where the
     * representative with largest distance have the lowest indices and
     * the one with smallest distance have the highest indices.
     * @param obs the observation
     * @return a sorted list of representatives
     */
    private List<Representative> sortRepresentatives(Observation obs) {

        // computes the distances between obs and all representatives
        Map<Representative, Double> distances = new HashMap<>();
	for (Representative r : representatives) {
	    Observation o = r.getRepresentativeObservation();    
	    Instance d1 = obs.getDistance();
	    Instance d2 = o.getDistance();
	    int numAttributes = d1.dataset().numAttributes();
	    double d = 0.0;
	    for (int j = 0; j < numAttributes; j++) {
		d += Math.abs(d1.value(j) - d2.value(j));
	    }
	    distances.put(r, d);
	}
        
        // sort the representatives according to their distances to obs
	// (in ascending order)
        List<Representative> sortedReps = new ArrayList<>();
        for (Representative r: representatives) {
            sortedReps.add(r);
        }
        Collections.sort(sortedReps, (Representative r1, Representative r2)
                         -> distances.get(r1).compareTo(distances.get(r2)));

        return sortedReps;
    }

    private Candidate createCandidate(Observation obs,
                                      List<Representative> sortedReps) {
        // get good neighbors
        int helpingNeighbors = options.getIntegerOptions()
            .getOption("helpingNeighbors").getValue();
        int k = Math.min(helpingNeighbors, representatives.size());
        List<Representative> neighbors = new ArrayList<>();
        for (int i = 0;  i < k; i++) {
            neighbors.add(sortedReps.get(i));
        }

        // collect observations from neighbors
        List<Observation> sample = new ArrayList<>();
        for (Representative n : neighbors) {
            List<Observation> buffer = n.getBuffer();
            for (Observation o : buffer) {
                sample.add(o);
            }
        }

        // create candidate from these observations
        Candidate c = new Candidate(obs);
        c.setSeed(options.getIntegerOptions().getOption("seed").getValue());
        List<Observation> mu = new ArrayList<>();
        mu.add(obs);
        c.setMu(Util.computeMu(mu));
        if (sample.size() > 0) {
            c.init(sample, 
                   options.getFloatOptions().getOption("mahalonobisDistance")
                   .getValue());
        } else {
            c.setMu(defaultMu);
            c.setSigma(defaultSigma);
        }
        
        return c;
    }

    /**
     * Remove candidates and representatives that have not been used for
     * a long time.
     */
    private void garbageCollection() {
        int maxTimeBeingUnused = options.getIntegerOptions()
            .getOption("maxTimeBeingUnused").getValue();

        // candidates
        List<Candidate> candsToBeRemoved = new ArrayList<>();
        for (Candidate c : candidates) {
            Timestamp t = c.getTimestamp();
            if (timestamp.difference(t) >  maxTimeBeingUnused) {
                candsToBeRemoved.add(c);
            }
        }
        candidates.removeAll(candsToBeRemoved);
        // representatives
	List<Representative> repsToBeRemoved = new ArrayList<>();
        for (Representative r : representatives) {
            Timestamp t = r.getTimestamp();
            if (timestamp.difference(t) > maxTimeBeingUnused) {
                repsToBeRemoved.add(r);
            }
        }
        representatives.removeAll(repsToBeRemoved);
    }

    /**
     * Creates a representative from the given candidate.
     * @param c the candidate that should be converted to a representative
     * @return a representative that has the same representative
     * observation and the same observations as {@code c}
     */
    private Representative convertToRepresentative(final Candidate c) {
        // create density estimator
        EDO edo = new EDO();
        try {    
            edo.init(header, targetVars, condVars);
        } catch (UnsupportedConfiguration ex) {
            throw new RuntimeException(ex.toString());
        }
        
        // create representative
	Observation obs = c.getRepresentativeObservation();
	Representative r = new Representative(obs, edo);
	r.setMu(c.getMu());
	r.setSeed(options.getIntegerOptions().getOption("seed").getValue());
	r.init(c.getBuffer(), 
               options.getFloatOptions().getOption("mahalonobisDistance")
               .getValue());

	return r;
    }
}
