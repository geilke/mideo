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
package org.kramerlab.mideo.data.filters;

import java.util.List;
import java.util.ArrayList;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.DiscreteRandomVariable;
import org.kramerlab.mideo.core.discretization.DiscretizationType;
import org.kramerlab.mideo.core.discretization.OnlineDiscretization;
import org.kramerlab.mideo.core.discretization.PartitionIncremental;

/**
 * Acts as a wrapper that discretizes streams on the fly. For each
 * instance, a given variable is discretized into a predefined number of
 * bins.
 *
 * @author Michael Geilke
 */
public class DiscretizationFilter implements Filter {

    private int INITIAL_SAMPLE_SIZE = 50;

    private OnlineDiscretization discretization;
    private List<Double> valueSample;
    private InstancesHeader origHeader;
    private InstancesHeader discHeader;
    private RandomVariable variable;
    private int numBins;
    private long numberOfInstances;

    /**
     * Creates the target header, in which the given variable is
     * discretized into the number of requested bins.
     * @param header the source header
     * @param var the variable that is supposed to be discretized
     * @param numBins the number of bins in which the variable {@code
     * var} should be discretized
     */
    public void init(InstancesHeader header, RandomVariable var, int numBins) {
        this.origHeader = header;
        this.variable = var;
        this.numBins = numBins;
        this.valueSample = new ArrayList<>();
        this.numberOfInstances = 0;
        this.discretization = null;

        // create the target header where the variable var has been
        // discretized into numBins many bins.
        String relationName = origHeader.getRelationName();
        List<Attribute> atts = new ArrayList<>();
        for (int i = 0; i < header.numAttributes(); i++) {
            String attName = origHeader.attribute(i).name();
            if (attName.equals(var.getAttribute().name())) {
                List<String> vals = new ArrayList<>();
                for (int j = 0; j < numBins; j++) {
                    vals.add("bin" + Integer.toString(j));
                }
                Attribute discAtt = new Attribute(attName, vals);
                atts.add(discAtt);
                this.variable = new DiscreteRandomVariable(attName, discAtt);
            } else {
                atts.add(origHeader.attribute(i));
            }
        }
        Instances ds = new Instances(relationName, atts, 0);
        this.discHeader = new InstancesHeader(ds);
        this.discHeader.setClassIndex(header.classIndex());
    }

    @Override
    public InstancesHeader getSourceHeader() {
	return origHeader;
    }

    @Override
    public InstancesHeader getTargetHeader() {
	return discHeader;
    }

    /**
     * @return the random variable to be discretized
     */
    public RandomVariable getDiscretizedVariable() {
        return variable;
    }

    /**
     * Forwards the given instance to the internal discretization object
     * for training.
     * @param inst an instance that is used by the discretization object
     * for training
     */
    public void addObservation(Instance inst) {
        Double obs = null;

        // extract the value of the attribute that is supposed to be
        // discretized
        String attName = variable.getName();
        for (int i = 0; i < origHeader.numAttributes(); i++) {
            if (attName.equals(origHeader.attribute(i).name())) {
                obs = inst.value(i);    
                break;
            }
        }

        // If not enough instance have been observed yet, we only
        // collect instances. When enough instances are available, the
        // discretization object is initialized.
        if (numberOfInstances < INITIAL_SAMPLE_SIZE) {
            valueSample.add(obs);

        } else if (discretization == null) {
            int bins = numBins;
            DiscretizationType type = DiscretizationType.EQUAL_WIDTH;
            this.discretization = new PartitionIncremental(bins, type,
                                                           valueSample);
        } else {
            discretization.addObservation(obs);
        }
        numberOfInstances++;
    }

    /**
     * Discretizes the given instance using the internal discretization
     * object.
     * @param inst the instance to be discretized
     */
    public Instance apply(Instance inst) {
        DenseInstance discInst = new DenseInstance(inst);
        discInst.setDataset(discHeader);
        Attribute discAtt = variable.getAttribute();
        for (int i = 0; i < discInst.numAttributes(); i++) {
            if (inst.attribute(i).name().equals(discAtt.name())) {
                // We have to check the equality of the attributes by
                // name, since the values are different.
                double discVal = discretization.apply(inst.value(i));
                discInst.setValue(i, discVal);
            } else {
                discInst.setValue(i, inst.value(i));
            }
        }
	return discInst;
    }
}
