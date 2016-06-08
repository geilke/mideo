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
package org.kramerlab.mideo.estimators.edo;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.Serializable;
import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.SerializeUtils;

import org.kramerlab.mideo.core.Utils;
import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.DiscreteRandomVariable;
import org.kramerlab.mideo.core.ContinuousRandomVariable;
import org.kramerlab.mideo.data.streams.Stream;
import org.kramerlab.mideo.estimators.EstimatorType;
import org.kramerlab.mideo.estimators.DensityEstimator;
// import org.kramerlab.mideo.estimators.occd.OCCDEstimator;
import org.kramerlab.mideo.estimators.trees.HoeffdingTreeCR;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;

/**
 * {@code ChainBasedEstimator} is an implementation of the online
 * density estimators proposed by Geilke et al. It is able to estimate
 * densities of the form {@literal f(X1, ..., Xk | Y1, ..., Yl}} using
 * various variants of classifier chains: estimators consisting of a
 * single classifier chain, an ensemble of classifier chains, or an
 * ensemble of weighted classifier chains. For details, we would like to
 * refer you to the paper:
 *
 * <p>Michael Geilke, Andreas Karwath, Eibe Frank, and Stefan
 * Kramer.<br> Online Estimation of Discrete Densities.<br> In:
 * Proceedings of the ICDM 2013, pages 191-200, IEEE 2013.</p>
 *
 * Please notice that we separated the density estimator and the
 * inference operations. Anything related to inference is provided by
 * the base estimators and {@link
 * org.kramerlab.mideo.estimators.edo.EDO}
 *
 * @author Michael Geilke
 */
public class ChainBasedEstimator implements DensityEstimator {

    private static Logger logger = LogManager.getLogger();

    private final int BUFFER_SIZE = 250;

    private Random random;
    private int ensembleSize;
    private double[] chainWeights;
    private boolean uniformChainWeights = false;
    private List<Instance> buffer;

    private List<RandomVariable> targetVars;
    private List<RandomVariable> conditionedVars;
    private List<List<RandomVariable> > chainOrderings;
    private BaseEstimator[][] baseEstimators;
    private List<List<InstancesHeader> > headers;

    private DensityEstimator templateDiscreteBaseEstimator;
    private DensityEstimator templateContinuousBaseEstimator;

    /**
     * @param ensembleSize number of classifier chains
     * @param uniformChainWeights false if the classifier chains are
     * supposed to weighted according to their performance
     */
    public ChainBasedEstimator(int ensembleSize, boolean uniformChainWeights) {

        // general setup
        this.headers = new ArrayList<>();
        setSeed(1);

        // prepare classifier chain(s)
        this.ensembleSize = ensembleSize;
        this.chainOrderings = new ArrayList<>();
        this.chainWeights = new double[ensembleSize];
        for (int i = 0; i < ensembleSize; i++) {
            chainWeights[i] = 1.0 / ensembleSize;
            chainOrderings.add(new ArrayList<>());
        }
        this.uniformChainWeights = uniformChainWeights;

        // base estimators
        int bins = 10;
        int maxKernels = 10000;
        this.templateDiscreteBaseEstimator = new HoeffdingTreeCR();
        // this.templateContinuousBaseEstimator = new OCCDEstimator(bins, 
        //                                                         maxKernels);
    }

    /**
     * Creates a chain-based estimator where all classifier chains have
     * the same weight.
     * @param ensembleSize number of classifier chains
     */
    public ChainBasedEstimator(int ensembleSize) {
        this(ensembleSize, true);
    }

    /**
     * @param baseSeed seed of the random number generator
     */
    public void setSeed(long baseSeed) {
        this.random = new Random(baseSeed);
    }

    /**
     * Specifies the ordering of the random variables for the classifier
     * chain with index {@code index}. It should only contain variables
     * that will act as target variables. Whether this requirement is
     * met will be check when initializing the density estimator.
     *
     * @param index index of classifier chain
     * @param ordering variable ordering
     */
    public void setChainOrdering(final int index, 
                                 List<RandomVariable> ordering) 
            throws IllegalArgumentException {
        if (index >= ensembleSize) {
            throw new IllegalArgumentException("index out of range");
        }
        chainOrderings.set(index, ordering);
        logger.info("chain: {} ", () -> { return index; });
        logger.info("ordering: {} ", () -> ordering.toString());
    }

