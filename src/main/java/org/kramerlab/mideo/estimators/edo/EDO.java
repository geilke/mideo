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
package org.kramerlab.mideo.estimators.edo;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.Utils;
import org.kramerlab.mideo.core.Option;
import org.kramerlab.mideo.core.Options;
import org.kramerlab.mideo.core.Configurable;
import org.kramerlab.mideo.estimators.Module;
import org.kramerlab.mideo.estimators.ModuleDetection;
import org.kramerlab.mideo.estimators.MetaInformation;
import org.kramerlab.mideo.estimators.DensityEstimator;
import org.kramerlab.mideo.estimators.EstimatorType;
import org.kramerlab.mideo.estimators.trees.HoeffdingTreeCR;
import org.kramerlab.mideo.estimators.occd.OCCDEstimator;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;

/**
 * {@code EDO} is an implementation of the online density estimators
 * proposed by Geilke et al. It is able to estimate densities of the
 * form {@literal f(X1, ..., Xk | Y1, ..., Yl}} using various variants
 * of classifier chains: estimators consisting of a single classifier
 * chain, an ensemble of classifier chains, or an ensemble of weighted
 * classifier chains. For details, we would like to refer you to the
 * paper:
 *
 * <p>Michael Geilke, Andreas Karwath, Eibe Frank and Stefan Kramer
 * <br /> Online Estimation of Discrete, Continuous, and Conditional
 * Joint Densities using Classifier Chains<br /> In: Data Mining and
 * Knowledge Discovery, Springer 2017.</p>
 *
 * To model variable interdependencies more explicitly, we added
 * modules to EDO, as initially proposed by Geilke et al.:
 *
 * <p>Michael Geilke, Andreas Karwath, and Stefan Kramer. <br> A
 * Probabilistic Condensed Representation of Data for Stream
 * Mining. <br> In: Proceedings of the DSAA 2014, pages 297-303, IEEE
 * 2014.</p>
 *
 * The implementation of the inference operations is currently still
 * in progress and not fully available yet.
 *
 * @author Michael Geilke
 */
public class EDO implements DensityEstimator, Configurable {

    public static final int MIN_NUM_INSTANCES = 40;

    private static Logger logger = LogManager.getLogger();

    private Options options;
    
    private Option<Integer> ensembleSize = new Option<>(
        "ensembleSize",
        "the number of classifier chains",
        1,
        s -> (s > 0));

    private Option<Boolean> uniformWeights = new Option<>(
        "uniformWeights",
        "specifies whether to use uniform weight or non-uniform weights",
        true);

    private Option<Integer> seed = new Option<>(
        "seed",
        "seed use for random number generators",
        1,
        s -> (s > 0));

    private Option<String> leafClassifier = new Option<>(
        "discreteBaseEstimator.leafClassifier",
        "the leaf classifier that is supposed to be used in the " + 
        "Hoeffding tree. Possible choices: [MC | NB | NBA], where " +
        "MC is MajorityClass, NB is NaiveBayes, and NBAdaptive is " + 
        "NaiveBayesAdaptive",
        "MC",
        s -> "MC".equals(s) || "NB".equals(s) || "NBAdaptive".equals(s));

    private Option<Integer> numBins = new Option<>(
        "continuousBaseEstimator.numBins",
        "the number of bins used for the class probability estimator",
        10,
        b -> (b > 0));

    private Option<Integer> maxNumberOfKernels = new Option<>(
        "continuousBaseEstimator.maxNumberOfKernels",
        "the maximal number of kernels allowed. If the that number is " + 
        "exceeded, the kernels are compressed to fewer kernels.",
        10000,
        m -> (m > 0));

    private Random random;

    private InstancesHeader header;
    private List<RandomVariable> targetVars;
    private List<RandomVariable> conditionedVars;

    private String name;
    private ModuleDetection moduleDetection;
    private List<Module> modules;
    private long instanceCounter;
    private List<Instance> buffer;

    public EDO() {
        this.options = new Options();
        options.getIntegerOptions().addOption(ensembleSize);
        options.getBooleanOptions().addOption(uniformWeights);
        options.getIntegerOptions().addOption(seed);
        options.getStringOptions().addOption(leafClassifier);
        options.getIntegerOptions().addOption(numBins);
        options.getIntegerOptions().addOption(maxNumberOfKernels);

        this.name = "";
    }

