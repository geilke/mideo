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
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonArray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.kramerlab.mideo.core.Option;
import org.kramerlab.mideo.core.Options;
import org.kramerlab.mideo.core.Configurable;
import org.kramerlab.mideo.data.streams.Stream;
import org.kramerlab.mideo.estimators.DensityEstimator;

/**
 * A JobCenter provides facilities to evaluate algorithms of the MiDEO
 * framework via command line. It expects an EVAL file, the index of the
 * first job to run, and the index of the last job to run.
 *
 * An EVAL file contains job descriptions in JSON format (see {@link
 * JobDescription}). Each job description has an identifier by which the
 * job center will refer to the job.
 *
 * @author Michael Geilke
 */
public class JobCenter {

    private static Logger logger = LogManager.getLogger();

    public static final String HELP = "Syntax: " +
	"-f FILE -startIndex INTEGER -endIndex INTEGER\n\n\n" + 
	"FILE a .jobs file\n" +
	"START_INDEX an integer i addressing the i-th job in FILE\n" +
	"END_INDEX an integer j addressing the j-th job in FILE\n\n" +
	"every job in the interval [i;j] will be run";

    private String evalFile;
    private int startIndex;
    private int endIndex;

    protected Map<Integer, Job> jobs;

    /**
     * @param evalFile file in EVAL format
     * @param startIndex first job that is supposed to be run
     * @param endIndex last job that is supposed to be run
     */
    public JobCenter(String evalFile, int startIndex, int endIndex) {
        this.evalFile = evalFile;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.jobs = new HashMap<>();
    }

    /**
     * @return the EVAL file with the job descriptions
     */
    public String getEvalFile() {
        return evalFile;
    }

    /**
     * @return all jobs having a job index that lies in the interval
     * {@literal [getStartIndex(); getEndIndex()]} are run.
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @return all jobs having a job index that lies in the interval
     * {@literal [getStartIndex(); getEndIndex()]} are run.
     */
    public int getEndIndex() {
        return endIndex;
    }

    /**
     * Creates jobs from the job descriptions given by {@code jobArray}.
     * @param jobArray a list of job descriptions in JSON format
     */
    public void prepareJobs(JsonArray jobArray) 
            throws IllegalArgumentException {

        for (int i = 0; i < jobArray.size(); i++) {
            JsonObject jsonJob = jobArray.getJsonObject(i);
            JsonObject jsonDesc = jsonJob.getJsonObject("jobDescription");

            // parse job description
            JobDescription desc = new JobDescription(jsonDesc);
            desc.setJobIndex(jsonDesc.getInt("jobIndex"));
            desc.setOutputFile(jsonDesc.getString("outputFile"));
            JsonObject section = jsonDesc.getJsonObject("stream");
            desc.setStream((Stream) configure(section));
            section = jsonDesc.getJsonObject("estimator");
            desc.setEstimator((DensityEstimator) configure(section));

            // create job, attach job description, and add job to jobs
            Job job = (Job) configure(jsonDesc.getJsonObject("evaluation"));
            job.setJobDescription(desc);
            this.jobs.put(desc.getJobIndex(), job);
            logger.info("job created");
        }
    }

    private Configurable configure(JsonObject json) 
            throws IllegalArgumentException {
        String type = json.getString("type");
        Configurable object = null;
        try {
            object = (Configurable) Class.forName(type).newInstance();
            configure(object.getOptions(), json);
        } catch (Exception ex) {
            String msg = "Could not load object: " + type + ".\n";
            msg += ex.toString();
            throw new IllegalArgumentException(msg);
        }
        return object;
    }

    private void configure(Options options, JsonObject json) 
            throws IllegalArgumentException {
        Options.OptionType<String> strOptions = options.getStringOptions();
        Options.OptionType<Integer> intOptions = options.getIntegerOptions();
        Options.OptionType<Float> floatOptions = options.getFloatOptions();
        Options.OptionType<Boolean> boolOptions = options.getBooleanOptions();

        for (String name : json.keySet()) {
            if (strOptions.hasOption(name)) {
                String value = json.getString(name);
                strOptions.getOption(name).setValue(value);
            } else if (intOptions.hasOption(name)) {
                int value = json.getInt(name);
                intOptions.getOption(name).setValue(value);
            } else if (floatOptions.hasOption(name)) {
                double value = json.getJsonNumber(name).doubleValue();
                floatOptions.getOption(name).setValue((float) value);
            } else if (boolOptions.hasOption(name)) {
                boolean value = json.getBoolean(name);
                boolOptions.getOption(name).setValue(value);
            }
        }
    }