    /**
     * @param index of classifier chain
     * @return ordering of the random variables of the classifier chain
     * with index {@code index}.
     */
    public List<RandomVariable> getChainOrdering(final int index) 
            throws IllegalArgumentException {
        if (index >= ensembleSize) {
            throw new IllegalArgumentException("index out of range");
        } else {
            return chainOrderings.get(index);
        }
    }

    /**
     * Specifies the base estimator that is supposed to be used for
     * estimating the conditional densities. It used as a template and
     * will be copied to initialize new base estimators. {@code type}
     * specifies for which type of target variable the density estimator
     * is to be used. Hence, one should provide one estimator for
     * nominal and one estimator for numeric attributes, if the dataset
     * contains mixed types of attributes.
     *
     * @param type specifies for which type of target variable the
     * density estimator is to be used
     * @param est a configured version of the estimator to be used
     */
    public void setBaseEstimator(EstimatorType type, DensityEstimator est) {
        if (type == EstimatorType.DISC_X1_I_Y1___Yl) {
            this.templateDiscreteBaseEstimator = est;
        } else if (type == EstimatorType.CONT_X1_I_Y1___Yl) {
            this.templateContinuousBaseEstimator = est;
        }
    }

    @Override
    public void init(InstancesHeader header, 
                     List<RandomVariable> targetVars, 
                     List<RandomVariable> condVars)
        throws UnsupportedConfiguration {

        // check whether the arguments match this type of estimator
        DensityEstimator.super.init(header, targetVars, condVars);

        int numVars = targetVars.size();
        this.baseEstimators = new BaseEstimator[ensembleSize][numVars];
        this.targetVars = targetVars;
        this.conditionedVars = condVars;
        this.buffer = new ArrayList<>();

        // generate chain orderings as necessary: If a chain has already
        // been defined (e.g., via the setChainOrdering method), we
        // check whether the ordering matches header and targetVars. If
        // it is not compatible or there is no chain, we generate a new
        // one.
        for (int i = 0; i < ensembleSize; i++) {
            List<RandomVariable> current = chainOrderings.get(i);
            boolean allTargetVars = current.size() > 0;
            for (RandomVariable rv : targetVars) {
                allTargetVars &= current.contains(rv);
            }
            if (allTargetVars && Utils.isCompatible(header, current)) {
                continue;
            }
            List<RandomVariable> o = new ArrayList<>();
            for (RandomVariable var : getTargetVariables()) {
                o.add(var);
            }
            Collections.shuffle(o, random);
            setChainOrdering(i, o);
        }
        logger.info("chain orderings generated");

        // prepare base estimators for class probabilities: Let o[i] be
        // the ordering of chain i. The estimator baseEstimator[i][j] is
        // supposed to estimate the variable o[i][j] and is conditioned
        // on the variables o[i][0], ..., o[i][j-1].
        for (int i = 0; i < chainOrderings.size(); i++) {
            List<RandomVariable> ordering = chainOrderings.get(i);
            for (int j = 0; j < ordering.size(); j++) {
                List<RandomVariable> ts = new ArrayList<>();
                List<RandomVariable> cs = new ArrayList<>();
                for (int k = 0; k <= j; k++) {
                    if (k == j) {
                        ts.add(ordering.get(k)); 
                    } else {
                        cs.add(ordering.get(k));
                    }
                }
                // add variables on which the density is conditioned on
                for (RandomVariable rv : conditionedVars) {
                    cs.add(rv);
                }

                baseEstimators[i][j] = new BaseEstimator();
                baseEstimators[i][j].init(header, ts, cs);
            }
        }
        logger.info("base estimators prepared");
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
        buffer.add(inst);
        if (buffer.size() >= BUFFER_SIZE) {
            processInstances();
            buffer.clear();
        }
    }

