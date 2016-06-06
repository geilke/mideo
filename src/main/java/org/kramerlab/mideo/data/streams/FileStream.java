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
package org.kramerlab.mideo.data.streams;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.io.Reader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.streams.ArffFileStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.kramerlab.mideo.core.Configurable;
import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.Utils;
import org.kramerlab.mideo.core.Option;
import org.kramerlab.mideo.core.Options;

/**
 * A file stream reads data instances from an ARFF file. The path to
 * that file can be set via the {@code streamSource} option. The class
 * index and the number of instances that are to be expected can be set
 * via the options {@code classIndex} and {@code numInstances}.
 * 
 * Changes to these options are only effective when {@code init} or
 * {@code restart} is invoked.

 * @author Michael Geilke
 */
public class FileStream implements Stream {

    private static Logger logger = LogManager.getLogger();

    private Options options;

    /**
     * The path to the ARFF file that contains the data instances.
     */
    private Option<String> streamSource = new Option<>(
        "streamSource",
        "path to ARFF file",
        "");

    /**
     * The index of the class attribute. It is either -1, which refers
     * to the last attribute, or a positive integer, which should be
     * smaller than the number of attributes.
     */
    private Option<Integer> classIndex = new Option<>(
        "classIndex",
        "class index of data stream",
        -1,
        index -> (index >= -1));

    /**
     * The number of instances that should be read from the file. It
     * should be smaller than or equal to the number of data instances
     * of the file specified by {@code streamSource}.
     */
    private Option<Integer> numInstances = new Option<>(
        "numInstances",
        "the number of instances that will be read from the stream",
        1000,
        numInsts -> (numInsts >= 0));

    private Reader reader;
    private Instances stream;
    private InstancesHeader header;
    private List<RandomVariable> variables;
    private long instanceCounter;
    private long numberOfInstances;

    public FileStream() {
        options = new Options();
        options.getStringOptions().addOption(streamSource);
        options.getIntegerOptions().addOption(classIndex);
        options.getIntegerOptions().addOption(numInstances);
    }

    /**
     * @param file a path to an ARFF file
     * @param classIndex the index of the class attribute. It is either
     * -1, which refers to the last attribute, or a positive integer,
     * which should be smaller than the number of attributes.
     * @param numInstances the number of instances that should be read
     * from {@code file}. It should be smaller than or equal to the
     * number of data instances contained in {@code file}.
     */
    public FileStream(String file, int classIndex, long numInstances) {
        this();
	this.streamSource.setValue(file);
        this.classIndex.setValue(classIndex);
        this.numInstances.setValue((int) numInstances);
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public void init() throws Exception {

        // read instances from file
        File f = new File(streamSource.getValue());
        FileInputStream fileStream = new FileInputStream(f.getAbsolutePath());
        this.reader = new BufferedReader(new InputStreamReader(fileStream)); 
        int size = 1;  // size does not seem to be used in Instances
        this.stream = new Instances(reader, size, classIndex.getValue());
	this.instanceCounter = 0;

	// Determine dataset information
	this.header = new InstancesHeader(stream);
        this.variables = Utils.getRandomVariables(header);
    }

    @Override
    public List<RandomVariable> getRandomVariables() {
        return variables;
    }

    @Override
    public long getNumberOfInstances() {
	return numInstances.getValue();
    }

    @Override
    public InstancesHeader getHeader() {
	return this.header;
    }

    @Override
    public boolean hasMoreInstances() {
        boolean moreAvailable = stream.readInstance(reader);
        boolean moreAllowed = instanceCounter < getNumberOfInstances();
        return moreAvailable && moreAllowed;
    }

    @Override
    public Instance nextInstance() {
	instanceCounter++;
        boolean instAvail = false;

        // check whether more instances are available and read them from
        // the file
        if (stream.numInstances() <= instanceCounter - 1) {
            instAvail = stream.readInstance(reader);
        } else {
            instAvail = true;
        }
        // read instance
        if (instAvail) {
            return stream.instance((int) instanceCounter - 1);
        } else {
            return null;
        }
    }

    @Override
    public boolean isRestartable() {
	return true;
    }

    @Override
    public void restart() throws Exception {
        init();
    }
}
