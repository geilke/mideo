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
package org.kramerlab.mideo.evaluation;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArrayBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.Instance;

import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.Option; 
import org.kramerlab.mideo.core.Options; 
import org.kramerlab.mideo.data.streams.Stream;
import org.kramerlab.mideo.estimators.DensityEstimator;
import org.kramerlab.mideo.evaluation.measures.PerformanceMeasure;
import org.kramerlab.mideo.evaluation.measures.LL;
import org.kramerlab.mideo.evaluation.measures.PrequentialLL;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;

/** 
 * Evaluates a density estimator with a performance measure on a
 * stream. It only uses the interfaces {@link DensityEstimator}, {@link
 * Stream}, {@link PerformanceMeasure}, so that any class implements the
 * corresponding interface is compatible.
 *
 * @author Michael Geilke
 */
public class DensityEstimation extends Job {

    private static Logger logger = LogManager.getLogger();
   
    private Option<String> measure = new Option<>(
        "measure",
        "is the evaluation measure that should be used to evaluate the " +
        "performance of the density estimator. Possible values are : " +
        "LL, PrequentialLL",
        "LL",
        m -> "LL".equals(m) || "PrequentialLL".equals(m));

    private Options options;
    private Stream stream;
    private DensityEstimator estimator;

    private PerformanceMeasure performance;
    private double elapsedTime;

    public DensityEstimation() {
        options = new Options();
        options.getStringOptions().addOption(measure);
    }

    @Override
    public Options getOptions() {
        return options;
    }

    /**
     * Initializes the data stream and the density estimator.
     */
    public void init() {
        // initialize stream
        try {
            this.stream = getJobDescription().getStream();
            stream.init();
        } catch (Exception ex) {
            logger.error("Could not read stream.\n" + ex.toString());
            System.exit(1);
        }

        // random variables of the stream
        List<RandomVariable> targetVars = new ArrayList<>();
        List<RandomVariable> condVars = new ArrayList<>();
        List<RandomVariable> vars = stream.getRandomVariables();
        for (RandomVariable rv : vars) {
            targetVars.add(rv);
        }

        // density estimator
        try {
            estimator = getJobDescription().getEstimator();
            estimator.init(stream.getHeader(), targetVars, condVars);
        } catch (UnsupportedConfiguration ex) {
            logger.error(ex.toString());
            System.exit(1);
        }
    }
    
    /**
     * Evaluates the specified density estimator using the specified
     * performance measure.
     */
    @Override
    public void run() {
        init();

        // identify performance measure
        if ("PrequentialLL".equals(measure.getValue())) {
            this.performance = new PrequentialLL(stream, estimator);
        } else {
            this.performance = new LL(stream, estimator);
        }

        // evaluate the performance and measure the time
        int oneSecond = 1000;
        long startTime = System.currentTimeMillis();
        performance.evaluate();
        long endTime = System.currentTimeMillis();

        this.elapsedTime = ((double) (endTime - startTime)) / oneSecond;
    }

    @Override
    public void writeOutput() throws IOException {

        // Format:
        // "jobDescription": {_}
        // "result": {"measure": _, "elapsedTime": _, "modelDescription": _}

        // preparing JSON output file
	File file = new File(getJobDescription().getOutputFile());
	FileWriter fw = new FileWriter(file.getAbsolutePath());
	BufferedWriter bw = new BufferedWriter(fw);
	JsonWriter jsonWriter = Json.createWriter(bw);
        JsonObjectBuilder o = Json.createObjectBuilder();
        o.add("jobDescription", getJobDescription().getJson());

        // create result object
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("measure", performance.getResult());
        result.add("elapsedTime", elapsedTime);
        result.add("modelDescription", estimator.getModelCharacteristics());
        o.add("result", result);

        // write result object and close file
	jsonWriter.writeObject(o.build());
	jsonWriter.close();
	bw.close();
    }
}
