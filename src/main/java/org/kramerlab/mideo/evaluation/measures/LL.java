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
package org.kramerlab.mideo.evaluation.measures;

import com.yahoo.labs.samoa.instances.Instance;

import org.kramerlab.mideo.estimators.DensityEstimator;
import org.kramerlab.mideo.data.streams.Stream;

/**
 * LL computes the average log-likelihood of an estimator based on a
 * given stream.
 *
 * @author Michael Geilke
 */
public class LL implements PerformanceMeasure {

    private Stream stream;
    private DensityEstimator estimator;

    private double ll;

    /**
     * LL uses half of the instances from <code>stream</code> for
     * training and the remaining instances for computing the average
     * log-likelihood.
     *
     * @param stream from which instances are drawn
     * @param estimator the estimator that is trained and evaluated with
     * instances from the stream
     */
    public LL(Stream stream, DensityEstimator estimator) {
        this.stream = stream;
        this.estimator = estimator;
        this.ll = 0.0;
    }

    @Override
    public void evaluate() {
        // We use half of the instances for training and half of the
        // instance to measure the performance.
        long numTrainInsts = stream.getNumberOfInstances() / 2;
        long numTestInsts = stream.getNumberOfInstances();
        numTestInsts -= numTrainInsts;

        // train
        long trainInsts = 0;
        while (stream.hasMoreInstances() && trainInsts < numTrainInsts) {
            estimator.update(stream.nextInstance());
            trainInsts++;
        }
        
        // evaluate
        long testInsts = 0;
        while (stream.hasMoreInstances() && testInsts < numTestInsts) {
            Instance inst = stream.nextInstance();
            ll += Math.log(estimator.getDensityValue(inst));
            testInsts++;
	}
        ll = ll / testInsts;
    }

    @Override
    public double getResult() {
        return ll;
    }
}
