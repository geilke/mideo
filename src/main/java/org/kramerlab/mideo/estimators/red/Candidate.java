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

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.LUDecomposition;

/**
 * A candidate is a precursor of a representative. It will be turned
 * into a representative, if it gathers enough distance vectors.
 *
 * @author Michael Geilke
 */
public class Candidate extends Cluster {
        
    /**
     * Creates a candidate with cluster center {@code obs}, which will
     * also be the representative observation of this cluster ({@link
     * #getRepresentativeObservation}).
     * @param obs the cluster center of the multi-variate Gaussian and
     * the representative observation of this cluster
     */
    public Candidate(Observation obs) {
        super(obs);
    }

    /**
     * Adds observation {@code obs} to the cluster and updates the
     * timestamp to {@code obs.getTimestamp}.
     * @param obs the observation that is supposed to be added to the
     * cluster
     */
    // @Override
    // public void addObservation(Observation obs) {
    //     super.addObservation(obs);
    // }
}
