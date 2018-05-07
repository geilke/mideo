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

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.math3.linear.RealMatrix;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import org.kramerlab.mideo.core.Utils;
import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.DiscreteRandomVariable;
import org.kramerlab.mideo.core.ContinuousRandomVariable;
import org.kramerlab.mideo.core.Option;
import org.kramerlab.mideo.core.Options;
import org.kramerlab.mideo.core.Configurable;
import org.kramerlab.mideo.estimators.DensityEstimator;
import org.kramerlab.mideo.estimators.EstimatorType;
import org.kramerlab.mideo.estimators.trees.HoeffdingTreeCR;
import org.kramerlab.mideo.estimators.occd.OCCDEstimator;
import org.kramerlab.mideo.estimators.edo.EDO;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;

/**
 *
 * @author Michael Geilke
 */
public class RED implements DensityEstimator, Configurable {

    private static Logger logger = LogManager.getLogger();

    /**
     * The number of instance that are used to initialize the density
     * estimate.
     */
    private final int INITIALIZATION_BATCH = 250;
    private List<Instance> buffer;
    private boolean isInitialized;

    private Options options;
    
    private Option<Integer> seed = new Option<>(
        "seed",
        "seed use for random number generators",
        1,
        s -> (s > 0));

    private Option<Integer> norm = new Option<>(
        "norm",
        "the norm, which is used for the distance measure",
        2,
        n -> (n > 0));
    
    private Option<Integer> numLandmarks = new Option<>(
        "numLandmarks",
        "the number of landmarks",
        1,
        l -> (l > 0));

    private Option<Float> mahalonobisDistance = new Option<>(
        "mahalonobisDistance",
        "the mahalonobisDistance used for the Gaussians",
        3.f,
        m -> (m > 0));

    private Option<Integer> thresholdBecomingRepresentative = new Option<>(
        "thresholdBecomingRepresentative",
        "the number of instances that a candidate has to gather," +
        "until a candidate is turned into a representative",
        200,
        t -> (t > 0));

    private Option<Integer> helpingNeighbors = new Option<>(
        "helpingNeighbors",
        "the number of neighbors that are used to create a candidate",
        3,
        n -> (n > 0));

    private Option<Integer> tresholdGarbageCollection = new Option<>(
        "tresholdGarbageCollection",
        "the number of instances until garbage collection kicks in",
        1000,
        t -> (t > 0));
    
    private Option<Integer> maxTimeBeingUnused = new Option<>(
        "maxTimeBeingUnused",
        "the maximum number of instances a cluster is allowed to be inactive",
        10000,
        t -> (t > 0));

    private Random random;
    
    private InstancesHeader sourceHeader;

    /**
     * For each variable, it maintains the minimal value that has been
     * observed. minValues[0] is the minimal value for the variable that
     * has the index 0 in sourceHeader.
     */
    private double[] minValues;

    /**
     * For each variable, it maintains the maximal value that has been
     * observed. maxValues[0] is the maximal value for the variable that
     * has the index 0 in sourceHeader.
     */
    private double[] maxValues;

    private InstancesHeader targetHeader;
    private List<RandomVariable> targetVars;
    private List<RandomVariable> condVars;

    protected List<Instance> landmarks;
    protected Layer layer;
    protected List<Decoder> decoders;

    protected long numObservations;

    protected double correctionFactor;
    protected long numInstancesForCorrectionFactor;

