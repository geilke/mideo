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
package org.kramerlab.mideo.estimators.trees;

import java.util.List;
import java.util.ArrayList;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.SerializeUtils;

import org.kramerlab.mideo.core.Utils;
import org.kramerlab.mideo.core.Option;
import org.kramerlab.mideo.core.Options;
import org.kramerlab.mideo.core.Configurable;
import org.kramerlab.mideo.core.RandomVariable;
import org.kramerlab.mideo.core.DiscreteRandomVariable;
import org.kramerlab.mideo.data.streams.Stream;
import org.kramerlab.mideo.classifiers.core.DoubleVector;
import org.kramerlab.mideo.classifiers.trees.HoeffdingTree;
import org.kramerlab.mideo.exceptions.UnsupportedConfiguration;
import org.kramerlab.mideo.estimators.EstimatorType;
import org.kramerlab.mideo.estimators.DensityEstimator;

/**
 * HoeffdingTreeCR is a probabilistic condensed representation of a
 * discrete random variable that is conditioned on several random
 * variables. For that purpose, it employs a Hoeffding tree to obtain
 * class probability estimates and provides functionality to perform
 * inference operations.
 *
 * Please notice that the implementation of the inference operations is
 * still missing.
 *
 * @author Michael Geilke
 */ 
public class HoeffdingTreeCR implements DensityEstimator {

    private Logger logger;

    private String leafClassifier = "MC";

    private List<EstimatorType> supportedTypes;
    private List<RandomVariable> targetVariables;    
    private List<RandomVariable> conditionedVariables;
    private HoeffdingTree ht;
    private int targetAttribute;

    public HoeffdingTreeCR() {
        this.logger = LogManager.getLogger();
        this.supportedTypes = new ArrayList<>();
        this.supportedTypes.add(EstimatorType.DISC_X1_I_Y1___Yl);
    }

    /**
     * @param leafClassifier the leaf classifier that is supposed to be
     * used by the Hoeffding tree. Possible choices: [MC | NB| NBA],
     * where MC is MajorityClass, NB is NaiveBayes, and NBAdaptive is
     * NaiveBayesAdaptive
     */
    public HoeffdingTreeCR(String leafClassifier) {
        this();
        this.leafClassifier = leafClassifier;
    }

    /**
     * @return the underlying HoeffdingTree
     */
    public HoeffdingTree getHoeffdingTree() {
        return ht;
    }

    @Override
    public void init(InstancesHeader header, List<RandomVariable> targetVars,
                     List<RandomVariable> condVars) 
        throws UnsupportedConfiguration {

        // check whether the arguments match this type of estimator
        DensityEstimator.super.init(header, targetVars, condVars);

        // set up density estimator
        this.targetVariables = targetVars;
        this.conditionedVariables = condVars;
        String target = targetVariables.get(0).getAttribute().name();
        this.targetAttribute = Utils.determineAttributeIndex(header, target);
        header.setClassIndex(targetAttribute);

        // prepare Hoeffding tree for training
        this.ht = new HoeffdingTree();
        ht.leafpredictionOption.setChosenLabel(leafClassifier); 
        ht.setModelContext(header);
        ht.prepareForUse();
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

    @Override
    public void update(Instance inst) {
        ht.trainOnInstance(inst);
    }

    /**
     * For each value of the target variable, it returns the number of
     * observed instances given the values of the conditioned variables
     * provided by {@code inst}. Let {@literal X} be the target variable
     * and {@literal Y_1, ..., Y_l} be the conditioned
     * variables. Further, let {@literal Y_1=v_1, ..., Y_l=v_l} be the
     * values provided by {@code inst}.
     *
     * @param inst provides the values {@literal v_1, ..., v_l}
     * @return an array where the element at i is the number of
     * instances observed for {@literal f(X=v \mid Y_1=v_1, ...,
     * Y_l=v_l)} and v is the i-th element of {@literal values(X)}
     */
    public double[] getObservationCounts(Instance inst) {
        int classValue = (int) inst.value(targetAttribute);
        double[] dist = ht.getVotesForInstance(inst);
        if (classValue >= dist.length) {
            double laplaceCorrection = DoubleVector.getLaplaceCorrection();
            double[] laplaceDist = new double[classValue + 1];
            System.arraycopy(dist, 0, laplaceDist, 0, dist.length);
            for (int i = dist.length; i < laplaceDist.length; i++) {
                laplaceDist[i] = laplaceCorrection;
            }
            dist = laplaceDist;
        }
        return dist;
    }

    @Override
    public double getDensityValue(Instance inst) {
        int classValue = (int) inst.value(targetAttribute);
        double[] dist = Utils.normalize(getObservationCounts(inst));
        return dist[classValue];
    }

    @Override
    public JsonObject getModelCharacteristics() {
        JsonObjectBuilder o = Json.createObjectBuilder();
        return o.build();
    }
}
