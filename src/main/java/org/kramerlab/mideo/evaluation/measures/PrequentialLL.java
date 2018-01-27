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

import java.util.List;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonObject;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArrayBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.Instance;

import org.kramerlab.mideo.data.streams.Stream;
import org.kramerlab.mideo.estimators.DensityEstimator;

/**
 * Computes the average prequential log-likelihood of a density
 * estimator on a data stream. Computing the log-likelihood
 * prequentially means that the density estimator first computes the
 * log-likelihood of an instance and then uses this instance for
 * training. The average over all of these values yields the average
 * prequential log-likelihood.
 *
 * {@code PrequentialLL} uses the first instances of the stream only for
 * training and ignores them for the computation of the prequential
 * log-likelihood. The length of this initial prefix is specified by the
 * private member {@code PREFIX_SIZE}.
 * 
 * @author Michael Geilke
 */
public class PrequentialLL implements PerformanceMeasure {

    private static Logger logger = LogManager.getLogger();

    private final int PREFIX_SIZE = 100;

    private Stream stream;
    private DensityEstimator estimator;

    private double ll;
    private long instCounter;
    private long startTime;
    private long endTime;
    private List<Measurement> measurements;

    /**
     * @param stream the data stream on which the average prequential
     * log-likelihood is to be computed
     * @param estimator the density estimator of which the average
     * prequential log-likelihood is to be computed
     */
    public PrequentialLL(Stream stream, DensityEstimator estimator) {
        this.stream = stream;
        this.estimator = estimator;
        this.measurements = new ArrayList<>();
    }

    /**
     * Reads all available instances from the stream and computes the
     * average prequential log-likelihood.
     */
    public void evaluate() {
        this.ll = 0.0;
        this.instCounter = 0;

        while (stream.hasMoreInstances()) {
            Instance inst = stream.nextInstance();
            estimator.update(inst);
            if (instCounter > PREFIX_SIZE) {
                double currentLL = Math.log(estimator.getDensityValue(inst));
                ll += currentLL;
            }
            double preqLL = ll / (instCounter - PREFIX_SIZE);
            logger.info("Instance {} with LL {}", instCounter, preqLL);
            measurements.add(new Measurement(instCounter, preqLL));
            instCounter++;
	}
    }

    /**
     * @return the average prequential log-likelihood over all processed
     * instances.
     */
    public double getResult() {
        return (ll / (instCounter - PREFIX_SIZE));
    }

    /**
     * Returns the average prequential log-likelihoods over time as JSON
     * array. Each element in the array is a tuple consisting of a
     * timestamp, which is the number of processed instances, and the
     * log-likelihood value.
     * 
     * @return a JSON array consisting of JSON objects that have the
     * attribute {@code timestamp} and {@code LL}
     */
    public JsonStructure getAdditionalInformation() {
        JsonArrayBuilder a = Json.createArrayBuilder();
        for (Measurement m : measurements) {
            JsonObjectBuilder o = Json.createObjectBuilder();
            o.add("timestamp", Long.toString(m.getTimestamp()));
            o.add("LL", Double.toString(m.getLL()));
            a.add(o.build());
        }
        return a.build();
    }

    /**
     * {@code Measurement} represents the average log-likelihood
     * measured at a certain timestamp.
     */
    public class Measurement {
        
        private long timestamp;
        private double ll;

        /**
         * @param timestamp is the length of the stream prefix that has
         * already been processed
         * @param ll the average log-likelihood, i.e., the overall
         * log-likelihood divided by the number of processed instances
         */
        public Measurement(long timestamp, double ll) {
            this.timestamp = timestamp;
            this.ll = ll;
        }

        /**
         * @return the length of the stream prefix that has already been
         * processed
         */
        public long getTimestamp() {
            return timestamp;
        }

        /**
         * @return the average log-likelihood, i.e., the overall
         * log-likelihood divided by the number of processed instances
         */
        public double getLL() {
            return ll;
        }
    }
}
