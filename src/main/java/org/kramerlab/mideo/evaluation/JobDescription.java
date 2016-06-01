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
import java.util.HashMap;

import javax.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.kramerlab.mideo.data.streams.Stream;
import org.kramerlab.mideo.estimators.DensityEstimator;

/**
 * JobDescription specifies the environment of a Job. This includes a
 * job index, the file to which the output of a job should be written, a
 * data stream, and a density estimator. To specify the environment,
 * MiDEO uses JSON objects:
 *
 * <pre>{@code 
 * {"jobIndex": 1, jobDescription": {
 *     "outputFile": "job-output.result", 
 *     "evaluation": {
 *         "type": "org.kramerlab.mideo.evaluation.DensityEstimation",
 *         "measure": "PrequentialLL"},
 *     "stream": {
 *         "label": "movielens", 
 *         "type": "org.kramerlab.mideo.data.streams.FileStream", 
 *         "streamSource": "datasets/movielens.arff",
 *         "numInstances": 49282, 
 *         "classIndex": -1}, 
 *     "estimator": {
 *         "type": "org.kramerlab.mideo.estimators.edo.EDO", 
 *         "label": "edo-cc-MC", 
 *         "discreteBaseEstimator.leafClassifier": "MC", 
 *         "ensembleSize": 1,
 *         "seed": 35315},
 *     } 
 * }}</pre>
 *
 * @author Michael Geilke
 */
public class JobDescription {

    private static Logger logger = LogManager.getLogger();

    private JsonObject json;
    private int jobIndex;
    private String outputFile;

    private Stream stream;
    private DensityEstimator estimator;

    /**
     * @param json a job description in JSON format (see above)
     */
    public JobDescription(JsonObject json) {
        this.json = json;
    }

    /**
     * @return a job description in JSON format (see above)
     */
    public JsonObject getJson() {
        return json;
    }
    
    /**
     * @param jobIndex an identifier of the job, which should be unique
     * within its scope (e.g., within an EVAL file)
     */
    public void setJobIndex(int jobIndex) {
        this.jobIndex = jobIndex;
        logger.info(jobIndex);
    }

    /**
     * @return an identifier of the job
     */
    public int getJobIndex() {
        return jobIndex;
    }

    /**
     * @param outputFile the file to which the output of the
     * corresponding job should be written
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
        logger.info(outputFile);
    }

    /**
     * @return the file to which the output of the corresponding job
     * should be written
     */
    public String getOutputFile() {
        return outputFile;
    }

    /**
     * @param stream the data stream on which the computation is
     * performed
     */
    public void setStream(Stream stream) {
        this.stream = stream;
    }

    /**
     * @return the data stream on which the computation is performed
     */
    public Stream getStream() {
	return stream;
    }

    /**
     * @param estimator the density estimator on which the computation
     * is based
     */
    public void setEstimator(DensityEstimator estimator) {
        this.estimator = estimator;
    }

    /**
     * @return the density estimator on which the computation
     * is based
     */
    public DensityEstimator getEstimator() {
	return estimator;
    }
}
