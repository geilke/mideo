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
package org.kramerlab.mideo.estimators.occd;

import java.util.List;
import java.util.ArrayList;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.Utils;
import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.data.filters.DiscretizationFilter;
import org.kramerlab.mideo.estimators.EstimatorType;
import org.kramerlab.mideo.estimators.DensityEstimator;
import org.kramerlab.mideo.estimators.trees.HoeffdingTreeCR;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;

/**
 * OCCDEstimator is an online density estimator for univariate,
 * continuous, conditional densities. It is an online version of the
 * approach described in "Conditional Density Estimation with Class
 * Probability Estimators" by Eibe Frank and Remco Bouckaert and "Fast
 * Conditional Density Estimation for Quantitative Structure-Activity
 * Relationships" by Fabian Buchwald, Tobias Girschick, Stefan Kramer,
 * and Eibe Frank.
 * 
 * @author Michael Geilke
 */
public class OCCDEstimator implements DensityEstimator {

    private Logger logger = LogManager.getLogger();

    private HoeffdingTreeCR discreteEstimator;
    private List<EstimatorType> supportedTypes;
    private List<RandomVariable> targetVariables;    
    private List<RandomVariable> conditionedVariables;
    private int targetAttribute;

    /**
     * This is the online discretization for the target variable. The
     * target variable will be discretized into numBins many bins.
     */
    private DiscretizationFilter discretization;
    private int numBins;

    /**
     * buffer is the instance buffer, which is many mainly used for
     * re-computing estimateMu and estimateSigma.
     */
    private List<BufferedInstance> buffer;
    private double estimateMu;
    private double estimateSigma;
    private int maxBufferSize = 100;

    private GaussianMixture kernels;
    private int maxKernels = 10000; // TODO How to choose that?
    private long[] n;
    private long numberOfInstances;
    private long numberOfDiscardedInstances;  // due to soft borders
    
    /**
     * @param numBins the number of discretization bins
     * @param maxKernels the maximal number of kernels allowed. If the
     * that number is exceeded, the kernels are compressed to fewer
     * kernels.
     */
    public OCCDEstimator(int numBins, int maxKernels) {
        // kernels
        this.maxKernels = maxKernels;
        double defaultSigma = 1.0;
        this.kernels = new GaussianMixture(maxKernels, defaultSigma);

        // initialize n (with laplace correction)
        this.numberOfInstances = 0;
	this.numBins = numBins;
        this.n = new long[numBins];
        for (int i = 0; i < n.length; i++) {
            this.n[i] = 1;
            this.numberOfInstances++;
        }
        this.numberOfDiscardedInstances = 0;

        // density estimator
        this.discreteEstimator = new HoeffdingTreeCR();
        this.supportedTypes = new ArrayList<>();
        this.supportedTypes.add(EstimatorType.CONT_X1_I_Y1___Yl);

        // instance buffer
        this.buffer = new ArrayList<>();
        this.estimateMu = 0.0;
        this.estimateSigma = 0.0;
    }

    /**
     * @return number of discretization bins
     */
    public int getNumberOfBins() {
        return numBins;
    }

    /**
     * @return the number of currently used kernels
     */
    public int determineNumberOfKernels() {
        return kernels.determineNumberOfKernels();
    }

    /**
     * @return number of instances discarded due to soft borders
     */
    public long getNumberOfDiscardedInstances() {
        return numberOfDiscardedInstances;
    }

    @Override
    public void init(InstancesHeader header, List<RandomVariable> targetVars,
                     List<RandomVariable> condVars) 
        throws UnsupportedConfiguration {

        // check whether the arguments match this type of estimator
        DensityEstimator.super.init(header, targetVars, condVars);
        this.targetVariables = targetVars;
        this.conditionedVariables = condVars;
        String targetAtt = targetVariables.get(0).getAttribute().name();
        int headerIndex  = Utils.determineAttributeIndex(header, targetAtt);
        this.targetAttribute = headerIndex;

        // configure discretization filter
        RandomVariable target = targetVars.get(0);
        this.discretization = new DiscretizationFilter();
        this.discretization.init(header, target, getNumberOfBins());
        
        // prepare underlying discrete density estimator
        InstancesHeader discHeader = discretization.getTargetHeader();
        List<RandomVariable> discTargetVars = new ArrayList<>();
        discTargetVars.add(discretization.getDiscretizedVariable());
        this.discreteEstimator.init(discHeader, discTargetVars, condVars);
    }

    @Override
    public List<EstimatorType> getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public List<RandomVariable> getTargetVariables() {
        return targetVariables;
    }

    @Override
    public List<RandomVariable> getConditionedVariables() {
        return conditionedVariables;
    }

    public void update(Instance inst) {
        if (numberOfInstances < maxBufferSize + numBins) {
            discretization.addObservation(inst);
            numberOfInstances++;
        } else if (discretization.belongsToSoftBorder(inst)) {
            discretization.addObservation(inst);
            numberOfDiscardedInstances++;
        } else {
            // discretize instance
            discretization.addObservation(inst);
            Instance discInst = discretization.apply(inst);
            buffer.add(new BufferedInstance(inst, discInst));
            // add instance to buffer
            if (buffer.size() >= maxBufferSize) {
                processInstances();
                buffer.clear();
            }
        }
    }