    /**
     * Updates base estimators with the instances from the buffer and
     * recomputes the weights of the base estimators if necessary.
    */
    private void processInstances() {
        // recompute weight of classifier chains if this is an EWCC
        if (!uniformChainWeights) {
            computeChainWeights();
        }
        
        // prepare update of base estimators by initializing threads: In
        // the best case, every base estimator is running on a separate
        // core.
        List<Thread> tasks = new ArrayList<>();
        for (int i = 0; i < baseEstimators.length; i++) {
            for (int j = 0; j < baseEstimators[i].length; j++) {
                final BaseEstimator est = baseEstimators[i][j];
                Runnable task = () -> { 
                    for (int k = 0; k < buffer.size(); k++) {
                        Instance inst = buffer.get(k);
                        Instance tInst = est.transformInstance(inst);
                        est.getEstimator().update(tInst);
                    }
                };
                tasks.add(new Thread(task));
            }
        }

        // perform update
        for (Thread t : tasks) {
            t.start();
        }
        for (Thread t : tasks) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                logger.error(ex.toString());
            }
        }
    }

    /**
     * Recomputes the chain weights based on the most recent instances.
     */
    private void computeChainWeights() {

        // compute losses
        double[] loss = new double[ensembleSize];
        boolean[] inverted = new boolean[ensembleSize];
        for (int i = 0; i < ensembleSize; i++) {
            loss[i] = 0.0;
            for (Instance inst : buffer) {
                double chainDensity = getDensityValue(inst, i);
                // avoid underflows
                chainDensity = Math.max(chainDensity, Double.MIN_VALUE);
                loss[i] -= Math.log(chainDensity);
                inverted[i] = loss[i] > 0;
            }
        }

        // find maximal loss
        double m = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < ensembleSize; i++) {
            m = Math.max(m, loss[i]);
        }
        
        // update weights
        for (int i = 0; i < ensembleSize; i++) {
            if (inverted[i]) {
                chainWeights[i] += (1 + loss[i] / m);
            } else {     
                chainWeights[i] += (1 - loss[i] / m);
            }            
        }
        chainWeights = Utils.normalize(chainWeights);
    }

    @Override
    public double getDensityValue(Instance inst) {
        // As a heuristic, we select only those classifier chains whose
        // weight does not deviate more than a certain percentage from
        // the heightest weights. In this case, we allow a deviate of
        // 30%. In case of single classifier chains or uniform weights,
        // this would not have any effect.
        double[] chainWeights = new double[this.chainWeights.length];
        double maxWeight = 0.0;
        for (int i = 0; i < chainWeights.length; i++) {
            maxWeight = Math.max(maxWeight, this.chainWeights[i]);
        }
        for (int i = 0; i < chainWeights.length; i++) {
            if (maxWeight - this.chainWeights[i] > 0.3) {
                chainWeights[i] = 0.0;
            } else {
                chainWeights[i] = this.chainWeights[i];
            }
        }
        chainWeights = Utils.normalize(chainWeights);

        double density = 0.0;
        for (int i = 0; i < baseEstimators.length; i++) {
            density += chainWeights[i] * getDensityValue(inst, i);
        }
        return density;
    }

    /**
     * Computes the density value for a single chain. This is useful for
     * the recomputation of their weights.
     */
    private double getDensityValue(Instance inst, int chain) {
        double chainDensity = 1.0;
        for (int j = 0; j < baseEstimators[chain].length; j++) {
            BaseEstimator est = baseEstimators[chain][j];
            Instance tInst = est.transformInstance(inst);
            DensityEstimator be = est.getEstimator();
            chainDensity *= be.getDensityValue(tInst);
        }
        return chainDensity;
    }

    /**
     * {@code BaseEstimator} provides a density estimator for a
     * conditional density with one target variable and related
     * information. We assume that the density to be estimated has the
     * form {@literal f(X | Y1, ..., Yk)}.
     */
    public class BaseEstimator implements Serializable {

        private DensityEstimator estimator;
        private InstancesHeader originalHeader;
        private RandomVariable targetVariable;
        private List<RandomVariable> variables;
        private InstancesHeader header;

        // The attribute indices in SAMOA are apparently not defined
        // properly. Since we had other problems with attribute indices
        // in the past, we avoid them as much as possible.
        private Map<Integer, Integer> attributeIndexMapping;

        /**
         * The random variable that is estimated
         * @return X
         */
        public RandomVariable getTargetVariable() {
            return targetVariable;
        }
        
        /**
         * Provides access to the underlying base estimator, which is a
         * Hoeffding tree.
         * @return the density estimator for {@literal f(X | Y1, ...,
         * Yk)}
         */
        public DensityEstimator getEstimator() {
            return estimator;
        }

        /**
         * Initializes the density estimator as an estimator for
         * {@literal f(X | Y1, ..., Yk)}, where {@literal {X} \cap {Y1,
         * ..., Yk} = {}}. To avoid unnecessary overhead, we require
         * that {@literal {X} \cup {Y1, ..., Yk} = header}. As
         * estimator, we employ the templates provided by
         * ChainBasedEstimator.
         *
         * Please notice that calling {@code init} after the density
         * estimator has been updated causes a reset, i.e., already
         * trained models are discarded.
         *
         * @param origHeader description of the attributes
         * @param targetVars {@literal X1, ..., Xk}
         * @param condVars {@literal Y1, ..., Yl}
         * @throws UnsupportedConfiguration is thrown if {@code
         * targetVars} does not contain exactly one variable or if
         * {@code targetVars} is contained in {@code condVars}
         */
        public void init(InstancesHeader origHeader, 
                         List<RandomVariable> targetVars,
                         List<RandomVariable> condVars) 
                throws UnsupportedConfiguration {

            // check arguments
            if (targetVars.size() != 1) {
                String msg = "Only one target variable is allowed.";
                throw new UnsupportedConfiguration(msg);
            }
            if (condVars.contains(targetVars.get(0))) {
                String msg = "The target variable must not be";
                msg += " a conditioned variable";
                throw new UnsupportedConfiguration(msg);
            }

            this.originalHeader = origHeader;
            this.targetVariable = targetVars.get(0);
            this.variables = new ArrayList<>(condVars);
            this.variables.add(targetVariable);

            // create header
            String relationName = originalHeader.getRelationName();
            List<Attribute> atts = new ArrayList<>();
            for (int i = 0; i < variables.size(); i++) {
                atts.add(variables.get(i).getAttribute());
            }
            Instances ds = new Instances(relationName, atts, 0);
            this.header = new InstancesHeader(ds);
            this.header.setClassIndex(header.numAttributes() - 1);

            // mapping of attribute indices
            this.attributeIndexMapping = new HashMap<>();
            for (int i = 0; i < variables.size(); i++) {
                String attName = variables.get(i).getAttribute().name();
                for (int j = 0; j < originalHeader.numAttributes(); j++) {
                    if (attName.equals(originalHeader.attribute(j).name())) {
                        attributeIndexMapping.put(i, j);
                    }
                }
            }
            
            // create estimator
            DensityEstimator template = null;
            if (targetVariable instanceof DiscreteRandomVariable) {
                template = templateDiscreteBaseEstimator;
            } else if (targetVariable instanceof ContinuousRandomVariable) {
                template = templateContinuousBaseEstimator;
            }
            this.estimator = template.makeCopy();
            estimator.init(header, targetVars, condVars);
        }

        /**
         * Transforms {@code inst} from an instance belonging to the
         * original dataset to an instance matching this base
         * estimator. This is necessary, since an instance that had
         * intially the form {@literal x1, ..., xn} now needs to be in
         * the format {@literal y1, ..., yk}.
         * 
         * @param inst an instance belonging to the original dataset.
         * @return an instance of the form  {@literal y1, ..., yk}
         */
        public Instance transformInstance(Instance inst) {
            int numAtts = header.numAttributes();
            Instance transformedInst = new DenseInstance(numAtts);
            transformedInst.setDataset(header);
            for (int i = 0; i < variables.size(); i++) {
                int attIndex = attributeIndexMapping.get(i);
                double attValue = inst.value(attIndex);
                transformedInst.setValue(i, attValue);
            }
            return transformedInst;
        }
    }

    @Override
    public JsonStructure getModelCharacteristics() {
        BaseEstimator[][] estimators = baseEstimators;
        JsonObjectBuilder o = Json.createObjectBuilder();
        for (int cc = 0; cc < estimators.length; cc++) {
            String chainId = "chain-" + Integer.toString(cc);
            JsonObjectBuilder ccObj = Json.createObjectBuilder();
            for (int cl = 0; cl < estimators[cc].length; cl++) {
                String estimatorId = "estimator-" + Integer.toString(cl);
                JsonObjectBuilder clObj = Json.createObjectBuilder();
                DensityEstimator de = estimators[cc][cl].getEstimator();
                clObj.add("estimator", de.getModelCharacteristics());
                ccObj.add(estimatorId, clObj);
            }
            o.add(chainId, ccObj);
        }
        return o.build();
    }
}