    /**
     * Reads the job description from the EVAL file and makes them
     * available to the job center.
     * @throws IllegalArgumentException if the file does not exists or
     * is not an EVAL file
     */
    public void init() throws IllegalArgumentException {
        try {
            JsonReader reader = Json.createReader(new FileReader(evalFile));
            JsonStructure structure  = (JsonArray) reader.read();
            if (structure instanceof JsonArray) {
                JsonArray jobArray = (JsonArray) structure;
                logger.info("json file is parsed");
                prepareJobs(jobArray);
                logger.info("jobs are prepared");
            } else {
                String msg = "The file does not contain an array of jobs.";
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }
        } catch (FileNotFoundException ex) {
            String msg = "Could not find file: " + evalFile;
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Runs all jobs having a job index that lies in the interval
     * {@literal [getStartIndex(); getEndIndex()]}.
     */
    public void run() {
        for (int i = startIndex; i <= endIndex; i++) {
            logger.info("Job index: {}", i);
            if (jobs.containsKey(i)) {
                jobs.get(i).run();
                try {
                    jobs.get(i).writeOutput();
                } catch (IOException ex) {
                    String msg = "Could not write job results of job ";
                    msg += Integer.toString(i) + "\n";
                    msg += ex.toString();
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }
                jobs.put(i, null);  // delete job to free memory
            }
        }
    }

    /**
     * Parses the CLI (command line interface) parameters and creates
     * the corresponding job center. It expects three arguments: "-f",
     * "-startIndex", "-endIndex", where "-f" is the job file (see class
     * documentation for details), "-startIndex" is the first job that
     * is supposed to be run, and "-endIndex" is the last job that is
     * supposed to be run.
     *
     * Example: {@literal -f FILE -startIndex 1 -endIndex 15}
     *
     * @param args the command line arguments where "-f" specifies the
     * job file, "-startIndex" specifies the index of the first job,
     * "-endIndex" specifies the index of the last job
     * @return the job center created from {@code args}
     * @throws IllegalArgumentException if the parameters do not match
     * the specified syntax
     */
    public static JobCenter parseCLI(String[] args) 
        throws IllegalArgumentException, NumberFormatException {

        String jobsFile = null;
        Integer startIndex = null;
        Integer endIndex = null;

        // parse command line arguments
        String currentParameter = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && currentParameter == null) {
                currentParameter = args[i].trim();
            } else if (args[i].startsWith("-") && currentParameter != null) {
                String msg = "Error while parsing arguments: ";
                msg += "Did not expect " + args[i];
                throw new IllegalArgumentException(msg);
            } else {
                if ("-f".equals(currentParameter)) {
                    jobsFile = args[i].trim();
                } else if ("-startIndex".equals(currentParameter)) {
                    startIndex = Integer.parseInt(args[i].trim());
                }  else if ("-endIndex".equals(currentParameter)) {
                    endIndex = Integer.parseInt(args[i].trim());
                }
                currentParameter = null;
            }
        }

        // create job center if all parameters have been specified
        if (jobsFile != null && startIndex != null && endIndex != null) {
            return new JobCenter(jobsFile, startIndex, endIndex);
        } else {
            String msg = "The command line arguments are incomplete.";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Parses the CLI (command line interface) parameters and creates
     * the corresponding job center. It expects three arguments: "-f",
     * "-startIndex", "-endIndex", where "-f" is the job file (see class
     * documentation for details), "-startIndex" is the first job that
     * is supposed to be run, and "-endIndex" is the last job that is
     * supposed to be run.
     *
     * Example: {@literal -f FILE -startIndex 1 -endIndex 15}
     *
     * @param args the command line arguments where "-f" specifies the
     * job file, "-startIndex" specifies the index of the first job,
     * "-endIndex" specifies the index of the last job
     * @throws IllegalArgumentException if the parameters do not match
     * the specified syntax
     */
    public static void main(String[] args) {
        JobCenter jobCenter = JobCenter.parseCLI(args);
        jobCenter.init();
        jobCenter.run();
    }
}