    public RED() {
        this.options = new Options();
        options.getIntegerOptions().addOption(seed);
        options.getIntegerOptions().addOption(norm);
        options.getIntegerOptions().addOption(numLandmarks);
        options.getIntegerOptions().addOption(thresholdBecomingRepresentative);
        options.getIntegerOptions().addOption(helpingNeighbors);
        options.getIntegerOptions().addOption(tresholdGarbageCollection);
        options.getIntegerOptions().addOption(maxTimeBeingUnused);
        options.getFloatOptions().addOption(mahalonobisDistance);
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public void init(InstancesHeader header, List<RandomVariable> targetVars, 
                     List<RandomVariable> condVars)
        throws UnsupportedConfiguration {

        this.random = new Random(seed.getValue());
        this.sourceHeader = header;
        this.targetVars = targetVars;
        this.condVars = condVars;

        this.buffer = new ArrayList<>();
        this.isInitialized = false;
    }

    @Override
    public List<EstimatorType> getSupportedTypes() {
        return Collections.singletonList(EstimatorType.X1___Xk_I_Y1___Yl);
    }

    @Override
    public List<RandomVariable> getConditionedVariables() {
        return condVars;
    }

    @Override
    public List<RandomVariable> getTargetVariables() {
        return targetVars;
    }

    @Override
    public void update(Instance inst) {
        int requiredInstances = INITIALIZATION_BATCH + numLandmarks.getValue();

        if (!isInitialized && buffer.size() < requiredInstances) {
            // we do not have enough instances for initialization yet
            buffer.add(inst);

        } else if (!isInitialized) {
            // we have enough instance to initialize the estimator
            Instances ds = buffer.get(0).dataset();
            InstancesHeader sourceHeader = new InstancesHeader(ds);

            // collect dataset statistics
            this.minValues = new double[sourceHeader.numAttributes()];	
            this.maxValues = new double[sourceHeader.numAttributes()];
            for (int i = 0; i < sourceHeader.numAttributes(); i++) {
                minValues[i] = 1.0;
                maxValues[i] = 1.0;
                Attribute att = sourceHeader.attribute(i);
                for (int j = 0; j < buffer.size(); j++) {
                    Instance bufferInst = buffer.get(j);
                    minValues[i] = Math.min(minValues[i], bufferInst.value(i));
                    maxValues[i] = Math.max(maxValues[i], bufferInst.value(i));
                }
            }

            selectLandmarks();
            createVectorSpace();
            initLayer();
            isInitialized = true;
            numInstancesForCorrectionFactor = 0;

        } else {
            // update estimators
            Instance d = computeDistanceVector(inst);
            Timestamp t = new Timestamp(numObservations + 1);
            layer.addObservation(new Observation(inst, d, t));

            // update decoders
            for (Decoder decoder : decoders) {
                decoder.update(inst);
            }

            numObservations++;
        }
    }

    /**
     * Selects the landmarks that span the vector space.
     */
    private void selectLandmarks() {
        this.landmarks = new ArrayList<>();

        // the amount by which each attribute value is increased when
        // generating another landmark
        double[] stepSize = new double[sourceHeader.numAttributes()];
        for (int i = 0; i < sourceHeader.numAttributes(); i++) {
            if (minValues[i] - maxValues[i] > 0) {
        	stepSize[i] = (maxValues[i] - minValues[i]);
                stepSize[i] /= numLandmarks.getValue();
            } else {
        	stepSize[i] = 0.0;
            }   
        }
        double maxStepSize = 0.1;

        // create landmarks
        for (int i = 0; i < numLandmarks.getValue(); i++) {
            Instance inst = new DenseInstance(sourceHeader.numAttributes());
            inst.setDataset(sourceHeader);
            for (int j = 0; j < sourceHeader.numAttributes(); j++) {
        	Attribute att = sourceHeader.attribute(j);
        	if (j < i) {
        	    if (att.numValues() > 0) {  // nominal attribute
        		inst.setValue(att, 0.0 + (i - j + 1) % att.numValues());
        	    } else {
        		inst.setValue(att, 0.0 + maxStepSize * (i - j + 1));
        	    }		 
        	} else if (j == i) {
        	    if (att.numValues() > 0) {  // nominal attribute
        		inst.setValue(att, att.numValues() - 1);
        	    } else {
        		inst.setValue(att, 1.0);
        	    }
        	} else {
        	    inst.setValue(att, 0.0);
        	}
            }
            landmarks.add(inst);
        }

        // landmark 0
        Instance inst = new DenseInstance(sourceHeader.numAttributes());
        inst.setDataset(sourceHeader);
        for (int j = 0; j < sourceHeader.numAttributes(); j++) {
            Attribute att = sourceHeader.attribute(j);
            inst.setValue(att, 0.0);
        }
        landmarks.add(inst);

        logger.info("{} landmarks", () -> landmarks.size());
        for (Instance l : landmarks) {
            logger.info("Landmark {}", () -> l.toString());
        }
    }

    /**
     * Creates the vector space spanned by the landmarks.
     */
    private void createVectorSpace() {
        // create an instance header for the vector space induced by the
        // landmarks and an instance header for the decoders
        String relationName = "distances_to_landmarks";
        ArrayList<Attribute> atts = new ArrayList<>();
        for (int i = 0; i < landmarks.size(); i++) {
            atts.add(new Attribute("landmark" + Integer.toString(i)));
        }
        Instances ds = new Instances(relationName, atts, 0);
        this.targetHeader = new InstancesHeader(ds);
        this.targetHeader.setClassIndex(targetHeader.numAttributes() - 1);

        // train decoders and build instance headers
        decoders = new ArrayList<>();
        for (int i = 0; i < sourceHeader.numAttributes(); i++) {
            Decoder decoder = new Decoder();
            decoder.init(targetHeader, sourceHeader.attribute(i), i);
            decoders.add(decoder);
        }

        // initialize correcting factor
        this.correctionFactor = 1.0;
        for (int i = 0; i < sourceHeader.numInstances(); i++) {
            updateCorrectionFactor(computeDistanceVector(buffer.get(i)));
        }

    }

    /**
     * Use the instances in the buffer to set the parameters of the
     * layer and update the layer.
     */
    private void initLayer() {
        List<RandomVariable> vars = Utils.getRandomVariables(targetHeader);
        List<RandomVariable> targetVars = new ArrayList<>();
        List<RandomVariable> condVars = new ArrayList<>();
        for (int i = 0; i < vars.size(); i++) {
            targetVars.add(vars.get(i));
        }
        this.layer = new Layer(options, targetHeader, targetVars, condVars);

        // create sample to compute sample mean and sample covariance
        List<Observation> sample = new ArrayList<>();
        for (int i = 0; i < buffer.size(); i++) {
            Instance d = computeDistanceVector(buffer.get(i));
            Timestamp t = new Timestamp(i);
            Observation obs = new Observation(buffer.get(i), d, t);
            sample.add(obs);
        }

        // compute default mu and sigma and set them as default for the
        // layer
        RealMatrix mu = Util.computeMu(sample);
        RealMatrix sigma = null;
        if (mu != null) {
            sigma = Util.computeSigma(sample, mu);
        }
        layer.setDefaultMu(mu);
        layer.setDefaultSigma(sigma);

        // update layer with the observations
        for (Observation obs: sample) {
            layer.addObservation(obs);
        }
    }

    /**
     * Transforms the given instance to the vector space spanned by the
     * landmarks.
     * @param instance the instance to be transformed
     * @return the transformed instance
     */
    private Instance computeDistanceVector(Instance instance) {
        Instance inst = new DenseInstance(targetHeader.numAttributes());
        inst.setDataset(targetHeader);
        for (int i = 0; i < landmarks.size(); i++) {
            double d = 0.0;
            for (int j = 0; j < sourceHeader.numAttributes(); j++) {
        	if (sourceHeader.attribute(j).isNumeric()) {
                    double stepSize = (maxValues[j] - minValues[j]);
        	    double val = instance.value(j) / stepSize;
        	    double dist = landmarks.get(i).value(j) - val;
        	    d += Math.pow(Math.abs(dist), norm.getValue());
        	} else {
                    int numValues = sourceHeader.attribute(j).numValues();
                    double dist = landmarks.get(i).value(j) - instance.value(j);
        	    d += Math.pow(Math.abs(dist / numValues), norm.getValue());
        	}
            }
            d = Math.pow(d, 1.0 / norm.getValue());
            inst.setValue(i, d);
        }
        return inst;
    }
    
    /**
     * Correction factor that compensates for fewer landmarks (see
     * paper).
     */ 
    private double updateCorrectionFactor(Instance d) {
        int numAttributes = sourceHeader.numAttributes();
        double factor = 1.0;
        for (int i = numLandmarks.getValue(); i < numAttributes; i++) {
            factor *= decoders.get(i).getExpectedValue(d);
        }	
        this.correctionFactor *= numInstancesForCorrectionFactor;
        this.correctionFactor += factor;
        this.correctionFactor /= numInstancesForCorrectionFactor + 1;
        numInstancesForCorrectionFactor++;
        return factor;
    }

    @Override
    public double getDensityValue(Instance inst) {
        Instance d = computeDistanceVector(inst);
        Timestamp t = new Timestamp(numObservations + 1);
        Observation obs = new Observation(inst, d, t);

        double correctionFactor = updateCorrectionFactor(d);

        int repIndex = 0;
        double minDist = Double.MAX_VALUE;
        boolean matchFound = false;
        for (int i = 0; i < layer.getRepresentatives().size(); i++) {
            Representative r = layer.getRepresentatives().get(i);
            Instance rDist = r.getRepresentativeObservation().getDistance();
	    
            double distance = 0.0;
            for (int j = 0; j < rDist.dataset().numAttributes(); j++) {
        	distance += Math.pow(d.value(j) - rDist.value(j), 2);
            }

	    // TODO: centeralize configuration
            if (distance < minDist && r.getNumberOfObservations() > 200) {
        	repIndex = i;
        	minDist = distance;
                matchFound = true;
            }
        }

	double p = 0.0;
        if (matchFound && layer.getRepresentatives().size() > 0) {
            p = layer.getRepresentatives().get(repIndex).getDensityValue(obs);
        }

        logger.info("Candidates: {}", () -> layer.getCandidates().size());
        logger.info("Representatives: {}", () -> layer.getRepresentatives().size());

        return correctionFactor * p;
    }

    @Override
    public JsonObject getModelCharacteristics() {
        JsonObjectBuilder o = Json.createObjectBuilder();
        return o.build();
    }

    public class Decoder {

        private Attribute att;
        private int attIndex;
        private InstancesHeader header;
        private DensityEstimator estimator;

        public InstancesHeader getHeader() {
            return header;
        }

        public DensityEstimator getEstimator() {
            return estimator;
        }

        public void init(InstancesHeader targetHeader, 
                         Attribute att, int attIndex) {

            // create header
            this.att = att;
            this.attIndex = attIndex;
            String relName = "distances_to_landmarks_plus_attribute";
            List<Attribute> atts = new ArrayList<>();
            for (int i = 0; i < targetHeader.numAttributes(); i++) {
                atts.add(targetHeader.attribute(i));
            }
            atts.add(att);
            this.header = new InstancesHeader(new Instances(relName, atts, 0));
            this.header.setClassIndex(header.numAttributes() - 1);

            // create density estimator for decoding
            if (att.numValues() > 0) {  // nominal attribute
                this.estimator = new HoeffdingTreeCR();
            } else {
                int bins = 10;
                int maxKernels = 1000;
                this.estimator = new OCCDEstimator(bins, maxKernels);
            }

            // intialize density estimator
            List<RandomVariable> vars = Utils.getRandomVariables(header);
            List<RandomVariable> targetVars = new ArrayList<>();
            List<RandomVariable> condVars = new ArrayList<>();
            for (int i = 0; i < vars.size() - 1; i++) {
                condVars.add(vars.get(i));
            }
            targetVars.add(vars.get(vars.size() - 1));

            try {
                estimator.init(header, targetVars, condVars);
            } catch (UnsupportedConfiguration ex) {
                throw new RuntimeException(ex.toString());
            }
        }

        public void update(Instance inst) {

            // compute distance vector
            Instance d = computeDistanceVector(inst);
            double[] dVals = d.toDoubleArray();

            // construct decoder instance
            double weight = 1.0;
            double[] vals = new double[header.numAttributes()];
            System.arraycopy(dVals, 0, vals, 0, dVals.length);
            vals[vals.length - 1] = inst.value(attIndex);
            DenseInstance decoderInst = new DenseInstance(weight, vals);
            decoderInst.setDataset(header);

            estimator.update(decoderInst);
        }

        public double getExpectedValue(Instance d) {
            double expValue = 0.0;            
            if (att.numValues() > 0) {
        	expValue = 1.0 / att.numValues();

            } else {
        	int numBins = 100;
                int i = attIndex;
        	double binSize = (maxValues[i] - minValues[i]) / numBins;
        	for (int j = 0; j < numBins; j++) {
        	    double x = minValues[i] + j * binSize + (binSize / 2);
        	    double weight = 1.0;
        	    double[] dVals = d.toDoubleArray();
        	    double[] vals = new double[header.numAttributes()];
        	    System.arraycopy(dVals, 0, vals, 0, dVals.length);
        	    vals[vals.length - 1] = x;

        	    DenseInstance inst = new DenseInstance(weight, vals);
        	    inst.setDataset(header);
        	    expValue += estimator.getDensityValue(inst) * binSize;
        	}
        	expValue /= numBins;
            }

            return expValue;
        }
    }
}