    /**
     * Re-computing the bandwidth using the instances from the buffer
     * and clear the buffer afterwards.
     */
    public void processInstances() {
        RandomVariable targetVar = getTargetVariables().get(0);

        // update model
        for (BufferedInstance inst : buffer) {
            Instance orig = inst.getOriginalInstance();
            Instance disc = inst.getDiscretizedInstance();
            discreteEstimator.update(disc);

            // update n
            int v = (int) disc.value(targetAttribute);
            n[v]++;

            // Add target value to list of kernels. Notice that the
            // variance is set when required. We actually want to use
            // the best possible value.
            KernelFactor weight = new KernelFactor(numBins);
            weight.setMultiplier(v, 1);
            Kernel kernel = new Kernel(orig.value(targetAttribute));
            kernel.setWeight(weight);
            kernels.add(kernel);
            numberOfInstances++;
        }

        // Update mu and sigma and set the new sigma value as new
        // default for Gaussian mixture model.
	updateMuAndSigma();
        kernels.setDefaultSigma(estimateSigma);

        logger.info("number of processed instances: {}", numberOfInstances);
    }

    /**
     * Re-compute mu and sigma. Please notice that we compute mu_X and
     * sigma_X from a fixed size buffer that only contains the recent
     * instances. We should consider other strategies here that also
     * take older instances into account.
     */
    private void updateMuAndSigma() {
        // determine counts for the instances in the buffer
        long instancesForEstimate = 0;
        double[] nPrime = new double[getNumberOfBins()];
        for (int i = 0; i < nPrime.length; i++) {
            nPrime[i] = 1;
            instancesForEstimate++;
        }
        for (BufferedInstance buffInst : buffer) {
            Instance disc = buffInst.getDiscretizedInstance();
            int bin = (int) disc.value(targetAttribute);
            nPrime[bin]++;
            instancesForEstimate++;
        }

        // determine the mu and the weight of the kernels emerging from
        // the buffered instances
        List<KernelSetting> yi_and_w = new ArrayList<>();
        for (BufferedInstance buffInst : buffer) {
            Instance orig = buffInst.getOriginalInstance();
            Instance disc = buffInst.getDiscretizedInstance();
            double yi = orig.value(targetAttribute);
            int bin = (int) disc.value(targetAttribute);
            double pr = discreteEstimator.getDensityValue(disc);
            double weight = instancesForEstimate * (pr / n[bin]);
            yi_and_w.add(new KernelSetting(yi, weight));
        }

        // compute mu
        double mu_X = 0.0;
        for (KernelSetting setting : yi_and_w) {
            double yi = setting.getMu();
            double weight = setting.getWeight();
            mu_X += weight * yi;
        }
        mu_X /= instancesForEstimate;
	
        // compute sigma
        double sigma_X = 0.0;
        for (KernelSetting setting : yi_and_w) {
            double yi = setting.getMu();
            double weight = setting.getWeight();
            sigma_X += weight * Math.pow((yi - mu_X), 2);
        }
        sigma_X /= (instancesForEstimate - 1);
        sigma_X = Math.sqrt(sigma_X);
        // The smoothing parameter should be in the order of
        // O(n^(1/4)). Since we compute sigma from a small sample, we
        // choose a larger constant to correct for the small sample.
        double smoothing = 1 * Math.pow(instancesForEstimate, 0.25);
        sigma_X = sigma_X / smoothing;

        // add correction in case sigma_X equals 0 (e.g., mu = 0.0)
        if (sigma_X == 0) {
            sigma_X = 0.1 / smoothing;
        }

        // update estimates for mu and sigma
        this.estimateMu = mu_X;
        this.estimateSigma = sigma_X;
    }

    @Override
    public double getDensityValue(Instance inst) {
        // c_{y_i} is the bin (i.e., class) containing the target value
        // y_i, prob_y[i] := p(c_{y_i} | X) is the predicted probability
        // of c_{y_i} given X, numberOfInstances is the total number of
        // target values, n[i] is the number of target values in bin
        // c_{y_i}
        
        // w(y_i | X) = n \cdot \frac{p(c_{y_i} | X)}{n_{c_{y_i}}}
        Instance discInstance = discretization.apply(inst);
        double[] prob_y = discreteEstimator.getObservationCounts(discInstance);
	double[] w = new double[getNumberOfBins()];
	if (w.length > prob_y.length) {
            // If some values have not been observed yet, perform
            // laplace correction.
            double[] ps = discreteEstimator.getObservationCounts(discInstance);
            String out = "";
	    prob_y = new double[w.length];
	    for (int i = 0; i < prob_y.length; i++) {
                if (i < ps.length) {
                    prob_y[i] = ps[i];
                } else {
                    prob_y[i] = 1.0;  // assign a count of 1
                }
	    }
        }
		
	// compute weight vector
        prob_y = Utils.normalize(prob_y);
        for (int i = 0; i < w.length; i++) {
            w[i] = numberOfInstances * (prob_y[i] / n[i]);
        }

        double density = kernels.evaluate(inst.classValue(), w);

        String att = getTargetVariables().get(0).getAttribute().name();        
        logger.info("Attribute {}, density value {}", att, density);

        return density;
    }

    @Override
    public JsonObject getModelCharacteristics() {
        JsonObjectBuilder o = Json.createObjectBuilder();
        o.add("kernels", determineNumberOfKernels());
        o.add("discardedInstances", getNumberOfDiscardedInstances());
        return o.build();
    }

    public class BufferedInstance {

        private Instance origInstance;
        private Instance discInstance;

        public BufferedInstance(Instance origInstance, Instance discInstance) {
            this.origInstance = origInstance;
            this.discInstance = discInstance;
        }
        
        public Instance getOriginalInstance() {
            return origInstance;
        }

        public Instance getDiscretizedInstance() {
            return discInstance;
        }
    }

    public class KernelSetting {

        private double mu;
        private double weight;

        public KernelSetting(double mu, double weight) {
            this.mu = mu;
            this.weight = weight;
        }
        
        public double getMu() {
            return mu;
        }

        public double getWeight() {
            return weight;
        }
    }
}