    public String getName() {
	return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public void init(InstancesHeader header, List<RandomVariable> targetVars, 
                     List<RandomVariable> condVars)
        throws UnsupportedConfiguration {
        
        // check whether the arguments match this type of estimator
        DensityEstimator.super.init(header, targetVars, condVars);

        this.header = header;
        this.targetVars = targetVars;
        this.conditionedVars = condVars;

        this.random = new Random(seed.getValue());
        this.modules = new ArrayList<>();
        this.buffer = new ArrayList<>();
        this.instanceCounter = 0;

        this.moduleDetection = new ModuleDetection();
        moduleDetection.init(header, targetVars, conditionedVars);
    }

    private void createModules() throws UnsupportedConfiguration {
	moduleDetection.prepareModules();
	for (int i = 0; i < moduleDetection.getNumberOfModules(); i++) {
	    MetaInformation meta = moduleDetection.getMetaInformation(i);

	    // configure chain-based estimator
	    int size = ensembleSize.getValue();
	    boolean weights = uniformWeights.getValue();
	    ChainBasedEstimator est = new ChainBasedEstimator(size, weights);
	    est.setSeed(seed.getValue());
	    // discrete base estimator
	    String leafCl = leafClassifier.getValue();
	    HoeffdingTreeCR htTemplate = new HoeffdingTreeCR(leafCl);
	    est.setBaseEstimator(EstimatorType.DISC_X1_I_Y1___Yl, htTemplate);
	    // continuous base estimator
	    int bins = numBins.getValue();
	    int maxKernels = maxNumberOfKernels.getValue();
	    OCCDEstimator occd = new OCCDEstimator(bins, maxKernels);
	    est.setBaseEstimator(EstimatorType.CONT_X1_I_Y1___Yl, occd);
	    est.init(meta.getHeader(),
		     meta.getTargetVariables(),
		     meta.getConditionedVariables());
	    
	    // create module
	    Module module = new Module();
	    module.init(meta.getHeader(),
			meta.getTargetVariables(),
			meta.getConditionedVariables());
	    module.setName(getName() + "-module" + Integer.toString(i));
	    module.setDensityEstimator(est);
	    for (Instance bufferedInst : buffer) {
		module.getDensityEstimator().update(bufferedInst);
	    }
	    modules.add(module);
	}
    }

    public List<Module> getModules() {
	return modules;
    }
    
    @Override
    public List<EstimatorType> getSupportedTypes() {
        return Collections.singletonList(EstimatorType.X1___Xk_I_Y1___Yl);
    }

    @Override
    public List<RandomVariable> getConditionedVariables() {
        return conditionedVars;
    }

    @Override
    public List<RandomVariable> getTargetVariables() {
        return targetVars;
    }

    @Override
    public void update(Instance inst) {
	// COMMENT: I think that module support works, if the update
	// method of the base estimators can deal with instances having
	// more instances than necessary!
        moduleDetection.update(inst);
        if (instanceCounter < MIN_NUM_INSTANCES) {
            buffer.add(inst);

        } else if (instanceCounter == MIN_NUM_INSTANCES) { 
	    try {
		createModules();
		buffer.clear();
	    } catch (UnsupportedConfiguration ex) {
		logger.error(ex.toString());
		System.exit(1);
	    }
	    
        } else if (instanceCounter > MIN_NUM_INSTANCES) {
            for (Module module : modules) {
                module.getDensityEstimator().update(inst);
            }
        }
        instanceCounter++;

        for (Module module : modules) {
            module.getDensityEstimator().update(inst);
        }
    }
   
    @Override
    public double getDensityValue(Instance inst) {
        double densityValue = 1.0;
        for (Module module : modules) {
            densityValue *= module.getDensityEstimator().getDensityValue(inst);
        }
        if (modules.size() == 0) {
            densityValue = 0.0;
        }
        return densityValue;
    }

    @Override
    public List<Instance> getSample() {
	return buffer;
    }
    
    @Override
    public JsonObject getModelCharacteristics() {
        JsonObjectBuilder o = Json.createObjectBuilder();
        int moduleCounter = 0;
        for (Module module : modules) {
            String moduleId = "module-" + Integer.toString(moduleCounter);
            DensityEstimator est = module.getDensityEstimator();
            o.add(moduleId, est.getModelCharacteristics());
        }
        return o.build();
    }
}
